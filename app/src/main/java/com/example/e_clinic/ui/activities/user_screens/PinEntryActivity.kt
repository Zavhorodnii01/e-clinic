package com.example.e_clinic.ui.activities.user_screens

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.e_clinic.services.functions.hashPin
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PinEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PinEntryScreen(
                onPinVerified = {
                    startActivity(Intent(this, UserActivity::class.java))
                    finish()
                },
                onSetupRequired = {
                    startActivity(Intent(this, UserActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun PinEntryScreen(
    onPinVerified: () -> Unit,
    onSetupRequired: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Check if user needs PIN setup
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val hasSetPin = doc.getBoolean("hasSetPin") ?: false
                    val rememberDevice = doc.getBoolean("rememberDevice") ?: false

                    if (!hasSetPin) {
                        // First login - redirect to main activity for PIN setup
                        onSetupRequired()
                    } else if (!rememberDevice) {
                        // PIN exists but device not remembered - require full login
                        auth.signOut()
                        context.startActivity(Intent(context, UserLogInActivity::class.java))
                        (context as Activity).finish()
                    }
                }
        } else {
            // No logged in user - go to login
            context.startActivity(Intent(context, UserLogInActivity::class.java))
            (context as Activity).finish()
        }
    }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4) pin = it
                error = null
                // Auto-submit when 4 digits entered
                if (it.length == 4) verifyPin(it, onPinVerified) { error = it }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (pin.length == 4) {
                verifyPin(pin, onPinVerified) { error = it }
            } else {
                error = "PIN must be 4 digits"
            }
        }) {
            Text("Verify")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Full login option
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                auth.signOut()
                context.startActivity(Intent(context, UserLogInActivity::class.java))
                (context as Activity).finish()
            }
        ) {
            Text("Use Email Login")
        }
    }
}

fun verifyPin(inputPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    if (inputPin.length != 4) {
        onError("PIN must be 4 digits")
        return
    }

    db.collection("users").document(currentUser!!.uid).get()
        .addOnSuccessListener { doc ->
            when {
                !doc.exists() -> onError("User data not found")
                doc.getString("pinCode") == hashPin(inputPin) -> {
                    // Mark device as remembered after successful PIN verification
                    db.collection("users").document(currentUser.uid)
                        .update("rememberDevice", true)
                        .addOnSuccessListener { onSuccess() }
                }
                else -> onError("Incorrect PIN")
            }
        }
        .addOnFailureListener { onError("Database error") }
}