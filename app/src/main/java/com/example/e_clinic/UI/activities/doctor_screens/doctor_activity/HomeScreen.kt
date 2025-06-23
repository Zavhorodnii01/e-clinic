package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import android.util.Log
import android.widget.Toast // Add this import
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.MedicalRecord
import com.example.e_clinic.Firebase.Repositories.AppointmentRepository
import com.example.e_clinic.Firebase.Repositories.DoctorRepository
import com.example.e_clinic.Firebase.Repositories.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.services.ZIMKit
import java.time.LocalDate
import java.time.ZoneId
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
    var comments by remember { mutableStateOf("") }

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
        // First check if appointment time has passed (with 5 second buffer)
        val currentTime = Timestamp.now()
        val appointmentTime = appointment.date ?: run {
            Toast.makeText(context, "Invalid appointment time", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if appointment time is in the future (even by 5 seconds)
        if (appointmentTime.seconds > currentTime.seconds ||
            (appointmentTime.seconds == currentTime.seconds && appointmentTime.nanoseconds > currentTime.nanoseconds)) {
            Toast.makeText(
                context,
                "Cannot finish future appointments",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Only proceed if appointment is in the past
        val record = MedicalRecord(
            appointment_id = appointment.id,
            user_id = appointment.user_id,
            doctor_id = doctorState.value ?: "",
            date = appointment.date,
            prescription_id = "",
            doctors_notes = ""
        )

        val db = FirebaseFirestore.getInstance()
        val recordRef = db.collection("medical_records").document()
        val appointmentRef = db.collection("appointments").document(appointment.id)
        medicalRecordId.value = recordRef.id

        db.runTransaction { transaction ->
            // Verify again in transaction to prevent race conditions
            val currentAppointment = transaction.get(appointmentRef)
            val apptTime = currentAppointment.getTimestamp("date")

            if (apptTime != null && (apptTime.seconds > currentTime.seconds ||
                        (apptTime.seconds == currentTime.seconds && apptTime.nanoseconds > currentTime.nanoseconds))) {
                throw Exception("Appointment is in the future")
            }

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
            val message = when (e.message) {
                "Appointment is in the future" -> "Cannot finish future appointments"
                else -> "Transaction failed: ${e.message}"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            currentAppointment.value = null
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background.copy(alpha = 0.85f) // 85% opaque, adjust as needed
            )
    ){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = when (todayAppointmentsCount.value) {
                0 -> "Hi ! You don't have any scheduled appointment today"
                1 -> "Hi ! Today you have scheduled ${todayAppointmentsCount.value} appointment"
                else -> "Hi ! Today you have scheduled ${todayAppointmentsCount.value} appointments"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DoctorAppointmentCalendar(
            appointments = appointments.filter { it.status != "FINISHED" },
            onAppointmentClick = { appointment ->
                selectedAppointment.value = appointment
            }
        )
    }
    }
    fun openChatWithPatient(appt: Appointment) {
        Log.d("ChatDebug", "Starting openChatWithPatient()")

        // 1. Validate appointment
        if (appt == null) {
            Log.e("ChatDebug", "ERROR: Null appointment object")
            Toast.makeText(context, "Invalid appointment data", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Validate patient ID
        val patientId = appt.user_id?.trim() ?: ""
        if (patientId.isBlank()) {
            Log.e("ChatDebug", "ERROR: Blank patient ID in appointment: ${appt.toString()}")
            Toast.makeText(context, "Invalid patient ID", Toast.LENGTH_LONG).show()
            return
        }
        Log.d("ChatDebug", "Patient ID validated: ${patientId.take(4)}...")

        // 3. Check Firebase auth
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e("ChatDebug", "ERROR: No authenticated Firebase user")
            Toast.makeText(context, "Please sign in first", Toast.LENGTH_LONG).show()
            return
        }

        // 4. Prepare user info with guaranteed non-empty name
        val selfId = firebaseUser.uid
        val selfName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "User_${selfId.takeLast(4)}"  // Final fallback

        Log.d("ChatDebug", "User Details - ID: ${selfId.take(4)}... | Name: $selfName")

        // 5. Verify ZIMKit initialization
        try {
            val localUser = ZIMKit.getLocalUser()
            Log.d("ChatDebug", "ZIMKit LocalUser: ${localUser?.id?.take(4)}...")

            val readyBlock = {
                Log.d("ChatDebug", "Navigating to chat with patient: ${patientId.take(4)}...")
                try {
                    ZIMKitRouter.toMessageActivity(
                        context,
                        patientId,
                        ZIMKitConversationType.ZIMKitConversationTypePeer
                    )
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Failed to start chat activity: ${e.message}")
                    Toast.makeText(context, "Failed to open chat", Toast.LENGTH_LONG).show()
                }
            }

            // 6. Connection logic with enhanced error handling
            if (localUser != null && localUser.id == selfId) {
                Log.d("ChatDebug", "Already connected to ZIMKit")
                readyBlock()
            } else {
                Log.d("ChatDebug", "Initiating ZIMKit connection...")
                ZIMKit.connectUser(selfId, selfName, "") { err ->
                    when {
                        err == null || err.code.value() == 0 -> {
                            Log.d("ChatDebug", "ZIMKit connection successful")
                            readyBlock()
                        }
                        err.code.value() == 6000011 -> { // PARAM_INVALID
                            Log.e("ChatDebug", "Invalid parameters - Name: '$selfName'")
                            Toast.makeText(context,
                                "Please set your display name in profile settings",
                                Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.e("ChatDebug",
                                "Connection failed (${err.code}): ${err.message}\n" +
                                        "Common fixes:\n" +
                                        "1. Verify ZIMKit.init() was called\n" +
                                        "2. Check Zego AppID/Signature\n" +
                                        "3. Ensure network connectivity")

                            Toast.makeText(context,
                                "Chat service unavailable (${err.code})",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDebug", "Critical error: ${e.javaClass.simpleName}\n${e.stackTraceToString()}")
            Toast.makeText(context, "Chat system error occurred", Toast.LENGTH_LONG).show()
        }
    }

    fun finishAppointmentWithComments(appointment: Appointment, comments: String = "") {
        // First check if appointment time has passed (with 5 second buffer
        val currentTime = Timestamp.now()
        val appointmentTime = appointment.date ?: run {
            Toast.makeText(context, "Invalid appointment time", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if appointment time is in the future (even by 5 seconds)
        if (appointmentTime.seconds > currentTime.seconds ||
            (appointmentTime.seconds == currentTime.seconds && appointmentTime.nanoseconds > currentTime.nanoseconds)) {
            Toast.makeText(
                context,
                "Cannot finish future appointments",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Only proceed if appointment is in the past
        val record = MedicalRecord(
            appointment_id = appointment.id,
            user_id = appointment.user_id,
            doctor_id = doctorState.value ?: "",
            date = appointment.date,
            prescription_id = "",
            doctors_notes = comments // Add comments here
        )

        val db = FirebaseFirestore.getInstance()
        val recordRef = db.collection("medical_records").document()
        val appointmentRef = db.collection("appointments").document(appointment.id)
        medicalRecordId.value = recordRef.id

        db.runTransaction { transaction ->
            // Verify again in transaction to prevent race conditions
            val currentAppointment = transaction.get(appointmentRef)
            val apptTime = currentAppointment.getTimestamp("date")

            if (apptTime != null && (apptTime.seconds > currentTime.seconds ||
                        (apptTime.seconds == currentTime.seconds && apptTime.nanoseconds > currentTime.nanoseconds))) {
                throw Exception("Appointment is in the future")
            }

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
            val message = when (e.message) {
                "Appointment is in the future" -> "Cannot finish future appointments"
                else -> "Transaction failed: ${e.message}"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            currentAppointment.value = null
        }
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
                                selectedAppointment.value?.let { appointment ->
                                    openChatWithPatient(appointment)
                                    selectedAppointment.value = null
                                }
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
            text = {
                Column {
                    Text("Would you like to add a prescription now?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Write comments (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val appointment = currentAppointment.value!!
                    // Update the finishAppointment function to include comments
                    finishAppointmentWithComments(appointment, comments)
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
                    finishAppointmentWithComments(currentAppointment.value!!, comments)
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day",  tint = colorScheme.onSurface)
            }
            Text(
                text = today.value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            IconButton(onClick = { today.value = today.value.plusDays(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Day",  tint = colorScheme.onSurface)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    if (appointmentAtHour != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .clickable { onAppointmentClick(appointmentAtHour) },
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)// Clickable card
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Time: ${
                                        appointmentAtHour.date?.toDate()?.let {
                                            SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) // Format only time
                                        } ?: "N/A"
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface
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
                            color = colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
