@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.specializations.DoctorSpecialization
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.collections.get
import kotlin.text.get
import kotlin.toString

@Composable
fun DataManagerScreen(id: String, type: String){
    val firestore = FirebaseFirestore.getInstance()
    val user by remember { mutableStateOf<User?>(null) }
    val doctor by remember { mutableStateOf<Doctor?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    var education by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }

    val genderOptions = listOf("Male", "Female")
    var genderExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = remember { java.util.Calendar.getInstance() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(id, type) {
        loading = true
        error = null
        success = false
        try {
            if (type == "user") {
                val doc = firestore.collection("users").document(id).get().await()
                val timestamp = doc.getTimestamp("dob")
                name = doc.getString("name") ?: ""
                surname = doc.getString("surname") ?: ""
                gender = doc.getString("gender") ?: ""
                phone = doc.getString("phone") ?: ""
                email = doc.getString("email") ?: ""
                address = doc.getString("address") ?: ""
                dob = timestamp?.toDate()?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it)
                } ?: ""
            } else if (type == "doctor") {
                val doc = firestore.collection("doctors").document(id).get().await()
                val timestamp = doc.getTimestamp("dob")
                name = doc.getString("name") ?: ""
                surname = doc.getString("surname") ?: ""
                gender = doc.getString("gender") ?: ""
                phone = doc.getString("phone") ?: ""
                email = doc.getString("e-mail") ?: ""
                address = doc.getString("address") ?: ""
                specialization = doc.getString("specialization") ?: ""
                experience = doc.getString("experience") ?: ""
                education = doc.getString("education") ?: ""

                dob = timestamp?.toDate()?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it)
                } ?: ""
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    if (loading) {
        CircularProgressIndicator()
        return
    }
    if (error != null) {
        Text("Error: $error")
        return
    }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
        .background(MaterialTheme.colorScheme.background)
    ) {

        Text(
            text = if (type == "user") "Edit User Details" else "Edit Doctor Details",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (type == "user") {
            Text("User ID: $id", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        } else if (type == "doctor") {
            Text("Doctor ID: $id", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }

        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        TextField(value = surname, onValueChange = { surname = it }, label = { Text("Surname") })
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            TextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                modifier = Modifier.menuAnchor()
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
        TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
        TextField(
            value = dob,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date of Birth") },
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
        if (type == "doctor") {
            val specializationOptions = DoctorSpecialization.entries.toTypedArray()
            var specializationExpanded by remember { mutableStateOf(false) }
            val selectedSpecialization = specializationOptions.find { it.displayName == specialization }
                ?: DoctorSpecialization.CARDIOLOGY

            ExposedDropdownMenuBox(
                expanded = specializationExpanded,
                onExpandedChange = { specializationExpanded = !specializationExpanded }
            ) {
                TextField(
                    value = selectedSpecialization.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Specialization") },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = specializationExpanded,
                    onDismissRequest = { specializationExpanded = false }
                ) {
                    specializationOptions.forEach { option ->
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
            TextField(value = experience, onValueChange = { experience = it }, label = { Text("Experience") })
            TextField(value = education, onValueChange = { education = it }, label = { Text("Education") })
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (type == "user") {
                val timestamp = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = sdf.parse(dob)
                    if (date != null) com.google.firebase.Timestamp(date) else null
                } catch (_: Exception) { null }
                val updateMap = mutableMapOf<String, Any>(
                    "name" to name,
                    "surname" to surname,
                    "gender" to gender,
                    "phone" to phone,
                    "email" to email,
                    "address" to address
                )
                if (timestamp != null) updateMap["dob"] = timestamp
                firestore.collection("users").document(id).update(updateMap)
                    .addOnSuccessListener { success = true }
                    .addOnFailureListener { error = it.message }
            }
            else if (type == "doctor") {
                val timestamp = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = sdf.parse(dob)
                    if (date != null) com.google.firebase.Timestamp(date) else null
                } catch (_: Exception) { null }
                val updateMap = mutableMapOf<String, Any>(
                    "name" to name,
                    "surname" to surname,
                    "gender" to gender,
                    "phone" to phone,
                    "e-mail" to email,
                    "address" to address,
                    "specialization" to (DoctorSpecialization.fromDisplayName(specialization)?.name ?: specialization),
                    "experience" to experience,
                    "education" to education
                )
                if (timestamp != null) updateMap["dob"] = timestamp
                firestore.collection("doctors").document(id).update(updateMap)
                    .addOnSuccessListener { success = true }
                    .addOnFailureListener { error = it.message }
            }
        }) {
            Text("Save Changes")
        }
        if (success) {
            Text("Changes saved!", color = MaterialTheme.colorScheme.primary)
        }
    }


}