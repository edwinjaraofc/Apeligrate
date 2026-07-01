package com.apeligrate.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.os.Looper

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double
)

fun interface LocationUpdatesHandle {
    fun stop()
}

private const val MAX_STALE_LOCATION_MS = 15_000L
private const val MAX_ACCEPTABLE_ACCURACY_METERS = 35f
private const val LOCATION_UPDATE_INTERVAL_MS = 2_000L
private const val LOCATION_MIN_UPDATE_INTERVAL_MS = 1_000L

class DeviceLocationProvider(
    context: Context
) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun getCurrentCoordinates(
        onResult: (DeviceCoordinates?) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        val provider = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).firstOrNull { candidate ->
            runCatching { locationManager.isProviderEnabled(candidate) }.getOrDefault(false)
        }
            ?: locationManager.getProviders(true).firstOrNull()

        if (provider == null) {
            onResult(bestLastKnownLocation()?.toCoordinates())
            return
        }

        val cachedLocation = bestLastKnownLocation()
        if (cachedLocation != null) {
            onResult(cachedLocation.toCoordinates())
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onResult(location?.toCoordinates() ?: cachedLocation?.toCoordinates())
            }
            .addOnFailureListener {
                onResult(cachedLocation?.toCoordinates())
            }
    }

    fun startLocationUpdates(
        onLocationChanged: (DeviceCoordinates) -> Unit
    ): LocationUpdatesHandle? {
        if (!hasLocationPermission()) return null

        var lastDeliveredElapsedNanos = 0L
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    if (!location.isFreshEnough()) return@forEach
                    if (location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) return@forEach
                    if (location.elapsedRealtimeNanos <= lastDeliveredElapsedNanos) return@forEach

                    lastDeliveredElapsedNanos = location.elapsedRealtimeNanos
                    onLocationChanged(location.toCoordinates())
                }
            }
        }

        bestLastKnownLocation()
            ?.takeIf { it.isFreshEnough() && it.accuracy <= MAX_ACCEPTABLE_ACCURACY_METERS }
            ?.let { cachedLocation ->
                lastDeliveredElapsedNanos = cachedLocation.elapsedRealtimeNanos
                onLocationChanged(cachedLocation.toCoordinates())
            }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_MIN_UPDATE_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        return LocationUpdatesHandle {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun bestLastKnownLocation(): Location? {
        val providers = locationManager.getProviders(true)
        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }
}

private fun Location.toCoordinates(): DeviceCoordinates {
    return DeviceCoordinates(
        latitude = latitude,
        longitude = longitude
    )
}

private fun Location.isFreshEnough(): Boolean {
    return System.currentTimeMillis() - time <= MAX_STALE_LOCATION_MS
}
