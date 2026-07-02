package com.apeligrate.util

import android.location.Location
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.DangerZone
import com.apeligrate.domain.model.Severity
import kotlin.math.max

object DangerZoneAggregator {
    const val DEFAULT_ZONE_RADIUS_METERS = 100f
    const val MIN_GROUP_SIZE = 3
    private const val GROUP_LINK_DISTANCE_METERS = 200f
    private const val GROUP_PADDING_METERS = 40f
    private const val MAX_GROUP_RADIUS_METERS = 260f

    fun buildDangerZones(alerts: List<Alert>): List<DangerZone> {
        // Ahora incluimos todos los reportes con coordenadas para que tengan área visual
        val validReports = alerts
            .filter { it.latitude != null && it.longitude != null }

        if (validReports.isEmpty()) return emptyList()

        val clusters = clusterReports(validReports)
        val zones = mutableListOf<DangerZone>()

        clusters.forEach { cluster ->
            if (cluster.size >= MIN_GROUP_SIZE) {
                val center = calculateCenter(cluster)
                val radius = calculateRadius(center.first, center.second, cluster)
                zones.add(
                    DangerZone(
                        id = "group_${stableZoneKey(cluster)}",
                        centerLatitude = center.first,
                        centerLongitude = center.second,
                        radiusMeters = radius,
                        reports = cluster,
                        grouped = true
                    )
                )
            } else {
                cluster.forEach { report ->
                    val coords = report.coordinatesOrNull() ?: return@forEach
                    zones.add(
                        DangerZone(
                            id = "zone_${report.id}",
                            centerLatitude = coords.first,
                            centerLongitude = coords.second,
                            radiusMeters = DEFAULT_ZONE_RADIUS_METERS,
                            reports = listOf(report),
                            grouped = false
                        )
                    )
                }
            }
        }

        return zones.sortedWith(
            compareByDescending<DangerZone> { it.grouped }
                .thenByDescending { it.reportCount }
        )
    }

    fun findContainingZone(
        latitude: Double,
        longitude: Double,
        zones: List<DangerZone>,
        bufferMeters: Float = 0f
    ): DangerZone? {
        return zones
            .filter { zone ->
                distanceBetween(latitude, longitude, zone.centerLatitude, zone.centerLongitude) <= zone.radiusMeters + bufferMeters
            }
            .minWithOrNull(
                compareByDescending<DangerZone> { it.grouped }
                    .thenBy { it.radiusMeters }
            )
    }

    private fun clusterReports(reports: List<Alert>): List<List<Alert>> {
        val remaining = reports.toMutableList()
        val clusters = mutableListOf<List<Alert>>()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val cluster = mutableListOf(seed)
            val queue = ArrayDeque<Alert>()
            queue.add(seed)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val iterator = remaining.iterator()

                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    if (distanceBetween(current, candidate) <= GROUP_LINK_DISTANCE_METERS) {
                        cluster.add(candidate)
                        queue.add(candidate)
                        iterator.remove()
                    }
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    private fun calculateCenter(cluster: List<Alert>): Pair<Double, Double> {
        val centerLat = cluster.mapNotNull { it.latitude }.average()
        val centerLng = cluster.mapNotNull { it.longitude }.average()
        return centerLat to centerLng
    }

    private fun calculateRadius(
        centerLatitude: Double,
        centerLongitude: Double,
        cluster: List<Alert>
    ): Float {
        val furthestDistance = cluster.maxOf { report ->
            val coords = report.coordinatesOrNull() ?: return@maxOf 0f
            distanceBetween(centerLatitude, centerLongitude, coords.first, coords.second)
        }
        return max(DEFAULT_ZONE_RADIUS_METERS, minOf(furthestDistance + GROUP_PADDING_METERS, MAX_GROUP_RADIUS_METERS))
    }

    private fun stableZoneKey(cluster: List<Alert>): String {
        return cluster
            .map { it.id }
            .sorted()
            .joinToString(separator = "_")
            .hashCode()
            .toUInt()
            .toString(16)
    }

    private fun distanceBetween(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results)
        return results[0]
    }

    private fun distanceBetween(first: Alert, second: Alert): Float {
        return distanceBetween(
            first.latitude ?: return Float.MAX_VALUE,
            first.longitude ?: return Float.MAX_VALUE,
            second.latitude ?: return Float.MAX_VALUE,
            second.longitude ?: return Float.MAX_VALUE
        )
    }

    private fun Alert.coordinatesOrNull(): Pair<Double, Double>? {
        val latitude = latitude ?: return null
        val longitude = longitude ?: return null
        return latitude to longitude
    }
}
