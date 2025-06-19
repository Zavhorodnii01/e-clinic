package com.example.e_clinic.UI.activities.user_screens.user_activity

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
import com.example.e_clinic.Services.Service
import com.example.e_clinic.Services.functions.appServices
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
                        when (service.name) {
                            "My Appointments" -> {
                                navController.navigate("appointment_screen/$userId")
                            }

                            "Chat with Doctor" -> {
                                launchZegoChat(context)
                            }

                            "Chat with AI Assistant" -> {
                                navController.navigate("ai_chat")
                            }

                            "My Prescriptions" -> {
                                navController.navigate("documents")
                            }

                            else -> {
                                // Handle any additional services here
                            }
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
