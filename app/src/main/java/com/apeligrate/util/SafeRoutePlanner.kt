package com.apeligrate.util

import android.location.Location
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.remote.RouteService
import com.apeligrate.domain.model.DangerZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class SafeRouteResult(
    val points: List<DeviceCoordinates>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val riskScore: Double,
    val touchedZones: Int,
    val usedWaypoints: List<DeviceCoordinates> = emptyList(),
    val escapedFromZone: Boolean = false
)

class SafeRoutePlanner(
    private val routeService: RouteService
) {
    suspend fun planSafeRoute(
        origin: DeviceCoordinates,
        destination: DeviceCoordinates,
        dangerZones: List<DangerZone>
    ): SafeRouteResult? {
        val candidates = buildWaypointCandidates(origin, destination, dangerZones)
            .distinctBy { it.coordinatesKey() }

        var bestSafeResult: SafeRouteResult? = null
        var bestRiskyResult: SafeRouteResult? = null

        for (candidate in candidates) {
            val response = runCatching {
                routeService.getRoute(
                    candidate.toCoordinateString(),
                    alternatives = false
                )
            }.getOrNull() ?: continue

            response.routes.forEach { route ->
                val decoded = PolylineUtil.decode(route.geometry)
                if (decoded.isEmpty()) return@forEach

                val evaluated = evaluateRoute(
                    points = decoded,
                    distanceMeters = route.distance,
                    durationSeconds = route.duration,
                    dangerZones = dangerZones,
                    usedWaypoints = candidate.waypoints,
                    escapedFromZone = candidate.escapedFromZone
                )

                if (evaluated.touchedZones == 0) {
                    if (bestSafeResult == null || evaluated.isBetterThan(bestSafeResult!!)) {
                        bestSafeResult = evaluated
                    }
                } else {
                    if (bestRiskyResult == null || evaluated.isBetterThan(bestRiskyResult!!)) {
                        bestRiskyResult = evaluated
                    }
                }
            }
        }

        return bestSafeResult ?: bestRiskyResult
    }

    private fun buildWaypointCandidates(
        origin: DeviceCoordinates,
        destination: DeviceCoordinates,
        dangerZones: List<DangerZone>
    ): List<RouteCandidate> {
        val direct = RouteCandidate(
            coordinates = listOf(origin, destination),
            waypoints = emptyList(),
            escapedFromZone = false
        )

        val beamWidth = 4
        val maxDepth = 4
        val arrivalThresholdMeters = 110.0
        val visitedStart = setOf(origin.roundKey())

        val initialNodes = mutableListOf<SearchNode>()
        initialNodes += SearchNode(
            current = origin,
            waypoints = emptyList(),
            score = 0.0,
            depth = 0,
            visited = visitedStart,
            escapedFromZone = isInsideAnyZone(origin, dangerZones)
        )

        val completed = mutableListOf<RouteCandidate>()
        completed += direct
        var frontier = initialNodes

        repeat(maxDepth) {
            if (frontier.isEmpty()) return@repeat

            val nextNodes = mutableListOf<SearchNode>()

            frontier.forEach { node ->
                val distanceToGoal = distanceBetween(node.current, destination)
                if (distanceToGoal <= arrivalThresholdMeters) {
                    completed += node.toCandidate(origin, destination)
                    return@forEach
                }

                val moves = buildMoves(node.current, destination, dangerZones)
                moves.forEach { move ->
                    if (move.point.roundKey() in node.visited) return@forEach

                    val pointRisk = riskAtPoint(move.point, dangerZones)
                    val newVisited = node.visited + move.point.roundKey()
                    val newWaypoints = node.waypoints + move.point
                    val newScore = node.score +
                        pointRisk +
                        move.turnPenalty +
                        (distanceBetween(move.point, destination) / 120.0)

                    nextNodes += SearchNode(
                        current = move.point,
                        waypoints = newWaypoints,
                        score = newScore,
                        depth = node.depth + 1,
                        visited = newVisited,
                        escapedFromZone = node.escapedFromZone || pointRisk == 0.0
                    )
                }
            }

            if (nextNodes.isEmpty()) return@repeat

            val safeNodes = nextNodes.filter { riskAtPoint(it.current, dangerZones) == 0.0 }
            val pool = if (safeNodes.isNotEmpty()) safeNodes else nextNodes
            frontier = pool.sortedBy { it.score }.take(beamWidth).toMutableList()
        }

        completed += frontier.map { it.toCandidate(origin, destination) }

        return if (completed.isNotEmpty()) {
            completed.distinctBy { it.coordinatesKey() }
                .sortedBy { it.waypoints.size }
        } else {
            listOf(direct)
        }
    }

    private fun buildMoves(
        current: DeviceCoordinates,
        destination: DeviceCoordinates,
        dangerZones: List<DangerZone>
    ): List<MoveOption> {
        val referenceLat = (current.latitude + destination.latitude) / 2.0
        val currentMeters = toMeters(current, referenceLat)
        val destMeters = toMeters(destination, referenceLat)

        val dx = destMeters.first - currentMeters.first
        val dy = destMeters.second - currentMeters.second
        val length = max(1.0, hypot(dx, dy))
        val ux = dx / length
        val uy = dy / length
        val px = -uy
        val py = ux

        val insideDanger = isInsideAnyZone(current, dangerZones)
        val step = min(280.0, max(90.0, length * 0.24))
        val lateral = min(170.0, max(70.0, step * 0.55))
        val forward = min(step, length)

        val straight = fromMeters(
            currentMeters.first + ux * forward,
            currentMeters.second + uy * forward,
            referenceLat
        )

        val left = fromMeters(
            currentMeters.first + ux * forward + px * lateral,
            currentMeters.second + uy * forward + py * lateral,
            referenceLat
        )

        val right = fromMeters(
            currentMeters.first + ux * forward - px * lateral,
            currentMeters.second + uy * forward - py * lateral,
            referenceLat
        )

        val options = mutableListOf(
            MoveOption(straight, turnPenalty = 0.0),
            MoveOption(left, turnPenalty = 35.0),
            MoveOption(right, turnPenalty = 35.0)
        )

        if (insideDanger) {
            val escapeTarget = escapePoint(current, destination, dangerZones)
            options.add(0, MoveOption(escapeTarget, turnPenalty = 0.0))
        }

        return options.distinctBy { it.point.roundKey() }
    }

    private fun evaluateRoute(
        points: List<DeviceCoordinates>,
        distanceMeters: Double,
        durationSeconds: Double,
        dangerZones: List<DangerZone>,
        usedWaypoints: List<DeviceCoordinates>,
        escapedFromZone: Boolean
    ): SafeRouteResult {
        var touchedZones = 0
        var riskScore = max(1.0, distanceMeters / 1000.0)

        dangerZones.sortedByDescending { zoneRiskWeight(it) }.forEach { zone ->
            var zoneTouched = false
            var zonePenalty = 0.0

            points.forEach { point ->
                val distanceToZone = distanceToZoneMeters(point, zone)
                when {
                    distanceToZone <= zone.radiusMeters -> {
                        zonePenalty += 2500.0 * zoneRiskWeight(zone)
                        zoneTouched = true
                    }
                    distanceToZone <= zone.radiusMeters + 50.0 -> {
                        zonePenalty += 400.0 * zoneRiskWeight(zone)
                    }
                    distanceToZone <= zone.radiusMeters + 120.0 -> {
                        zonePenalty += 80.0 * zoneRiskWeight(zone)
                    }
                }
            }

            if (zoneTouched) {
                touchedZones += 1
            }

            riskScore += zonePenalty
        }

        if (escapedFromZone) {
            riskScore *= 0.92
        }

        riskScore += usedWaypoints.size * 15.0

        return SafeRouteResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            riskScore = riskScore,
            touchedZones = touchedZones,
            usedWaypoints = usedWaypoints,
            escapedFromZone = escapedFromZone
        )
    }

    private fun riskAtPoint(point: DeviceCoordinates, dangerZones: List<DangerZone>): Double {
        var risk = 0.0

        dangerZones.forEach { zone ->
            val distanceToZone = distanceToZoneMeters(point, zone)
            when {
                distanceToZone <= zone.radiusMeters -> risk += 1000.0 * zoneRiskWeight(zone)
                distanceToZone <= zone.radiusMeters + 50.0 -> risk += 120.0 * zoneRiskWeight(zone)
                distanceToZone <= zone.radiusMeters + 120.0 -> risk += 30.0 * zoneRiskWeight(zone)
            }
        }

        return risk
    }

    private fun isInsideAnyZone(point: DeviceCoordinates, dangerZones: List<DangerZone>): Boolean {
        return dangerZones.any { zone -> distanceToZoneMeters(point, zone) <= zone.radiusMeters }
    }

    private fun escapePoint(
        current: DeviceCoordinates,
        destination: DeviceCoordinates,
        dangerZones: List<DangerZone>
    ): DeviceCoordinates {
        val zone = dangerZones.maxByOrNull { zoneRiskWeight(it) } ?: return destination
        val referenceLat = zone.centerLatitude
        val currentMeters = toMeters(current, referenceLat)
        val destMeters = toMeters(destination, referenceLat)
        val zoneMeters = toMeters(
            DeviceCoordinates(zone.centerLatitude, zone.centerLongitude),
            referenceLat
        )

        val awayX = currentMeters.first - zoneMeters.first
        val awayY = currentMeters.second - zoneMeters.second
        val awayLength = hypot(awayX, awayY)

        val direction = if (awayLength > 1.0) {
            awayX / awayLength to awayY / awayLength
        } else {
            val dx = destMeters.first - currentMeters.first
            val dy = destMeters.second - currentMeters.second
            val len = max(1.0, hypot(dx, dy))
            (-dy / len) to (dx / len)
        }

        val offset = zone.radiusMeters + max(150.0, zone.radiusMeters * 0.55)
        return fromMeters(
            currentMeters.first + direction.first * offset,
            currentMeters.second + direction.second * offset,
            referenceLat
        )
    }

    private fun zoneRiskWeight(zone: DangerZone): Double {
        val groupedWeight = if (zone.grouped) 3.0 else 1.5
        val sizeWeight = min(2.0, zone.reportCount / 2.0)
        val radiusWeight = min(2.0, zone.radiusMeters / 180.0)
        return groupedWeight + sizeWeight + radiusWeight
    }

    private fun distanceToZoneMeters(point: DeviceCoordinates, zone: DangerZone): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point.latitude,
            point.longitude,
            zone.centerLatitude,
            zone.centerLongitude,
            results
        )
        return results[0]
    }

    private fun distanceBetween(first: DeviceCoordinates, second: DeviceCoordinates): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            first.latitude,
            first.longitude,
            second.latitude,
            second.longitude,
            results
        )
        return results[0].toDouble()
    }

    private fun toMeters(point: DeviceCoordinates, referenceLat: Double): Pair<Double, Double> {
        val metersPerLat = 111_320.0
        val metersPerLon = 111_320.0 * cos(Math.toRadians(referenceLat)).coerceAtLeast(0.1)
        return (point.longitude * metersPerLon) to (point.latitude * metersPerLat)
    }

    private fun fromMeters(x: Double, y: Double, referenceLat: Double): DeviceCoordinates {
        val metersPerLat = 111_320.0
        val metersPerLon = 111_320.0 * cos(Math.toRadians(referenceLat)).coerceAtLeast(0.1)
        return DeviceCoordinates(
            latitude = y / metersPerLat,
            longitude = x / metersPerLon
        )
    }

    private fun RouteCandidate.toCoordinateString(): String {
        return coordinates.joinToString(separator = ";") { "${it.longitude},${it.latitude}" }
    }

    private fun RouteCandidate.coordinatesKey(): String {
        return coordinates.joinToString(separator = "|") {
            "${"%.5f".format(it.latitude)},${"%.5f".format(it.longitude)}"
        }
    }

    private fun SafeRouteResult.isBetterThan(other: SafeRouteResult): Boolean {
        return when {
            riskScore != other.riskScore -> riskScore < other.riskScore
            touchedZones != other.touchedZones -> touchedZones < other.touchedZones
            distanceMeters != other.distanceMeters -> distanceMeters < other.distanceMeters
            else -> usedWaypoints.size < other.usedWaypoints.size
        }
    }

    private fun SearchNode.toCandidate(
        origin: DeviceCoordinates,
        destination: DeviceCoordinates
    ): RouteCandidate {
        val waypoints = this@toCandidate.waypoints.distinctBy { it.roundKey() }

        val coordinates = buildList {
            add(origin)
            addAll(waypoints)
            add(destination)
        }.distinctBy { it.roundKey() }

        return RouteCandidate(
            coordinates = coordinates,
            waypoints = waypoints,
            escapedFromZone = escapedFromZone
        )
    }

    private fun DeviceCoordinates.roundKey(): String {
        return "${"%.5f".format(latitude)},${"%.5f".format(longitude)}"
    }

    private data class MoveOption(
        val point: DeviceCoordinates,
        val turnPenalty: Double
    )

    private data class SearchNode(
        val current: DeviceCoordinates,
        val waypoints: List<DeviceCoordinates>,
        val score: Double,
        val depth: Int,
        val visited: Set<String>,
        val escapedFromZone: Boolean
    )

    private data class RouteCandidate(
        val coordinates: List<DeviceCoordinates>,
        val waypoints: List<DeviceCoordinates>,
        val escapedFromZone: Boolean
    )
}
