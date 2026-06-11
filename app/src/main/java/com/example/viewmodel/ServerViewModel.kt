package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.models.LogType
import com.example.models.RemoteProcess
import com.example.models.ResourceHistoryPoint
import com.example.models.SshOutputLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ServerRepository(db)

    // Current app state
    val servers = repository.allServers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logs = repository.recentLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alerts = repository.allAlerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unreadAlerts = repository.unreadAlerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active session target
    private val _selectedServer = MutableStateFlow<ServerEntity?>(null)
    val selectedServer: StateFlow<ServerEntity?> = _selectedServer.asStateFlow()

    // Real-time server resource stream
    private val _resourceHistory = MutableStateFlow<List<ResourceHistoryPoint>>(emptyList())
    val resourceHistory: StateFlow<List<ResourceHistoryPoint>> = _resourceHistory.asStateFlow()

    // Real-time processes
    private val _processes = MutableStateFlow<List<RemoteProcess>>(emptyList())
    val processes: StateFlow<List<RemoteProcess>> = _processes.asStateFlow()

    // Remote Terminal Output Stream
    private val _sshLines = MutableStateFlow<List<SshOutputLine>>(emptyList())
    val sshLines: StateFlow<List<SshOutputLine>> = _sshLines.asStateFlow()

    // Current simulated user
    private val _currentUser = MutableStateFlow("admin_user")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    // Active simulated server polling job
    private var telemetryJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed DB with some useful starter admin variables if completely empty
            repository.allServers.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedData()
                }
            }
        }
    }

    private suspend fun seedData() {
        // Core primary system admin accounts
        repository.insertUser(UserEntity(username = "admin_user", role = "Administrator", email = "admin@system.monitor", mfaEnabled = true))
        repository.insertUser(UserEntity(username = "operator_alex", role = "Operator", email = "alex@system.monitor"))
        repository.insertUser(UserEntity(username = "security_auditor", role = "Read-Only Viewer", email = "audit@system.monitor"))

        // Add typical cloud server virtual clusters
        val serverAId = repository.insertServer(
            ServerEntity(
                name = "Web-Prod-US-East",
                host = "104.244.42.1",
                username = "root",
                authType = "SSH Key",
                status = "Warning",
                cpuUsage = 82f,
                ramUsage = 71f,
                diskUsage = 58f,
                activeAlertsCount = 1,
                colorHex = "#E91E63" // Pink
            )
        )

        val serverBId = repository.insertServer(
            ServerEntity(
                name = "Kube-Cluster-Primary",
                host = "192.168.12.10",
                username = "ubuntu",
                status = "Online",
                cpuUsage = 41f,
                ramUsage = 55f,
                diskUsage = 42f,
                activeAlertsCount = 0,
                colorHex = "#2196F3" // Blue
            )
        )

        val serverCId = repository.insertServer(
            ServerEntity(
                name = "Database-Postgres-Replica",
                host = "10.0.8.44",
                username = "postgres",
                status = "Online",
                cpuUsage = 25f,
                ramUsage = 88f,
                diskUsage = 79f,
                activeAlertsCount = 0,
                colorHex = "#4CAF50" // Green
            )
        )

        // Seed logs
        repository.insertLog(ActivityLogEntity(username = "admin_user", serverName = "Web-Prod-US-East", action = "Initialized secure SSH connection with server profile"))
        repository.insertLog(ActivityLogEntity(username = "system", serverName = "Web-Prod-US-East", action = "CPU Alert threshold reached above 80%", status = "Warning"))
        repository.insertLog(ActivityLogEntity(username = "operator_alex", serverName = "Kube-Cluster-Primary", action = "Restarted system service: docker.service"))

        // Seed alert notification events too
        repository.insertAlert(
            AlertEntity(
                serverId = serverAId.toInt(),
                serverName = "Web-Prod-US-East",
                metric = "CPU",
                message = "CPU Usage reached 82% on root worker thread index 3",
                level = "Warning"
            )
        )
    }

    // Connect to specific server and initialize telemetry stream
    fun selectServer(server: ServerEntity?) {
        _selectedServer.value = server
        telemetryJob?.cancel()
        _sshLines.value = emptyList()

        if (server == null) {
            _resourceHistory.value = emptyList()
            _processes.value = emptyList()
            return
        }

        // Add standard connection system entry
        _sshLines.value = listOf(
            SshOutputLine("Connecting to ${server.username}@${server.host}:${server.port}...", LogType.SYSTEM),
            SshOutputLine("SSH Handshake completed. Using cryptographic RSA certificate authentication...", LogType.SYSTEM),
            SshOutputLine("Welcome to ${server.name} (GNU/Linux Kernel-V5.15-generic arm64)", LogType.OUTPUT),
            SshOutputLine("Last Login: Thursday Jun 11 01:12:30 UTC from admin.ip.client", LogType.OUTPUT),
            SshOutputLine("Type 'help' or commands to execute securely.", LogType.SYSTEM)
        )

        viewModelScope.launch {
            repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = server.name, action = "Established high-level monitoring session"))
        }

        // Generate baseline server performance stats history
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        var initList = mutableListOf<ResourceHistoryPoint>()
        var baselineTime = System.currentTimeMillis() - 100000
        for (i in 0 until 12) {
            baselineTime += 8000
            val mockCpu = (server.cpuUsage + Random.nextInt(-10, 10)).coerceIn(10f, 99f)
            val mockRam = (server.ramUsage + Random.nextInt(-4, 4)).coerceIn(10f, 99f)
            initList.add(
                ResourceHistoryPoint(
                    timestamp = formatter.format(Date(baselineTime)),
                    cpu = mockCpu,
                    ram = mockRam,
                    networkIn = Random.nextFloat() * 4.5f,
                    networkOut = Random.nextFloat() * 12.8f
                )
            )
        }
        _resourceHistory.value = initList

        // Generate mock processes running on that server
        regenerateProcesses()

        // Continuous telemetry updates simulating live host metrics and triggers
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val currentHist = _resourceHistory.value.toMutableList()
                if (currentHist.size > 14) currentHist.removeAt(0)

                val activeServer = _selectedServer.value ?: break
                val latestCpu = (activeServer.cpuUsage + Random.nextInt(-15, 15)).coerceIn(10f, 98f)
                val latestRam = (activeServer.ramUsage + Random.nextInt(-5, 5)).coerceIn(10f, 98f)

                // Update server record status too in local SQLite database
                val updatedServer = activeServer.copy(
                    cpuUsage = latestCpu,
                    ramUsage = latestRam
                )
                repository.updateServer(updatedServer)
                _selectedServer.value = updatedServer

                currentHist.add(
                    ResourceHistoryPoint(
                        timestamp = formatter.format(Date()),
                        cpu = latestCpu,
                        ram = latestRam,
                        networkIn = Random.nextFloat() * 6f,
                        networkOut = Random.nextFloat() * 15f
                    )
                )
                _resourceHistory.value = currentHist

                // Randomly trigger an automated critical alert event simulation
                if (latestCpu > 85f && Random.nextFloat() > 0.6f) {
                    val alertMsg = "Critical spikes in CPU workload detected: Host $latestCpu%"
                    repository.insertAlert(
                        AlertEntity(
                            serverId = activeServer.id,
                            serverName = activeServer.name,
                            metric = "CPU",
                            message = alertMsg,
                            level = "Critical"
                        )
                    )
                    // Auto notify
                    _sshLines.value = _sshLines.value + SshOutputLine("[SYSTEM EXCEPTION] CPU spike warning logged: $alertMsg", LogType.ERROR)
                }

                // Minor random shuffle of running processes
                _processes.value = _processes.value.map { proc ->
                    if (Random.nextFloat() > 0.7f && proc.name != "systemd") {
                        proc.copy(
                            cpuPercentage = (proc.cpuPercentage + Random.nextInt(-8, 8)).coerceIn(0.1f, 95f),
                            memoryPercentage = (proc.memoryPercentage + Random.nextInt(-2, 2)).coerceIn(0.1f, 80f)
                        )
                    } else {
                        proc
                    }
                }
            }
        }
    }

    private fun regenerateProcesses() {
        _processes.value = listOf(
            RemoteProcess(1, "systemd", 0.1f, 1.2f, "root", "Running"),
            RemoteProcess(221, "nginx: master", 0.5f, 2.8f, "root", "Running"),
            RemoteProcess(222, "nginx: worker", 12.4f, 8.4f, "www-data", "Running"),
            RemoteProcess(1105, "postgres: writer", 2.2f, 18.2f, "postgres", "Sleeping"),
            RemoteProcess(1106, "postgres: wal receiver", 0.8f, 12.8f, "postgres", "Running"),
            RemoteProcess(5489, "node /app/index.js", 34.5f, 22.4f, "node", "Running"),
            RemoteProcess(6011, "ssh_daemon", 1.1f, 4.1f, "root", "Running"),
            RemoteProcess(6120, "python3 -m telemetry", 8.4f, 5.2f, "ubuntu", "Running"),
            RemoteProcess(7622, "cron_executor", 0.0f, 0.9f, "root", "Sleeping")
        ).sortedByDescending { it.cpuPercentage }
    }

    // Action execution on remote process management
    fun manageProcess(proc: RemoteProcess, action: String) {
        viewModelScope.launch {
            val serverName = _selectedServer.value?.name ?: "Unknown Server"
            repository.insertLog(
                ActivityLogEntity(
                    username = _currentUser.value,
                    serverName = serverName,
                    action = "Issued signal '$action' to remote PID: ${proc.pid} (${proc.name})"
                )
            )

            val successMsg = if (action == "KILL") {
                _processes.value = _processes.value.filter { it.pid != proc.pid }
                "Process PID ${proc.pid} forcefully terminated with SIGKILL"
            } else {
                _processes.value = _processes.value.map {
                    if (it.pid == proc.pid) it.copy(status = "Stopped", cpuPercentage = 0.0f) else it
                }
                "Process PID ${proc.pid} signaling state change: $action SUCCESS"
            }

            _sshLines.value = _sshLines.value + listOf(
                SshOutputLine("$ ${action.lowercase()} ${proc.pid}", LogType.INPUT),
                SshOutputLine(successMsg, LogType.SYSTEM)
            )
        }
    }

    // Interactive terminal line command receiver
    fun executeSshCommand(command: String) {
        val cmdClean = command.trim()
        val newLines = mutableListOf<SshOutputLine>()
        newLines.add(SshOutputLine("$ $cmdClean", LogType.INPUT))

        val server = _selectedServer.value
        val serverName = server?.name ?: "Unknown"

        viewModelScope.launch {
            repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = serverName, action = "Secure SSH prompt execution: '$cmdClean'"))
        }

        when {
            cmdClean.isEmpty() -> {}
            cmdClean == "help" -> {
                newLines.add(SshOutputLine("Available Remote Server Commands:\n" +
                        "  clear                      - Clear local virtual shell buffer\n" +
                        "  uname -a                   - Print host architecture and OS kernel logs\n" +
                        "  top                        - Show system utilization processes\n" +
                        "  service --status-all       - Query operational states of remote daemons\n" +
                        "  reboot                     - Instruct high-level server virtual shutdown override\n" +
                        "  sudo rm -rf /              - Secure threat simulated verification alert test", LogType.OUTPUT))
            }
            cmdClean == "clear" -> {
                _sshLines.value = emptyList()
                return
            }
            cmdClean == "uname -a" -> {
                newLines.add(SshOutputLine("Linux ${serverName.lowercase().replace("-", "")} 5.15.0-88-generic #98-Ubuntu SMP Mon Oct 2 15:18:56 UTC 2026 aarch64 aarch64 GNU/Linux", LogType.OUTPUT))
            }
            cmdClean == "top" -> {
                newLines.add(SshOutputLine("Tasks: 142 total,   2 running, 140 sleeping,   0 stopped,   0 zombie\n" +
                        "%Cpu(s):  18.4 us,   4.2 sy,   0.0 ni,  76.4 id,   1.0 wa\n" +
                        "MiB Mem :   7844.2 total,   2488.1 free,   3554.8 used,   1801.3 buff/cache", LogType.OUTPUT))
                _processes.value.take(5).forEach {
                    newLines.add(SshOutputLine("  PID: ${it.pid} \t CPU: ${it.cpuPercentage}% \t MEM: ${it.memoryPercentage}% \t CMD: ${it.name}", LogType.OUTPUT))
                }
            }
            cmdClean == "service --status-all" -> {
                newLines.add(SshOutputLine(" [ + ]  nginx web server controller\n" +
                        " [ + ]  postgresql database master daemon\n" +
                        " [ - ]  apache2 legacy service\n" +
                        " [ + ]  sshd secure shell engine\n" +
                        " [ + ]  systemd-journald logs sync daemon", LogType.OUTPUT))
            }
            cmdClean == "reboot" -> {
                newLines.add(SshOutputLine("WARNING: ROOT user triggered secure reboot parameters remotely override", LogType.ERROR))
                viewModelScope.launch {
                    repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = serverName, action = "Triggered server physical reboot directive", status = "Warning"))
                    if (server != null) {
                        repository.updateServer(server.copy(status = "Offline", cpuUsage = 0f, ramUsage = 0f))
                        _selectedServer.value = server.copy(status = "Offline", cpuUsage = 0f, ramUsage = 0f)
                    }
                    delay(3000)
                    newLines.add(SshOutputLine("Broadcasting remote system shutdown sequence. Connection terminated safely.", LogType.SYSTEM))
                    _sshLines.value = _sshLines.value + newLines
                }
                return
            }
            cmdClean.startsWith("sudo rm") || cmdClean.contains("rm -rf") -> {
                newLines.add(SshOutputLine("CRITICAL SECURITY VIOLATION: Access denied. Root protection mechanism successfully terminated illegal process. Alert generated and emailed to secure administration.", LogType.ERROR))
                viewModelScope.launch {
                    repository.insertAlert(
                        AlertEntity(
                            serverId = server?.id ?: 0,
                            serverName = serverName,
                            metric = "SSH",
                            message = "CRITICAL: Malicious system destructive terminal command 'rm -rf' attempted from terminal console by root.",
                            level = "Critical"
                        )
                    )
                }
            }
            else -> {
                newLines.add(SshOutputLine("exec: $command: Successful simulation of administrative execution log. Output cached.", LogType.OUTPUT))
            }
        }

        _sshLines.value = _sshLines.value + newLines
    }

    // Toggle specific server profile alerts notification rules
    fun createServer(name: String, host: String, port: Int, username: String, authType: String, colorHex: String) {
        viewModelScope.launch {
            val server = ServerEntity(
                name = name,
                host = host,
                port = port,
                username = username,
                authType = authType,
                cpuUsage = 0f,
                ramUsage = 0f,
                diskUsage = 0f,
                status = "Online",
                colorHex = colorHex
            )
            repository.insertServer(server)
            repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = name, action = "Added a new server connection registration"))
        }
    }

    // Interactive custom simulated events notification alerts trigger
    fun dismissAlert(alertId: Int) {
        viewModelScope.launch {
            repository.markAlertAsRead(alertId)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.markAllAlertsAsRead()
        }
    }

    // Manage simulated system administrator roles
    fun createUserAdminProfile(username: String, role: String, email: String, mfaEnabled: Boolean) {
        viewModelScope.launch {
            val dummySecret = if (mfaEnabled) generateBase32Secret(username) else ""
            val user = UserEntity(
                username = username,
                role = role,
                email = email,
                mfaEnabled = mfaEnabled,
                mfaSecret = dummySecret
            )
            repository.insertUser(user)
            repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = "Global", action = "Created administrative access account: $username ($role)"))
        }
    }

    fun deleteUserAdminProfile(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            repository.insertLog(ActivityLogEntity(username = _currentUser.value, serverName = "Global", action = "Revoked user access control rights for: ${user.username}"))
        }
    }

    private fun generateBase32Secret(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.take(10).joinToString("") { String.format("%02X", it) }
    }
}
