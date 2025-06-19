package com.example.e_clinic.ui.activities.doctor_screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.ui.activities.user_screens.user_activity.ProfileInfoItem
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen() {
    val doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var doctor by remember { mutableStateOf<Doctor?>(null) }
    var visitsCount by remember { mutableStateOf(0) }

    LaunchedEffect(doctorId) {
        DoctorRepository().getDoctorById(doctorId) { loadedDoctor ->
            doctor = loadedDoctor
            visitsCount = 24 // Placeholder
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Doctor Profile") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DoctorProfileHeader(doctor, visitsCount)
            Spacer(modifier = Modifier.height(24.dp))
            DoctorInfoSection(doctor)
            // No SecurityOptions here – doctors don't use PIN
        }
    }
}

@Composable
fun DoctorProfileHeader(doctor: Doctor?, visits: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Doctor Photo",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Dr. ${doctor?.name ?: "Loading..."} ${doctor?.surname ?: ""}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = doctor?.specialization ?: "Specialization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "$visits visits this month", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DoctorInfoSection(doctor: Doctor?) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoItem(Icons.Default.Home, "Address", doctor?.address ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Default.Email, "Email", doctor?.email ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Default.Phone, "Phone", doctor?.phone ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Filled.Work, "Specialization", doctor?.specialization ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Filled.School, "Education", doctor?.education ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Default.Star, "Experience", doctor?.experience ?: "Loading...")
        }
    }
}
