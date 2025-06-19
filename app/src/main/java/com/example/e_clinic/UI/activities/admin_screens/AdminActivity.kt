package com.example.e_clinic.UI.activities.admin_screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                AdminActivityScreen()
            }
        }
    }
}
private fun generateTimeSlots(date: LocalDate, start: String, end: String): List<Timestamp> {
    val slots = mutableListOf<Timestamp>()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val startTime = LocalTime.parse(start, timeFormatter)
    val endTime = LocalTime.parse(end, timeFormatter)

    var currentTime = startTime

    while (currentTime.isBefore(endTime)) {
        val dateTime = LocalDateTime.of(date, currentTime)
        val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        // Create Timestamp using seconds and nanoseconds
        val timestamp = Timestamp(instant.epochSecond, instant.nano)
        slots.add(timestamp)
        currentTime = currentTime.plusMinutes(20)
    }

    return slots
}


private fun validateTimeInput(start: String, end: String): Boolean {
    val timePattern = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    return timePattern.matcher(start).matches() && timePattern.matcher(end).matches()
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun AdminActivityScreen() {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val tabs = listOf("Manage Doctors", "Manage Timeslots")

    Column {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }
        }

        HorizontalPager(
            count = tabs.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> ManageDoctorsScreen()
                1 -> ManageTimeslotsScreen()
            }
        }
    }
}

@Composable
fun ManageDoctorsScreen() {
    var specialization by remember { mutableStateOf("") }
    var doctorEmail by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var doctorSurname by remember { mutableStateOf("") }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(specialization) {
        val snapshot = if (specialization.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("doctors")
                .whereEqualTo("specialization", specialization)
                .get()
                .await()
        } else {
            FirebaseFirestore.getInstance()
                .collection("doctors")
                .get()
                .await()
        }
        doctors = snapshot.documents.map {
            val doc = it.toObject(Doctor::class.java)!!
            doc.id = it.id
            doc
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = doctorName,
                onValueChange = { doctorName = it },
                label = { Text("Doctor Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = doctorSurname,
                onValueChange = { doctorSurname = it },
                label = { Text("Doctor Surname") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = specialization,
                onValueChange = { specialization = it },
                label = { Text("Specialization") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = doctorEmail,
                onValueChange = { doctorEmail = it },
                label = { Text("Doctor Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Button(
                onClick = {
                    if (doctorName.isNotEmpty() && doctorSurname.isNotEmpty() && doctorEmail.isNotEmpty() && specialization.isNotEmpty()) {
                        val doctor = Doctor(
                            name = doctorName,
                            surname = doctorSurname,
                            email = doctorEmail,
                            specialization = specialization,
                            address = "",
                            education = "",
                            experience = "",
                            gender = "",
                            phone = ""
                        )

                        FirebaseFirestore.getInstance()
                            .collection("doctors")
                            .add(doctor)
                            .addOnSuccessListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Doctor added successfully")
                                }
                                // Reset fields
                                doctorName = ""
                                doctorSurname = ""
                                doctorEmail = ""
                                specialization = ""
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error adding doctor: ${e.message}")
                                }
                            }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Doctor", textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(doctors) { doctor ->
                    DoctorItem(
                        doctor = doctor,
                        onEdit = {
                            selectedDoctor = doctor
                            showEditDialog = true
                        },
                        onDelete = {
                            FirebaseFirestore.getInstance()
                                .collection("doctors")
                                .document(doctor.id)
                                .delete()
                                .addOnSuccessListener {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Doctor deleted successfully")
                                    }
                                }
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog && selectedDoctor != null) {
        var editSpecialization by remember { mutableStateOf(selectedDoctor!!.specialization) }
        var editEmail by remember { mutableStateOf(selectedDoctor!!.email) }
        var editName by remember { mutableStateOf(selectedDoctor!!.name) }
        var editSurname by remember { mutableStateOf(selectedDoctor!!.surname) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Doctor") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSurname,
                        onValueChange = { editSurname = it },
                        label = { Text("Surname") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editSpecialization,
                        onValueChange = { editSpecialization = it },
                        label = { Text("Specialization") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    FirebaseFirestore.getInstance()
                        .collection("doctors")
                        .document(selectedDoctor!!.id)
                        .update(
                            mapOf(
                                "e_mail" to editEmail,
                                "specialization" to editSpecialization,
                                "name" to editName,
                                "surname" to editSurname
                            )
                        )
                        .addOnSuccessListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Doctor updated successfully")
                            }
                            showEditDialog = false
                        }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DoctorItem(doctor: Doctor, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${doctor.name} ${doctor.surname}")
            Text(doctor.specialization)
            Text(doctor.email)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ManageTimeslotsScreen() {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var doctorId by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    val dateDialogState = rememberMaterialDialogState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Date Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Date: ${selectedDate.format(DateTimeFormatter.ISO_DATE)}")
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { dateDialogState.show() }) {
                    Text("Select Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Inputs
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Start Time (HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text("End Time (HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Doctor and Specialization
            OutlinedTextField(
                value = doctorId,
                onValueChange = { doctorId = it },
                label = { Text("Doctor ID") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = specialization,
                onValueChange = { specialization = it },
                label = { Text("Specialization") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (validateTimeInput(startTime, endTime) && doctorId.isNotEmpty() && specialization.isNotEmpty()) {
                        val slots = generateTimeSlots(selectedDate, startTime, endTime)

                        val timeSlot = TimeSlot(
                            doctor_id = doctorId,
                            specialization = specialization,
                            available_slots = slots
                        )

                        // ✅ Updated Firestore write logic
                        FirebaseFirestore.getInstance()
                            .collection("timeslots")
                            .add(timeSlot)
                            .addOnSuccessListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Timeslots added successfully")
                                }
                                startTime = ""
                                endTime = ""
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error adding timeslots: ${e.message}")
                                }
                            }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields correctly")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Timeslots")
            }
        }
    }

MaterialDialog(
    dialogState = dateDialogState,
    buttons = {
        positiveButton("Ok")
        negativeButton("Cancel")
    }
) {
    datepicker { date ->
        selectedDate = date
    }
}

Spacer(modifier = Modifier.height(24.dp))

Text("Existing Time Slots:", style = MaterialTheme.typography.titleMedium)
Spacer(modifier = Modifier.height(8.dp))

LazyColumn {
    items(timeSlots) { slot ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                FirebaseFirestore.getInstance()
                    .collection("timeslots")
                    .document(slot.id)
                    .delete()
                    .addOnSuccessListener {
                        scope.launch {
                            snackbarHostState.showSnackbar("Time slot removed")
                        }
                    }
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
}


