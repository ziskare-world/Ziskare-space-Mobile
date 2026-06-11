package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityLogEntity
import com.example.data.UserEntity
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.viewmodel.ServerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Access Control, 1 = Activity Logs
    var showAddUserDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SYSTEM ACCESS AND AUDITING",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tab subrow selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabHeader(
                title = "USER CONTROL",
                isActive = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_user_control")
            )
            TabHeader(
                title = "ACTIVITY AUDIT LOGS",
                isActive = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_activity_logs")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == 0) {
            UserControlView(
                users = users,
                onCreateUserClick = { showAddUserDialog = true },
                onDeleteUser = { viewModel.deleteUserAdminProfile(it) }
            )
        } else {
            ActivityAuditsView(logs = logs)
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, role, email, mfa ->
                viewModel.createUserAdminProfile(username, role, email, mfa)
                showAddUserDialog = false
            }
        )
    }
}

@Composable
fun UserControlView(
    users: List<UserEntity>,
    onCreateUserClick: () -> Unit,
    onDeleteUser: (UserEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Granular Access Keys",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Button(
                onClick = onCreateUserClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("add_user_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add administrator")
                Spacer(modifier = Modifier.width(6.dp))
                Text("ADD ADMIN")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users) { usr ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("user_card_${usr.username}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(usr.username, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = RoyalPurple.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = usr.role,
                                        color = RoyalPurple,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(usr.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (usr.mfaEnabled) Icons.Default.VpnKey else Icons.Default.LockOpen,
                                    contentDescription = "MFA state",
                                    tint = if (usr.mfaEnabled) SystemSuccess else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (usr.mfaEnabled) "2FA/MFA OTP ACTIVE: ${usr.mfaSecret}" else "MFA AUTH BYPASS DISABLED",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (usr.mfaEnabled) SystemSuccess else Color.Gray
                                )
                            }
                        }

                        // Protect main admin from casual delete simulation
                        if (usr.username != "admin_user") {
                            IconButton(onClick = { onDeleteUser(usr) }, modifier = Modifier.testTag("delete_user_${usr.username}")) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke User Access", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityAuditsView(logs: List<ActivityLogEntity>) {
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Consolidated Node Activity Audit Trail (Local SQLite Saved)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (log.status == "Success") SystemSuccess else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log.username.uppercase(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "@ ${log.serverName}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.action, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = timeFormatter.format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Operator") }
    var mfaEnabled by remember { mutableStateOf(true) }

    val rolesList = listOf("Administrator", "Operator", "Read-Only Viewer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PROVISION ACCESS PERMISSIONS") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth().testTag("add_user_username_field")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Corporate Email Address") },
                    modifier = Modifier.fillMaxWidth().testTag("add_user_email_field")
                )

                Text("Assigned Role Matrix Group:", fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rolesList.forEach { r ->
                        val isSel = role == r
                        val containerCol = if (isSel) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(containerCol)
                                .clickable { role = r }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r, fontSize = 10.sp, color = if (isSel) NeonCyan else Color.Gray)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("De-risk OTP Multi-Factor Authentication", fontSize = 12.sp)
                    Switch(
                        checked = mfaEnabled,
                        onCheckedChange = { mfaEnabled = it },
                        modifier = Modifier.testTag("mfa_toggle_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, role, email, mfaEnabled) },
                modifier = Modifier.testTag("submit_add_user")
            ) {
                Text("COMMIT POLICY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
