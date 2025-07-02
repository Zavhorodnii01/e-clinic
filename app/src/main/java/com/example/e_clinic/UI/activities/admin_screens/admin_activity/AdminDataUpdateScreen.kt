package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Administrator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun AdminDataUpdateScreen(id: String) {
    val firestore = FirebaseFirestore.getInstance()
    //val administrator = remember { mutableStateOf<Administrator?>(null)}
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        try {
            val document = firestore.collection("administrators").document(id).get().await()
            if (document.exists()) {
                val admin = document.toObject(Administrator::class.java)
                name = admin?.name ?: ""
                surname = admin?.surname ?: ""
                phone = admin?.phone ?: ""
            } else {
                error = "Administrator not found"
            }
        } catch (e: Exception) {
            error = "Error fetching data: ${e.message}"
        }
        loading = false
    }

    if (loading) {
        CircularProgressIndicator()
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background) // Set background color
    ) {
        Text("Edit your data", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Surname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (!isValidPhoneNumber(phone)) {
                    error = "Invalid phone number format. It should be 9 digits long."
                    return@Button
                }
                val updateMap = mapOf(
                    "name" to name,
                    "surname" to surname,
                    "phone" to phone
                )
                firestore.collection("administrators").document(id).update(updateMap)
                    .addOnSuccessListener { success = true }
                    .addOnFailureListener { e -> error = e.message }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
        if (success) {
            Text("Changes saved!", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Error") },
            text = { Text(error ?: "An unknown error occurred") },
            confirmButton = {
                Button(onClick = { error = null }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^\\d{9}$"))
}
