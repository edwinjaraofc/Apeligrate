package com.apeligrate.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.remote.RouteService
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.toAlert
import com.apeligrate.domain.use_case.GetLatestAlertsUseCase
import com.apeligrate.util.NotificationHelper
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
    val dangerOnRoute: Boolean = false
)

class MainViewModel(
    private var notificationHelper: NotificationHelper? = null,
    private val getLatestAlertsUseCase: GetLatestAlertsUseCase? = null,
    private var geofenceManager: GeofenceManager? = null,
    private var routeService: RouteService? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val notifiedAlertIds = mutableSetOf<String>()

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
        _uiState.update { it.copy(alerts = alerts) }

        if (geofenceManager != null) {
            geofenceManager?.addGeofences(alerts)
        }
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val mockAlerts = listOf(
                Alert(
                    id = "1",
                    title = "SISTEMA ACTIVO",
                    description = "Vigilancia comunitaria en curso.",
                    severity = Severity.SAFE,
                    timestamp = System.currentTimeMillis(),
                ),
                Alert(
                    id = "2",
                    title = "ROBO REPORTADO",
                    description = "Asalto con arma de fuego reportado cerca de tu posición.",
                    severity = Severity.CRITICAL,
                    timestamp = System.currentTimeMillis(),
                    latitude = -12.0673,
                    longitude = -77.0336
                ),
            )
            _uiState.update { it.copy(alerts = mockAlerts, isLoading = false) }

            if (geofenceManager != null) {
                geofenceManager?.addGeofences(mockAlerts)
            }
        }
    }

    fun onLocationUpdated(latitude: Double, longitude: Double) {
        val userLoc = DeviceCoordinates(latitude, longitude)
        _uiState.update { it.copy(userLocation = userLoc) }
        checkProximity(latitude, longitude)
    }

    private fun checkProximity(latitude: Double, longitude: Double) {
        val currentAlerts = _uiState.value.alerts
        val zones = geofenceManager?.getReportZones(currentAlerts) ?: emptyList()

        var alertToShow: Alert? = null
        var zoneMessage = ""

        for (zone in zones) {
            val results = FloatArray(1)
            Location.distanceBetween(latitude, longitude, zone.latitude, zone.longitude, results)

            if (results[0] <= 15) {
                val firstCritical = zone.reports.firstOrNull { it.severity.name == "CRITICAL" }
                alertToShow = firstCritical ?: zone.reports.first()
                zoneMessage = "Alerta crítica en tu ubicación inmediata."

                if (!notifiedAlertIds.contains(zone.id)) {
                    notificationHelper?.showProximityAlert("¡ZONA PELIGROSA!", zoneMessage)
                    notifiedAlertIds.add(zone.id)
                }
                break
            }
        }

        if (alertToShow == null) {
            val currentLocation = DeviceCoordinates(latitude, longitude)
            alertToShow = geofenceManager?.checkIfInsideZone(currentLocation, currentAlerts)

            if (alertToShow != null) {
                _uiState.update { it.copy(proximityAlert = alertToShow) }
                if (!notifiedAlertIds.contains(alertToShow.id)) {
                    notificationHelper?.showProximityAlert("¡ALERTA DE SEGURIDAD!", alertToShow.description)
                    notifiedAlertIds.add(alertToShow.id)
                }
            } else {
                _uiState.update { it.copy(proximityAlert = null) }
            }
        } else {
            _uiState.update { it.copy(proximityAlert = alertToShow) }
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
        val alerts = _uiState.value.alerts
        var dangerFound = false
        
        for (point in route) {
            for (alert in alerts) {
                if (alert.latitude != null && alert.longitude != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(point.latitude, point.longitude, alert.latitude, alert.longitude, results)
                    if (results[0] < 8 && alert.severity == Severity.CRITICAL) {
                        dangerFound = true
                        break
                    }
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
