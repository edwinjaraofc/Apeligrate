package com.apeligrate.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.domain.model.Alert

class GeofenceManager(private val context: Context) {
    companion object {
        const val TAG = "GeofenceManager"
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun addGeofences(alerts: List<Alert>) {
        Log.d(TAG, "📍 Intentando agregar geofences para ${alerts.size} alertas")

        val geofences = alerts.mapNotNull { alert ->
            if (alert.latitude != null && alert.longitude != null && alert.severity.name == "CRITICAL") {
                Log.d(TAG, "✅ Agregando geofence para: ${alert.id} en (${alert.latitude}, ${alert.longitude})")
                Geofence.Builder()
                    .setRequestId(alert.id)
                    .setCircularRegion(
                        alert.latitude,
                        alert.longitude,
                        500f // radio en metros
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_DWELL
                    )
                    .setLoiteringDelay(5000) // 5 segundos dentro = notificación
                    .build()
            } else {
                Log.d(TAG, "⏭️ Saltando alerta (no CRÍTICA o sin coordenadas): ${alert.id}")
                null
            }
        }

        if (geofences.isEmpty()) {
            Log.w(TAG, "⚠️ No se agregaron geofences (lista vacía)")
            return
        }

        // Remover geofences antiguos primero
        try {
            geofencingClient.removeGeofences(geofences.map { it.requestId })
            Log.d(TAG, "🗑️ Geofences antiguos removidos para actualizar")
        } catch (e: Exception) {
            Log.d(TAG, "ℹ️ No había geofences antiguos")
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofences)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            Log.d(TAG, "✅ ${geofences.size} geofences agregados exitosamente")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de permisos al agregar geofences: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al agregar geofences: ${e.message}", e)
        }
    }

    // Checar manualmente si el usuario está dentro de alguna zona peligrosa
    fun checkIfInsideZone(userLocation: DeviceCoordinates, alerts: List<Alert>): Alert? {
        for (alert in alerts) {
            if (alert.latitude != null && alert.longitude != null && alert.severity.name == "CRITICAL") {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    alert.latitude, alert.longitude,
                    results
                )

                if (results[0] <= 500) {
                    Log.d(TAG, "🚨 Usuario está dentro de zona: ${alert.id} (${results[0].toInt()}m)")
                    return alert
                }
            }
        }
        return null
    }
    // Agrupar reportes cercanos en "zonas" para alertas consolidadas
    fun getReportZones(alerts: List<Alert>): List<ReportZone> {
        val zones = mutableListOf<ReportZone>()
        val processed = mutableSetOf<String>()
        
        for (alert in alerts) {
            if (alert.latitude == null || alert.longitude == null) continue
            if (processed.contains(alert.id)) continue
            
            // Encontrar todos los reportes dentro de 150m
            val nearbyAlerts = alerts.filter { other ->
                if (other.latitude == null || other.longitude == null) return@filter false
                val results = FloatArray(1)
                Location.distanceBetween(
                    alert.latitude, alert.longitude,
                    other.latitude, other.longitude,
                    results
                )
                results[0] <= 150f
            }
            
            // Si hay 3+ reportes, crear una zona
            if (nearbyAlerts.size >= 3) {
                Log.d(TAG, "🗺️ Zona detectada con ${nearbyAlerts.size} reportes")
                
                // Centro de la zona = promedio de coordenadas
                val centerLat = nearbyAlerts.mapNotNull { it.latitude }.average()
                val centerLng = nearbyAlerts.mapNotNull { it.longitude }.average()
                val zoneId = "${(centerLat * 1000).toInt()}_${(centerLng * 1000).toInt()}"
                
                zones.add(
                    ReportZone(
                        id = zoneId,
                        latitude = centerLat,
                        longitude = centerLng,
                        reportCount = nearbyAlerts.size,
                        reports = nearbyAlerts,
                        severity = if (nearbyAlerts.any { it.severity.name == "CRITICAL" }) "CRITICAL" else "WARNING"
                    )
                )
                
                nearbyAlerts.forEach { processed.add(it.id) }
            }
        }
        
        return zones
    }
}

data class ReportZone(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val reportCount: Int,
    val reports: List<Alert>,
    val severity: String
)



