package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.content.Intent
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
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.services.Service
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ServicesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val services: List<Service> = appServices()
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
                        when (service.name.lowercase()) {
                            "appointments" -> navController.navigate("appointment_screen/$userId")
                            "doctors" -> context.startActivity(Intent(context, DoctorRepository::class.java))
                            "chat" -> launchZegoChat(context)
                            else -> {} // No action
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
