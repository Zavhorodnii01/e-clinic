package com.example.e_clinic.ui.activities.user_screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.e_clinic.services.functions.hashPin
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SetPinAfterLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetPinAfterLoginScreen()
        }
    }
}
@Composable
fun SetPinAfterLoginScreen() {
    val context = LocalContext.current
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // Dodajemy stan ładowania

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Set Your PIN", style = MaterialTheme.typography.headlineMedium)
        Text("For faster login next time", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    newPin = it
                    error = null
                }
            },
            label = { Text("New PIN (4 digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    confirmPin = it
                    error = null
                }
            },
            label = { Text("Confirm PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    newPin.length != 4 -> error = "PIN must be 4 digits"
                    confirmPin.length != 4 -> error = "Confirm your PIN"
                    newPin != confirmPin -> error = "PINs don't match"
                    else -> {
                        isLoading = true
                        val hashedPin = hashPin(newPin)

                        // Używamy set() z merge zamiast update()
                        val userData = hashMapOf(
                            "pinCode" to hashedPin,
                            "hasSetPin" to true,
                            "rememberDevice" to true
                        )

                        db.collection("users").document(userId!!)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener {
                                isLoading = false
                                // Przekierowanie do PinEntryActivity
                                val intent = Intent(context, PinEntryActivity::class.java)
                                context.startActivity(intent)
                                (context as Activity).finish()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                error = "Error saving PIN: ${e.message}"
                                Log.e("SetPin", "Error saving PIN", e)
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Blokuj przycisk podczas ładowania
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp))
            } else {
                Text("Save PIN")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                // Skip PIN setup
                val userData = hashMapOf(
                    "hasSetPin" to false,
                    "rememberDevice" to false
                )

                db.collection("users").document(userId!!)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        val intent = Intent(context, UserActivity::class.java)
                        context.startActivity(intent)
                        (context as Activity).finish()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Up Later")
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
