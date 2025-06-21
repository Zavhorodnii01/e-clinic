package com.example.e_clinic.UI.activities.admin_screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.example.e_clinic.UI.activities.admin_screens.admin_activity.AdminActivity
import com.example.e_clinic.UI.activities.doctor_screens.DoctorPinEntryActivity
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class AdminLogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                AdminLoginScreen()
            }
        }
    }
}

@Composable
fun AdminLoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val pinManager = remember { com.example.e_clinic.Services.PinManager(context) }
    val currentUser = auth.currentUser
    var showPinEntry by remember { mutableStateOf(false) }

    if (currentUser != null && pinManager.getPin() != null && !showPinEntry) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "eClinic Management",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = { showPinEntry = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Sign In")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            auth.signOut()
                            context.startActivity(Intent(context, AdminLogInActivity::class.java))
                            (context as? ComponentActivity)?.finish()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Sign Out", color = Color.White)
                    }
                }
            }
        }
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text("Error") },
                text = { Text(errorMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
        return
    }
    else{

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Log to eClinic Management",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (errorMessage != null) {
                        AlertDialog(
                            onDismissRequest = { errorMessage = null },
                            title = { Text("Error") },
                            text = { Text(errorMessage ?: "") },
                            confirmButton = {
                                TextButton(onClick = { errorMessage = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        val uid = user?.uid ?: run {
                                            errorMessage = "Authentication error: Missing UID"
                                            return@addOnCompleteListener
                                        }

                                        db.collection("administrators").document(uid).get()
                                            .addOnSuccessListener { document ->
                                                if (document.exists() && document.getString("role") == "admin") {
                                                    // Success: Navigate to AdminActivity
                                                    val pinManager = com.example.e_clinic.Services.PinManager(context)
                                                    val intent = if (pinManager.getPin() != null) {
                                                        Intent(context, AdminPinEntryActivity::class.java)
                                                    } else {
                                                        Intent(context,
                                                            SetAdminPinAfterLoginActivity::class.java)
                                                    }
                                                    context.startActivity(intent)
                                                    (context as? ComponentActivity)?.finish()
                                                } else {
                                                    errorMessage = "Access denied: Admin privileges required"
                                                    auth.signOut() // Force logout
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                errorMessage = "Database error: ${e.message}"
                                                Log.e("AdminLogin", "Firestore error", e)
                                                auth.signOut()
                                            }
                                    } else {
                                        errorMessage = "Login failed: ${task.exception?.message}"
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Login")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            showResetPassword = true

                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?")
                    }
                }
            }

        }
    }
    }
    if (showResetPassword) {
        ResetPasswordScreen(
            onDismiss = { showResetPassword = false }
        )
    }
    if (showPinEntry) {
        val intent = Intent(context, AdminPinEntryActivity::class.java)
        context.startActivity(intent)
        (context as? ComponentActivity)?.finish()
        return
    }
}
