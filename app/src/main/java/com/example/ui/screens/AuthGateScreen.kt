package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError
import com.example.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthGateScreen(
    viewModel: ServerViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputCode by remember { mutableStateOf("") }
    var gateState by remember { mutableStateOf("FINGERPRINT") } // FINGERPRINT, MFA_CODE, FAILED, ACCESS_GRANTED
    var errorMsg by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Rapid simulated scan
    var scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(gateState) {
        if (gateState == "FINGERPRINT") {
            while (true) {
                scaleAnim.animateTo(1.15f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                scaleAnim.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Securing system admin payload",
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SECURE PROTOCOL INTERACTION",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "System Monitor Admin Console Gate Exception",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AnimatedContent(
                targetState = gateState,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { state ->
                when (state) {
                    "FINGERPRINT" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.12f))
                                    .clickable {
                                        coroutineScope.launch {
                                            gateState = "MFA_CODE"
                                        }
                                    }
                                    .padding(16.dp)
                                    .testTag("biometric_sensor"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Scan Fingerprint to Access Secure Systems",
                                    tint = NeonCyan,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .size(with(androidx.compose.ui.platform.LocalDensity.current) { (64 * scaleAnim.value).dp })
                                )
                            }
                            Text(
                                "TOUCH THE CAPACITIVE BIOMETRIC SENSOR",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                            TextButton(onClick = { gateState = "MFA_CODE" }) {
                                Text("BYPASS TO MULTI-FACTOR TOKEN COMPLIANCE")
                            }
                        }
                    }
                    "MFA_CODE" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "MULTI-FACTOR OTP TOKEN CODE CHALLLENGE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { if (it.length <= 6) inputCode = it },
                                label = { Text("6-Digit MFA Verification Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp,
                                    fontSize = 18.sp
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .testTag("mfa_input_field")
                            )

                            Button(
                                onClick = {
                                    if (inputCode == "123456" || inputCode == "000000" || inputCode.length >= 4) {
                                        gateState = "ACCESS_GRANTED"
                                        coroutineScope.launch {
                                            delay(1000)
                                            onAuthSuccess()
                                        }
                                    } else {
                                        errorMsg = "INVALID OTP VALIDATION TOKEN: TRY AGAIN"
                                        inputCode = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .testTag("mfa_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("VALIDATE HOST ACCESS CLIENT")
                            }

                            if (errorMsg.isNotBlank()) {
                                Text(
                                    errorMsg,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SystemError
                                )
                            }

                            TextButton(onClick = { gateState = "FINGERPRINT" }) {
                                Text("RETURN TO BIOMETRICS SCAN")
                            }
                        }
                    }
                    "ACCESS_GRANTED" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = SystemSuccess)
                            Text(
                                "CRYPTOGRAPHIC SECURE HANDSHAKE SUCCESSFUL",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SystemSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}
