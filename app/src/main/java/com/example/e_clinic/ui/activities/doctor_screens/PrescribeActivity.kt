package com.example.e_clinic.ui.activities.doctor_screens



import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_clinic.ui.activities.doctor_screens.DoctorActivity

class PrescribeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrescribeScreen(
                onReturn = {
                    startActivity(Intent(this, DoctorActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun PrescribeScreen(onReturn: () -> Unit) {
    val patients = listOf("John Doe", "Jane Smith", "Alice Johnson") // Mock patient list
    val drugs = listOf("Drug A", "Drug B", "Drug C") // Mock drug list
    var selectedPatient by remember { mutableStateOf("") }
    var selectedDrug by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var prescriptionType by remember { mutableStateOf("Rp") }
    var dosageDescription by remember { mutableStateOf("") }
    var expandedPatientList by remember { mutableStateOf(false) }
    var expandedDrugList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Patient selection
        Text("Select Patient")
        TextButton(onClick = { expandedPatientList = !expandedPatientList }) {
            Text(if (expandedPatientList) "Hide Patients" else "Show Patients")
        }
        if (expandedPatientList) {
            LazyColumn(modifier = Modifier.height(100.dp)) {
                items(patients) { patient ->
                    ListItem(
                        headlineContent = { Text(patient) },
                        modifier = Modifier.clickable { selectedPatient = patient }
                    )
                }
            }
        }
        Text("Selected Patient: $selectedPatient")

        // Drug selection
        Text("Select Drug")
        TextButton(onClick = { expandedDrugList = !expandedDrugList }) {
            Text(if (expandedDrugList) "Hide Drugs" else "Show Drugs")
        }
        if (expandedDrugList) {
            LazyColumn(modifier = Modifier.height(100.dp)) {
                items(drugs) { drug ->
                    ListItem(
                        headlineContent = { Text(drug) },
                        modifier = Modifier.clickable { selectedDrug = drug }
                    )
                }
            }
        }
        Text("Selected Drug: $selectedDrug")

        // Amount input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (packages)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Prescription type selection
        Text("Prescription Type")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RadioButton(
                selected = prescriptionType == "Rp",
                onClick = { prescriptionType = "Rp" }
            )
            Text("Rp")
            RadioButton(
                selected = prescriptionType == "Rpw",
                onClick = { prescriptionType = "Rpw" }
            )
            Text("Rpw")
        }

        // Dosage description input
        OutlinedTextField(
            value = dosageDescription,
            onValueChange = { dosageDescription = it },
            label = { Text("Dosage Description") },
            modifier = Modifier.fillMaxWidth()
        )

        // Buttons
        Button(
            onClick = { /* Mock implementation for making a prescription */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Make Prescription")
        }

        Button(
            onClick = onReturn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Return")
        }
    }
}
