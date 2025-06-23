package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.Services.functions.doctorServices
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ServicesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val services = doctorServices()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Map service names to icons
    fun serviceIcon(name: String) = when (name) {
        "Appointments" -> Icons.Filled.CalendarToday
        "Chat with Patients" -> Icons.Filled.Chat
        "New Prescription" -> Icons.Filled.MedicalServices
        else -> Icons.Filled.MedicalServices
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
    ){
        Column(
            modifier = Modifier.fillMaxSize()
        ){
        Text(
            text = "Your Services",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(services) { service ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (service.name) {
                            "Appointments" -> navController.navigate("appointments")
                            "Chat with Patients" -> launchZegoChat(context)
                            "New Prescription" -> navController.navigate("prescriptions")
                        }
                    },
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = serviceIcon(service.name),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = service.displayedName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    }
    }
}