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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.e_clinic.services.functions.hashPin
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity

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

    var showSetupDialog by remember { mutableStateOf(false) }
    val currentUser = auth.currentUser

    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.getBoolean("hasSetPin") != true) {
                        showSetupDialog = true
                    }
                }
        }
    }

    if (showSetupDialog) {
        SetPinDialog(
            onPinSet = { pin ->
                val hashedPin = pin?.let { hashPin(it) }
                db.collection("users").document(currentUser!!.uid)
                    .update("pinCode", hashedPin, "hasSetPin", true)
                    .addOnSuccessListener {
                        onPinVerified()
                    }
            },
            onSkip = {
                db.collection("users").document(currentUser!!.uid)
                    .update("hasSetPin", true) // Oznaczamy że widział propozycję
                onSetupRequired()
            }
        )
    } else {
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
        }
    }
}

@Composable
fun SetPinDialog(
    onPinSet: (String) -> Unit,
    onSkip: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Secure Your Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) pin = it
                        error = null
                    },
                    label = { Text("4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pin.length == 4) {
                    onPinSet(pin)
                } else {
                    error = "Enter 4-digit PIN"
                }
            }) {
                Text("Set PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }
        }
    )
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
                doc.getString("pinCode") == hashPin(inputPin) -> onSuccess()
                else -> onError("Incorrect PIN")
            }
        }
        .addOnFailureListener { onError("Database error") }
}