package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.services.Service
import com.example.e_clinic.services.functions.appServices
import com.example.e_clinic.services.functions.doctorServices
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ServicesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val services = doctorServices()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(services) { service ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (service.name) {
                            "Appointments" -> navController.navigate("appointment_screen/$userId")
                            "Chat with Patients" -> launchZegoChat(context)
                            "New Prescription" -> navController.navigate("prescriptions")
                        }
                    }
            ) {
                ListItem(
                    headlineContent = { Text(service.displayedName) },
                    supportingContent = { Text(service.description) }
                )
            }
        }
    }
}
