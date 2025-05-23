package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

fun bookAppointment(
    userId: String,
    doctorId: String,
    appointmentDateTime: Timestamp,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    val appointment = Appointment(
        date = appointmentDateTime,
        doctor_id = doctorId,
        user_id = userId,
        status = "not finished"
    )
    val appointmentRepository = AppointmentRepository()
    appointmentRepository.bookAppointment(appointment) { success ->
        if (success) {
            Toast.makeText(context, "Appointment created", Toast.LENGTH_SHORT).show()
            onSuccess()
        } else {
            Toast.makeText(context, "Failed to create appointment", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(userId: String, onAppointmentMade: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val doctors = remember { mutableStateListOf<String>() }

    var showBookingForm by remember { mutableStateOf(false) }

    var selectedDoctor by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Timestamp?>(null) }
    var selectedTime by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var displayDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("doctors").get().addOnSuccessListener { result ->
            for (document in result) {
                val doctorName = document.getString("name") ?: "Unknown"
                val doctorSurname = document.getString("surname") ?: "Unknown"
                val doctorId = document.id
                val doctorSpecialization = document.getString("specialization") ?: "Unknown"
                doctors.add("$doctorName $doctorSurname ($doctorSpecialization), ID: $doctorId")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Upcoming Appointments")
        // TODO: Load and show upcoming appointments here

        Text("Past Appointments")
        // TODO: Load and show past appointments here

        Button(onClick = { showBookingForm = !showBookingForm }) {
            Text(if (showBookingForm) "Hide Booking Form" else "Make New Appointment")
        }

        if (showBookingForm) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                TextField(
                    value = selectedDoctor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Doctor") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text(doctor) },
                            onClick = {
                                selectedDoctor = doctor
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDate = Timestamp(selectedCalendar.time)
                    displayDate = "${dayOfMonth}/${month + 1}/$year"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }) {
                Text(if (displayDate.isEmpty()) "Select Date" else displayDate)
            }

            Button(onClick = {
                val calendar = Calendar.getInstance()
                TimePickerDialog(context, { _, hour, minute ->
                    selectedTime = String.format("%02d:%02d", hour, minute)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }) {
                Text(if (selectedTime.isEmpty()) "Select Time" else selectedTime)
            }

            Button(
                onClick = {
                    if (selectedDoctor.isNotEmpty() && selectedDate != null && selectedTime.isNotEmpty()) {
                        val doctorId = selectedDoctor.substringAfter("ID: ").trim()
                        val calendar = Calendar.getInstance().apply {
                            time = selectedDate!!.toDate()
                            val (hours, minutes) = selectedTime.split(":").map { it.toInt() }
                            set(Calendar.HOUR_OF_DAY, hours)
                            set(Calendar.MINUTE, minutes)
                        }
                        val appointmentDateTime = Timestamp(calendar.time)
                        bookAppointment(userId, doctorId, appointmentDateTime, context) {
                            onAppointmentMade()
                            showBookingForm = false
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book Appointment")
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
}

@Preview(showBackground = true)
@Composable
fun AppointmentsScreenPreview() {
    AppointmentsScreen(userId = "test_user") {}
}
