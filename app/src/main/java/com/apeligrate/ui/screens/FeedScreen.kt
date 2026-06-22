package com.apeligrate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.viewmodel.FeedViewModel

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onReportClick: (IncidentReport) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Feed de Actividad",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Valida incidentes para mantener a la comunidad segura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = Color(0xFF4DB6AC).copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4DB6AC).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation, 
                        contentDescription = null, 
                        modifier = Modifier.size(12.dp), 
                        tint = Color(0xFF4DB6AC)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cerca de ti", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color(0xFF4DB6AC),
                        softWrap = false // Fix for vertical text artifact
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(uiState.reports) { report ->
                FeedItem(
                    report = report,
                    onValidate = { viewModel.validateReport(report.id, true) },
                    onFalse = { viewModel.validateReport(report.id, false) },
                    onClick = { onReportClick(report) }
                )
            }
        }
    }
}

@Composable
fun FeedItem(
    report: IncidentReport,
    onValidate: () -> Unit,
    onFalse: () -> Unit,
    onClick: () -> Unit
) {
    val categoryColor = when (report.status) {
        "critical" -> Color(0xFFFF5252)
        "warning" -> Color(0xFFFFC107)
        "safe" -> Color(0xFF4DB6AC)
        else -> Color.Gray
    }

    val icon = when (report.category) {
        "Robo a mano armada" -> Icons.Default.Warning
        "Hurto/Arrebato" -> Icons.Default.Security
        "Acoso" -> Icons.Default.Person
        "Zona peligrosa" -> Icons.Default.LocationOn
        else -> Icons.Default.Info
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = report.category,
                            style = MaterialTheme.typography.titleMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = getTimeAgo(report.reportedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿ES REAL ESTE REPORTE?",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Button Real
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.8f))
                        .clickable { onValidate() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SÍ, ES REAL", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color.Black, 
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }

                // Button False
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onFalse() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "FALSO", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color.Gray, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

fun getTimeAgo(time: Long): String {
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60000 -> "ahora"
        diff < 3600000 -> "hace ${diff / 60000}min"
        diff < 86400000 -> "hace ${diff / 3600000}h"
        else -> "hace ${diff / 86400000}d"
    }
}
