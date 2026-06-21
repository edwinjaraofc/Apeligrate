package com.apeligrate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Logout
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
import kotlinx.coroutines.launch
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelBottomBar
import com.apeligrate.ui.components.SentinelTab
import com.apeligrate.ui.theme.ApeligrateTheme

import com.apeligrate.domain.use_case.PerformLoginUseCase
import com.apeligrate.domain.use_case.RegisterUserUseCase
import com.apeligrate.ui.screens.FeedScreen
import com.apeligrate.ui.screens.LoginScreen
import com.apeligrate.ui.screens.MainScreen
import com.apeligrate.ui.screens.ProfileScreen
import com.apeligrate.ui.screens.RegisterScreen
import com.apeligrate.ui.screens.ReportScreen
import com.apeligrate.ui.screens.SplashScreen
import com.apeligrate.ui.viewmodel.FeedViewModel
import com.apeligrate.ui.viewmodel.LoginViewModel
import com.apeligrate.ui.viewmodel.MainViewModel
import com.apeligrate.ui.viewmodel.RegisterViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    // SessionManager + Repositories
    private val sessionManager by lazy { com.apeligrate.data.local.SessionManager(this.applicationContext) }
    private val authRepository by lazy { com.apeligrate.data.repository.RemoteAuthRepository(sessionManager) }
    private val incidentRepository by lazy { com.apeligrate.data.repository.IncidentReportRepositoryImpl() }
    
    private val loginUseCase by lazy { PerformLoginUseCase(authRepository) }
    private val registerUseCase by lazy { RegisterUserUseCase(authRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApeligrateTheme {
                val isLoggedIn by sessionManager.isLoggedInFlow.collectAsState(initial = null)
                var currentScreen by remember { mutableStateOf<String?>(null) }
                var selectedTab by remember { mutableStateOf(SentinelTab.INICIO) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn != null) {
                        Log.d(TAG, "Session state determined: isLoggedIn=$isLoggedIn")
                        if (currentScreen == null) {
                            currentScreen = if (isLoggedIn == true) "main" else "login"
                        } else if (isLoggedIn == false && currentScreen != "login") {
                            currentScreen = "login"
                        }
                    }
                }

                if (currentScreen == null) {
                    SplashScreen()
                } else {
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
                            val mainViewModel: MainViewModel = viewModel()
                            SentinelBackground {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = Color.Transparent,
                                    topBar = {
                                        SentinelTopBar(
                                            onLogoutClick = {
                                                coroutineScope.launch {
                                                    authRepository.logout()
                                                    selectedTab = SentinelTab.INICIO
                                                }
                                            }
                                        )
                                    },
                                    bottomBar = {
                                        SentinelBottomBar(
                                            selectedTab = selectedTab,
                                            onTabSelected = { selectedTab = it }
                                        )
                                    }
                                ) { innerPadding ->
                                    Box(modifier = Modifier.padding(innerPadding)) {
                                        when (selectedTab) {
                                            SentinelTab.INICIO -> MainScreen(
                                                incidentRepository = incidentRepository,
                                                onNavigateToReport = { selectedTab = SentinelTab.REPORTAR },
                                                viewModel = mainViewModel
                                            )
                                            SentinelTab.FEED -> {
                                                val feedViewModel = remember { FeedViewModel(incidentRepository) }
                                                FeedScreen(
                                                    viewModel = feedViewModel,
                                                    onReportClick = { report ->
                                                        if (report.latitude != null && report.longitude != null) {
                                                            mainViewModel.focusLocation(report.latitude, report.longitude)
                                                            selectedTab = SentinelTab.INICIO
                                                        }
                                                    }
                                                )
                                            }
                                            SentinelTab.REPORTAR -> ReportScreen(
                                                repository = incidentRepository,
                                                onReportSubmitted = { selectedTab = SentinelTab.INICIO }
                                            )
                                            SentinelTab.PERFIL -> ProfileScreen()
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
}

@Composable
fun SentinelTopBar(onLogoutClick: () -> Unit) {
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
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x33FF4D4D))
                .border(1.dp, Color(0x55FF6B6B), RoundedCornerShape(999.dp))
                .clickable(onClick = onLogoutClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Cerrar sesión",
                tint = Color(0xFFFFB3B3),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Salir",
                color = Color(0xFFFFD6D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
