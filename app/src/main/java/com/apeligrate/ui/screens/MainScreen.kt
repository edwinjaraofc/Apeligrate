package com.apeligrate.ui.screens

import android.Manifest
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.local.DeviceLocationProvider
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.ui.components.SentinelButton
import com.apeligrate.ui.components.SentinelCard
import com.apeligrate.ui.components.SentinelMapPanel
import com.apeligrate.ui.theme.SafeGreen
import com.apeligrate.ui.theme.WarningAmber
import com.apeligrate.ui.viewmodel.MainViewModel
import com.apeligrate.util.GeofenceManager
import com.apeligrate.util.NotificationHelper

data class LimaPlace(val name: String, val lat: Double, val lng: Double)

val recommendedPlaces = listOf(
    LimaPlace("Larcomar", -12.1322, -77.0300),
    LimaPlace("Plaza de Armas", -12.0453, -77.0310),
    LimaPlace("Jockey Plaza", -12.0864, -76.9761),
    LimaPlace("Parque Reserva", -12.0714, -77.0344),
    LimaPlace("Costa Verde", -12.1265, -77.0422)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    incidentRepository: IncidentReportRepository,
    onNavigateToReport: () -> Unit
) {
    val context = LocalContext.current
    val notificationHelper = remember(context) { NotificationHelper(context) }
    val geofenceManager = remember(context) { GeofenceManager(context) }

    LaunchedEffect(geofenceManager, notificationHelper) {
        viewModel.setGeofenceManager(geofenceManager)
        viewModel.setNotificationHelper(notificationHelper)
    }

    val uiState by viewModel.uiState.collectAsState()
    val incidentReports by incidentRepository.getReports().collectAsState(initial = emptyList())
    val locationProvider = remember(context) { DeviceLocationProvider(context) }
    var deviceCoordinates by remember { mutableStateOf<DeviceCoordinates?>(null) }
    var destinationText by remember { mutableStateOf("") }
    var locationUpdatesEnabled by remember { mutableStateOf(locationProvider.hasLocationPermission()) }
    val sortedAlerts = remember(uiState.alerts, uiState.userLocation) {
        sortAlertsByDistance(uiState.alerts, uiState.userLocation)
    }

    LaunchedEffect(incidentReports) {
        if (incidentReports.isNotEmpty()) {
            viewModel.updateAlertsFromReports(incidentReports)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationUpdatesEnabled = true
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                coordinates?.let { viewModel.onLocationUpdated(it.latitude, it.longitude) }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainScreen", "Permiso de notificaciones CONCEDIDO")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (locationProvider.hasLocationPermission()) {
            locationUpdatesEnabled = true
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    LaunchedEffect(locationUpdatesEnabled) {
        if (locationUpdatesEnabled) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                coordinates?.let { viewModel.onLocationUpdated(it.latitude, it.longitude) }
            }

            while (true) {
                delay(5000)
                locationProvider.getCurrentCoordinates { coordinates ->
                    if (coordinates != null) {
                        deviceCoordinates = coordinates
                        viewModel.onLocationUpdated(coordinates.latitude, coordinates.longitude)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SENTINEL SYSTEM",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ESTADO: VIGILANCIA ACTIVA",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeGreen,
                        )
                    }

                    Surface(
                        onClick = { viewModel.triggerTestNotification() },
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test Alerta",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = destinationText,
                    onValueChange = { destinationText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("A donde vas?", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (destinationText.isNotEmpty() || uiState.destination != null) {
                            IconButton(onClick = {
                                destinationText = ""
                                viewModel.clearRoute()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (uiState.destination == null) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recommendedPlaces) { place ->
                            SuggestionChip(
                                onClick = {
                                    destinationText = place.name
                                    viewModel.setDestination(place.lat, place.lng)
                                },
                                label = { Text(place.name, color = Color.White, maxLines = 1) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            )
                        }
                    }
                }
            }

            if (destinationText.isNotEmpty() && uiState.destination == null) {
                item {
                    Button(
                        onClick = {
                            deviceCoordinates?.let {
                                viewModel.setDestination(it.latitude + 0.005, it.longitude + 0.005)
                            } ?: viewModel.setDestination(-12.0673, -77.0336)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trazar Ruta Segura", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            if (uiState.dangerOnRoute) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF5252)),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Ruta en riesgo! Se detectaron areas peligrosas en el camino.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                SentinelMapPanel(
                    centerCoordinates = uiState.focusedLocation ?: deviceCoordinates,
                    reportMarkers = incidentReports,
                    dangerZones = uiState.dangerZones,
                    routePoints = uiState.currentRoutePoints,
                    destination = uiState.destination,
                    title = if (uiState.destination != null) "Navegacion Activa" else "Mapa de vigilancia",
                    subtitle = if (uiState.dangerOnRoute) "Evita las zonas marcadas en rojo." else "Ruta despejada hasta el destino."
                )
            }

            if (uiState.focusedLocation != null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { viewModel.clearFocus() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text("Volver a mi ubicacion", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Alertas recientes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(sortedAlerts, key = { alert -> alert.id }) { alert ->
                AlertItem(alert, uiState.userLocation)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SentinelButton(
                        text = "EMERGENCIA",
                        onClick = onNavigateToReport,
                        modifier = Modifier.weight(1f),
                    )
                    SentinelButton(
                        text = "REPORTAR",
                        onClick = onNavigateToReport,
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.proximityAlert != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.proximityAlert?.let { alert ->
                ProximityAlertCard(
                    alert = alert,
                    onDismiss = { viewModel.dismissProximityAlert() }
                )
            }
        }
    }
}

@Composable
fun ProximityAlertCard(alert: Alert, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ALERTA CERCANA!",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun AlertItem(alert: Alert, userLocation: DeviceCoordinates?) {
    val distanceText = remember(alert, userLocation) {
        formatDistance(alert.latitude, alert.longitude, userLocation)
    }
    val timeText = remember(alert.timestamp) {
        formatTimeAgo(alert.timestamp)
    }

    SentinelCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    color = when (alert.severity) {
                        Severity.CRITICAL -> MaterialTheme.colorScheme.primary
                        Severity.WARNING -> WarningAmber
                        Severity.SAFE -> SafeGreen
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

private fun formatDistance(
    alertLatitude: Double?,
    alertLongitude: Double?,
    userLocation: DeviceCoordinates?
): String {
    if (alertLatitude == null || alertLongitude == null || userLocation == null) {
        return "Distancia: no disponible"
    }

    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude,
        userLocation.longitude,
        alertLatitude,
        alertLongitude,
        results
    )

    val meters = results[0].toInt()
    return if (meters < 1000) {
        "A $meters m de ti"
    } else {
        "A ${"%.1f".format(meters / 1000f)} km de ti"
    }
}

private fun sortAlertsByDistance(
    alerts: List<Alert>,
    userLocation: DeviceCoordinates?
): List<Alert> {
    if (userLocation == null) return alerts

    return alerts.mapIndexed { index, alert ->
        val distance = distanceToAlert(alert, userLocation)
        Triple(alert, distance, index)
    }.sortedWith(
        compareBy<Triple<Alert, Float, Int>> { it.second }
            .thenBy { it.third }
    ).map { it.first }
}

private fun distanceToAlert(alert: Alert, userLocation: DeviceCoordinates): Float {
    if (alert.latitude == null || alert.longitude == null) return Float.MAX_VALUE

    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude,
        userLocation.longitude,
        alert.latitude,
        alert.longitude,
        results
    )
    return results[0]
}

private fun formatTimeAgo(time: Long): String {
    val normalizedTime = if (time in 1..9999999999L) time * 1000L else time
    val diff = System.currentTimeMillis() - normalizedTime
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    return when {
        diff < 60000 -> "hace un momento"
        diff < 3600000 -> "hace $minutes min"
        diff < 86400000 -> "hace $hours h"
        days < 30 -> "hace $days d"
        days < 60 -> "hace 1 mes"
        else -> "hace ${days / 30} meses"
    }
}
