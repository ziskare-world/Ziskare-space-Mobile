package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.StorageItem
import com.example.viewmodel.ApiState
import com.example.viewmodel.ZiskareViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: ZiskareViewModel,
    modifier: Modifier = Modifier
) {
    val storageListState by viewModel.storageList.collectAsState()
    val storageStatusState by viewModel.storageStatus.collectAsState()
    val storageSettingsState by viewModel.storageSettings.collectAsState()

    var activeStorageType by remember { mutableStateOf("local") } // "local" or "drive"
    var currentPath by remember { mutableStateOf("/") }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var activeRenameTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showUploadSimulatedDialog by remember { mutableStateOf(false) }
    var showQuotaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeStorageType, currentPath) {
        viewModel.fetchStorageList(activeStorageType, currentPath)
    }

    LaunchedEffect(Unit) {
        viewModel.fetchStorageStatus()
        viewModel.fetchStorageSettings()
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Left Column: Quotas and Mount States
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                "STORAGE CHASSIS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // STORAGE MOUNT CHOOSERS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { activeStorageType = "local"; currentPath = "/" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeStorageType == "local") NeonCyan else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = "Local Mount", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LOCAL", fontSize = 11.sp)
                }
                Button(
                    onClick = { activeStorageType = "drive"; currentPath = "/" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeStorageType == "drive") RoyalPurple else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Cloud Mount", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DRIVE", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OVERALL HEALTH CAPACITIES OR QUOTAS
            Text("HEALTH STATUS", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = storageStatusState) {
                is ApiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally))
                is ApiState.Success -> {
                    val info = state.data
                    val usedSpace = info.usedSpaceBytes ?: 0L
                    val totalSpace = info.totalSpaceBytes ?: 1024L
                    val pct = (usedSpace.toDouble() / totalSpace.toDouble()).coerceIn(0.0, 1.0)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usage Space", fontSize = 11.sp, color = Color.Gray)
                                Text("${(pct * 100).toInt()}%", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = pct.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Total Cap: ${totalSpace / 1024 / 1024} MB",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray
                            )
                            Text(
                                "Active Chassis Node: ${info.health ?: "GOOD"}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SystemSuccess
                            )
                        }
                    }
                }
                else -> {
                    // Fallback visual
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Mock Mount Cap: 4.8 GB / 10 GB", fontSize = 11.sp)
                            LinearProgressIndicator(progress = 0.48f, modifier = Modifier.fillMaxWidth().height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showQuotaDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Edit Limits", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("QUOTA CONFIGS", fontSize = 11.sp)
            }
        }

        // Right Column: Explorer Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "FILE EXPLORER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "PATH: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            currentPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.fetchStorageList(activeStorageType, currentPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-list paths", tint = NeonCyan)
                    }
                    Button(
                        onClick = { showCreateFolderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add dir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NEW DIRECTORY", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showUploadSimulatedDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UPLOAD", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upper Parent Directory bar
            if (currentPath != "/") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable {
                            val parts = currentPath.split("/").filter { it.isNotEmpty() }
                            val newPath = "/" + parts.dropLast(1).joinToString("/")
                            currentPath = if (newPath == "//") "/" else newPath
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back dir", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(".. [UP TO PARENT DIRECTORY]", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Storage listing files items
            when (val state = storageListState) {
                is ApiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
                is ApiState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No files or folders found here. Trigger uploads or make folders above.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(list) { item ->
                                val isDir = item.type == "folder" || item.type == "directory"
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isDir) {
                                                currentPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = "Chassis Type",
                                            tint = if (isDir) NeonCyan else Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                if (isDir) "Subdirectory Folder" else "Capacity: ${item.sizeBytes ?: 0} Bytes | Modified: ${item.lastModified ?: "Unknown"}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                activeRenameTarget = item
                                                showRenameDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename item", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteStorageItem(activeStorageType, item.path, currentPath)
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = SystemError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is ApiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Active partition disconnect: ${state.message}", color = SystemError)
                    }
                }
                else -> {}
            }
        }
    }

    // Interactive Dialog System
    if (showCreateFolderDialog) {
        var folderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("CREATE SYSTEM SUBDIRECTORY") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Subfolder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotEmpty()) {
                            viewModel.createStorageFolder(activeStorageType, currentPath, folderNameInput)
                        }
                        showCreateFolderDialog = false
                    }
                ) { Text("PROVISION") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showRenameDialog && activeRenameTarget != null) {
        var newNameInput by remember { mutableStateOf(activeRenameTarget!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("RENAME CHASSIS ARTIFACT") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("New Identifier Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotEmpty()) {
                            viewModel.renameStorageItem(activeStorageType, activeRenameTarget!!.path, newNameInput, currentPath)
                        }
                        showRenameDialog = false
                    }
                ) { Text("RENAME") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showUploadSimulatedDialog) {
        var simulatedFileName by remember { mutableStateOf("system_report.txt") }
        var simulatedContent by remember { mutableStateOf("SYSTEM LOG REPORT V1.0.0 OK") }
        AlertDialog(
            onDismissRequest = { showUploadSimulatedDialog = false },
            title = { Text("MULTIPART UPLOAD SIMULATOR") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Writes a temporary operational cache file on the mobile node and performs a direct, real multipart POST payload upload.", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = simulatedFileName,
                        onValueChange = { simulatedFileName = it },
                        label = { Text("File Name Identifier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = simulatedContent,
                        onValueChange = { simulatedContent = it },
                        label = { Text("File Data Payload (Raw)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (simulatedFileName.isNotEmpty()) {
                            // Write file locally.
                            try {
                                val tempDir = File(viewModel.getApplication<android.app.Application>().cacheDir, "sim_uploads")
                                if (!tempDir.exists()) tempDir.mkdirs()
                                val uploadSourceFile = File(tempDir, simulatedFileName)
                                uploadSourceFile.writeText(simulatedContent)

                                viewModel.uploadStorageFile(
                                    localFile = uploadSourceFile,
                                    type = activeStorageType,
                                    path = currentPath,
                                    userId = "admin_client_upload"
                                )
                            } catch (e: Exception) {
                                // Handled safely
                            }
                        }
                        showUploadSimulatedDialog = false
                    }
                ) { Text("INJECT MULTIPART FILE") }
            },
            dismissButton = {
                TextButton(onClick = { showUploadSimulatedDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showQuotaDialog) {
        var maxLimitMgmt by remember { mutableStateOf("104857600") } // 100 MB default
        var baseMountPath by remember { mutableStateOf("/var/ziskare/data") }
        AlertDialog(
            onDismissRequest = { showQuotaDialog = false },
            title = { Text("CHASSIS CONFIGURATION PARAMS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxLimitMgmt,
                        onValueChange = { maxLimitMgmt = it },
                        label = { Text("Max Quota (Bytes)") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = baseMountPath,
                        onValueChange = { baseMountPath = it },
                        label = { Text("Base UNIX Local Path") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBytes = maxLimitMgmt.toLongOrNull() ?: 10000000L
                        viewModel.updateStorageSettings(
                            com.example.data.api.StorageSettings(
                                totalLimitBytes = parsedBytes,
                                baseLocalPath = baseMountPath,
                                driveSyncEnabled = true
                            )
                        )
                        showQuotaDialog = false
                    }
                ) { Text("SAVE LIMITS") }
            },
            dismissButton = {
                TextButton(onClick = { showQuotaDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
