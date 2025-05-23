package com.example.e_clinic.ui.activities.user_screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AppointmentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("user_id") ?: "unknown_user"
        setContent {
            AppointmentScreen(userId = userId) {
                // Navigate back to UserActivity after successful appointment
                startActivity(Intent(this, UserActivity::class.java))
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(userId: String, onAppointmentMade: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val doctors = remember { mutableStateListOf<String>() }
    val appointmentRepository = AppointmentRepository()
    var selectedDoctor by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Timestamp?>(null) }
    var selectedTime by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var displayDate by remember { mutableStateOf("") }

    // Fetch doctors from Firestore
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
        // Doctor selection dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            TextField(
                value = selectedDoctor,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Doctor") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
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

        // Date picker
        Button(onClick = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDate = Timestamp(selectedCalendar.time)
                    displayDate = "${dayOfMonth}/${month + 1}/$year"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }) {
            Text(text = if (displayDate.isEmpty()) "Select Date" else displayDate)
        }

        // Time picker
        Button(onClick = {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    selectedTime = String.format("%02d:%02d", hour, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }) {
            Text(text = if (selectedTime.isEmpty()) "Select Time" else selectedTime)
        }

        // Submit button
        Button(
            onClick = {
                if (selectedDoctor.isNotEmpty() && selectedDate != null && selectedTime.isNotEmpty()) {
                    val doctorId = selectedDoctor.substringAfter("ID: ").trim()

                    // Combine date and time
                    val calendar = Calendar.getInstance().apply {
                        time = selectedDate!!.toDate()
                        val (hours, minutes) = selectedTime.split(":").map { it.toInt() }
                        set(Calendar.HOUR_OF_DAY, hours)
                        set(Calendar.MINUTE, minutes)
                    }
                    val appointmentDateTime = Timestamp(calendar.time)

                    val appointment = Appointment(
                        date = appointmentDateTime,
                        doctor_id = doctorId,
                        user_id = userId,
                        status = "not finished"
                    )

                    appointmentRepository.bookAppointment(appointment) { success ->
                        if (success) {
                            Toast.makeText(context, "Appointment created", Toast.LENGTH_SHORT).show()
                            onAppointmentMade()
                        } else {
                            Toast.makeText(context, "Failed to create appointment", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Make Appointment")
        }

        // Return button
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