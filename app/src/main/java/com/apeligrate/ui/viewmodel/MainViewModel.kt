package com.apeligrate.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.remote.RouteService
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.DangerZone
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.toAlert
import com.apeligrate.domain.use_case.GetLatestAlertsUseCase
import com.apeligrate.util.NotificationHelper
import com.apeligrate.util.DangerZoneAggregator
import com.apeligrate.util.GeofenceManager
import com.apeligrate.util.PolylineUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val proximityAlert: Alert? = null,
    val focusedLocation: DeviceCoordinates? = null,
    val userLocation: DeviceCoordinates? = null,
    val destination: DeviceCoordinates? = null,
    val currentRoutePoints: List<DeviceCoordinates> = emptyList(),
    val dangerOnRoute: Boolean = false,
    val dangerZones: List<DangerZone> = emptyList()
)

class MainViewModel(
    private var notificationHelper: NotificationHelper? = null,
    private val getLatestAlertsUseCase: GetLatestAlertsUseCase? = null,
    private var geofenceManager: GeofenceManager? = null,
    private var routeService: RouteService? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val notifiedZoneIds = mutableSetOf<String>()

    init {
        Log.d("MainViewModel", "🎬 INIT: ViewModel creado")
        loadAlerts()
    }

    fun setGeofenceManager(manager: GeofenceManager) {
        geofenceManager = manager
        loadAlerts()
    }

    fun setNotificationHelper(helper: NotificationHelper) {
        this.notificationHelper = helper
    }

    fun setRouteService(service: RouteService) {
        Log.d("MainViewModel", "📡 RouteService configurado")
        this.routeService = service
        
        // Recalcular ruta si ya existe un destino y ubicación de usuario
        val origin = _uiState.value.userLocation
        val dest = _uiState.value.destination
        if (origin != null && dest != null) {
            calculateRoute(origin, dest)
        }
    }

    fun updateAlertsFromReports(incidentReports: List<IncidentReport>) {
        val alerts = incidentReports.map { it.toAlert() }
        val zones = geofenceManager?.buildDangerZones(alerts) ?: emptyList()
        _uiState.update { it.copy(alerts = alerts, dangerZones = zones) }

        if (geofenceManager != null) {
            geofenceManager?.addGeofences(zones)
        }
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(alerts = emptyList(), dangerZones = emptyList(), isLoading = false) }
        }
    }

    fun onLocationUpdated(latitude: Double, longitude: Double) {
        val userLoc = DeviceCoordinates(latitude, longitude)
        _uiState.update { it.copy(userLocation = userLoc) }
        checkProximity(latitude, longitude)
    }

    private fun checkProximity(latitude: Double, longitude: Double) {
        val currentState = _uiState.value
        val zones = currentState.dangerZones.ifEmpty {
            geofenceManager?.buildDangerZones(currentState.alerts) ?: emptyList()
        }
        val activeZone = geofenceManager?.findContainingZone(latitude, longitude, zones)
            ?: DangerZoneAggregator.findContainingZone(latitude, longitude, zones)

        if (activeZone != null) {
            val zoneMessage = if (activeZone.grouped) {
                "Se activó una zona agrupada con ${activeZone.reportCount} incidentes cercanos."
            } else {
                "Ya estás dentro del radio de una zona de peligro."
            }
            val alertToShow = activeZone.primaryReport.copy(
                title = if (activeZone.grouped) "ZONA AGRUPADA ACTIVA" else activeZone.primaryReport.title,
                description = zoneMessage
            )

            _uiState.update { it.copy(proximityAlert = alertToShow, dangerZones = zones) }

            if (notifiedZoneIds.add(activeZone.id)) {
                notificationHelper?.showProximityAlert(
                    if (activeZone.grouped) "¡ZONA AGRUPADA!" else "¡ALERTA DE SEGURIDAD!",
                    zoneMessage
                )
            }
        } else {
            if (notifiedZoneIds.isNotEmpty()) {
                notifiedZoneIds.clear()
            }
            _uiState.update { it.copy(proximityAlert = null, dangerZones = zones) }
        }
    }

    fun setDestination(latitude: Double, longitude: Double) {
        val origin = _uiState.value.userLocation
        val dest = DeviceCoordinates(latitude, longitude)
        _uiState.update { it.copy(destination = dest, focusedLocation = dest) }
        
        Log.d("MainViewModel", "📍 Destino fijado: $latitude, $longitude")
        
        if (origin != null) {
            calculateRoute(origin, dest)
        } else {
            // Si no hay origen, aún no podemos trazar por calles reales
            calculateStraightRoute(dest, dest)
        }
    }

    fun calculateRoute(destination: DeviceCoordinates) {
        val origin = _uiState.value.userLocation ?: return
        calculateRoute(origin, destination)
    }

    fun calculateRoute(origin: DeviceCoordinates, destination: DeviceCoordinates) {
        if (routeService != null) {
            Log.d("MainViewModel", "🛣️ Calculando ruta real (OSRM)...")
            viewModelScope.launch {
                try {
                    val coordinates = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
                    val response = routeService?.getRoute(coordinates)
                    val encodedPolyline = response?.routes?.firstOrNull()?.geometry
                    
                    if (encodedPolyline != null) {
                        Log.d("MainViewModel", "✅ Ruta obtenida con éxito")
                        val points = PolylineUtil.decode(encodedPolyline)
                        _uiState.update { it.copy(currentRoutePoints = points) }
                        checkDangerOnRoute(points)
                    } else {
                        Log.w("MainViewModel", "⚠️ OSRM no devolvió geometría, usando línea recta")
                        calculateStraightRoute(origin, destination)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "❌ Error calculando ruta por calles: ${e.message}")
                    calculateStraightRoute(origin, destination)
                }
            }
        } else {
            Log.w("MainViewModel", "⚠️ RouteService nulo, usando línea recta")
            calculateStraightRoute(origin, destination)
        }
    }

    private fun calculateStraightRoute(start: DeviceCoordinates, dest: DeviceCoordinates) {
        val points = if (start == dest) {
            listOf(start)
        } else {
            listOf(
                DeviceCoordinates(start.latitude, start.longitude),
                DeviceCoordinates((start.latitude + dest.latitude) / 2, (start.longitude + dest.longitude) / 2),
                DeviceCoordinates(dest.latitude, dest.longitude)
            )
        }
        _uiState.update { it.copy(currentRoutePoints = points) }
        checkDangerOnRoute(points)
    }

    private fun checkDangerOnRoute(route: List<DeviceCoordinates>) {
        val zones = _uiState.value.dangerZones.ifEmpty {
            geofenceManager?.buildDangerZones(_uiState.value.alerts) ?: emptyList()
        }
        var dangerFound = false

        for (point in route) {
            for (zone in zones) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    point.latitude,
                    point.longitude,
                    zone.centerLatitude,
                    zone.centerLongitude,
                    results
                )
                if (results[0] <= zone.radiusMeters) {
                        dangerFound = true
                        break
                }
            }
            if (dangerFound) break
        }
        
        _uiState.update { it.copy(dangerOnRoute = dangerFound) }
        if (dangerFound) {
            notificationHelper?.showProximityAlert("Ruta Peligrosa", "Incidente detectado en tu camino exacto.")
        }
    }

    fun clearRoute() {
        _uiState.update { it.copy(destination = null, currentRoutePoints = emptyList(), dangerOnRoute = false, focusedLocation = null) }
    }

    fun focusLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(focusedLocation = DeviceCoordinates(latitude, longitude)) }
    }

    fun clearFocus() {
        _uiState.update { it.copy(focusedLocation = null) }
    }

    fun triggerTestNotification() {
        val testAlert = Alert(
            id = "test_${System.currentTimeMillis()}",
            title = "SIMULACIÓN DE ROBO",
            description = "Se ha simulado un reporte de robo en tu zona actual.",
            severity = Severity.CRITICAL,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(proximityAlert = testAlert) }
        notificationHelper?.showProximityAlert("¡ALERTA CRÍTICA!", "Zona de peligro detectada.")
    }

    fun dismissProximityAlert() {
        _uiState.update { it.copy(proximityAlert = null) }
    }
}
