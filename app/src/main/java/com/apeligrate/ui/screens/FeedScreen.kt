package com.apeligrate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                border = BorderStroke(1.dp, Color(0xFF4DB6AC).copy(alpha = 0.5f))
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
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        uiState.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF8A80)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(
                items = uiState.reports,
                key = { report -> report.id }
            ) { report ->
                val hasValidated = uiState.currentUserId?.let(report.validationVoterIds::contains) == true
                val hasMarkedFalse = uiState.currentUserId?.let(report.falseVoterIds::contains) == true
                val isSubmitting = report.id in uiState.submittingReportIds
                val hasAlreadyVoted = hasValidated || hasMarkedFalse
                FeedItem(
                    report = report,
                    onValidate = { viewModel.validateReport(report.id, true) },
                    onFalse = { viewModel.validateReport(report.id, false) },
                    onClick = { onReportClick(report) },
                    hasValidated = hasValidated,
                    hasMarkedFalse = hasMarkedFalse,
                    votingEnabled = !hasAlreadyVoted && !isSubmitting,
                    isSubmitting = isSubmitting
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
    onClick: () -> Unit,
    hasValidated: Boolean,
    hasMarkedFalse: Boolean,
    votingEnabled: Boolean,
    isSubmitting: Boolean
) {
    val categoryColor = when (report.status) {
        "critical" -> Color(0xFFFF5252)
        "warning" -> Color(0xFFFFC107)
        "safe" -> Color(0xFF4DB6AC)
        else -> Color.Gray
    }
    val voteAccentColor = when {
        hasMarkedFalse -> Color(0xFFFF8A80)
        hasValidated -> Color(0xFF7EE787)
        else -> categoryColor
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
            .border(
                width = if (votingEnabled) 1.dp else 2.dp,
                color = if (votingEnabled) Color.White.copy(alpha = 0.08f) else voteAccentColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp)
            )
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
                Column(horizontalAlignment = Alignment.End) {
                    if (!votingEnabled || isSubmitting) {
                        Surface(
                            color = voteAccentColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, voteAccentColor.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = if (isSubmitting) "ENVIANDO VOTO" else "YA VOTASTE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = voteAccentColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = getTimeAgo(report.reportedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            if (!votingEnabled && !isSubmitting) {
                Surface(
                    color = voteAccentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, voteAccentColor.copy(alpha = 0.42f))
                ) {
                    Text(
                        text = if (hasMarkedFalse) {
                            "Tu voto en este reporte: FALSO"
                        } else {
                            "Tu voto en este reporte: SI, ES REAL"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasMarkedFalse) Color(0xFFFFB4AB) else Color(0xFFC8F7D0),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isSubmitting) {
                Surface(
                    color = categoryColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.42f))
                ) {
                    Text(
                        text = "Registrando tu voto...",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "ES REAL ESTE REPORTE?",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                hasValidated -> Color(0xFF7EE787)
                                votingEnabled -> categoryColor.copy(alpha = 0.8f)
                                else -> categoryColor.copy(alpha = 0.3f)
                            }
                        )
                        .clickable(enabled = votingEnabled) { onValidate() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasValidated) "YA MARCADO" else "SI, ES REAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (hasMarkedFalse) {
                                Color(0xFFFF8A80).copy(alpha = 0.22f)
                            } else {
                                Color.White.copy(alpha = if (votingEnabled) 0.05f else 0.03f)
                            }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable(enabled = votingEnabled) { onFalse() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (hasMarkedFalse) Color(0xFFFF8A80) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasMarkedFalse) "YA MARCADO" else "FALSO",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasMarkedFalse) Color(0xFFFF8A80) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!votingEnabled && !isSubmitting) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (hasMarkedFalse) {
                        Color(0xFFFF8A80).copy(alpha = 0.14f)
                    } else {
                        categoryColor.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        1.dp,
                        if (hasMarkedFalse) Color(0xFFFF8A80).copy(alpha = 0.45f)
                        else categoryColor.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasMarkedFalse) Icons.Default.Cancel else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hasMarkedFalse) Color(0xFFFF8A80) else categoryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasMarkedFalse) {
                                "Ya votaste este reporte: marcaste FALSO"
                            } else {
                                "Ya votaste este reporte: marcaste SI, ES REAL"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasMarkedFalse) Color(0xFFFFB4AB) else Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold
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
