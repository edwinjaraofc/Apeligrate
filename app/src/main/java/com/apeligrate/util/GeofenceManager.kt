package com.apeligrate.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.DangerZone

class GeofenceManager(private val context: Context) {
    companion object {
        const val TAG = "GeofenceManager"
        const val DANGER_RADIUS_METERS = DangerZoneAggregator.DEFAULT_ZONE_RADIUS_METERS
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun buildDangerZones(alerts: List<Alert>): List<DangerZone> {
        return DangerZoneAggregator.buildDangerZones(alerts)
    }

    fun addGeofences(zones: List<DangerZone>) {
        Log.d(TAG, "📍 Agregando geofences para ${zones.size} zonas")

        val geofences = zones.mapNotNull { zone ->
            if (zone.radiusMeters > 0f) {
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(
                        zone.centerLatitude,
                        zone.centerLongitude,
                        zone.radiusMeters
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_DWELL
                    )
                    .setLoiteringDelay(2000)
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

    fun findContainingZone(
        latitude: Double,
        longitude: Double,
        zones: List<DangerZone>,
        bufferMeters: Float = 0f
    ): DangerZone? {
        return DangerZoneAggregator.findContainingZone(latitude, longitude, zones, bufferMeters)
    }
}
