package com.apeligrate.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double
)

class DeviceLocationProvider(
    context: Context
) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                provider,
                null,
                ContextCompat.getMainExecutor(appContext)
            ) { location: Location? ->
                onResult(location?.toCoordinates() ?: cachedLocation?.toCoordinates())
            }
        } else {
            onResult(cachedLocation?.toCoordinates())
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
