package com.apeligrate

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.apeligrate.domain.model.Contact
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.domain.model.User
import com.apeligrate.domain.repository.IncidentReportRepository
import com.apeligrate.domain.repository.UserProgressRepository
import com.apeligrate.domain.use_case.PerformLoginUseCase
import com.apeligrate.domain.use_case.RegisterUserUseCase
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelBottomBar
import com.apeligrate.ui.components.SentinelTab
import com.apeligrate.ui.screens.*
import com.apeligrate.ui.theme.ApeligrateTheme
import com.apeligrate.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainActivity"

enum class DrawerTab {
    NONE, SAFETY_SETTINGS, TRUSTED_CONTACTS, EMERGENCY_SERVICES, HISTORY, HELP_CENTER
}

class MainActivity : ComponentActivity() {
    private val sessionManager by lazy { com.apeligrate.data.local.SessionManager(this.applicationContext) }
    private val authRepository by lazy { com.apeligrate.data.repository.RemoteAuthRepository(sessionManager) }
    private val incidentRepository by lazy { com.apeligrate.data.repository.IncidentReportRepositoryImpl() }
    private val userProgressRepository: UserProgressRepository by lazy { com.apeligrate.data.repository.SupabaseUserProgressRepository() }
    
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
                var selectedDrawerTab by remember { mutableStateOf(DrawerTab.NONE) }
                var detailReport by remember { mutableStateOf<IncidentReport?>(null) }
                
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn != null) {
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
                            val profileViewModel = remember { ProfileViewModel(userProgressRepository) }
                            val userId by sessionManager.userIdFlow.collectAsState(initial = null)
                            
                            LaunchedEffect(userId) {
                                userId?.let {
                                    userProgressRepository.ensureUser(it)
                                    profileViewModel.loadUserProfile(it)
                                }
                            }

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                gesturesEnabled = false,
                                drawerContent = {
                                    SentinelDrawerContent(
                                        user = profileViewModel.uiState.collectAsState().value.user,
                                        selectedDrawerTab = selectedDrawerTab,
                                        onTabSelected = { tab ->
                                            selectedDrawerTab = tab
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                        onLogout = {
                                            coroutineScope.launch {
                                                drawerState.close()
                                                authRepository.logout()
                                                selectedTab = SentinelTab.INICIO
                                                selectedDrawerTab = DrawerTab.NONE
                                            }
                                        }
                                    )
                                }
                            ) {
                                SentinelBackground {
                                    Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        containerColor = Color.Transparent,
                                        topBar = {
                                            SentinelTopBar(
                                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                                onLogoutClick = {
                                                    coroutineScope.launch {
                                                        authRepository.logout()
                                                        selectedTab = SentinelTab.INICIO
                                                        selectedDrawerTab = DrawerTab.NONE
                                                    }
                                                }
                                            )
                                        },
                                        bottomBar = {
                                            if (detailReport == null && selectedDrawerTab == DrawerTab.NONE) {
                                                SentinelBottomBar(
                                                    selectedTab = selectedTab,
                                                    onTabSelected = { 
                                                        selectedTab = it
                                                        selectedDrawerTab = DrawerTab.NONE
                                                    }
                                                )
                                            }
                                        }
                                    ) { innerPadding ->
                                        Box(modifier = Modifier.padding(innerPadding)) {
                                            if (detailReport != null) {
                                                ReportDetailScreen(
                                                    report = detailReport!!,
                                                    onBack = { detailReport = null },
                                                    onShowOnMap = {
                                                        if (detailReport!!.latitude != null && detailReport!!.longitude != null) {
                                                            mainViewModel.focusLocation(detailReport!!.latitude!!, detailReport!!.longitude!!)
                                                            selectedTab = SentinelTab.INICIO
                                                            detailReport = null
                                                        }
                                                    }
                                                )
                                            } else if (selectedDrawerTab != DrawerTab.NONE) {
                                                DrawerScreenContent(
                                                    tab = selectedDrawerTab, 
                                                    onBack = { selectedDrawerTab = DrawerTab.NONE },
                                                    incidentRepository = incidentRepository,
                                                    currentUserId = userId,
                                                    onReportClick = { detailReport = it }
                                                )
                                            } else {
                                                when (selectedTab) {
                                                    SentinelTab.INICIO -> MainScreen(
                                                        viewModel = mainViewModel,
                                                        incidentRepository = incidentRepository,
                                                        onNavigateToReport = { selectedTab = SentinelTab.REPORTAR }
                                                    )
                                                    SentinelTab.FEED -> {
                                                        val feedViewModel = remember(userId) {
                                                            FeedViewModel(
                                                                repository = incidentRepository,
                                                                userProgressRepository = userProgressRepository,
                                                                currentUserId = userId
                                                            )
                                                        }
                                                        FeedScreen(
                                                            viewModel = feedViewModel,
                                                            onReportClick = { report ->
                                                                detailReport = report
                                                            }
                                                        )
                                                    }
                                                    SentinelTab.REPORTAR -> ReportScreen(
                                                        repository = incidentRepository,
                                                        userProgressRepository = userProgressRepository,
                                                        onReportSubmitted = { selectedTab = SentinelTab.INICIO }
                                                    )
                                                    SentinelTab.PERFIL -> ProfileScreen(viewModel = profileViewModel)
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
    }
}

@Composable
fun ReportDetailScreen(
    report: IncidentReport,
    onBack: () -> Unit,
    onShowOnMap: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = report.category,
            style = MaterialTheme.typography.headlineMedium,
            color = if (report.status == "critical") Color(0xFFFF5252) else Color(0xFFFFC107),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = formatTimestamp(report.reportedAt),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (report.images.isNotEmpty()) {
            AsyncImage(
                model = report.images.first(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ImageNotSupported, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Text(
            text = "DETALLES DEL REPORTE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = report.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )

        if (report.isAnonymous) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviado de forma anónima", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (report.latitude != null && report.longitude != null) {
            Button(
                onClick = onShowOnMap,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(12.dp))
                Text("UBICAR EN EL MAPA", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun DrawerScreenContent(
    tab: DrawerTab, 
    onBack: () -> Unit,
    incidentRepository: IncidentReportRepository,
    currentUserId: String?,
    onReportClick: (IncidentReport) -> Unit
) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        when (tab) {
            DrawerTab.SAFETY_SETTINGS -> SafetySettingsScreen()
            DrawerTab.TRUSTED_CONTACTS -> TrustedContactsScreen()
            DrawerTab.EMERGENCY_SERVICES -> EmergencyServicesScreen()
            DrawerTab.HISTORY -> HistoryScreen(incidentRepository, currentUserId, onReportClick)
            DrawerTab.HELP_CENTER -> HelpCenterScreen()
            else -> {}
        }
    }
}

@Composable
fun SentinelDrawerContent(
    user: User?,
    selectedDrawerTab: DrawerTab,
    onTabSelected: (DrawerTab) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color(0xFF131313)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(60.dp).border(1.dp, Color(0xFFFF5252), CircleShape),
                    shape = CircleShape,
                    color = Color.DarkGray
                ) {
                    if (user?.profileImageUrl != null) {
                        AsyncImage(model = user.profileImageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(12.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user?.name ?: "Guardian User", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "Trust Level: ${user?.reputationTitle ?: "High"}", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        DrawerTabItem(icon = Icons.Default.Shield, label = "Safety Settings", isSelected = selectedDrawerTab == DrawerTab.SAFETY_SETTINGS, onClick = { onTabSelected(DrawerTab.SAFETY_SETTINGS) })
        DrawerTabItem(icon = Icons.Default.People, label = "Trusted Contacts", isSelected = selectedDrawerTab == DrawerTab.TRUSTED_CONTACTS, onClick = { onTabSelected(DrawerTab.TRUSTED_CONTACTS) })
        DrawerTabItem(icon = Icons.Default.Star, label = "Emergency Services", isSelected = selectedDrawerTab == DrawerTab.EMERGENCY_SERVICES, onClick = { onTabSelected(DrawerTab.EMERGENCY_SERVICES) })
        DrawerTabItem(icon = Icons.Default.History, label = "History", isSelected = selectedDrawerTab == DrawerTab.HISTORY, onClick = { onTabSelected(DrawerTab.HISTORY) })
        DrawerTabItem(icon = Icons.AutoMirrored.Filled.Help, label = "Help Center", isSelected = selectedDrawerTab == DrawerTab.HELP_CENTER, onClick = { onTabSelected(DrawerTab.HELP_CENTER) })
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth().clickable { onLogout() }.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF5252))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Logout", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DrawerTabItem(icon: ImageVector, label: String, isSelected: Boolean = false, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(32.dp)).background(if (isSelected) Color(0xFFFFB300) else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                color = if (isSelected) Color.Black else Color.White, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, 
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SentinelTopBar(onMenuClick: () -> Unit, onLogoutClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).background(Color(0xCC131313)).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Apeligrate", 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x33FF4D4D)).border(1.dp, Color(0x55FF6B6B), RoundedCornerShape(999.dp)).clickable(onClick = onLogoutClick).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color(0xFFFFB3B3), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Salir", color = Color(0xFFFFD6D6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, softWrap = false)
        }
    }
}

@Composable
fun SafetySettingsScreen() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Configuración de Seguridad", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        SafetyItem("Alertas de Proximidad", true)
        SafetyItem("Modo Incógnito Automático", false)
        SafetyItem("Compartir Ubicación en Emergencia", true)
    }
}

@Composable
fun SafetyItem(label: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun TrustedContactsScreen() {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(listOf(Contact("1", "Mamá", "987654321"), Contact("2", "Hermano", "912345678"))) }
    var showAddDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    if (nameIndex >= 0 && idIndex >= 0) {
                        val name = it.getString(nameIndex)
                        val id = it.getString(idIndex)
                        
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        var phone = "Sin número"
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val numIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIndex >= 0) {
                                    phone = pc.getString(numIndex)
                                }
                            }
                        }
                        contacts = contacts + Contact(id, name, phone)
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text("Contactos de Confianza", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts) { contact ->
                ContactItem(contact.name, contact.phone) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }, 
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DB6AC))
            ) {
                Icon(Icons.Default.ContactPage, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("De agenda", fontSize = 12.sp, softWrap = false)
            }
            Button(
                onClick = { showAddDialog = true }, 
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manual", color = Color.Black, fontSize = 12.sp, softWrap = false)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Contacto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                    TextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        contacts = contacts + Contact(System.currentTimeMillis().toString(), name, phone)
                        showAddDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun ContactItem(name: String, phone: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = phone, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF4DB6AC))
        }
    }
}

@Composable
fun EmergencyServicesScreen() {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Servicios de Emergencia", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        EmergencyItem("Policía", "105", Icons.Default.LocalPolice) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:105"))
            context.startActivity(intent)
        }
        EmergencyItem("Bomberos", "116", Icons.Default.FireTruck) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:116"))
            context.startActivity(intent)
        }
        EmergencyItem("Ambulancia", "117", Icons.Default.MedicalServices) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117"))
            context.startActivity(intent)
        }
    }
}

@Composable
fun EmergencyItem(name: String, number: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text("$name: $number", fontSize = 18.sp, fontWeight = FontWeight.Black, softWrap = false)
    }
}

@Composable
fun HistoryScreen(
    repository: IncidentReportRepository, 
    userId: String?,
    onReportClick: (IncidentReport) -> Unit
) {
    val reports by repository.getReports().collectAsState(initial = emptyList())
    val myActions = remember(reports, userId) {
        reports.filter { it.userId == userId }
            .sortedByDescending { it.reportedAt }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text("Tu Historial Real", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (myActions.isEmpty()) {
            Text("Aún no tienes reportes registrados.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(myActions) { report ->
                    HistoryItem(
                        type = "Reporte: ${report.category}",
                        detail = report.description,
                        date = formatTimestamp(report.reportedAt),
                        onClick = { onReportClick(report) }
                    )
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun HistoryItem(type: String, detail: String, date: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { 
            Text(
                type, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            ) 
        },
        supportingContent = { 
            Text(
                detail, 
                color = Color.Gray, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            ) 
        },
        trailingContent = { Text(date, color = Color.DarkGray, fontSize = 10.sp, softWrap = false) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    HorizontalDivider(color = Color.White.copy(0.05f))
}

@Composable
fun HelpCenterScreen() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Centro de Ayuda", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("¿Cómo reportar un incidente?", color = Color(0xFFFFB300), modifier = Modifier.padding(vertical = 8.dp))
        Text("Gestionar mis contactos de confianza", color = Color(0xFFFFB300), modifier = Modifier.padding(vertical = 8.dp))
        Text("Preguntas frecuentes", color = Color(0xFFFFB300), modifier = Modifier.padding(vertical = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Versión de la app: 1.0.42", color = Color.Gray, fontSize = 12.sp)
    }
}
