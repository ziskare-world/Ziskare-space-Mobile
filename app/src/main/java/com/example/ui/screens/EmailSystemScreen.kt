package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.EmailSettings
import com.example.data.api.SendEmailRequest
import com.example.viewmodel.ApiState
import com.example.viewmodel.ZiskareViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSystemScreen(
    viewModel: ZiskareViewModel,
    modifier: Modifier = Modifier
) {
    val emailSettingsState by viewModel.emailSettings.collectAsState()
    val emailTemplatesState by viewModel.emailTemplates.collectAsState()

    // SMTP settings fields
    var smtpHostInput by remember { mutableStateOf("") }
    var smtpPortInput by remember { mutableStateOf("587") }
    var smtpAuthUserInput by remember { mutableStateOf("") }
    var apiEndpointInput by remember { mutableStateOf("") }
    var secureSmtpChecked by remember { mutableStateOf(true) }

    // Instant send fields
    var emailToInput by remember { mutableStateOf("") }
    var emailSubjectInput by remember { mutableStateOf("") }
    var emailBodyInput by remember { mutableStateOf("") }
    var emailFromInput by remember { mutableStateOf("no-reply@ziskare.space") }
    var emailPriorityInput by remember { mutableStateOf("normal") } // "normal", "high", "low"

    // Templates Fetcher fields
    var templateDbInput by remember { mutableStateOf("ziskare_db") }
    var templateCollInput by remember { mutableStateOf("email_templates") }

    LaunchedEffect(Unit) {
        viewModel.fetchEmailSettings()
    }

    // Capture loaded settings to fill inputs
    LaunchedEffect(emailSettingsState) {
        (emailSettingsState as? ApiState.Success)?.data?.let { settings ->
            smtpHostInput = settings.smtpHost ?: ""
            smtpPortInput = (settings.smtpPort ?: 587).toString()
            smtpAuthUserInput = settings.authUser ?: ""
            apiEndpointInput = settings.apiEndpoint ?: ""
            secureSmtpChecked = settings.secure ?: true
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Left Column: SMPT / Transmission settings
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                "SMTP CONTROLLER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = emailSettingsState) {
                is ApiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = smtpHostInput,
                            onValueChange = { smtpHostInput = it },
                            label = { Text("SMTP Server Host") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = smtpPortInput,
                            onValueChange = { smtpPortInput = it },
                            label = { Text("SMTP Server Port") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = smtpAuthUserInput,
                            onValueChange = { smtpAuthUserInput = it },
                            label = { Text("Auth Username / API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = apiEndpointInput,
                            onValueChange = { apiEndpointInput = it },
                            label = { Text("External Webmail Endpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = secureSmtpChecked, onCheckedChange = { secureSmtpChecked = it })
                            Text("Use Secure Socket SSL/TLS", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                viewModel.updateEmailSettings(
                                    EmailSettings(
                                        smtpHost = smtpHostInput,
                                        smtpPort = smtpPortInput.toIntOrNull() ?: 587,
                                        secure = secureSmtpChecked,
                                        authUser = smtpAuthUserInput,
                                        apiEndpoint = apiEndpointInput
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OlympicYellowColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE SYSTEM SMTP CONFIG")
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Templates connector
            Text("TEMPLATES MONITOR", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = templateDbInput,
                onValueChange = { templateDbInput = it },
                label = { Text("Database") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = templateCollInput,
                onValueChange = { templateCollInput = it },
                label = { Text("Collection") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.loadTemplates(templateDbInput, templateCollInput) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FindInPage, contentDescription = "Templates load", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("FETCH TEMPLATES", fontSize = 11.sp)
            }
        }

        // Right Column: Broadcast screen and lists
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                "TRANSACTIONAL EMAIL TRANSMISSION",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Trigger immediate real-time corporate broadcasts or load database templates.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transmission inputs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = emailToInput,
                            onValueChange = { emailToInput = it },
                            label = { Text("Recipient Email (To)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = emailFromInput,
                            onValueChange = { emailFromInput = it },
                            label = { Text("Sender Domain Mask (From)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = emailSubjectInput,
                        onValueChange = { emailSubjectInput = it },
                        label = { Text("Broadcast Subject Matter") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = emailBodyInput,
                        onValueChange = { emailBodyInput = it },
                        label = { Text("Html Markdown / Text Body Payload") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Transmission Urgency:", fontSize = 11.sp, color = Color.Gray)
                            AssistChip(
                                onClick = { emailPriorityInput = "high" },
                                label = { Text("HIGH", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (emailPriorityInput == "high") SystemError else Color.Gray
                                )
                            )
                            AssistChip(
                                onClick = { emailPriorityInput = "normal" },
                                label = { Text("NORMAL", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (emailPriorityInput == "normal") NeonCyan else Color.Gray
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (emailToInput.isNotEmpty() && emailSubjectInput.isNotEmpty()) {
                                    viewModel.sendEmailPayload(
                                        SendEmailRequest(
                                            to = emailToInput,
                                            subject = emailSubjectInput,
                                            text = emailBodyInput,
                                            html = emailBodyInput,
                                            from = emailFromInput,
                                            priority = emailPriorityInput
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DELIVER QUEUE BROADCAST")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Templates list rendering
            Text("TEMPLATES LISTING", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            when (val state = emailTemplatesState) {
                is ApiState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
                is ApiState.Success -> {
                    val templates = state.data
                    if (templates.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No templates fetched yet.", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(templates) { template ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            emailSubjectInput = template.subject
                                            emailBodyInput = template.content
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(template.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text("Subject: ${template.subject}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Pick template", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                is ApiState.Error -> Text("Failed to parse template indices: ${state.message}", fontSize = 11.sp, color = SystemError)
                else -> {
                    Text("Database templates not queried. Fetch using the sidebar left controls.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

private val OlympicYellowColor = Color(0xFFEAB308)
