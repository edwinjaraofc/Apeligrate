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
        // Radio de detección MÍNIMO para evitar falsos positivos (20 metros)
        const val DANGER_RADIUS_METERS = 20f
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun addGeofences(alerts: List<Alert>) {
        Log.d(TAG, "📍 Agregando geofences de alta precisión para ${alerts.size} alertas")

        val geofences = alerts.mapNotNull { alert ->
            if (alert.latitude != null && alert.longitude != null && alert.severity.name == "CRITICAL") {
                Geofence.Builder()
                    .setRequestId(alert.id)
                    .setCircularRegion(
                        alert.latitude,
                        alert.longitude,
                        DANGER_RADIUS_METERS
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_DWELL
                    )
                    .setLoiteringDelay(2000) // 2 segundos dentro
                    .build()
            } else null
        }

        if (geofences.isEmpty()) return

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar geofences", e)
        }
    }

    fun checkIfInsideZone(userLocation: DeviceCoordinates, alerts: List<Alert>): Alert? {
        for (alert in alerts) {
            if (alert.latitude != null && alert.longitude != null && alert.severity.name == "CRITICAL") {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    alert.latitude, alert.longitude,
                    results
                )
                if (results[0] <= DANGER_RADIUS_METERS) return alert
            }
        }
        return null
    }

    fun getReportZones(alerts: List<Alert>): List<ReportZone> {
        val zones = mutableListOf<ReportZone>()
        val processed = mutableSetOf<String>()
        for (alert in alerts) {
            if (alert.latitude == null || alert.longitude == null || processed.contains(alert.id)) continue
            val nearbyAlerts = alerts.filter { other ->
                if (other.latitude == null || other.longitude == null) return@filter false
                val results = FloatArray(1)
                Location.distanceBetween(alert.latitude, alert.longitude, other.latitude, other.longitude, results)
                results[0] <= DANGER_RADIUS_METERS * 2
            }
            if (nearbyAlerts.size >= 3) {
                val centerLat = nearbyAlerts.mapNotNull { it.latitude }.average()
                val centerLng = nearbyAlerts.mapNotNull { it.longitude }.average()
                zones.add(ReportZone("${centerLat}_${centerLng}", centerLat, centerLng, nearbyAlerts.size, nearbyAlerts, "CRITICAL"))
                nearbyAlerts.forEach { processed.add(it.id) }
            }
        }
        return zones
    }
}

data class ReportZone(val id: String, val latitude: Double, val longitude: Double, val reportCount: Int, val reports: List<Alert>, val severity: String)
