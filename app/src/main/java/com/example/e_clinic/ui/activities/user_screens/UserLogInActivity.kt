package com.example.e_clinic.ui.activities.user_screens

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
import com.example.e_clinic.ui.activities.user_screens.user_activity.UserActivity
import com.example.e_clinic.ui.activities.user_screens.PinEntryActivity
import com.example.e_clinic.ui.activities.user_screens.SetPinAfterLoginActivity
import com.example.e_clinic.ui.activities.user_screens.UserSignUpActivity
import com.example.e_clinic.ui.theme.EClinicTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class UserLogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null){
            val intent = Intent(this, PinEntryActivity::class.java)
            startActivity(intent)
            finish()
        }
        else{

        enableEdgeToEdge()
        setContent {
            EClinicTheme {
                LogInScreen(
                    onSignUpClick = {
                        startActivity(Intent(this, UserSignUpActivity::class.java))
                    },
                    onContinueAsUser = {
                        startActivity(Intent(this, PinEntryActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
    }
}

@Composable
fun LogInScreen(
    onSignUpClick: () -> Unit,
    onContinueAsUser: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = credential.result
            val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(authCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                                .addOnSuccessListener { doc ->
                                    val intent = if (doc.getBoolean("hasSetPin") == true) {
                                        Intent(context, PinEntryActivity::class.java)
                                    } else {
                                        Intent(context, SetPinAfterLoginActivity::class.java)
                                    }
                                    context.startActivity(intent)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Błąd pobierania danych użytkownika", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to eClinic",
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp)
        )

//        if (currentUser != null) {
//            Text("Zalogowano jako: ${currentUser.email}", fontSize = 14.sp)
//            Spacer(modifier = Modifier.height(8.dp))
//            Button(
//                onClick = onContinueAsUser,
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text("Kontynuuj jako ${currentUser.email}")
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//            OutlinedButton(
//                onClick = {
//                    auth.signOut()
//                    GoogleSignIn.getClient(
//                        context,
//                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                            .requestIdToken(context.getString(R.string.default_web_client_id))
//                            .requestEmail()
//                            .build()
//                    ).signOut()
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Wyloguj")
//            }
//            Spacer(modifier = Modifier.height(24.dp))
//        }

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
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
                                        val intent = if (doc.getBoolean("hasSetPin") == true) {
                                            Intent(context, PinEntryActivity::class.java)
                                        } else {
                                            Intent(context, SetPinAfterLoginActivity::class.java)
                                        }
                                        context.startActivity(intent)
                                    }
                                    .addOnFailureListener {
                                        errorMessage = "Failed to fetch user data"
                                    }
                            } else {
                                auth.signOut()
                                errorMessage = "Email is not verified. Please check your inbox."
                            }
                        } else {
                            errorMessage = "Incorrect email or password"
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        OutlinedButton(
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (email.isNotBlank()) {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                successMessage = "Password reset email sent to $email"
                            } else {
                                errorMessage = "Failed to send reset email"
                            }
                        }
                } else {
                    errorMessage = "Enter your email to reset password"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Forgot Password", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
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

        AnimatedVisibility(
            visible = errorMessage != null || successMessage != null,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (errorMessage != null) Color(0xFFFFE0E0) else Color(0xFFE0FFE0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (errorMessage != null) Icons.Filled.Close else Icons.Filled.CheckCircle,
                        contentDescription = "Status Icon",
                        tint = if (errorMessage != null) Color.Red else Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: successMessage ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
