// PinEntryActivity.kt
package com.example.e_clinic.UI.activities.doctor_screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.e_clinic.Services.PinManager
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.DoctorActivity

class DoctorPinEntryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoctorPinEntryScreen(
                onPinVerified = {
                    startActivity(Intent(this, DoctorActivity::class.java))
                    finish()
                },
                onSetupRequired = {
                    startActivity(Intent(this, SetDoctorPinAfterLoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun DoctorPinEntryScreen(
    onPinVerified: () -> Unit,
    onSetupRequired: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = PinManager(context)

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

    if (!showPinScreen){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    else{
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Welcome back!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter PIN code",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val filled = i <= pin.length
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (filled) MaterialTheme.colorScheme.primary else Color.LightGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 3) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        for (col in 1..3) {
                            val number = (row * 3 + col).toString()
                            DoctorNumberButtonSet(number) {
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
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(64.dp))

                    DoctorNumberButtonSet("0") {
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
                            .size(64.dp)
                            .clickable {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                    error = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            fontSize = 24.sp
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
                    .clickable {
                        onSetupRequired()
                    }
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun NumberButton(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
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

//class PinEntryActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: android.os.Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            PinEntryScreen(
//                onPinVerified = {
//                    startActivity(Intent(this, UserActivity::class.java))
//                    finish()
//                },
//                onSetupRequired = {
//                    startActivity(Intent(this, UserActivity::class.java))
//                    finish()
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun PinEntryScreen(
//    onPinVerified: () -> Unit,
//    onSetupRequired: () -> Unit
//) {
//    val context = LocalContext.current
//    val auth = FirebaseAuth.getInstance()
//    val db = FirebaseFirestore.getInstance()
//    val currentUser = auth.currentUser
//
//    // Check if user needs PIN setup
//    LaunchedEffect(key1 = currentUser) {
//        if (currentUser != null) {
//            db.collection("users").document(currentUser.uid).get()
//                .addOnSuccessListener { doc ->
//                    val hasSetPin = doc.getBoolean("hasSetPin") ?: false
//                    val rememberDevice = doc.getBoolean("rememberDevice") ?: false
//
//                    if (!hasSetPin) {
//                        // First login - redirect to main activity for PIN setup
//                        onSetupRequired()
//                    } else if (!rememberDevice) {
//                        // PIN exists but device not remembered - require full login
//                        auth.signOut()
//                        context.startActivity(Intent(context, UserLogInActivity::class.java))
//                        (context as Activity).finish()
//                    }
//                }
//        } else {
//            // No logged in user - go to login
//            context.startActivity(Intent(context, UserLogInActivity::class.java))
//            (context as Activity).finish()
//        }
//    }
//
//    var pin by remember { mutableStateOf("") }
//    var error by remember { mutableStateOf<String?>(null) }
//
//    Column(Modifier.fillMaxSize().padding(16.dp)) {
//        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall)
//        Spacer(modifier = Modifier.height(16.dp))
//        TextField(
//            value = pin,
//            onValueChange = {
//                if (it.length <= 4) pin = it
//                error = null
//                // Auto-submit when 4 digits entered
//                if (it.length == 4) verifyPin(it, onPinVerified) { error = it }
//            },
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.NumberPassword,
//                imeAction = ImeAction.Done
//            ),
//            visualTransformation = PasswordVisualTransformation()
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = {
//            if (pin.length == 4) {
//                verifyPin(pin, onPinVerified) { error = it }
//            } else {
//                error = "PIN must be 4 digits"
//            }
//        }) {
//            Text("Verify")
//        }
//        error?.let {
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(it, color = MaterialTheme.colorScheme.error)
//        }
//
//        // Full login option
//        Spacer(modifier = Modifier.height(24.dp))
//        OutlinedButton(
//            onClick = {
//                auth.signOut()
//                context.startActivity(Intent(context, UserLogInActivity::class.java))
//                (context as Activity).finish()
//            }
//        ) {
//            Text("Use Email Login")
//        }
//    }
//}
//
//fun verifyPin(inputPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
//    val currentUser = FirebaseAuth.getInstance().currentUser
//    val db = FirebaseFirestore.getInstance()
//
//    if (inputPin.length != 4) {
//        onError("PIN must be 4 digits")
//        return
//    }
//
//    db.collection("users").document(currentUser!!.uid).get()
//        .addOnSuccessListener { doc ->
//            when {
//                !doc.exists() -> onError("User data not found")
//                doc.getString("pinCode") == hashPin(inputPin) -> {
//                    // Mark device as remembered after successful PIN verification
//                    db.collection("users").document(currentUser.uid)
//                        .update("rememberDevice", true)
//                        .addOnSuccessListener { onSuccess() }
//                }
//                else -> onError("Incorrect PIN")
//            }
//        }
//        .addOnFailureListener { onError("Database error") }
//}