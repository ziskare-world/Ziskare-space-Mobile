package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.LogType
import com.example.models.RemoteProcess
import com.example.ui.components.MetricGauge
import com.example.ui.components.RealTimeHistoryGraph
import com.example.viewmodel.ServerViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalYellow

@Composable
fun ServerDetailScreen(
    viewModel: ServerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val server by viewModel.selectedServer.collectAsState()
    val history by viewModel.resourceHistory.collectAsState()
    val processes by viewModel.processes.collectAsState()
    val sshLines by viewModel.sshLines.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Telemetry Graphs, 1 = Live Processes, 2 = SSH Terminal

    if (server == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a Server Profile to Begin Telemetry Monitoring")
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to nodes")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = server!!.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${server!!.username}@${server!!.host}:${server!!.port}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(onClick = { viewModel.selectServer(server) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Force Socket Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Tab Row Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabHeader(
                        title = "TELEMETRY",
                        isActive = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_telemetry")
                    )
                    TabHeader(
                        title = "PROCESSES",
                        isActive = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_processes")
                    )
                    TabHeader(
                        title = "SSH SHELL",
                        isActive = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_ssh_terminal")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> TelemetrySubView(cpu = server!!.cpuUsage, ram = server!!.ramUsage, disk = server!!.diskUsage, history = history)
                1 -> LiveProcessesSubView(processes = processes, onKillProcess = { viewModel.manageProcess(it, "KILL") })
                2 -> VirtualTerminalSubView(sshLines = sshLines, onSendCommand = { viewModel.executeSshCommand(it) })
            }
        }
    }
}

@Composable
fun TabHeader(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun TelemetrySubView(
    cpu: Float,
    ram: Float,
    disk: Float,
    history: List<com.example.models.ResourceHistoryPoint>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricGauge(value = cpu, label = "CPU WORKLOAD", color = NeonCyan, modifier = Modifier.weight(1f))
            MetricGauge(value = ram, label = "RAM USAGE", color = Color(0xFF9C27B0), modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricGauge(value = disk, label = "DISK LOAD", color = Color(0xFFFF5722), modifier = Modifier.weight(1f))
            MetricGauge(value = 45f, label = "VNET (IN)", prefix = "▲ ", color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
        }

        RealTimeHistoryGraph(history = history)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HARDWARE CORE METRICS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                HardwareParamRow("Motherboard Virtual Socket", "Intel Xeon Broadwell (16 Cores)")
                HardwareParamRow("Active Swap Space Cluster", "16,384 MiB RAM Node")
                HardwareParamRow("Virtual NIC Driver Interface", "vEth0 Virtual Switch Loop")
                HardwareParamRow("Core Temp Status Limit", "42°C (Healthy Range)")
            }
        }
    }
}

@Composable
fun HardwareParamRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(valStr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = NeonCyan)
    }
}

@Composable
fun LiveProcessesSubView(
    processes: List<RemoteProcess>,
    onKillProcess: (RemoteProcess) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("RUNNING UNIX PROCESSES", style = MaterialTheme.typography.labelMedium)
            Text("${processes.size} active tasks", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }

        // Unix top style process header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PID", modifier = Modifier.weight(1f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text("NAME", modifier = Modifier.weight(2.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text("CPU%", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text("MEM%", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text("KILL", modifier = Modifier.weight(1f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(processes) { proc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${proc.pid}", modifier = Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(proc.name, modifier = Modifier.weight(2.5f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${proc.cpuPercentage}%", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("${proc.memoryPercentage}%", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onKillProcess(proc) }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Kill Daemon Process",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun VirtualTerminalSubView(
    sshLines: List<com.example.models.SshOutputLine>,
    onSendCommand: (String) -> Unit
) {
    var cmdInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Keep scrolled to bottom whenever lines list updates
    LaunchedEffect(sshLines.size) {
        if (sshLines.isNotEmpty()) {
            listState.animateScrollToItem(sshLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sshLines) { line ->
                val lineCol = when (line.type) {
                    LogType.INPUT -> NeonCyan
                    LogType.ERROR -> Color.Red
                    LogType.SYSTEM -> TerminalYellow
                    else -> Color.White
                }
                Text(
                    text = line.message,
                    color = lineCol,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shell input prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", fontFamily = FontFamily.Monospace, color = NeonCyan)
            OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ssh_command_input"),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (cmdInput.isNotBlank()) {
                        onSendCommand(cmdInput)
                        cmdInput = ""
                    }
                })
            )
            IconButton(
                onClick = {
                    if (cmdInput.isNotBlank()) {
                        onSendCommand(cmdInput)
                        cmdInput = ""
                    }
                },
                modifier = Modifier.testTag("ssh_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Execute CLI prompt", tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
        }
    }
}
