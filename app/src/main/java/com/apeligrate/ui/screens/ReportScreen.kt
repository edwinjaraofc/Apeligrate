package com.apeligrate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.theme.Secondary
import com.apeligrate.ui.theme.Tertiary

@Composable
fun ReportScreen() {
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val categories = listOf(
        ReportCategory("Robo a mano armada", Icons.Default.Security, MaterialTheme.colorScheme.primary, true),
        ReportCategory("Hurto/Arrebato", Icons.Default.Warning, Secondary),
        ReportCategory("Zona de microcomercialización", Icons.Default.LocalHospital, Tertiary),
        ReportCategory("Acoso", Icons.Default.Person, Secondary),
        ReportCategory("Calle sin iluminación", Icons.Default.Lightbulb, MaterialTheme.colorScheme.onSurfaceVariant, isFullWidth = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 100.dp) // Extra bottom padding for BottomBar
    ) {
        Text(
            text = "Reportar Incidente",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            text = "Seleccione la categoría que mejor describa la situación.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Categories Grid
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

        Spacer(modifier = Modifier.height(24.dp))

        // Map Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "UBICACIÓN DEL INCIDENTE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Actualizar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
        ) {
            // Map Placeholder
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        Text(
            text = "DETALLES ADICIONALES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
                if (description.isEmpty()) {
                    Text(
                        "Ej: Moto lineal blanca sin placa...",
                        color = Color.White.copy(0.3f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Anonymous Toggle
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
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
                        Text("Tu identidad no será visible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
                Switch(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryContainer),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TactileButton(
            text = "Enviar Reporte",
            onClick = { /* TODO */ },
            icon = Icons.AutoMirrored.Filled.Send,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
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
        border = if (category.isPrimary) androidx.compose.foundation.BorderStroke(2.dp, PrimaryContainer) else null
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
                Text(category.name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, lineHeight = 10.sp)
            }
        }
    }
}
