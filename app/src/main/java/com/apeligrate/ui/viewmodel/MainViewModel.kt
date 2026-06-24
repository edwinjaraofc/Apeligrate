package com.apeligrate.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.toAlert
import com.apeligrate.domain.use_case.GetLatestAlertsUseCase
import com.apeligrate.util.NotificationHelper
import com.apeligrate.util.GeofenceManager
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
    val destination: DeviceCoordinates? = null,
    val routePoints: List<DeviceCoordinates> = emptyList(),
    val dangerOnRoute: Boolean = false
)

class MainViewModel(
    private var notificationHelper: NotificationHelper? = null,
    private val getLatestAlertsUseCase: GetLatestAlertsUseCase? = null,
    private var geofenceManager: GeofenceManager? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val notifiedAlertIds = mutableSetOf<String>()
    private var lastUserLocation: DeviceCoordinates? = null

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
        lastUserLocation = DeviceCoordinates(latitude, longitude)
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

            // Radio de zona ultra-reducido a 15m
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
        val dest = DeviceCoordinates(latitude, longitude)
        _uiState.update { it.copy(destination = dest, focusedLocation = dest) }
        calculateRoute(dest)
    }

    private fun calculateRoute(dest: DeviceCoordinates) {
        val start = lastUserLocation ?: return
        val points = listOf(
            DeviceCoordinates(start.latitude, start.longitude),
            DeviceCoordinates((start.latitude + dest.latitude) / 2 + 0.0005, (start.longitude + dest.longitude) / 2 + 0.0005),
            DeviceCoordinates(dest.latitude, dest.longitude)
        )
        _uiState.update { it.copy(routePoints = points) }
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
                    // Radio reducido a 8 metros para máxima precisión
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
        _uiState.update { it.copy(destination = null, routePoints = emptyList(), dangerOnRoute = false, focusedLocation = null) }
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
