package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.e_clinic.CSV.collections.Drug
import com.example.e_clinic.CSV.functions.QuerryFilters
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Prescription
import com.example.e_clinic.Firebase.Storage.uploadPrescriptionToStorage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescribeScreenForm(doctorId: String, medicalRecordId: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val db = FirebaseFirestore.getInstance()

    // State for medical records with empty prescription IDs
    val medicalRecords = remember { mutableStateListOf<Pair<String, String>>() } // Pair<recordId, displayText>
    var selectedRecord by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var showRecordsDialog by rememberSaveable { mutableStateOf(false) }

    var selectedMedication by rememberSaveable { mutableStateOf<Drug?>(null) }
    var showMedicationDialog by rememberSaveable { mutableStateOf(false) }

    var dosage by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Load medical records with empty prescription IDs
    LaunchedEffect(doctorId) {
        isLoading = true
        db.collection("medical_records")
            .whereEqualTo("doctor_id", doctorId)
            .whereEqualTo("prescription_id", "")
            .get()
            .addOnSuccessListener { recordsResult ->
                medicalRecords.clear()

                recordsResult.documents.forEach { record ->
                    val patientId = record.getString("user_id") ?: ""
                    val date = record.getTimestamp("date")?.toDate()?.formatToString() ?: "Unknown date"

                    // Get patient name
                    db.collection("users").document(patientId).get()
                        .addOnSuccessListener { patientDoc ->
                            val name = patientDoc.getString("name") ?: ""
                            val surname = patientDoc.getString("surname") ?: ""
                            val displayText = "$name $surname - $date"
                            medicalRecords.add(Pair(record.id, displayText))
                        }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Medical record selection button
            Button(
                onClick = { showRecordsDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedRecord?.second ?: "Select Medical Record")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medication selection button
            Button(
                onClick = { showMedicationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedMedication?.name ?: "Select Medication")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMedication != null) {
                Text("Active Substance: ${selectedMedication!!.activeSubstance}")
                Text("Form: ${selectedMedication!!.form}")
                Text("Type of Prescription: ${selectedMedication!!.typeOfPrescription}")
                Text("Amount of Substance: ${selectedMedication!!.amountOfSubstance}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Amount (boxes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (selectedRecord != null && selectedMedication != null && dosage.isNotBlank()) {
                    val prescription = Prescription(
                        doctor_id = doctorId,
                        user_id = "", // Will be set from medical record
                        issued_date = Timestamp.now(),
                        link_to_storage = "link",
                        appointment_id = "", // Will be set from medical record
                        doctor_comment = instructions,
                    )

                    // First get the medical record to get patient_id and appointment_id
                    db.collection("medical_records").document(selectedRecord!!.first).get()
                        .addOnSuccessListener { recordDoc ->
                            val patientId = recordDoc.getString("user_id") ?: ""
                            val appointmentId = recordDoc.getString("appointment_id") ?: ""

                            prescription.user_id = patientId
                            prescription.appointment_id = appointmentId

                            addPrescription(
                                drug = selectedMedication!!,
                                dosage = dosage,
                                amount = amount,
                                prescription = prescription,
                                medicalRecordId = selectedRecord!!.first
                            )
                            showSuccessDialog = true
                        }
                }
            },
            enabled = selectedRecord != null && selectedMedication != null && dosage.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prescribe")
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Prescription Created") },
            text = { Text("The prescription has been successfully created.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Medical records selection dialog
    if (showRecordsDialog) {
        AlertDialog(
            onDismissRequest = { showRecordsDialog = false },
            title = { Text("Select Medical Record") },
            text = {
                if (medicalRecords.isEmpty()) {
                    Text("No medical records found without prescriptions")
                } else {
                    LazyColumn {
                        items(medicalRecords) { record ->
                            Text(
                                text = record.second,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRecord = record
                                        showRecordsDialog = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecordsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Medication selection dialog
    if (showMedicationDialog) {
        MedicationSearchDialog(
            onMedicationSelected = { drug ->
                selectedMedication = drug
                showMedicationDialog = false
            },
            onDismiss = { showMedicationDialog = false }
        )
    }
}

// Helper extension function to format Date
fun Date.formatToString(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this)
}
@Composable
fun MedicationSearchDialog(
    onMedicationSelected: (Drug) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val medications = remember {
        QuerryFilters.loadDrugsFromCSV(context)
    }

    var activeSubstanceQuery by rememberSaveable { mutableStateOf("") }
    var drugNameQuery by rememberSaveable { mutableStateOf("") }

    var selectedActiveSubstance by remember { mutableStateOf<String?>(null) }
    var filteredActiveSubstances by remember { mutableStateOf<List<String>>(emptyList()) }
    var filteredDrugNames by remember { mutableStateOf<List<Drug>>(emptyList()) }

    var selectedDrug by remember { mutableStateOf<Drug?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Medication") },
        text = {
            Column {
                // Active Substance Search
                OutlinedTextField(
                    value = activeSubstanceQuery,
                    onValueChange = {
                        activeSubstanceQuery = it
                        selectedActiveSubstance = null
                        drugNameQuery = ""
                        filteredDrugNames = emptyList()

                        if (it.length >= 3) {
                            filteredActiveSubstances = medications
                                .map { drug -> drug.activeSubstance }
                                .distinct()
                                .filter { substance -> substance.contains(it, ignoreCase = true) }
                                .sorted()
                        } else {
                            filteredActiveSubstances = emptyList()
                        }
                    },
                    label = { Text("Enter Active Substance") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(filteredActiveSubstances) { substance ->
                        Text(
                            text = substance,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedActiveSubstance = substance
                                    activeSubstanceQuery = substance
                                    filteredActiveSubstances = emptyList()
                                    filteredDrugNames = medications.filter { it.activeSubstance == substance }
                                    drugNameQuery = ""
                                }
                                .padding(8.dp)
                        )
                    }
                }

                if (selectedActiveSubstance != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = drugNameQuery,
                        onValueChange = {
                            drugNameQuery = it
                            filteredDrugNames = medications.filter { drug ->
                                drug.activeSubstance == selectedActiveSubstance &&
                                        drug.name.contains(it, ignoreCase = true)
                            }
                        },
                        label = { Text("Enter Drug Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(filteredDrugNames) { drug ->
                            Text(
                                text = drug.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDrug = drug
                                        drugNameQuery = drug.name
                                        filteredDrugNames = emptyList()
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    selectedDrug?.let { drug ->
                        Text("Selected Drug: ${drug.name}")
                        Text("Form: ${drug.form}")
                        Text("Type of Prescription: ${drug.typeOfPrescription}")
                        Text("Amount of Substance: ${drug.amountOfSubstance}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDrug?.let { onMedicationSelected(it) }
                },
                enabled = selectedDrug != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescribeScreen(
    fromCalendar: Boolean = false,
    patientId: String? = null,
    appointmentId: String? = null,
    medicalRecordId: String? = null,
    onDismiss: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val doctorEmail = auth.currentUser?.email ?: ""
    val doctorId = remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()
    val showErrorDialog = remember { mutableStateOf(false) }

    // Load doctor ID
    LaunchedEffect(doctorEmail) {
        if (doctorEmail.isNotEmpty()) {
            db.collection("doctors").whereEqualTo("e-mail", doctorEmail).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        doctorId.value = documents.documents[0].id
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prescribe Medication") },
                modifier = Modifier.padding(top = 16.dp),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            doctorId.value?.let { id ->
                if (fromCalendar && patientId != null) {
                    // Load medical record ID if not provided
                    if (medicalRecordId == null) {
                        LaunchedEffect(patientId) {
                            db.collection("medical_records")
                                .whereEqualTo("user_id", patientId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        val medicalRecordId = result.documents[0].id
                                        // Now we can show the prescription form
                                    } else {
                                        showErrorDialog.value = true
                                    }
                                }
                        }
                    }

                    PrescribeScreenWithGivenPatient(
                        doctorId = id,
                        patientId = patientId,
                        appointmentId = appointmentId,
                        medicalRecordId = medicalRecordId ?: "", // Handle null case
                        onDismiss = onDismiss
                    )
                } else {
                    PrescribeScreenForm(doctorId = id, medicalRecordId = medicalRecordId ?: "")
                }
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showErrorDialog.value) {
        AlertDialog(
            onDismissRequest = { showErrorDialog.value = false },
            title = { Text("Error") },
            text = { Text("Medical record not found for this patient") },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog.value = false
                    onDismiss()
                }) {
                    Text("OK")
                }
            }
        )
    }
}


private fun addPrescription(
    drug: Drug,
    dosage: String,
    amount: String,
    prescription: Prescription,
    medicalRecordId: String
) {
    uploadPrescriptionToStorage(
        medication = drug,
        dosage = dosage,
        quantity = amount,
        prescription = prescription,
        medicalRecordId = medicalRecordId
    )
}

@Composable
fun PrescribeScreenWithGivenPatient(
    doctorId: String,
    patientId: String,
    appointmentId: String? = null,
    medicalRecordId: String? = null,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val db = FirebaseFirestore.getInstance()

    var selectedMedication by remember { mutableStateOf<Drug?>(null) }
    var showMedicationDialog by rememberSaveable { mutableStateOf(false) }

    var dosage by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var patientName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(patientId) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(patientId)
            .get()
            .addOnSuccessListener { doc ->
                patientName = "${doc.getString("name") ?: "Unknown"} ${doc.getString("surname") ?: ""}".trim()
            }
            .addOnFailureListener {
                patientName = "Unknown"
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text("Patient: $patientName")

            Spacer(modifier = Modifier.height(16.dp))

            // Medication selection button
            Button(
                onClick = { showMedicationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedMedication?.name ?: "Select Medication")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMedication != null) {
                Text("Active Substance: ${selectedMedication!!.activeSubstance}")
                Text("Form: ${selectedMedication!!.form}")
                Text("Type of Prescription: ${selectedMedication!!.typeOfPrescription}")
                Text("Amount of Substance: ${selectedMedication!!.amountOfSubstance}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Amount (boxes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (selectedMedication != null && dosage.isNotBlank()) {
                    val prescription = Prescription(
                        doctor_id = doctorId,
                        user_id = patientId,
                        issued_date = Timestamp.now(),
                        link_to_storage = "link",
                        appointment_id = appointmentId ?: "appointmentid",
                        doctor_comment = instructions,
                    )
                    addPrescription(
                        drug = selectedMedication!!,
                        dosage = dosage,
                        amount = amount,
                        prescription = prescription,
                        medicalRecordId = medicalRecordId.toString()
                    )
                    showSuccessDialog = true
                }
            },
            enabled = selectedMedication != null && dosage.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prescribe")
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onDismiss()
            },
            title = { Text("Prescription Created") },
            text = { Text("The prescription has been successfully created.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onDismiss()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Medication selection dialog
    if (showMedicationDialog) {
        MedicationSearchDialog(
            onMedicationSelected = { drug ->
                selectedMedication = drug
                showMedicationDialog = false
            },
            onDismiss = { showMedicationDialog = false }
        )
    }
}