@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity


import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.specializations.DoctorSpecialization
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.text.set

@Composable
fun NewDoctorScreen(onDoctorAdded: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf(DoctorSpecialization.CARDIOLOGY.displayName) }
    var address by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var specializationExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = remember { java.util.Calendar.getInstance() }
    var showDatePicker by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female")
    var education by remember { mutableStateOf("") }
    var genderExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Add New Doctor",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Surname") },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            TextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            gender = option
                            genderExpanded = false
                        }
                    )
                }
            }
        }
        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = specializationExpanded,
            onExpandedChange = { specializationExpanded = !specializationExpanded }
        ) {
            TextField(
                value = specialization,
                onValueChange = {},
                readOnly = true,
                label = { Text("Specialization") },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = specializationExpanded,
                onDismissRequest = { specializationExpanded = false }
            ) {
                DoctorSpecialization.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            specialization = option.displayName
                            specializationExpanded = false
                        }
                    )
                }
            }
        }
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = experience,
            onValueChange = { experience = it },
            label = { Text("Experience (years)") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = education,
            onValueChange = { education = it },
            label = { Text("Education") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = dob,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date of Birth") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )

        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Pick Date")
        }
        if (showDatePicker) {
            LaunchedEffect(showDatePicker) {
                val parts = dob.split("-")
                val year = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: calendar.get(Calendar.MONTH)
                val day = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        dob = "%04d-%02d-%02d".format(y, m + 1, d)
                        showDatePicker = false
                    },
                    year, month, day
                ).apply {
                    setOnDismissListener { showDatePicker = false }
                    show()
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (!isValidDob(dob)) {
                     Toast.makeText(context, "Invalid date of birth. Must be at least 21 years old.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isValidPhoneNumber(phone)) {
                    Toast.makeText(context, "Invalid phone number. Must be 9 digits.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isValidEmail(email)) {
                    Toast.makeText(context, "Invalid email address.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                else{
                val password = UUID.randomUUID().toString().substring(0, 8) // Simple random password
                val auth = FirebaseAuth.getInstance()
                val timestamp = try{
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = sdf.parse(dob)
                    if (date != null) com.google.firebase.Timestamp(date) else null
                }catch (_: Exception) { null }
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: return@addOnSuccessListener
                        val doctorData =mutableMapOf<String, Any>(
                            "uid" to uid,
                            "name" to name,
                            "surname" to surname,
                            "gender" to gender,
                            "phone" to phone,
                            "e-mail" to email,
                            "specialization" to (DoctorSpecialization.fromDisplayName(specialization)?.name ?: specialization),
                            "address" to address,
                            "experience" to experience,
                            "education" to education
                        )
                        if (timestamp != null) {
                            doctorData["dob"] = timestamp // Only if Firestore expects a Timestamp
                        }
                        FirebaseFirestore.getInstance().collection("doctors")
                            .document(uid)
                            .set(doctorData)
                            .addOnSuccessListener {
                                // Send password reset email
                                auth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener { onDoctorAdded() }
                                    .addOnFailureListener { it.printStackTrace() }
                            }
                    }
                    .addOnFailureListener {
                        // Handle error
                        it.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Doctor")
        }
    }
}


private fun isValidDob(dob: String): Boolean {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val birthDate = sdf.parse(dob) ?: return false
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.YEAR, -21)
    return !birthDate.after(calendar.time)
}

private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^\\d{9}$"))
}

private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
