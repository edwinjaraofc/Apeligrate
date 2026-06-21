package com.apeligrate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.apeligrate.data.local.SessionManager
import com.apeligrate.domain.model.Achievement
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.userIdFlow.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        userId?.let { viewModel.loadUserProfile(it) }
    }

    uiState.user?.let { user ->
        if (uiState.isEditing) {
            EditProfileDialog(
                name = uiState.editName,
                city = uiState.editCity,
                onNameChange = viewModel::onNameChange,
                onCityChange = viewModel::onCityChange,
                onSave = viewModel::saveProfile,
                onDismiss = viewModel::toggleEditMode
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
        ) {
            // Header: Avatar and Name
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, PrimaryContainer, CircleShape),
                            shape = CircleShape,
                            color = Color.DarkGray
                        ) {
                            if (user.profileImageUrl != null) {
                                AsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                        
                        if (user.isVerified) {
                            Surface(
                                color = Color(0xFFFFC107),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.offset(y = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VERIFICADO", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(text = user.name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(text = user.city, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }

            // Trust Level Card
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("NIVEL DE CONFIANZA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(user.reputationTitle, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF4DB6AC), fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4DB6AC).copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { user.experience.toFloat() / user.nextLevelExperience },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFF4DB6AC),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nivel ${user.level}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            Text("${(user.experience * 100 / user.nextLevelExperience)}% para el siguiente rango", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }

            // Stats Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = user.reportsCount.toString(),
                        label = "REPORTES",
                        icon = Icons.Default.Campaign
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = user.validationsCount.toString(),
                        label = "VALIDACIONES",
                        icon = Icons.AutoMirrored.Filled.FactCheck
                    )
                }
            }

            // Achievements Section
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logros Obtenidos", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                }
            }

            items(user.achievements) { achievement ->
                AchievementItem(achievement)
            }

            // Edit Profile Button
            item {
                TactileButton(
                    text = "Editar Perfil",
                    onClick = viewModel::toggleEditMode,
                    icon = Icons.Default.Edit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    GlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val icon = when (achievement.iconName) {
        "visibility" -> Icons.Default.Visibility
        "diamond" -> Icons.Default.Diamond
        "star" -> Icons.Default.Star
        else -> Icons.Default.EmojiEvents
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(achievement.colorHex.toColorInt()).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(achievement.colorHex.toColorInt()))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(achievement.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    name: String,
    city: String,
    onNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil", color = Color.White) },
        containerColor = Color(0xFF1B1B1B),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Ciudad") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Guardar", color = PrimaryContainer) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        }
    )
}
