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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.RegisterSimpleRequest
import com.example.viewmodel.ApiState
import com.example.viewmodel.ZiskareViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: ZiskareViewModel,
    modifier: Modifier = Modifier
) {
    val usersState by viewModel.apiUsers.collectAsState()

    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showAssignQuotaDialog by remember { mutableStateOf(false) }
    var selectedUserIdForQuota by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadApiUsers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "USER MANAGEMENT PANEL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Manage user access keys, assign cloud space, and allocate role credentials.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.loadApiUsers() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Users", tint = NeonCyan)
                }
                Button(
                    onClick = { showCreateUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add user", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CREATE NEW USER")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = usersState) {
            is ApiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
            is ApiState.Success -> {
                val list = state.data
                if (list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No corporate users registered yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list) { user ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (user.role == "admin") SystemSuccess.copy(alpha = 0.2f) else RoyalPurple.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "User",
                                                tint = if (user.role == "admin") SystemSuccess else RoyalPurple
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("@${user.username} | ${user.email}", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            Row(
                                                modifier = Modifier.padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(user.role.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                selectedUserIdForQuota = user.id
                                                showAssignQuotaDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Cloud, contentDescription = "Limit logo", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("LIMITS", fontSize = 10.sp)
                                        }

                                        // Role promotion controls
                                        IconButton(
                                            onClick = {
                                                user.id?.let {
                                                    val newRole = if (user.role == "admin") "user" else "admin"
                                                    viewModel.updateApiUserPermissions(it, newRole)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = "Switch Roles", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                user.id?.let { viewModel.deleteApiUserAccount(it) }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Revoke user", tint = SystemError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is ApiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Connection failure: ${state.message}", color = SystemError)
                }
            }
            else -> {}
        }
    }

    // Modal Admin dialogs
    if (showCreateUserDialog) {
        var createName by remember { mutableStateOf("") }
        var createUsername by remember { mutableStateOf("") }
        var createEmail by remember { mutableStateOf("") }
        var createPassword by remember { mutableStateOf("") }
        var createRoleSelected by remember { mutableStateOf("user") }

        AlertDialog(
            onDismissRequest = { showCreateUserDialog = false },
            title = { Text("PROVISION NEW WORKSPACE CLIENT") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Full Name Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createUsername,
                        onValueChange = { createUsername = it },
                        label = { Text("Username handle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createEmail,
                        onValueChange = { createEmail = it },
                        label = { Text("Corporate Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = { createPassword = it },
                        label = { Text("Password Protection") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Initial Authority Role:", fontSize = 11.sp, color = Color.Gray)
                        AssistChip(
                            onClick = { createRoleSelected = "admin" },
                            label = { Text("ADMIN", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = if (createRoleSelected == "admin") SystemSuccess else Color.Gray
                            )
                        )
                        AssistChip(
                            onClick = { createRoleSelected = "user" },
                            label = { Text("STANDARD USER", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = if (createRoleSelected == "user") RoyalPurple else Color.Gray
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createName.isNotEmpty() && createUsername.isNotEmpty()) {
                            viewModel.createApiUser(
                                RegisterSimpleRequest(
                                    name = createName,
                                    username = createUsername,
                                    email = createEmail,
                                    password = createPassword,
                                    role = createRoleSelected
                                )
                            )
                        }
                        showCreateUserDialog = false
                    }
                ) { Text("PROVISION") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showAssignQuotaDialog && selectedUserIdForQuota != null) {
        var userQuotaBytes by remember { mutableStateOf("10485760") } // 10 MB allocation default
        AlertDialog(
            onDismissRequest = { showAssignQuotaDialog = false },
            title = { Text("ALLOCATE USER STORAGE CAPACITY") },
            text = {
                OutlinedTextField(
                    value = userQuotaBytes,
                    onValueChange = { userQuotaBytes = it },
                    label = { Text("Maximum Quota (Bytes)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bytesVal = userQuotaBytes.toLongOrNull() ?: 10000000L
                        viewModel.setUserStorageQuota(selectedUserIdForQuota!!, bytesVal)
                        showAssignQuotaDialog = false
                    }
                ) { Text("ALLOCATE SPACE") }
            },
            dismissButton = {
                TextButton(onClick = { showAssignQuotaDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
