package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.Firebase.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(){
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val doctorEmail = auth.currentUser?.email ?: ""
    val doctorRepository = DoctorRepository()
    val db = FirebaseFirestore.getInstance()
    val appointmentRepository = AppointmentRepository()
    val userRepository = UserRepository()
    val appointments = remember { mutableStateListOf<Appointment>() }
    val todayAppointmentsCount = remember { mutableStateOf(0) }
    val doctorState = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(doctorEmail) {
        if (doctorEmail.isNotEmpty()) {
            db.collection("doctors").whereEqualTo("e-mail", doctorEmail).get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doctor = documents.documents[0]
                    doctorState.value = doctor.id
                    doctorState.value?.let { doctorId ->
                        appointmentRepository.getAppointmentsForDoctor(doctorId) { fetchedAppointments ->
                            appointments.clear()
                            appointments.addAll(fetchedAppointments)
                            val today = LocalDate.now()
                            val counter = fetchedAppointments.count { appointment ->
                                val appointmentDate = appointment.date?.toDate()?.toInstant()
                                    ?.atZone(ZoneId.systemDefault())
                                    ?.toLocalDate()
                                appointmentDate == today
                            }
                            todayAppointmentsCount.value = counter
                        }
                    }
                }

            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = if (todayAppointmentsCount.value == 0)
                "You don't have any scheduled appointment today"
            else if (todayAppointmentsCount.value == 1)
                "Today you have scheduled ${todayAppointmentsCount.value} appointment"
            else
                "Today you have scheduled ${todayAppointmentsCount.value} appointments",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DoctorAppointmentCalendar(
            appointments = appointments,
            onAppointmentClick = { appointment ->
                // Handle appointment click, e.g., navigate to appointment details
                // For now, just print the appointment ID
                println("Clicked on appointment: ${appointment.id}")
            }
        )

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(appointments, key = { it.id }) { appointment ->
                AppointmentItem(
                    title = "Patient: ${appointment.user_id}",
                    description = "Date & Time: ${appointment.date}"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DoctorAppointmentCalendar(
    appointments: List<Appointment>,
    onAppointmentClick : (Appointment) -> Unit

){
    val today = remember { mutableStateOf(LocalDate.now()) }
    val dayAppointments = appointments.filter { appointment ->
        appointment.date?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate() == today.value
    }.sortedBy { it.date?.toDate() }
    val colorScheme = MaterialTheme.colorScheme
    val userRepository = UserRepository()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { today.value = today.value.minusDays(1) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
            }
            Text(
                text = today.value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            IconButton(onClick = { today.value = today.value.plusDays(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(24) { hour ->
                val appointmentAtHour = dayAppointments.find { appointment ->
                    (appointment.status == "NOT_FINISHED" || appointment.status == "FINISHED") &&
                            appointment.date?.toDate()?.toInstant()
                            ?.atZone(ZoneId.systemDefault())
                            ?.hour == hour
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (appointmentAtHour != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .clickable { onAppointmentClick(appointmentAtHour) }
                        ) {

                            Column(modifier = Modifier.padding(8.dp)) {
                                val patientName = remember(appointmentAtHour.user_id) { mutableStateOf("Loading...") }
                                LaunchedEffect(appointmentAtHour.user_id) {
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(appointmentAtHour.user_id)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            val name = doc.getString("name") ?: ""
                                            val surname = doc.getString("surname") ?: ""
                                            patientName.value = "$name $surname"
                                        }
                                        .addOnFailureListener {
                                            patientName.value = "Unknown"
                                        }
                                }
                                Text(
                                    text = "Patient: ${patientName.value}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Date: ${
                                        appointmentAtHour.date?.toDate()?.let {
                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(it)
                                        } ?: "N/A"
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

    }




}

