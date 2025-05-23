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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.e_clinic.services.functions.hashPin
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity
import kotlinx.coroutines.tasks.await

class PinEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PinEntryScreen {
                startActivity(Intent(this, UserActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun PinEntryScreen(onPinVerified: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            verifyPin(pin, onPinVerified) { error = it }
        }) {
            Text("Verify")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

fun verifyPin(inputPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    if (currentUser == null) {
        onError("User not authenticated")
        return
    }

    db.collection("users").document(currentUser.uid).get()
        .addOnSuccessListener { doc ->
            val storedHash = doc.getString("pinCode")
            if (storedHash == hashPin(inputPin)) {
                onSuccess()
            } else {
                onError("Incorrect PIN")
            }
        }
        .addOnFailureListener {
            onError("Error reading PIN")
        }
}