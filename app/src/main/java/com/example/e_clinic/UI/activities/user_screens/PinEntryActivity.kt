// PinEntryActivity.kt
package com.example.e_clinic.UI.activities.user_screens

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricPrompt
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.e_clinic.Services.PinManager
import com.example.e_clinic.UI.activities.LogInActivity
import com.example.e_clinic.UI.activities.user_screens.user_activity.UserActivity
import com.example.e_clinic.UI.theme.EClinicTheme

class PinEntryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EClinicTheme {
            PinEntryScreen(
                onPinVerified = {
                    startActivity(Intent(this, UserActivity::class.java))
                    finish()
                },
                onSetupRequired = {
                    startActivity(Intent(this, SetPinAfterLoginActivity::class.java))
                    finish()
                }
            )
        }}
    }
}

@Composable
fun PinEntryScreen(
    onPinVerified: () -> Unit,
    onSetupRequired: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = PinManager(context)

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showPinScreen by remember { mutableStateOf(false) }
    var forgotPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showBiometricPrompt(context) { success, _ ->
            if (success) {
                onPinVerified()
            } else {
                showPinScreen = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (!showPinScreen) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Welcome back!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Enter PIN code",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val filled = i <= pin.length
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (filled) MaterialTheme.colorScheme.primary else Color.LightGray)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0 until 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 1..3) {
                                val number = (row * 3 + col).toString()
                                NumberButtonSet(
                                    number = number,
                                    size = 80.dp
                                ) {
                                    if (pin.length < 4) {
                                        pin += number
                                        error = null
                                        if (pin.length == 4) {
                                            if (pinManager.validatePin(pin)) {
                                                onPinVerified()
                                            } else {
                                                error = "Incorrect PIN"
                                                pin = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(80.dp))
                        NumberButtonSet(
                            number = "0",
                            size = 80.dp
                        ) {
                            if (pin.length < 4) {
                                pin += "0"
                                error = null
                                if (pin.length == 4) {
                                    if (pinManager.validatePin(pin)) {
                                        onPinVerified()
                                    } else {
                                        error = "Incorrect PIN"
                                        pin = ""
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (pin.isNotEmpty()) {
                                        pin = pin.dropLast(1)
                                        error = null
                                    }
                                }
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⌫",
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "Forgot PIN code?",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { forgotPassword = true }
                        .padding(16.dp)
                )
            }
        }
    }
    if (forgotPassword) {
        LaunchedEffect(Unit) {
            val pinManager = PinManager(context)
            pinManager.clearPin() // Clear the current password
            context.startActivity(Intent(context, LogInActivity::class.java))
        }
    }
}

@Composable
fun NumberButtonSet(number: String, size: Dp = 64.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}


fun showBiometricPrompt(
    context: Context, onAuthenticationResult: (Boolean, Boolean) -> Unit
) {
    var failureCount = 0
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticationResult(true, false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                 onAuthenticationResult(false, true) // Show PIN entry screen

            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onAuthenticationResult(false, true) // Handle cancel action
                } else {
                    onAuthenticationResult(false, false)
                }
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Use your fingerprint or face to authenticate")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}