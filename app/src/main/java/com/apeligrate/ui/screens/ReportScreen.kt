package com.apeligrate.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.theme.Secondary
import com.apeligrate.ui.theme.Tertiary
import com.apeligrate.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportScreen() {
    val context = LocalContext.current
    
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var confirmData by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    val scrollState = rememberScrollState()

    // Inicializar ViewModel con repositorio
    val reportViewModel = remember {
        val repository = com.apeligrate.data.repository.IncidentReportRepositoryImpl()
        val useCase = com.apeligrate.domain.use_case.SubmitIncidentReportUseCase(repository)
        ReportViewModel(useCase)
    }
    val uiState by reportViewModel.uiState.collectAsState()
    val isLoading = uiState.isLoading

    // Observar cambios en el estado
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "¡Reporte enviado exitosamente!", Toast.LENGTH_SHORT).show()
            selectedCategory = ""
            description = ""
            dateText = ""
            timeText = ""
            isAnonymous = false
            confirmData = false
            reportViewModel.resetState()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Toast.makeText(context, "Error: ${uiState.error}", Toast.LENGTH_SHORT).show()
        }
    }

    val categories = listOf(
        ReportCategory("Robo a mano armada", Icons.Default.Security, MaterialTheme.colorScheme.primary, true),
        ReportCategory("Hurto/Arrebato", Icons.Default.Security, Secondary),
        ReportCategory("Acoso", Icons.Default.Person, Tertiary),
        ReportCategory("Zona peligrosa", Icons.Default.LocationOn, Secondary),
        ReportCategory("Calle sin iluminación", Icons.Default.MyLocation, MaterialTheme.colorScheme.onSurfaceVariant, isFullWidth = true)
    )

    fun submitReport() {
        if (selectedCategory.isEmpty()) {
            Toast.makeText(context, "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (description.isBlank()) {
            Toast.makeText(context, "Por favor completa la descripción del incidente", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!confirmData) {
            Toast.makeText(context, "Por favor confirma la información", Toast.LENGTH_SHORT).show()
            return
        }
        
        reportViewModel.submitReport(
            category = selectedCategory,
            description = description,
            isAnonymous = isAnonymous,
            userId = null, // Se obtendrá del SessionManager si es necesario
            latitude = null, // Se obtendrá del mapa cuando esté integrado
            longitude = null, // Se obtendrá del mapa cuando esté integrado
            address = null // Se obtendrá del mapa cuando esté integrado
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 120.dp)
    ) {
        Text(
            text = "Reportar incidente",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            text = "Completa los datos del incidente antes de enviarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        SectionLabel(title = "TIPO DE INCIDENTE")
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryItem(categories[0], selectedCategory == categories[0].name, Modifier.weight(1f)) { selectedCategory = it }
                CategoryItem(categories[1], selectedCategory == categories[1].name, Modifier.weight(1f)) { selectedCategory = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryItem(categories[2], selectedCategory == categories[2].name, Modifier.weight(1f)) { selectedCategory = it }
                CategoryItem(categories[3], selectedCategory == categories[3].name, Modifier.weight(1f)) { selectedCategory = it }
            }
            CategoryItem(categories[4], selectedCategory == categories[4].name, Modifier.fillMaxWidth()) { selectedCategory = it }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(title = "DETALLE DEL INCIDENTE")
        Spacer(modifier = Modifier.height(8.dp))
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            ReportTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Describe qué pasó, con quién, cómo y cualquier detalle útil...",
                minHeight = 120.dp,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(title = "UBICACIÓN")
        Spacer(modifier = Modifier.height(8.dp))
        LocationPreview()


        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(title = "FECHA Y HORA")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniFieldCard(
                icon = Icons.Default.CalendarMonth,
                label = "Fecha",
                value = dateText.ifEmpty { "Seleccionar fecha" },
                modifier = Modifier.weight(1f),
            ) {
                showDatePicker = true
            }
            MiniFieldCard(
                icon = Icons.Default.MyLocation,
                label = "Hora aproximada",
                value = timeText.ifEmpty { "Seleccionar hora" },
                modifier = Modifier.weight(1f),
            ) {
                showTimePicker = true
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
                            val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
                            dateText = dateFormat.format(calendar.time)
                        }
                        showDatePicker = false
                    }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time Picker Dialog
        if (showTimePicker) {
            TimePickerDialog(
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                onConfirm = { hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    timeText = String.format("%02d:%02d", hour, minute)
                    showTimePicker = false
                },
                onDismiss = {
                    showTimePicker = false
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(title = "REPORTANTE")
        Spacer(modifier = Modifier.height(8.dp))
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Reportar anónimamente", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = Color.White)
                            Text("Tu identidad no será visible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryContainer),
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnonymous) "Se enviará sin asociar tu identidad" else "Se enviará con tu cuenta activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(20.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = confirmData,
                    onCheckedChange = { confirmData = it },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryContainer, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmo que la información ingresada es correcta",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TactileButton(
            text = "Enviar reporte",
            onClick = { if (!isLoading) submitReport() },
            icon = Icons.AutoMirrored.Filled.Send,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReportTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isBlank()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.35f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LocationPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1B1B1B))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Vista de ubicación pendiente",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Aquí luego se conectará Google Maps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MiniFieldCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun TimePickerDialog(
    selectedHour: Int,
    selectedMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(selectedHour) }
    var minute by remember { mutableStateOf(selectedMinute) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Selecciona la hora", color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mostrar hora seleccionada
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Contenedor para los dos carrouseles verticales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Carrusel de horas (vertical)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Horas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 60.dp)
                        ) {
                            items((0..23).toList()) { h ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (h == hour) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            1.dp,
                                            if (h == hour) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { hour = h },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", h),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (h == hour) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Separador
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(150.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Carrusel de minutos (vertical)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minutos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 60.dp)
                        ) {
                            items((0..59 step 5).toList()) { m ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (m == minute) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            1.dp,
                                            if (m == minute) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { minute = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", m),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (m == minute) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("Aceptar", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = Color(0xFF1B1B1B),
        textContentColor = Color.White
    )
}


data class ReportCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isPrimary: Boolean = false,
    val isFullWidth: Boolean = false
)

@Composable
fun CategoryItem(category: ReportCategory, isSelected: Boolean, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Surface(
        onClick = { onSelect(category.name) },
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) category.color.copy(0.2f) else Color(0xCC1E1E1E),
        border = if (category.isPrimary) BorderStroke(2.dp, PrimaryContainer) else null
    ) {
        if (category.isFullWidth) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(category.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
