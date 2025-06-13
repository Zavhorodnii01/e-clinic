package com.example.e_clinic.ui.activities.user_screens

import android.app.Activity
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.services.functions.hashPin
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
//import com.google.firebase.firestore.util.Executors
import java.util.concurrent.Executor
import java.util.concurrent.Executors

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
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(1) } // 1 = enter PIN, 2 = confirm PIN

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = if (step == 1) "Set your PIN" else "Confirm your PIN",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (step == 1) "Enter a 4-digit PIN" else "Repeat your PIN",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN circles indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val currentPin = if (step == 1) pin else confirmPin
                val filled = i <= currentPin.length
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Numeric keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rows 1-3
            for (row in 0 until 3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    for (col in 1..3) {
                        val number = (row * 3 + col).toString()
                        NumberButton(number) {
                            if (step == 1 && pin.length < 4) {
                                pin += number
                                if (pin.length == 4) step = 2
                            } else if (step == 2 && confirmPin.length < 4) {
                                confirmPin += number
                                if (confirmPin.length == 4) {
                                    if (pin == confirmPin) {
                                        savePin(pin, userId, db) {
                                            val intent = Intent(context, PinEntryActivity::class.java)
                                            context.startActivity(intent)
                                            (context as Activity).finish()
                                        }
                                    } else {
                                        error = "PINs do not match"
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Last row: 0 and backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(64.dp))

                NumberButton("0") {
                    if (step == 1 && pin.length < 4) {
                        pin += "0"
                        if (pin.length == 4) step = 2
                    } else if (step == 2 && confirmPin.length < 4) {
                        confirmPin += "0"
                        if (confirmPin.length == 4) {
                            if (pin == confirmPin) {
                                savePin(pin, userId, db) {
                                    val intent = Intent(context, PinEntryActivity::class.java)
                                    context.startActivity(intent)
                                    (context as Activity).finish()
                                }
                            } else {
                                error = "PINs do not match"
                                confirmPin = ""
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            if (step == 1 && pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            } else if (step == 2 && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
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

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

fun savePin(pin: String, userId: String, db: FirebaseFirestore, onSuccess: () -> Unit) {
    val hashedPin = hashPin(pin)
    val userData = mapOf(
        "pinCode" to hashedPin,
        "hasSetPin" to true,
        "rememberDevice" to true
    )

    db.collection("users").document(userId)
        .set(userData, SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
}


//class SetPinAfterLoginActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            SetPinAfterLoginScreen()
//        }
//    }
//}
//@Composable
//fun SetPinAfterLoginScreen() {
//    val context = LocalContext.current
//    var newPin by remember { mutableStateOf("") }
//    var confirmPin by remember { mutableStateOf("") }
//    var error by remember { mutableStateOf<String?>(null) }
//    var isLoading by remember { mutableStateOf(false) } // Dodajemy stan ładowania
//
//    val auth = FirebaseAuth.getInstance()
//    val db = FirebaseFirestore.getInstance()
//    val userId = auth.currentUser?.uid
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("Set Your PIN", style = MaterialTheme.typography.headlineMedium)
//        Text("For faster login next time", style = MaterialTheme.typography.bodyMedium)
//        Spacer(modifier = Modifier.height(24.dp))
//
//        OutlinedTextField(
//            value = newPin,
//            onValueChange = {
//                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
//                    newPin = it
//                    error = null
//                }
//            },
//            label = { Text("New PIN (4 digits)") },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = confirmPin,
//            onValueChange = {
//                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
//                    confirmPin = it
//                    error = null
//                }
//            },
//            label = { Text("Confirm PIN") },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Button(
//            onClick = {
//                when {
//                    newPin.length != 4 -> error = "PIN must be 4 digits"
//                    confirmPin.length != 4 -> error = "Confirm your PIN"
//                    newPin != confirmPin -> error = "PINs don't match"
//                    else -> {
//                        isLoading = true
//                        val hashedPin = hashPin(newPin)
//
//                        // Używamy set() z merge zamiast update()
//                        val userData = hashMapOf(
//                            "pinCode" to hashedPin,
//                            "hasSetPin" to true,
//                            "rememberDevice" to true
//                        )
//
//                        db.collection("users").document(userId!!)
//                            .set(userData, SetOptions.merge())
//                            .addOnSuccessListener {
//                                isLoading = false
//                                // Przekierowanie do PinEntryActivity
//                                val intent = Intent(context, PinEntryActivity::class.java)
//                                context.startActivity(intent)
//                                (context as Activity).finish()
//                            }
//                            .addOnFailureListener { e ->
//                                isLoading = false
//                                error = "Error saving PIN: ${e.message}"
//                                Log.e("SetPin", "Error saving PIN", e)
//                            }
//                    }
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = !isLoading // Blokuj przycisk podczas ładowania
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(Modifier.size(20.dp))
//            } else {
//                Text("Save PIN")
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedButton(
//            onClick = {
//                // Skip PIN setup
//                val userData = hashMapOf(
//                    "hasSetPin" to false,
//                    "rememberDevice" to false
//                )
//
//                db.collection("users").document(userId!!)
//                    .set(userData, SetOptions.merge())
//                    .addOnSuccessListener {
//                        val intent = Intent(context, UserActivity::class.java)
//                        context.startActivity(intent)
//                        (context as Activity).finish()
//                    }
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Set Up Later")
//        }
//
//        error?.let {
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(text = it, color = MaterialTheme.colorScheme.error)
//        }
//    }
//}
