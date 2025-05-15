package com.example.e_clinic.ui.activities.user_screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.e_clinic.ui.activities.doctor_screens.DoctorActivity
import com.example.e_clinic.ui.activities.doctor_screens.PrescribeScreen

class PrescriptionListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("user_id") ?: "unknown_user"
        setContent {
            val navController = rememberNavController()
            PrescriptionListScreen(userId = userId) {
                // Navigate back to UserActivity after successful appointment
                startActivity(Intent(this, UserActivity::class.java))
                finish()
            }
    }
    }
}


@Composable
fun PrescriptionListScreen(userId: String, onNavigateBack: () -> Unit) {
    // Mock data for prescriptions - todo: Replace with actual data from Firestore and data class
    val prescriptions = listOf(
        mapOf(
            "id" to "1",
            "drugName" to "Paracetamol",
            "dateIssued" to "2025-05-01",
            "dosage" to "500mg",
            "type" to "Rp",
            "amount" to "20 tablets",
            "doctorName" to "Dr. John Doe",
            "details" to "Take one tablet every 6 hours after meals."
        )
    )
    val context = LocalContext.current
    val navController = rememberNavController() //Code breaks here - todo: Fix this


    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prescriptions) { prescription ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("prescriptionDetails/${prescription["id"]}")
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = prescription["drugName"] as String,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Issued: ${prescription["dateIssued"]}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    Button(
        onClick = {
            context.startActivity(Intent(context, UserActivity::class.java))
            if (context is ComponentActivity) context.finish()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Return")
    }
}

@Composable
fun PrescriptionDetailsScreen(prescription: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Prescription Details",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "Drug Name: ${prescription["drugName"]}")
        Text(text = "Dosage: ${prescription["dosage"]}")
        Text(text = "Type: ${prescription["type"]}")
        Text(text = "Amount: ${prescription["amount"]}")
        Text(text = "Prescribed by: ${prescription["doctorName"]}")
        Text(text = "Details: ${prescription["details"]}")
        // Placeholder for QR code
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "QR Code Placeholder")
        }
    }
}