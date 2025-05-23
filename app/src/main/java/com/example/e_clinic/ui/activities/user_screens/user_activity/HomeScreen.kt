package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val appointmentRepository = AppointmentRepository()
    val appointments = remember { mutableStateListOf<Appointment>() }

    LaunchedEffect(user) {
        user?.let {
            appointmentRepository.getAppointmentsForUser(it.uid) { loadedAppointments ->
                val doctorRepository = DoctorRepository()
                appointments.clear()
                loadedAppointments.forEach { appointment ->
                    doctorRepository.getDoctorById(appointment.doctor_id) { doctor ->
                        val doctorName = doctor?.name ?: "Unknown Doctor"
                        appointments.add(appointment.copy(doctor_id = doctorName))
                    }
                }
            }
        }
    }

    val services = appServices()
    Column {
        Text(text = "Upcoming Appointments", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(appointments) { appointment ->
                AppointmentItem(title = "Doctor: ${appointment.doctor_id}", description = "Date & Time: ${appointment.date}")
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        ServicesSection(services = services, onServiceClick = { service ->
            """when (service.name) {
                "appointment" -> {
                    val intent = Intent(context, AppointmentActivity::class.java)
                    intent.putExtra("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    context.startActivity(intent)
                }
            }"""
        })
    }
}
