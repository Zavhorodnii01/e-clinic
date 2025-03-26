package com.example.e_clinic.ui.activities.user_screens

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserSignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            RegistrationScreen(onSignUpSuccess = {
                val intent = Intent(this, UserLogInActivity::class.java)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun RegistrationScreen(onSignUpSuccess: () -> Unit = {}) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("Select Date of Birth") }
    var selectedGender by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun showDatePickerDialog() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                dob = dateFormat.format(selectedDate.time)
            },
            calendar.get(Calendar.YEAR) - 18,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun registerUser() {
        if (!emailValidator(email)) {
            errorMessage = "Invalid email format."
            return
        }
        if (!passwordValidator(password)) {
            errorMessage = "Password must be at least 8 characters, contain a capital letter, a number, and a special character."
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        if (!dobValidator(dob)) {
            errorMessage = "You must be at least 18 years old."
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = mapOf(
                        "id" to userId,
                        "email" to email.trim(),
                        "name" to name,
                        "surname" to surname,
                        "phone" to phone,
                        "dob" to dob,
                        "gender" to selectedGender
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
        Text(text = dob, modifier = Modifier.clickable { showDatePickerDialog() }, color = Color.Blue)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { registerUser() }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
        Spacer(modifier = Modifier.height(16.dp))
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