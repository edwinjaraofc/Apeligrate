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
import com.apeligrate.util.SafeRoutePlanner
import com.apeligrate.util.NetworkConfig
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
    private var safeRoutePlanner: SafeRoutePlanner? = null

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
        this.safeRoutePlanner = SafeRoutePlanner(service)
        
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
        val oldLoc = _uiState.value.userLocation
        _uiState.update { it.copy(userLocation = userLoc) }
        checkProximity(latitude, longitude)
        
        // Recalcular ruta automáticamente si hay un destino fijado y el usuario se movió significativamente (> 10m)
        val dest = _uiState.value.destination
        if (dest != null) {
            val shouldRecalculate = if (oldLoc == null) {
                true
            } else {
                val results = FloatArray(1)
                Location.distanceBetween(oldLoc.latitude, oldLoc.longitude, userLoc.latitude, userLoc.longitude, results)
                results[0] > 10
            }
            
            if (shouldRecalculate) {
                calculateRoute(userLoc, dest)
            }
        }
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
        val planner = safeRoutePlanner
        if (planner != null) {
            Log.d("MainViewModel", "🛣️ Iniciando cálculo de ruta por calles...")
            viewModelScope.launch {
                try {
                    val zones = currentDangerZones()
                    val ignoredZoneIds = zoneIdsContainingPoint(origin, zones)
                    val zonesForAvoidance = zones.filterNot { it.id in ignoredZoneIds }
                    val attempts = buildAvoidanceAttempts(zonesForAvoidance)

                    var result: com.apeligrate.util.SafeRouteResult? = null
                    for ((index, attemptZones) in attempts.withIndex()) {
                        if (index > 0) {
                            Log.w("MainViewModel", "⚠️ Ajustando evasión de zonas (nivel $index) para encontrar ruta viable...")
                        }
                        result = planner.planSafeRoute(origin, destination, attemptZones, NetworkConfig.ORS_API_KEY)
                        if (result != null) break
                    }

                    if (result != null) {
                        val routeRiskZones = zones.filterNot { it.id in ignoredZoneIds }
                        val touchedZones = countTouchedZonesOnRoute(result.points, routeRiskZones)

                        Log.d("MainViewModel", "✅ Ruta por calles obtenida con ${result.points.size} puntos")
                        _uiState.update {
                            it.copy(
                                currentRoutePoints = result.points,
                                dangerOnRoute = touchedZones > 0
                            )
                        }
                        if (touchedZones > 0) {
                            notificationHelper?.showProximityAlert(
                                "Ruta con riesgo",
                                "Se eligió la mejor ruta disponible, pero cruza algunas zonas de reporte."
                            )
                        }
                    } else {
                        Log.e("MainViewModel", "❌ El servicio de mapas no devolvió ninguna ruta, usando línea recta como último recurso")
                        calculateStraightRoute(origin, destination)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "❌ Error en el proceso de navegación: ${e.message}")
                    calculateStraightRoute(origin, destination)
                }
            }
        } else {
            Log.w("MainViewModel", "⚠️ SafeRoutePlanner no inicializado, usando línea recta")
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
        val zones = currentDangerZones()
        val startPoint = route.firstOrNull()
        val ignoredZoneIds = if (startPoint != null) {
            zoneIdsContainingPoint(startPoint, zones)
        } else {
            emptySet()
        }
        val relevantZones = zones.filterNot { it.id in ignoredZoneIds }
        val dangerFound = countTouchedZonesOnRoute(route, relevantZones) > 0
        
        _uiState.update { it.copy(dangerOnRoute = dangerFound) }
        if (dangerFound) {
            notificationHelper?.showProximityAlert("Ruta Peligrosa", "Incidente detectado en tu camino exacto.")
        }
    }

    private fun currentDangerZones(): List<DangerZone> {
        return _uiState.value.dangerZones.ifEmpty {
            geofenceManager?.buildDangerZones(_uiState.value.alerts) ?: emptyList()
        }
    }

    private fun buildAvoidanceAttempts(zones: List<DangerZone>): List<List<DangerZone>> {
        val attempts = mutableListOf<List<DangerZone>>()

        fun addAttempt(candidate: List<DangerZone>) {
            val ids = candidate.map { it.id }.sorted()
            if (attempts.none { it.map { zone -> zone.id }.sorted() == ids }) {
                attempts.add(candidate)
            }
        }

        val sortedAll = sortZonesByPriority(zones)
        val groupedAndCritical = sortZonesByPriority(
            zones.filter { it.grouped || zoneMaxSeverity(it) == Severity.CRITICAL }
        )
        val groupedOnly = sortZonesByPriority(zones.filter { it.grouped })

        addAttempt(sortedAll)
        addAttempt(groupedAndCritical)
        addAttempt(groupedOnly)
        addAttempt(emptyList())

        return attempts
    }

    private fun zoneIdsContainingPoint(
        point: DeviceCoordinates,
        zones: List<DangerZone>
    ): Set<String> {
        return zones
            .filter { zone ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    point.latitude,
                    point.longitude,
                    zone.centerLatitude,
                    zone.centerLongitude,
                    results
                )
                results[0] <= zone.radiusMeters
            }
            .map { it.id }
            .toSet()
    }

    private fun sortZonesByPriority(zones: List<DangerZone>): List<DangerZone> {
        return zones.sortedWith(
            compareByDescending<DangerZone> { it.grouped }
                .thenByDescending { severityWeight(zoneMaxSeverity(it)) }
                .thenByDescending { it.reportCount }
                .thenByDescending { it.radiusMeters }
        )
    }

    private fun zoneMaxSeverity(zone: DangerZone): Severity {
        return zone.reports.maxByOrNull { severityWeight(it.severity) }?.severity ?: Severity.WARNING
    }

    private fun severityWeight(severity: Severity): Int {
        return when (severity) {
            Severity.CRITICAL -> 3
            Severity.WARNING -> 2
            Severity.SAFE -> 1
        }
    }

    private fun countTouchedZonesOnRoute(route: List<DeviceCoordinates>, zones: List<DangerZone>): Int {
        return zones.count { zone ->
            route.any { point ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    point.latitude,
                    point.longitude,
                    zone.centerLatitude,
                    zone.centerLongitude,
                    results
                )
                results[0] <= zone.radiusMeters
            }
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
