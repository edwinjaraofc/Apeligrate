package com.apeligrate.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var locationMessage by remember { mutableStateOf("Solicitando ubicacion del dispositivo...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationMessage = "Ubicacion detectada. Centrado del mapa actualizado."
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                if (coordinates == null) {
                    locationMessage = "No se pudo obtener la ubicacion actual. Se muestra un mapa general."
                }
            }
        } else {
            locationMessage = "Sin permiso de ubicacion. Se muestra un mapa general."
        }
    }

    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                locationMessage = if (coordinates != null) {
                    "Ubicacion detectada. Centrado del mapa actualizado."
                } else {
                    "No se pudo obtener la ubicacion actual. Se muestra un mapa general."
                }
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        SentinelMapPanel(
            centerCoordinates = deviceCoordinates,
            reportMarkers = incidentReports,
            title = "Mapa principal",
            subtitle = if (incidentReports.isEmpty()) {
                "$locationMessage Aun no hay reportes comunitarios visibles."
            } else {
                "$locationMessage ${incidentReports.size} reportes visibles para la comunidad."
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

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
