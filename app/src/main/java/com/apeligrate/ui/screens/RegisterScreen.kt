package com.apeligrate.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelTextField
import com.apeligrate.ui.components.SocialButton
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.Primary
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.theme.Secondary
import com.apeligrate.ui.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            Toast.makeText(context, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
            onRegisterSuccess()
        }
    }

    SentinelBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Header - Compact
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Únete a la Comunidad",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                        color = Primary
                    )
                    Text(
                        text = "Ayuda a mantener tu ciudad segura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }

                // Registration Card
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SentinelTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label = "Nombre completo",
                            placeholder = "Juan Pérez",
                            leadingIcon = Icons.Default.Person,
                        )
                        SentinelTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Correo electrónico",
                            placeholder = "usuario@apeligrate.com",
                            leadingIcon = Icons.Default.Mail,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        SentinelTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Contraseña",
                            placeholder = "••••••••",
                            leadingIcon = Icons.Default.Lock,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { passwordVisible = !passwordVisible },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        SentinelTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = "Confirmar contraseña",
                            placeholder = "••••••••",
                            leadingIcon = Icons.Default.LockReset,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { confirmPasswordVisible = !confirmPasswordVisible },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        // Terms - Compact
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = uiState.termsAccepted,
                                onCheckedChange = viewModel::onTermsToggle,
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryContainer),
                                modifier = Modifier.scale(0.7f).size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acepto términos y políticas de datos.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (uiState.error != null) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        TactileButton(
                            text = "Crear Cuenta",
                            onClick = viewModel::register,
                            icon = Icons.Default.AppRegistration,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )

                        // Divider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f))
                            Text(
                                text = "O únete con",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f))
                        }

                        SocialButton(
                            text = "Únete con Google",
                            onClick = { /* Google Register */ },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        )
                    }
                }

                // Footer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¿Ya tienes una cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Secondary,
                        modifier = Modifier.clickable { onLoginClick() },
                    )
                }
            }
        }
    }
}
