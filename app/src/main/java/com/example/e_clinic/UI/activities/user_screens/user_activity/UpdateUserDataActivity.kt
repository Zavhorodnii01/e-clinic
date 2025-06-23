package com.example.e_clinic.UI.activities.user_screens.user_activity


import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class UpdateUserDataActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
            UpdateUserDataScreen(
                onFinish = { finish() }
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateUserDataScreen(onFinish: () -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

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

    val genderOptions = listOf("Male", "Female")
    var genderExpanded by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(userId) {
        loading = true
        error = null
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            val timestamp = doc.getTimestamp("dob")
            name = doc.getString("name") ?: ""
            surname = doc.getString("surname") ?: ""
            gender = doc.getString("gender") ?: ""
            phone = doc.getString("phone") ?: ""
            email = doc.getString("email") ?: ""
            address = doc.getString("address") ?: ""
            dob = timestamp?.toDate()?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
            } ?: ""
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Error: $error")
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Edit Your Data",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = surname, onValueChange = { surname = it }, label = { Text("Surname") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                TextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
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
            Spacer(Modifier.height(8.dp))
            TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = email, onValueChange = {}, readOnly = true, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date of Birth") },
                modifier = Modifier.fillMaxWidth()
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
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val timestamp = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
                    firestore.collection("users").document(userId).update(updateMap)
                        .addOnSuccessListener {
                            success = true
                            onFinish()
                        }
                        .addOnFailureListener { error = it.message }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
            if (success) {
                Text("Changes saved!", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
