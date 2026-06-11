package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerEntity
import com.example.viewmodel.ServerViewModel
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError
import com.example.ui.theme.SystemNeutral
import com.example.ui.theme.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ServerViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val serversList by viewModel.servers.collectAsState()
    val unreadAlerts by viewModel.unreadAlerts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var isUpdateAvailable by remember { mutableStateOf(true) }
    var updateVersion by remember { mutableStateOf("v2.4.1") }
    var releaseNotes by remember { mutableStateOf("Simulated biometrics enrollment, live build monitoring, and secure OTP token visibility are now fully enabled.") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_server_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register New Node")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // GitHub Update Available Banner
            AnimatedVisibility(visible = isUpdateAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("github_update_banner")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "GitHub Update", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UPDATE AVAILABLE: $updateVersion (Stable Release)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { isUpdateAvailable = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = releaseNotes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Simulation of release packaging download
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(32.dp).testTag("download_apk_btn"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DOWNLOAD APK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Unread Alert warning banner bar if any exists
            AnimatedVisibility(visible = unreadAlerts.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("unread_alerts_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Triggered Alerts", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE EVENT WARNINGS: ${unreadAlerts.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearAllAlerts() },
                            modifier = Modifier.testTag("dismiss_all_alerts")
                        ) {
                            Text("DISMISS ALL", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Quick Network Health Summary Card Indicator Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStateItem(
                    title = "CONVERGED NODES",
                    value = "${serversList.size}",
                    icon = Icons.Default.Dns,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryStateItem(
                    title = "GLOBAL HEALTH",
                    value = if (unreadAlerts.isNotEmpty()) "82%" else "100%",
                    icon = Icons.Default.NetworkCheck,
                    color = if (unreadAlerts.isNotEmpty()) Color(0xFFFFA726) else SystemSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MANAGED SERVER INFRASTRUCTURE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (serversList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = "No Connections Registered",
                            modifier = Modifier.size(64.dp),
                            tint = SystemNeutral
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Server Profiles Registered Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemNeutral
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add an SSH endpoint mapping using the FAB below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemNeutral.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(serversList, key = { it.id }) { server ->
                        ServerCardItem(
                            server = server,
                            onClick = {
                                viewModel.selectServer(server)
                                onNavigateToDetail(server.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, host, port, user, authType, color ->
                viewModel.createServer(name, host, port, user, authType, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SummaryStateItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                title,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun ServerCardItem(
    server: ServerEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("server_card_${server.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header node info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(server.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Surface(
                    color = when (server.status) {
                        "Online" -> SystemSuccess.copy(alpha = 0.15f)
                        "Warning" -> Color(0xFFFFA726).copy(alpha = 0.15f)
                        else -> SystemError.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = server.status,
                        color = when (server.status) {
                            "Online" -> SystemSuccess
                            "Warning" -> Color(0xFFFFA726)
                            else -> SystemError
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${server.username}@${server.host}:${server.port}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Rapid indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MiniProgressMetric(label = "CPU", percent = server.cpuUsage)
                }
                Box(modifier = Modifier.weight(1f)) {
                    MiniProgressMetric(label = "RAM", percent = server.ramUsage)
                }
            }
        }
    }
}

@Composable
fun MiniProgressMetric(label: String, percent: Float) {
    val barColor = when {
        percent > 80f -> SystemError
        percent > 65f -> Color(0xFFFFA726)
        else -> NeonCyan
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("${percent.toInt()}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = barColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent / 100f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("root") }
    var authType by remember { mutableStateOf("Password") }
    var colorSelected by remember { mutableStateOf("#2196F3") }

    val colorsMap = listOf("#2196F3", "#E91E63", "#4CAF50", "#9C27B0", "#FF5722", "#00BCD4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("REGISTER CRYPTO-NODE") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App Server Label Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_server_name_field")
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP Host / Domain Address") },
                    modifier = Modifier.fillMaxWidth().testTag("add_server_host_field")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("SSH Port") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("User Context") },
                        modifier = Modifier.weight(1.5f).testTag("add_server_user_field")
                    )
                }

                Text("Profile Node Primary Theme Accent:", fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorsMap.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { colorSelected = hex }
                                .padding(2.dp)
                        ) {
                            if (colorSelected == hex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pVal = port.toIntOrNull() ?: 22
                    onConfirm(name, host, pVal, user, authType, colorSelected)
                },
                modifier = Modifier.testTag("submit_add_server")
            ) {
                Text("PROVISION RESOURCE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
