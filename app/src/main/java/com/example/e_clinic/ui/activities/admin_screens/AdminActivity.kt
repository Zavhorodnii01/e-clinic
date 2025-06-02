package com.example.e_clinic.ui.activities.admin_screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.Firebase.collections.TimeSlot
import com.example.e_clinic.ui.theme.EClinicTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
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
    var tempPassword by remember { mutableStateOf("") }
    // ADDED: Name and surname fields
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
            // ADDED: Name and surname input fields
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

            OutlinedTextField(
                value = tempPassword,
                onValueChange = { tempPassword = it },
                label = { Text("Temporary Password") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Button(
                onClick = {
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(doctorEmail, tempPassword)
                        .addOnSuccessListener {
                            val doctor = Doctor(
                                e_mail = doctorEmail,
                                specialization = specialization,
                                name = doctorName, // ADDED: Include name
                                surname = doctorSurname // ADDED: Include surname
                            )
                            FirebaseFirestore.getInstance()
                                .collection("doctors")
                                .add(doctor)
                                .addOnSuccessListener {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Doctor added successfully")
                                    }
                                    // Clear all fields after success
                                    doctorEmail = ""
                                    tempPassword = ""
                                    doctorName = ""
                                    doctorSurname = ""
                                    specialization = ""
                                }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error adding doctor: ${it.message}")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Doctor", textAlign = TextAlign.Center)
            }

            LazyColumn {
                items(doctors) { doctor ->
                    DoctorItem(doctor,
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
        var editEmail by remember { mutableStateOf(selectedDoctor!!.e_mail) }
        // ADDED: Name and surname fields for editing
        var editName by remember { mutableStateOf(selectedDoctor!!.name) }
        var editSurname by remember { mutableStateOf(selectedDoctor!!.surname) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Doctor") },
            text = {
                Column {
                    // ADDED: Name and surname fields
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
                                "name" to editName, // ADDED: Update name
                                "surname" to editSurname // ADDED: Update surname
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
            Text(doctor.e_mail)
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
    var selectedSpecialization by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var timeRanges by remember { mutableStateOf(listOf<String>()) }
    var newTimeRange by remember { mutableStateOf("") }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }
    val dateDialogState = rememberMaterialDialogState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    LaunchedEffect(selectedSpecialization) {
        val snapshot = if (selectedSpecialization.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("doctors")
                .whereEqualTo("specialization", selectedSpecialization)
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

    LaunchedEffect(selectedDoctorId, selectedDate) {
        if (selectedDoctorId.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            calendar.set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth, 0, 0)
            val startOfDay = Timestamp(calendar.time)

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = Timestamp(calendar.time)

            val snapshot = FirebaseFirestore.getInstance()
                .collection("timeslots")
                .whereEqualTo("doctorId", selectedDoctorId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .await()

            timeSlots = snapshot.documents.map {
                val slot = it.toObject(TimeSlot::class.java)!!
                slot.id = it.id
                slot
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = selectedSpecialization,
                onValueChange = {
                    selectedSpecialization = it
                    selectedDoctorId = ""
                },
                label = { Text("Specialization") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = doctors.find { it.id == selectedDoctorId }?.let {
                        "${it.name} (${it.specialization})"
                    } ?: "Select Doctor",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text("${doctor.name} (${doctor.specialization})") },
                            onClick = {
                                selectedDoctorId = doctor.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { dateDialogState.show() }) {
                Text("Date: ${selectedDate.format(dateFormatter)}")
            }

            MaterialDialog(
                dialogState = dateDialogState,
                buttons = {
                    positiveButton("OK")
                    negativeButton("Cancel")
                }
            ) {
                datepicker { date -> selectedDate = date }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Add Time Ranges:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedTextField(
                    value = newTimeRange,
                    onValueChange = { newTimeRange = it },
                    label = { Text("e.g. 10:00-14:00") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val pattern = Pattern.compile("^([01]?\\d|2[0-3]):[0-5]\\d-([01]?\\d|2[0-3]):[0-5]\\d$")
                    if (pattern.matcher(newTimeRange).matches()) {
                        timeRanges = timeRanges + newTimeRange
                        newTimeRange = ""
                    }
                }) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(timeRanges) { range ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(range, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            timeRanges = timeRanges - range
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedDoctorId.isNotEmpty()) {
                        generateTimeSlots(
                            doctorId = selectedDoctorId,
                            date = selectedDate,
                            ranges = timeRanges.map { it.split("-") }
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Time slots added successfully")
                        }
                        timeRanges = emptyList()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select a doctor")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Slots")
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
                        Text("${slot.startTime} - ${slot.endTime}")
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
    }
}

fun generateTimeSlots(doctorId: String, date: LocalDate, ranges: List<List<String>>) {
    val db = FirebaseFirestore.getInstance()
    val calendar = Calendar.getInstance()

    ranges.forEach { range ->
        val start = LocalTime.parse(range[0])
        val end = LocalTime.parse(range[1])
        var current = start

        while (current.isBefore(end)) {
            val slotEnd = current.plusMinutes(20)
            if (slotEnd.isAfter(end)) break

            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth,
                current.hour, current.minute)
            val timestamp = Timestamp(calendar.time)

            val slot = hashMapOf(
                "doctorId" to doctorId,
                "date" to timestamp,
                "startTime" to current.toString(),
                "endTime" to slotEnd.toString(),
                "isBooked" to false
            )

            db.collection("timeslots").add(slot)
            current = slotEnd
        }
    }
}