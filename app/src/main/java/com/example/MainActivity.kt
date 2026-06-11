package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError
import com.example.viewmodel.ServerViewModel
import com.example.viewmodel.ZiskareViewModel
import com.example.viewmodel.ApiState
import com.example.data.api.LoginRequest
import com.example.data.api.RegisterSimpleRequest
import com.example.data.api.NetworkConfig

class MainActivity : ComponentActivity() {

    private val serverViewModel: ServerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ServerViewModel(application) as T
            }
        }
    }

    private val ziskareViewModel: ZiskareViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ZiskareViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var isAuthenticated by remember { mutableStateOf(false) }

                if (!isAuthenticated) {
                    AuthGateScreen(
                        viewModel = serverViewModel,
                        onAuthSuccess = { isAuthenticated = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainAppScaffold(
                        serverViewModel = serverViewModel,
                        ziskareViewModel = ziskareViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    serverViewModel: ServerViewModel,
    ziskareViewModel: ZiskareViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Collect global action alerts/toasts from Ziskare ViewModel
    val feedbackMessage by ziskareViewModel.actionFeedback.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            ziskareViewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = currentRoute == "chassis" || currentRoute?.startsWith("detail") == true,
                    onClick = { navController.navigate("chassis") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "Active Telemetry Chassis") },
                    label = { Text("Chassis", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_chassis")
                )
                NavigationBarItem(
                    selected = currentRoute == "database",
                    onClick = { navController.navigate("database") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "MongoDB Console Links") },
                    label = { Text("MongoDB", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_database")
                )
                NavigationBarItem(
                    selected = currentRoute == "storage",
                    onClick = { navController.navigate("storage") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = "Storage Partition Vault") },
                    label = { Text("Storage", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_storage")
                )
                NavigationBarItem(
                    selected = currentRoute == "email",
                    onClick = { navController.navigate("email") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Email, contentDescription = "SMTP Broadcast Hub") },
                    label = { Text("Email", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_email")
                )
                NavigationBarItem(
                    selected = currentRoute == "users",
                    onClick = { navController.navigate("users") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.People, contentDescription = "Corporate Accounts and Quotas") },
                    label = { Text("Users", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_users")
                )
                NavigationBarItem(
                    selected = currentRoute == "gateway" || currentRoute == "settings",
                    onClick = { navController.navigate("gateway") { popUpTo("chassis") { inclusive = false } } },
                    icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Ziskare Server gateway") },
                    label = { Text("Gateway", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_btn_gateway")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chassis",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chassis") {
                DashboardScreen(
                    viewModel = serverViewModel,
                    onNavigateToDetail = { serverId ->
                        navController.navigate("detail/$serverId")
                    }
                )
            }
            composable(
                route = "detail/{serverId}",
                arguments = listOf(navArgument("serverId") { type = NavType.IntType })
            ) { backStackEntry ->
                ServerDetailScreen(
                    viewModel = serverViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("database") {
                DatabaseAdminScreen(viewModel = ziskareViewModel)
            }
            composable("storage") {
                StorageScreen(viewModel = ziskareViewModel)
            }
            composable("email") {
                EmailSystemScreen(viewModel = ziskareViewModel)
            }
            composable("users") {
                UserManagementScreen(viewModel = ziskareViewModel)
            }
            composable("gateway") {
                GatewayScreen(
                    viewModel = ziskareViewModel,
                    onManageLocalSimulations = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(viewModel = serverViewModel)
            }
        }
    }
}

@Composable
fun GatewayScreen(
    viewModel: ZiskareViewModel,
    onManageLocalSimulations: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawUrlInput by remember { mutableStateOf("") }
    val currentUrl by viewModel.serverUrl.collectAsState(initial = "")
    val currentName by viewModel.userName.collectAsState(initial = "")
    val currentRole by viewModel.userRole.collectAsState(initial = "")
    val currentToken by viewModel.authToken.collectAsState(initial = "")

    val authState by viewModel.authState.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val networkConfig by viewModel.networkConfig.collectAsState()

    // Login inputs
    var loginUser by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }

    // Register parameters
    var regName by remember { mutableStateOf("") }
    var regUser by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPass by remember { mutableStateOf("") }

    LaunchedEffect(currentUrl) {
        rawUrlInput = currentUrl
        viewModel.checkNetworkStatus()
        viewModel.loadNetworkConfig()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ZISKARE GATEWAY SYSTEM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configure base connections, audit network states, and authenticate terminal sessions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Button(
                    onClick = onManageLocalSimulations,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = "Local audits", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOCAL SIMULATED LOGS", fontSize = 11.sp)
                }
            }
        }

        // Connections Gateway status card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE SERVER PATH", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = rawUrlInput,
                            onValueChange = { rawUrlInput = it },
                            label = { Text("Ziskare Server Base Endpoint (URL)") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        Button(
                            onClick = {
                                if (rawUrlInput.isNotBlank()) {
                                    viewModel.updateServerUrl(rawUrlInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("CONFIGURE")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Router, contentDescription = "Route ping", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Server Link Status: ", fontSize = 12.sp)

                            when (val state = networkStatus) {
                                is ApiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(14.dp))
                                is ApiState.Success -> {
                                    val stat = state.data
                                    Text(
                                        if (stat.online == true) "ONLINE @ ${stat.port}" else "UNREACHABLE",
                                        color = if (stat.online == true) SystemSuccess else SystemError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                else -> Text("OFFLINE", color = SystemError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        IconButton(onClick = { viewModel.checkNetworkStatus() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Ping active core server", tint = NeonCyan)
                        }
                    }
                }
            }
        }

        // Auth management login
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Section Left: Sign In console
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AUTHENTICATE ADMINISTRATOR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonCyan)

                        if (!currentToken.isNullOrEmpty()) {
                            Text("SESSION ACTIVE", fontWeight = FontWeight.Bold, color = SystemSuccess, fontSize = 11.sp)
                            Text("Name: $currentName", fontSize = 12.sp)
                            Text("Role Assigned: ${currentRole.uppercase()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)

                            Button(
                                onClick = { viewModel.performServerLogOut() },
                                colors = ButtonDefaults.buttonColors(containerColor = SystemError),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("LOGOUT SESSION")
                            }
                        } else {
                            OutlinedTextField(
                                value = loginUser,
                                onValueChange = { loginUser = it },
                                label = { Text("Username / Email Handle") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = loginPass,
                                onValueChange = { loginPass = it },
                                label = { Text("Gateway Credentials Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            Button(
                                onClick = {
                                    if (loginUser.isNotEmpty() && loginPass.isNotEmpty()) {
                                        viewModel.login(
                                            LoginRequest(
                                                username = loginUser,
                                                identifier = loginUser,
                                                password = loginPass
                                            )
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("LOG IN SECURELY")
                            }

                            when (val loginState = authState) {
                                is ApiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                is ApiState.Error -> Text(loginState.message, color = SystemError, fontSize = 11.sp)
                                else -> {}
                            }
                        }
                    }
                }

                // Section Right: Create initial account on nodes
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REGISTER GATEWAY ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalPurple)

                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text("Full administrative name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regUser,
                            onValueChange = { regUser = it },
                            label = { Text("Username handle") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Corporate Email Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = regPass,
                            onValueChange = { regPass = it },
                            label = { Text("Password protection") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Button(
                            onClick = {
                                if (regUser.isNotEmpty() && regPass.isNotEmpty()) {
                                    viewModel.register(
                                        RegisterSimpleRequest(
                                            name = regName,
                                            username = regUser,
                                            email = regEmail,
                                            password = regPass,
                                            role = "admin"
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("REGISTER MASTER USER")
                        }
                    }
                }
            }
        }

        // Live Network reboot adjustments config
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LIVE HARDWARE CONFIG", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    when (val stat = networkConfig) {
                        is ApiState.Loading -> CircularProgressIndicator(color = NeonCyan)
                        is ApiState.Success -> {
                            val info = stat.data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("IP Bind: ${info.host ?: "0.0.0.0"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text("Port allocation: ${info.port ?: 8080}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text("Rate Limits threshold: ${info.rateLimit ?: 100}/min", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.updateNetworkSettings(
                                                NetworkConfig(
                                                    host = info.host,
                                                    port = (info.port ?: 8080) + 1, // trigger shift
                                                    sslEnabled = info.sslEnabled,
                                                    timeoutMs = info.timeoutMs,
                                                    rateLimit = info.rateLimit
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("RE-PORT SYSTEM")
                                    }

                                    Button(
                                        onClick = { viewModel.resetNetworkSettings() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemError)
                                    ) {
                                        Text("HARD FACTORY RESET")
                                    }
                                }
                            }
                        }
                        else -> {
                            Text("Hardware system config unavailable offline. Point to reachable live node above.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
