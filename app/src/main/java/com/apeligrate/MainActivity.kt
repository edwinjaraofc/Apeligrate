package com.apeligrate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelBottomBar
import com.apeligrate.ui.components.SentinelButton
import com.apeligrate.ui.components.SentinelCard
import com.apeligrate.ui.components.SentinelTab
import com.apeligrate.ui.theme.ApeligrateTheme
import com.apeligrate.ui.theme.SafeGreen
import com.apeligrate.ui.theme.WarningAmber
import com.apeligrate.ui.viewmodel.MainViewModel

import com.apeligrate.data.repository.MockAuthRepository
import com.apeligrate.domain.use_case.PerformLoginUseCase
import com.apeligrate.domain.use_case.RegisterUserUseCase
import com.apeligrate.ui.screens.LoginScreen
import com.apeligrate.ui.screens.RegisterScreen
import com.apeligrate.ui.screens.ReportScreen
import com.apeligrate.ui.viewmodel.LoginViewModel
import com.apeligrate.ui.viewmodel.RegisterViewModel

class MainActivity : ComponentActivity() {
    private val authRepository = MockAuthRepository()
    private val loginUseCase = PerformLoginUseCase(authRepository)
    private val registerUseCase = RegisterUserUseCase(authRepository)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApeligrateTheme {
                var currentScreen by remember { mutableStateOf("login") }
                var selectedTab by remember { mutableStateOf(SentinelTab.INICIO) }

                when (currentScreen) {
                    "login" -> {
                        val viewModel = remember { LoginViewModel(loginUseCase) }
                        LoginScreen(
                            viewModel = viewModel,
                            onRegisterClick = { currentScreen = "register" },
                            onLoginSuccess = { currentScreen = "main" },
                        )
                    }
                    "register" -> {
                        val viewModel = remember { RegisterViewModel(registerUseCase) }
                        RegisterScreen(
                            viewModel = viewModel,
                            onLoginClick = { currentScreen = "login" },
                            onRegisterSuccess = { currentScreen = "main" }
                        )
                    }
                    "main" -> {
                        SentinelBackground {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color.Transparent,
                                topBar = { SentinelTopBar() },
                                bottomBar = {
                                    SentinelBottomBar(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when (selectedTab) {
                                        SentinelTab.INICIO -> MainScreen()
                                        SentinelTab.REPORTAR -> ReportScreen()
                                        else -> PlaceholderScreen(selectedTab.name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentinelTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .background(Color(0xCC131313))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Apeligrate",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Pantalla de $name en desarrollo", color = Color.White)
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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

        Spacer(modifier = Modifier.height(24.dp))

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
                onClick = { /* Emergency Action */ },
                modifier = Modifier.weight(1f),
            )
            SentinelButton(
                text = "REPORTAR",
                onClick = { /* Report Action */ },
                modifier = Modifier.weight(1f),
                isPrimary = false,
            )
        }
    }
}

@Composable
fun AlertItem(alert: Alert) {
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
