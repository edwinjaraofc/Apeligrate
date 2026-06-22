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
    private val notificationHelper: NotificationHelper? = null,
    private val getLatestAlertsUseCase: GetLatestAlertsUseCase? = null,
    private var geofenceManager: GeofenceManager? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val notifiedAlertIds = mutableSetOf<String>()
    private var lastUserLocation: DeviceCoordinates? = null

    init {
        Log.d("MainViewModel", "🎬 INIT: ViewModel creado")
        Log.d("MainViewModel", "📍 geofenceManager: $geofenceManager")
        Log.d("MainViewModel", "📍 notificationHelper: $notificationHelper")
        loadAlerts()
    }

    fun setGeofenceManager(manager: GeofenceManager) {
        geofenceManager = manager
        // Recargar alertas para que se agreguen los geofences
        loadAlerts()
    }

    fun updateAlertsFromReports(incidentReports: List<IncidentReport>) {
        Log.d("MainViewModel", "🔄 Actualizando alertas desde reportes: ${incidentReports.size} reportes")

        // Convertir reportes a alertas
        val alerts = incidentReports.map { it.toAlert() }
        Log.d("MainViewModel", "📍 Alertas convertidas: ${alerts.size}")

        // Actualizar estado
        _uiState.update { it.copy(alerts = alerts) }

        // Actualizar geofences
        if (geofenceManager != null) {
            Log.d("MainViewModel", "🗺️ Actualizando geofences...")
            geofenceManager?.addGeofences(alerts)
        } else {
            Log.w("MainViewModel", "⚠️ GeofenceManager es nulo")
        }
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Log.d("MainViewModel", "📂 Cargando alertas...")
            Log.d("MainViewModel", "📂 alerts actuales: ${_uiState.value.alerts.size}")

            // Mocking data for now
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
                    latitude = -12.0673, // Near Lima
                    longitude = -77.0336
                ),
            )
            Log.d("MainViewModel", "✅ Alertas creadas: ${mockAlerts.size}")
            _uiState.update { it.copy(alerts = mockAlerts, isLoading = false) }

            // Agregar geofences cuando carguen las alertas
            if (geofenceManager != null) {
                Log.d("MainViewModel", "🗺️ Agregando geofences... (manager: $geofenceManager)")
                geofenceManager?.addGeofences(mockAlerts)
            } else {
                Log.w("MainViewModel", "⚠️ GeofenceManager es NULO en loadAlerts()")
            }
        }
    }

    fun onLocationUpdated(latitude: Double, longitude: Double) {
        lastUserLocation = DeviceCoordinates(latitude, longitude)
        Log.d("MainViewModel", "📍 onLocationUpdated: $latitude, $longitude")
        checkProximity(latitude, longitude)
    }

    private fun checkProximity(latitude: Double, longitude: Double) {
        val currentAlerts = _uiState.value.alerts
        Log.d("MainViewModel", "🔍 checkProximity - alertas: ${currentAlerts.size}, geofenceManager: ${geofenceManager != null}")

        val currentLocation = DeviceCoordinates(latitude, longitude)

        // Obtener zonas agrupadas
        val zones = geofenceManager?.getReportZones(currentAlerts) ?: emptyList()
        Log.d("MainViewModel", "📊 Zonas detectadas: ${zones.size}")

        // Primero chequear zonas (3+ reportes)
        var alertToShow: Alert? = null
        var zoneMessage = ""

        for (zone in zones) {
            val results = FloatArray(1)
            Location.distanceBetween(
                latitude, longitude,
                zone.latitude, zone.longitude,
                results
            )

            if (results[0] <= 500) {
                Log.d("MainViewModel", "🚨 Usuario EN ZONA: ${zone.id} (${zone.reportCount} reportes, ${results[0].toInt()}m)")

                // Crear alerta consolidada para la zona
                val firstCritical = zone.reports.firstOrNull { it.severity.name == "CRITICAL" }
                alertToShow = firstCritical ?: zone.reports.first()
                zoneMessage = "Zona peligrosa detectada: ${zone.reportCount} incidentes en área cercana"

                if (!notifiedAlertIds.contains(zone.id)) {
                    Log.d("MainViewModel", "📢 Enviando notificación por ZONA")
                    notificationHelper?.showProximityAlert(
                        title = "¡ZONA PELIGROSA!",
                        message = zoneMessage
                    )
                    notifiedAlertIds.add(zone.id)
                }
                break
            }
        }

        // Si no hay zona, chequear reportes individuales
        if (alertToShow == null) {
            val currentLocation2 = DeviceCoordinates(latitude, longitude)
            alertToShow = geofenceManager?.checkIfInsideZone(currentLocation2, currentAlerts)

            if (alertToShow != null) {
                Log.d("MainViewModel", "🚨 Usuario DENTRO de incidente individual: ${alertToShow.id}")
                _uiState.update { it.copy(proximityAlert = alertToShow) }

                if (!notifiedAlertIds.contains(alertToShow.id)) {
                    Log.d("MainViewModel", "📢 Enviando notificación por INCIDENTE")
                    notificationHelper?.showProximityAlert(
                        title = "¡ALERTA DE SEGURIDAD!",
                        message = alertToShow.description
                    )
                    notifiedAlertIds.add(alertToShow.id)
                }
            } else {
                Log.d("MainViewModel", "✅ Usuario en zona segura")
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
        
        // Trazamos una ruta simulada (línea con un punto intermedio de desvío)
        val points = listOf(
            DeviceCoordinates(start.latitude, start.longitude),
            DeviceCoordinates((start.latitude + dest.latitude) / 2 + 0.002, (start.longitude + dest.longitude) / 2 + 0.002),
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
                    if (results[0] < 400 && alert.severity == Severity.CRITICAL) {
                        dangerFound = true
                        break
                    }
                }
            }
            if (dangerFound) break
        }
        
        _uiState.update { it.copy(dangerOnRoute = dangerFound) }
        if (dangerFound) {
            notificationHelper?.showProximityAlert("Ruta Peligrosa", "Se detectaron zonas de riesgo en tu camino.")
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
