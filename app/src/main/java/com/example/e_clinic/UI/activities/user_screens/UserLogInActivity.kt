package com.example.e_clinic.UI.activities.user_screens

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.R
import com.example.e_clinic.Services.PinManager
import com.example.e_clinic.UI.activities.doctor_screens.DoctorLogInActivity
import com.example.e_clinic.UI.activities.doctor_screens.DoctorPinEntryActivity
import com.example.e_clinic.UI.activities.doctor_screens.ResetPasswordScreen
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class UserLogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                LogInScreen(
                    onSignUpClick = {
                        startActivity(Intent(this, UserSignUpActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}


@Composable
fun LogInScreen(
    onSignUpClick: () -> Unit,

) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var isLoading by remember { mutableStateOf(false) }
    val pinManager = PinManager(context)
    var showResetPassword by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPinEntry by remember { mutableStateOf(false) }

    if (currentUser != null && pinManager.getPin() != null && !showPinEntry) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "eClinic Patient",
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
                            context.startActivity(Intent(context, UserLogInActivity::class.java))
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
    else {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val credential = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = credential.result
                    val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)

                    auth.signInWithCredential(authCredential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(user.uid).get()
                                        .addOnSuccessListener { doc ->
                                            val intent = if (doc.getBoolean("hasSetPin") == true) {
                                                Intent(context, PinEntryActivity::class.java)
                                            } else {
                                                Intent(
                                                    context,
                                                    SetPinAfterLoginActivity::class.java
                                                )
                                            }
                                            context.startActivity(intent)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Fetching User Data Failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxSize(
                ),
                contentAlignment = Alignment.Center,

                ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        Text(
                            text = "Log in to eClinic Patient",
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
                                        if (task.isSuccessful) {
                                            val user = task.result.user
                                            if (user != null && user.isEmailVerified) {
                                                FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(user.uid)
                                                    .get()
                                                    .addOnSuccessListener { doc ->
                                                        val intent =
                                                            if (pinManager.getPin() != null) {
                                                                Intent(
                                                                    context,
                                                                    PinEntryActivity::class.java
                                                                )
                                                            } else {
                                                                Intent(
                                                                    context,
                                                                    SetPinAfterLoginActivity::class.java
                                                                )
                                                            }
                                                        context.startActivity(intent)
                                                    }
                                                    .addOnFailureListener {
                                                        errorMessage = "Failed to fetch user data"
                                                    }
                                            } else {
                                                auth.signOut()
                                                errorMessage =
                                                    "Email is not verified. Please check your inbox."
                                            }
                                        } else {
                                            errorMessage = "Incorrect email or password"
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
                                Text(
                                    text = "Log In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onSignUpClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text(
                                text = "Sign Up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = { showResetPassword = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        IconButton(
                            onClick = {
                                val googleSignInClient = GoogleSignIn.getClient(
                                    context,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                )

                                googleSignInClient.signOut()

                                val signInIntent = googleSignInClient.signInIntent
                                val pendingIntent = PendingIntent.getActivity(
                                    context, 0, signInIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                )
                                launcher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            },
                            modifier = Modifier.size(200.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.google_sign_in),
                                contentDescription = "Google Sign-In",
                                tint = Color.Unspecified,
                                modifier = Modifier.fillMaxSize()
                            )
                        }


                    }
                }
            }
        }
            }

            if (showResetPassword) {
                ResetPasswordScreen(
                    onDismiss = { showResetPassword = false },
                )
            }
            if (showPinEntry) {
                val intent = Intent(context, PinEntryActivity::class.java)
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
                return

        }

}
