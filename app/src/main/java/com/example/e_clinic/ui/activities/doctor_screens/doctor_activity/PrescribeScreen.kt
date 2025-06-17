package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.e_clinic.CSV.collections.Drug
import com.example.e_clinic.CSV.functions.QuerryFilters
import com.example.e_clinic.Firebase.collections.Prescription
import com.example.e_clinic.Firebase.storage.uploadPrescriptionToStorage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescribeScreenForm(doctorId: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val db = FirebaseFirestore.getInstance()
    val patients = remember { mutableStateListOf<Pair<String, String>>() }

    var selectedPatient by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var showPatientDialog by rememberSaveable { mutableStateOf(false) }

    var selectedMedication by rememberSaveable { mutableStateOf<Drug?>(null) }
    var showMedicationDialog by rememberSaveable { mutableStateOf(false) }

    var dosage by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Load patients from Firestore
    LaunchedEffect(doctorId) {
        isLoading = true
        db.collection("appointments")
            .whereEqualTo("doctor_id", doctorId)
            .get()
            .addOnSuccessListener { result ->
                val userIds = result.documents.mapNotNull { it.getString("user_id") }
                patients.clear()

                userIds.forEach { userId ->
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: ""
                            val surname = userDoc.getString("surname") ?: ""
                            if (name.isNotEmpty() && surname.isNotEmpty()) {
                                patients.add(Pair(userId, "$name $surname"))
                            }
                        }
                        .addOnFailureListener {
                            // Handle error if needed

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
        ){
        // Patient selection button
        Button(
            onClick = { showPatientDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedPatient?.second ?: "Select Patient")
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
                if (selectedPatient != null && selectedMedication != null && dosage.isNotBlank()) {

                    val prescription = Prescription(doctor_id = doctorId,
                        user_id = selectedPatient!!.first,
                        issued_date = Timestamp.now(),
                        link_to_storage="link",
                        appointment_id = "appointmentid" ,
                        doctor_comment = instructions,
                    )
                    addPrescription(
                        drug = selectedMedication!!,
                        dosage = dosage,
                        amount = amount,
                        prescription = prescription
                    )
                    showSuccessDialog = true
                }
            },
            enabled = selectedPatient != null && selectedMedication != null && dosage.isNotBlank(),
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

    // Patient selection dialog
    if (showPatientDialog) {
        AlertDialog(
            onDismissRequest = { showPatientDialog = false },
            title = { Text("Select Patient") },
            text = {
                LazyColumn {
                    items(patients) { patient ->
                        Text(
                            text = patient.second,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPatient = patient
                                    showPatientDialog = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPatientDialog = false }) {
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
fun PrescribeScreen(fromCalendar: Boolean = false, patientId: String? = null) {
    val auth = FirebaseAuth.getInstance()
    val doctorEmail = auth.currentUser?.email ?: ""
    val doctorId = remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(doctorEmail) {
        if (doctorEmail.isNotEmpty()) {
            db.collection("doctors").whereEqualTo("e-mail", doctorEmail).get().addOnSuccessListener { documents ->
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
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            doctorId.value?.let { id ->
                if (fromCalendar && patientId != null) {
                    PrescribeScreenWithGivenPatient(doctorId = id, patientId = patientId)
                } else {
                    PrescribeScreenForm(doctorId = id)
                }
            }
        }
    }
}

private fun addPrescription(
    drug: Drug,
    dosage: String,
    amount: String,
    prescription: Prescription
){
    uploadPrescriptionToStorage(
        medication = drug,
        dosage = dosage,
        quantity = amount,
        prescription = prescription
    )
}

@Composable
fun PrescribeScreenWithGivenPatient(
    doctorId: String,
    patientId: String
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

    Column(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text("Patient: ${remember { mutableStateOf("Loading...") }.apply {
                LaunchedEffect(patientId) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(patientId)
                        .get()
                        .addOnSuccessListener { doc ->
                            value = "${doc.getString("name") ?: "Unknown"} ${doc.getString("surname") ?: ""}".trim()
                        }
                        .addOnFailureListener {
                            value = "Unknown"
                        }
                }
            }.value}")

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
                        appointment_id = "appointmentid",
                        doctor_comment = instructions,
                    )
                    addPrescription(
                        drug = selectedMedication!!,
                        dosage = dosage,
                        amount = amount,
                        prescription = prescription
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

