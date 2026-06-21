package com.apeligrate.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun MainScreen(
    incidentRepository: IncidentReportRepository,
    onNavigateToReport: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val incidentReports by incidentRepository.getReports().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val locationProvider = remember(context) { DeviceLocationProvider(context) }
    var deviceCoordinates by remember { mutableStateOf<DeviceCoordinates?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                coordinates?.let { viewModel.onLocationUpdated(it.latitude, it.longitude) }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                coordinates?.let { viewModel.onLocationUpdated(it.latitude, it.longitude) }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SENTINEL SYSTEM",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = "ESTADO: VIGILANCIA ACTIVA",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeGreen,
                    )
                }
                
                // Test Notification Button (Now Visible)
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

            Spacer(modifier = Modifier.height(20.dp))

            SentinelMapPanel(
                centerCoordinates = uiState.focusedLocation ?: deviceCoordinates,
                reportMarkers = incidentReports,
                title = if (uiState.focusedLocation != null) "Ubicación del incidente" else "Mapa de vigilancia",
                subtitle = if (uiState.focusedLocation != null) "Mostrando zona del reporte seleccionado." 
                          else if (incidentReports.isEmpty()) "Aun no hay reportes comunitarios visibles."
                          else "${incidentReports.size} reportes visibles para la comunidad."
            )
            
            if (uiState.focusedLocation != null) {
                TextButton(
                    onClick = { viewModel.clearFocus() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Volver a mi ubicación", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Alertas recientes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.alerts) { alert ->
                    AlertItem(alert)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

        // In-App Proximity Alert
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
                    text = "¡ALERTA CERCANA!",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun AlertItem(alert: Alert) {
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
                )
                Text(
                    text = "ID: ${alert.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
