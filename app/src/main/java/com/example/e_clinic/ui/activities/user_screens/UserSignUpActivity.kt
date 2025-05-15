package com.example.e_clinic.ui.activities.user_screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.User
import com.example.e_clinic.R
import com.example.e_clinic.ui.activities.LogInActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserSignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            RegistrationScreen(onSignUpSuccess = {
                val intent = Intent(this, LogInActivity::class.java)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun RegistrationScreen(onSignUpSuccess: () -> Unit = {}, preFilledEmail: String? = null) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(preFilledEmail ?: "") } // Use preFilledEmail if available
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("Select Date of Birth") }
    var selectedGender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val calendar = remember { Calendar.getInstance() }
    val genderOptions = listOf("Male", "Female")
    var expanded by remember { mutableStateOf(false) }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dob = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val googleSignUpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email // Get the email from Google account

                // Check if email is already registered in Firebase
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods
                            if (signInMethods != null && signInMethods.isNotEmpty()) {
                                // Email is already registered, sign the user in
                                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                                FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnCompleteListener { authTask ->
                                        if (authTask.isSuccessful) {
                                            // User is successfully logged in
                                            //successMessage = "Welcome back!"
                                            //onSignUpSuccess()
                                        } else {
                                            errorMessage = "Google Sign-In Failed: ${authTask.exception?.message}"
                                        }
                                    }
                            } else {
                                // Email not registered, proceed with sign-up
                                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                                FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnCompleteListener { authTask ->
                                        if (authTask.isSuccessful) {
                                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
                                            val userRef = db.collection("users").document(userId)

                                            userRef.get().addOnSuccessListener { document ->
                                                if (!document.exists()) {
                                                    // New Google user, store data in Firestore
                                                    val newUser = User(
                                                        id = userId,
                                                        email = account.email.orEmpty(),
                                                        name = account.displayName.orEmpty(),
                                                        surname = "", // Google doesn't provide this
                                                        phone = FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty(),
                                                        dob = "",
                                                        gender = "",
                                                        address = ""
                                                    )
                                                    userRef.set(newUser).addOnSuccessListener {
                                                        successMessage = "Google Sign-Up Successful!"
                                                        onSignUpSuccess()
                                                    }.addOnFailureListener { exception ->
                                                        errorMessage = "Firestore error: ${exception.message}"
                                                    }
                                                } else {
                                                    // This should never happen because of the check above, but we handle it gracefully
                                                    successMessage = "This email is registered"
                                                    //onSignUpSuccess()
                                                }
                                            }
                                        } else {
                                            errorMessage = "Google Sign-Up Failed: ${authTask.exception?.message}"
                                        }
                                    }
                            }
                        } else {
                            errorMessage = task.exception?.message ?: "Error checking email."
                        }
                    }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-Up Failed: ${e.message}"
            }
        }
    }

    fun signUpWithGoogle() {
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        // Sign out first to ensure account selection prompt
        GoogleSignIn.getLastSignedInAccount(context)?.let {
            googleSignInClient.signOut()  // Explicit sign-out
        }

        val signInIntent = googleSignInClient.signInIntent
        val pendingIntent = PendingIntent.getActivity(context, 0, signInIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        googleSignUpLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }

    fun registerUser() {
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || phone.isEmpty() || dob == "Select Date of Birth" || selectedGender.isEmpty()) {
            errorMessage = "All fields must be filled!"
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match!"
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = User(
                        id = userId,
                        email = email.trim(),
                        name = name,
                        surname = surname,
                        phone = phone,
                        dob = dob,
                        gender = selectedGender,
                        address = address
                    )
                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            successMessage = "Registration successful!"
                            onSignUpSuccess()
                        }
                        .addOnFailureListener { exception ->
                            errorMessage = "Error registering user: ${exception.message}"
                        }
                } else {
                    errorMessage = task.exception?.message ?: "Registration failed."
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dob,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
                .padding(12.dp),
            color = if (dob == "Select Date of Birth") Color.Gray else Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            genderOptions.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender) },
                    onClick = {
                        selectedGender = gender
                        expanded = false
                    }
                )
            }
        }
        Text(
            text = selectedGender,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(12.dp),
            color = if (selectedGender == "Select Gender") Color.Gray else Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { registerUser() }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-Up Button (as an alternative method)
        IconButton(
            onClick = { signUpWithGoogle() },
            modifier = Modifier.size(200.dp) // Adjust size for the icon
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_sign_up), // Use your Google icon
                contentDescription = "Google Sign-Up",
                tint = Color.Unspecified, // Keeps original icon colors
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        errorMessage?.let { Text(text = it, color = Color.Red) }
        successMessage?.let { Text(text = it, color = Color.Green) }
    }
}

fun emailValidator(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
fun passwordValidator(password: String) = Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$").matches(password)
fun dobValidator(dob: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dobDate = sdf.parse(dob)
        val calendar = Calendar.getInstance()
        calendar.time = dobDate
        val age = Calendar.getInstance().get(Calendar.YEAR) - calendar.get(Calendar.YEAR)
        age >= 18
    } catch (e: Exception) {
        false
    }
}