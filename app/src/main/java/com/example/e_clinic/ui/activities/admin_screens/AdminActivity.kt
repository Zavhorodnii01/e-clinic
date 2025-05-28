// AdminActivity.kt
package com.example.e_clinic.ui.activities.admin_screens

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
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Doctor
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
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }

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

    Column(modifier = Modifier.padding(16.dp)) {
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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tempPassword,
            onValueChange = { tempPassword = it },
            label = { Text("Temporary Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(doctorEmail, tempPassword)
                    .addOnSuccessListener {
                        val doctor = Doctor(
                            e_mail = doctorEmail,
                            specialization = specialization
                        )
                        FirebaseFirestore.getInstance()
                            .collection("doctors")
                            .add(doctor)
                    }
                    .addOnFailureListener {
                        // TODO: Show error snackbar
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Doctor", textAlign = TextAlign.Center)
        }

        LazyColumn {
            items(doctors) { doctor ->
                DoctorItem(doctor) { /* TODO: Edit doctor */ }
            }
        }
    }
}

@Composable
fun DoctorItem(doctor: Doctor, onEdit: () -> Unit) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${doctor.name} ${doctor.surname}")
            Text(doctor.specialization)
            Text(doctor.e_mail)
            Button(onClick = onEdit) {
                Text("Edit")
            }
        }
    }
}

@Composable
fun ManageTimeslotsScreen() {
    var selectedDoctorId by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var timeRanges by remember { mutableStateOf(listOf("10:00-14:00", "16:00-19:30")) }
    var newTimeRange by remember { mutableStateOf("") }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    val dateDialogState = rememberMaterialDialogState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    LaunchedEffect(Unit) {
        val snapshot = FirebaseFirestore.getInstance().collection("doctors").get().await()
        doctors = snapshot.documents.map {
            val doc = it.toObject(Doctor::class.java)!!
            doc.id = it.id
            doc
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
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

        Text("Time Ranges:")
        timeRanges.forEach { Text(it) }

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

        Button(
            onClick = {
                if (selectedDoctorId.isNotEmpty()) {
                    generateTimeSlots(
                        doctorId = selectedDoctorId,
                        date = selectedDate,
                        ranges = timeRanges.map { it.split("-") }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Slots")
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
                "doctor_Id" to doctorId,
                "date" to timestamp,
                "start_Time" to current.toString(),
                "end_Time" to slotEnd.toString(),
                "is_Booked" to false
            )

            db.collection("timeslots").add(slot)
            current = slotEnd
        }
    }
}
