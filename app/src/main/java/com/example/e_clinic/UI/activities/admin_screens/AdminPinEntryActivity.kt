package com.example.e_clinic.UI.activities.admin_screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.e_clinic.Services.PinManager
import com.example.e_clinic.UI.activities.admin_screens.admin_activity.AdminActivity
import com.example.e_clinic.UI.activities.LogInActivity
import com.example.e_clinic.UI.activities.user_screens.NumberButtonSet
import com.example.e_clinic.UI.theme.EClinicTheme
import kotlin.collections.plusAssign
import kotlin.compareTo


class AdminPinEntryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
            AdminPinEntryScreen(
                onPinVerified = {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish()
                },
                onSetupRequired = {
                    startActivity(Intent(this, SetAdminPinAfterLoginActivity::class.java))
                    finish()
                }
            )
        }
        }
    }
}


@Composable
fun AdminPinEntryScreen(
    onPinVerified: () -> Unit,
    onSetupRequired: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = PinManager(context)
    var forgotPassword by remember { mutableStateOf(false) }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    //var biometricFailures by remember { mutableStateOf(0) }
    var showPinScreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showBiometricPrompt(context){
                success, canceled ->
            if (success) {
                onPinVerified()
            }
            else {
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
