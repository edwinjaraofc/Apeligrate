package com.apeligrate.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.ui.components.GlassPanel
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelTextField
import com.apeligrate.ui.components.SocialButton
import com.apeligrate.ui.components.TactileButton
import com.apeligrate.ui.theme.Primary
import com.apeligrate.ui.theme.PrimaryContainer
import com.apeligrate.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            Toast.makeText(context, "Inicio de Sesión exitoso", Toast.LENGTH_SHORT).show()
            onLoginSuccess()
        }
    }

    SentinelBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Brand Identity
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(PrimaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Apeligrate",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Primary
                    )
                    Text(
                        text = "Vigilancia comunitaria inteligente para tu seguridad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Login Form
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SentinelTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Correo Electrónico",
                            placeholder = "ejemplo@correo.com",
                            leadingIcon = Icons.Default.Mail,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )

                        Column {
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
                            Text(
                                text = "¿Olvidé mi contraseña?",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 8.dp)
                                    .clickable { },
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
                            text = "Iniciar Sesión",
                            onClick = viewModel::login,
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        )

                        // Divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f))
                            Text(
                                text = "O continuar con",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.1f))
                        }

                        SocialButton(
                            text = "Continuar con Google",
                            onClick = { /* Google Login */ },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }

                // Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "¿No tienes una cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Registrarse",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary,
                        modifier = Modifier.clickable { onRegisterClick() },
                    )
                }
            }
        }
    }
}
