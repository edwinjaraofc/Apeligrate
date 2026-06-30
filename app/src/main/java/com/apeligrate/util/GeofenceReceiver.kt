package com.apeligrate.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "GeofenceReceiver"
        private val notifiedZones = mutableSetOf<String>()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "🔔 onReceive llamado - intent: $intent")

        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context o Intent es nulo")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "❌ GeofencingEvent es nulo")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ Error en GeofencingEvent: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        Log.d(TAG, "📍 Transición: $geofenceTransition, Geofences: ${triggeringGeofences?.size ?: 0}")

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "🚨 ENTRASTE a zona peligrosa: ${triggeringGeofences?.map { it.requestId }}")
                triggeringGeofences?.forEach { geofence ->
                    val zoneId = geofence.requestId
                    if (notifiedZones.add(zoneId)) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.showProximityAlert(
                            "¡ALERTA CRÍTICA!",
                            "Acabas de entrar a una zona de peligro"
                        )
                    }
                }
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "⏱️ PERMANENCIA en zona: ${triggeringGeofences?.map { it.requestId }}")
                // No notificar nuevamente si ya se notificó en ENTER
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "✅ SALISTE de zona peligrosa: ${triggeringGeofences?.map { it.requestId }}")
                triggeringGeofences?.forEach { geofence ->
                    val zoneId = geofence.requestId
                    notifiedZones.remove(zoneId)
                }
            }
            else -> {
                Log.w(TAG, "⚠️ Transición desconocida: $geofenceTransition")
            }
        }
    }
}

