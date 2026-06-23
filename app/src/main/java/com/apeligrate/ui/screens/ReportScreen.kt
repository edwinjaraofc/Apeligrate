package com.apeligrate.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.local.DeviceLocationProvider
import com.apeligrate.data.local.SessionManager
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.UserProgressRepository
import com.apeligrate.domain.use_case.SubmitIncidentReportUseCase
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.SentinelMapPanel
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.theme.Secondary
import com.apeligrate.ui.theme.Tertiary
import com.apeligrate.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    repository: IncidentReportRepository,
    userProgressRepository: UserProgressRepository,
    onReportSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val userId by sessionManager.userIdFlow.collectAsState(initial = null)
    val locationProvider = remember(context) { DeviceLocationProvider(context) }
    var deviceCoordinates by remember { mutableStateOf<DeviceCoordinates?>(null) }
    var locationStatus by remember { mutableStateOf("Detectando tu ubicación...") }

    val reportViewModel = remember(repository, userProgressRepository) {
        val useCase = SubmitIncidentReportUseCase(repository)
        ReportViewModel(useCase, userProgressRepository)
    }

    val uiState by reportViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                locationStatus = if (coordinates != null) "Ubicación detectada" else "No se pudo obtener la ubicación"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            locationProvider.getCurrentCoordinates { coordinates ->
                deviceCoordinates = coordinates
                locationStatus = if (coordinates != null) "Ubicación detectada" else "No se pudo obtener la ubicación"
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5),
        onResult = { uris -> reportViewModel.addImages(uris) }
    )

    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var confirmData by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Reporte enviado exitosamente", Toast.LENGTH_LONG).show()
            confirmData = false
            reportViewModel.resetState()
            onReportSubmitted()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val categories = listOf(
        ReportCategory("Robo a mano armada", Icons.Default.Security, Color(0xFFFF5252)),
        ReportCategory("Hurto/Arrebato", Icons.Default.Security, Color(0xFFFF7043)),
        ReportCategory("Acoso", Icons.Default.Person, Color(0xFFE91E63)),
        ReportCategory("Zona peligrosa", Icons.Default.LocationOn, Color(0xFFFFC107)),
        ReportCategory("Calle sin iluminacion", Icons.Default.Lightbulb, Color(0xFF90A4AE), isFullWidth = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 32.dp)
    ) {
        Text(text = "Reportar incidente", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text(text = "Tu reporte ayuda a mantener la comunidad segura.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(title = "TIPO DE INCIDENTE")
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryItem(categories[0], uiState.category == categories[0].name, Modifier.weight(1f)) { reportViewModel.onCategoryChange(it) }
                CategoryItem(categories[1], uiState.category == categories[1].name, Modifier.weight(1f)) { reportViewModel.onCategoryChange(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryItem(categories[2], uiState.category == categories[2].name, Modifier.weight(1f)) { reportViewModel.onCategoryChange(it) }
                CategoryItem(categories[3], uiState.category == categories[3].name, Modifier.weight(1f)) { reportViewModel.onCategoryChange(it) }
            }
            CategoryItem(categories[4], uiState.category == categories[4].name, Modifier.fillMaxWidth()) { reportViewModel.onCategoryChange(it) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(title = "UBICACIÓN (Toca el mapa para seleccionar)")
        Spacer(modifier = Modifier.height(12.dp))
        SentinelMapPanel(
            centerCoordinates = uiState.pickedLocation ?: deviceCoordinates,
            title = if (uiState.pickedLocation != null) "Ubicación seleccionada" else "Tu ubicación actual",
            subtitle = "Presiona en el mapa para marcar el lugar exacto.",
            isPickerMode = true,
            onLocationPicked = { lat, lng -> reportViewModel.onLocationPicked(lat, lng) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(title = "EVIDENCIA VISUAL (MAX. 5)")
        Spacer(modifier = Modifier.height(12.dp))
        ImageSelectionSection(
            selectedImages = uiState.selectedImages,
            onAddClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemoveImage = { reportViewModel.removeImage(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(title = "DETALLE DEL INCIDENTE")
        Spacer(modifier = Modifier.height(12.dp))
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            ReportTextField(
                value = uiState.description,
                onValueChange = { reportViewModel.onDescriptionChange(it) },
                placeholder = "Describe qué pasó, cómo eran los involucrados, etc...",
                minHeight = 120.dp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel(title = "FECHA Y HORA")
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniFieldCard(icon = Icons.Default.CalendarMonth, label = "Fecha", value = dateText.ifEmpty { "Hoy" }, modifier = Modifier.weight(1f)) { showDatePicker = true }
            MiniFieldCard(icon = Icons.Default.Schedule, label = "Hora", value = timeText.ifEmpty { "Ahora" }, modifier = Modifier.weight(1f)) { showTimePicker = true }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { confirmData = !confirmData }.padding(vertical = 8.dp)) {
            Checkbox(checked = confirmData, onCheckedChange = { confirmData = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryContainer))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Certifico que la información es verídica.", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TactileButton(
            text = if (uiState.isLoading) "Enviando..." else "Enviar Reporte",
            onClick = {
                if (confirmData) {
                    reportViewModel.submitReport(
                        userId = userId,
                        latitude = deviceCoordinates?.latitude,
                        longitude = deviceCoordinates?.longitude
                    )
                } else {
                    Toast.makeText(context, "Confirma los datos", Toast.LENGTH_SHORT).show()
                }
            },
            icon = if (uiState.isLoading) null else Icons.AutoMirrored.Filled.Send,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !uiState.isLoading && confirmData
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDateMillis = millis
                        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                        dateText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            selectedHour = selectedHour,
            selectedMinute = selectedMinute,
            onConfirm = { h, m ->
                selectedHour = h
                selectedMinute = m
                timeText = String.format("%02d:%02d", h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun ImageSelectionSection(selectedImages: List<Uri>, onAddClick: () -> Unit, onRemoveImage: (Uri) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
        item {
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryContainer)
                    Text("Agregar", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        items(selectedImages) { uri ->
            Box(modifier = Modifier.size(100.dp)) {
                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                IconButton(onClick = { onRemoveImage(uri) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ReportTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, minHeight: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.03f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(14.dp)) {
        if (value.isBlank()) {
            Text(text = placeholder, color = Color.White.copy(alpha = 0.25f), style = MaterialTheme.typography.bodyMedium)
        }
        BasicTextField(value = value, onValueChange = onValueChange, textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MiniFieldCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PrimaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TimePickerDialog(selectedHour: Int, selectedMinute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var hour by remember { mutableStateOf(selectedHour) }
    var minute by remember { mutableStateOf(selectedMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Seleccionar Hora", color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(String.format("%02d:%02d", hour, minute), style = MaterialTheme.typography.displayMedium, color = PrimaryContainer)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { if (hour < 23) hour++ else hour = 0 }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                        IconButton(onClick = { if (hour > 0) hour-- else hour = 23 }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { if (minute < 59) minute++ else minute = 0 }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                        IconButton(onClick = { if (minute > 0) minute-- else minute = 59 }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

data class ReportCategory(val name: String, val icon: ImageVector, val color: Color, val isFullWidth: Boolean = false)

@Composable
fun CategoryItem(category: ReportCategory, isSelected: Boolean, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Surface(
        onClick = { onSelect(category.name) },
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) category.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) category.color else Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (category.isFullWidth) Arrangement.Start else Arrangement.Center) {
            Icon(category.icon, contentDescription = null, tint = if (isSelected) category.color else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = category.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f))
        }
    }
}
