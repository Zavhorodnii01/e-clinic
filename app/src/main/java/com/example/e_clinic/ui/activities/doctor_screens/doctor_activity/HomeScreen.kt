package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.content.Intent
import android.widget.Toast // Add this import
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.e_clinic.Firebase.collections.MedicalRecord
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.Firebase.repositories.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date // Make sure you import Date if your Appointment class uses it
import java.text.SimpleDateFormat // Make sure you import SimpleDateFormat
import java.util.Locale

// Assuming PrescribeScreen is defined elsewhere and imported correctly
// import com.example.e_clinic.ui.activities.doctor_screens.PrescribeScreen // Example Import

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
    var selectedAppointment = remember { mutableStateOf<Appointment?>(null) }
    val showPrescriptionChoice = remember { mutableStateOf(false) }
    val showPrescribeScreen = remember { mutableStateOf(false) }
    val currentAppointment = remember { mutableStateOf<Appointment?>(null) }
    val prescribePatientId = remember { mutableStateOf<String?>(null) }
    val prescribeAppointmentId = remember { mutableStateOf<String?>(null) }
    var medicalRecordId = remember { mutableStateOf<String?>(null) }

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

    // TODO TRANSACTION
    fun finishAppointment(appointment: Appointment) {
        val record = MedicalRecord(
            // Use the same ID as the appointment
            appointment_id = appointment.id, // Assuming it's a Timestamp
            user_id = appointment.user_id,
            doctor_id = doctorState.value ?: "",
            date = appointment.date,
            prescription_id = "", // You can fill this in later after prescription is added
            doctors_notes = "" // You can allow doctors to edit notes later
        )


        // Create a new medical record
        val db = FirebaseFirestore.getInstance()
        val recordRef = db.collection("medical_records").document()
        val appointmentRef = db.collection("appointments").document(appointment.id)
        medicalRecordId.value = recordRef.id
        db.runTransaction { transaction ->
            transaction.set(recordRef, record)
            transaction.update(appointmentRef, "status", "FINISHED")
            null
        }.addOnSuccessListener {
            Toast.makeText(context, "Appointment finished", Toast.LENGTH_SHORT).show()

            doctorState.value?.let { doctorId ->
                appointmentRepository.getAppointmentsForDoctor(doctorId) { fetchedAppointments ->
                    appointments.clear()
                    appointments.addAll(fetchedAppointments)
                }
            }

            currentAppointment.value = null
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_SHORT).show()
            currentAppointment.value = null
        }
    }

        Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = when (todayAppointmentsCount.value) {
                0 -> "You don't have any scheduled appointment today"
                1 -> "Today you have scheduled ${todayAppointmentsCount.value} appointment"
                else -> "Today you have scheduled ${todayAppointmentsCount.value} appointments"
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DoctorAppointmentCalendar(
            appointments = appointments.filter { it.status != "FINISHED" },
            onAppointmentClick = { appointment ->
                selectedAppointment.value = appointment
            }
        )
    }

    if (selectedAppointment.value != null) {
        AlertDialog(
            onDismissRequest = { selectedAppointment.value = null },
            title = { Text(text = "Appointment Details") },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val patientName = remember(selectedAppointment.value?.user_id) { mutableStateOf("Loading...") }
                        LaunchedEffect(selectedAppointment.value?.user_id) {
                            selectedAppointment.value?.user_id?.let { userId ->
                                db.collection("users")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val name = doc.getString("name") ?: ""
                                        val surname = doc.getString("surname") ?: ""
                                        patientName.value = if (name.isNotEmpty() || surname.isNotEmpty()) "$name $surname" else "Unknown"
                                    }
                                    .addOnFailureListener {
                                        patientName.value = "Unknown"
                                    }
                            }
                        }

                        Text(text = "Patient: ${patientName.value}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Date: ${
                                selectedAppointment.value?.date?.toDate()?.let {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                                } ?: "N/A"
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Status: ${selectedAppointment.value?.status}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = {
                                println("Chat with patient: ${selectedAppointment.value?.id}")
                                selectedAppointment.value = null
                            }) {
                                Text(text = "Chat")
                            }
                            Button(onClick = {
                                currentAppointment.value = selectedAppointment.value
                                selectedAppointment.value = null
                                showPrescriptionChoice.value = true
                            }) {
                                Text(text = "Finish")
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPrescriptionChoice.value && currentAppointment.value != null) {
        AlertDialog(
            onDismissRequest = {
                showPrescriptionChoice.value = false
                currentAppointment.value = null
            },
            title = { Text("Finish Appointment") },
            text = { Text("Would you like to add a prescription now?") },
            confirmButton = {
                Button(onClick = {
                    val appointment = currentAppointment.value!!
                    finishAppointment(appointment)
                    showPrescriptionChoice.value = false
                    prescribePatientId.value = appointment.user_id
                    prescribeAppointmentId.value = appointment.id
                    showPrescribeScreen.value = true
                }) {
                    Text("Yes, Add Prescription")
                }
            },
            dismissButton = {
                Button(onClick = {
                    finishAppointment(currentAppointment.value!!)
                    showPrescriptionChoice.value = false
                }) {
                    Text("No, Finish Without")
                }
            }
        )
    }

    if (
        showPrescribeScreen.value &&
        prescribePatientId.value != null &&
        prescribeAppointmentId.value != null
    ) {
        PrescribeScreen(
            fromCalendar = true,
            patientId = prescribePatientId.value!!,
            appointmentId = prescribeAppointmentId.value!!,
            medicalRecordId = medicalRecordId.value!!,
            onDismiss = {
                showPrescribeScreen.value = false
                prescribePatientId.value = null
                prescribeAppointmentId.value = null
            }
        )
    }
}

// Keep your existing DoctorAppointmentCalendar composable unchanged if its logic is fine
@Composable
fun DoctorAppointmentCalendar(
    appointments: List<Appointment>,
    onAppointmentClick : (Appointment) -> Unit
){
    // ... (Your existing DoctorAppointmentCalendar code) ...
    val today = remember { mutableStateOf(LocalDate.now()) }
    // Filter for today's appointments, exclude FINISHED status from the *calendar* view
    val dayAppointments = appointments.filter { appointment ->
        (appointment.status == "NOT_FINISHED" || appointment.status == "FINISHED") && // Keep FINISHED status in list but maybe not clickable? Or filter later?
                appointment.date?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDate() == today.value
    }.sortedBy { it.date?.toDate() } // Sort by time

    // Re-filter dayAppointments for display in calendar slots to only show NOT_FINISHED
    val displayDayAppointments = dayAppointments.filter { it.status == "NOT_FINISHED" } // Only show NOT_FINISHED in time slots

    val colorScheme = MaterialTheme.colorScheme
    val userRepository = UserRepository() // Unused?

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

        // List of time slots
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(24) { hour ->
                // Find appointment for this hour among the *displayable* appointments (NOT_FINISHED)
                val appointmentAtHour = displayDayAppointments.find { appointment ->
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
                                .clickable { onAppointmentClick(appointmentAtHour) } // Clickable card
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // --- Patient Name Loading ---
                                val patientName = remember(appointmentAtHour.user_id) { mutableStateOf("Loading...") }
                                LaunchedEffect(appointmentAtHour.user_id) {
                                    FirebaseFirestore.getInstance() // Use instance directly or inject
                                        .collection("users")
                                        .document(appointmentAtHour.user_id)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            val name = doc.getString("name") ?: ""
                                            val surname = doc.getString("surname") ?: ""
                                            patientName.value = if (name.isNotEmpty() || surname.isNotEmpty()) "$name $surname" else "Unknown"
                                        }
                                        .addOnFailureListener {
                                            patientName.value = "Unknown"
                                        }
                                }
                                // --- End Patient Name Loading ---
                                Text(
                                    text = "Patient: ${patientName.value}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Time: ${
                                        appointmentAtHour.date?.toDate()?.let {
                                            SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) // Format only time
                                        } ?: "N/A"
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        // Display an empty slot or text if no appointment
                        Text(
                            text = "Available", // Or just Spacer
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray // Indicate it's an empty slot
                        )
                    }
                }
            }
        }
    }
}

/*// Assuming AppointmentItem is a simple composable for lists
@Composable
fun AppointmentItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}*/

// You MUST have a PrescribeScreen composable defined somewhere else.
// It needs to accept patientId and an onDismiss lambda.

/*@Composable
fun PrescribeScreen(
    fromCalendar: Boolean,
    patientId: String?,
    appointmentId: String?, // Added appointmentId, you might need this
    onDismiss: () -> Unit // This is the lambda to call when done
) {
    // ... your existing PrescribeScreen UI and logic ...

    // Example of how to call onDismiss (e.g., from a back button)
    Column {
        IconButton(onClick = onDismiss) { // This button closes the screen
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        // ... rest of your prescription form/UI ...
    }
}*/
