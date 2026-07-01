package com.apeligrate.util

import android.location.Location
import android.util.Log
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.remote.*
import com.apeligrate.domain.model.DangerZone
import kotlin.math.*

data class SafeRouteResult(
    val points: List<DeviceCoordinates>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val touchedZones: Int
)

class SafeRoutePlanner(
    private val routeService: RouteService
) {
    suspend fun planSafeRoute(
        origin: DeviceCoordinates,
        destination: DeviceCoordinates,
        dangerZones: List<DangerZone>,
        apiKey: String
    ): SafeRouteResult? {
        // Mapeo de coordenadas para ORS: [Longitud, Latitud]
        val coordinates = listOf(
            listOf(origin.longitude, origin.latitude),
            listOf(destination.longitude, destination.latitude)
        )

        // Convertir DangerZones a MultiPolygon. Cada zona es un Polígono individual [ [AnilloExterior] ]
        val avoidPolygons = if (dangerZones.isNotEmpty()) {
            ORSAvoidPolygons(
                coordinates = dangerZones.map { zone ->
                    listOf(createCirclePolygon(zone.centerLatitude, zone.centerLongitude, zone.radiusMeters.toDouble()))
                }
            )
        } else null

        // Estructura de petición siguiendo la documentación técnica de ORS V2
        val request = ORSRequest(
            coordinates = coordinates,
            options = if (avoidPolygons != null) ORSOptions(avoidPolygons) else null,
        )

        return try {
            // Aplicamos el plan de respaldo: asegurar el esquema 'Bearer' para el token JWT
            val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

            Log.d("SafeRoutePlanner", "Enviando petición a ORS para ruta entre (${origin.latitude},${origin.longitude}) y (${destination.latitude},${destination.longitude})")

            val response = routeService.getRoute(
                profile = "foot-walking",
                apiKey = authHeader,
                request = request
            )

            val route = response.routes.firstOrNull() ?: return null
            val points = PolylineUtil.decode(route.geometry)

            Log.d("SafeRoutePlanner", "Ruta obtenida con ${points.size} puntos.")

            SafeRouteResult(
                points = points,
                distanceMeters = route.summary.distance,
                durationSeconds = route.summary.duration,
                touchedZones = countTouchedZones(points, dangerZones)
            )
        } catch (e: Exception) {
            Log.e("SafeRoutePlanner", "Error en la petición ORS: ${e.message}")
            null
        }
    }

    private fun createCirclePolygon(lat: Double, lon: Double, radius: Double): List<List<Double>> {
        val points = mutableListOf<List<Double>>()
        val sides = 16 
        val dist = radius / 111320.0 

        for (i in 0 until sides) {
            val angle = Math.toRadians((i * 360.0 / sides))
            val pLat = lat + dist * cos(angle)
            val pLon = lon + dist * sin(angle) / cos(Math.toRadians(lat))
            points.add(listOf(pLon, pLat))
        }
        points.add(points[0]) // Cerrar anillo
        return points
    }

    private fun countTouchedZones(points: List<DeviceCoordinates>, zones: List<DangerZone>): Int {
        return zones.count { zone ->
            points.any { point ->
                val res = FloatArray(1)
                Location.distanceBetween(point.latitude, point.longitude, zone.centerLatitude, zone.centerLongitude, res)
                res[0] <= zone.radiusMeters
            }
        }
    }
}
