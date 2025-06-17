package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.collections.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DoctorAppointments"

@Composable
fun AppointmentsScreen(doctorId: String) {
    Log.d(TAG, "AppointmentsScreen loaded with doctorId: $doctorId")
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val upcomingAppointments = remember { mutableStateListOf<Appointment>() }
    val pastAppointments = remember { mutableStateListOf<Appointment>() }
    val patientsCache = remember { mutableStateMapOf<String, User>() }
    var patientsLoading by remember { mutableStateOf(false) }
    var completingAppointmentId by remember { mutableStateOf<String?>(null) }

    // Debug log for initial state
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing appointments screen")
    }

    // Function to open chat with the patient
    fun openChatWithPatient(appointment: Appointment) {
        Log.d(TAG, "Attempting to chat with patient: ${appointment.user_id}")
        val patientId = appointment.user_id
        if (patientId.isBlank()) {
            Toast.makeText(context, "Invalid patient ID", Toast.LENGTH_SHORT).show()
            return
        }
        ZIMKitRouter.toMessageActivity(
            context,
            patientId,
            ZIMKitConversationType.ZIMKitConversationTypePeer
        )
    }

    // Function to complete the appointment
    fun completeAppointment(appointment: Appointment) {
        Log.d(TAG, "Completing appointment: ${appointment.id}")
        completingAppointmentId = appointment.id
        FirebaseFirestore.getInstance()
            .collection("appointments")
            .document(appointment.id)
            .update("status", "FINISHED")
            .addOnCompleteListener { task ->
                completingAppointmentId = null
                if (task.isSuccessful) {
                    Log.d(TAG, "Appointment ${appointment.id} completed successfully")
                    Toast.makeText(context, "Appointment completed", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to complete appointment: ${task.exception?.message}")
                    Toast.makeText(context, "Failed to complete appointment", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Loading upcoming appointments
    LaunchedEffect(doctorId) {
        Log.d(TAG, "Loading upcoming appointments for doctor: $doctorId")
        isLoading = true
        upcomingAppointments.clear()
        try {
            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("doctor_id", doctorId) // Match your Firestore field name
                .whereEqualTo("status", "NOT_FINISHED")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d(TAG, "Upcoming appointments query completed")
                    if (snapshot.isEmpty) {
                        Log.d(TAG, "No upcoming appointments found")
                    } else {
                        Log.d(TAG, "Found ${snapshot.size()} upcoming appointments")
                        for (document in snapshot.documents) {
                            try {
                                val appointment = document.toObject(Appointment::class.java)
                                appointment?.let {
                                    Log.d(TAG, "Appointment details: $it")
                                    upcomingAppointments.add(it)
                                    // Fetch patient info if not cached
                                    if (!patientsCache.containsKey(it.user_id)) { // Match your Firestore field name
                                        Log.d(TAG, "Fetching patient info for ${it.user_id}")
                                        fetchPatientInfo(it.user_id, patientsCache) {
                                            patientsLoading = false
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing document: ${e.message}")
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading appointments: ${e.message}")
                    error = "Failed to load appointments: ${e.localizedMessage}"
                    isLoading = false
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
            Log.e(TAG, "Exception loading upcoming appointments: ${e.message}")
            isLoading = false
        }
    }
    // Loading past appointments
    LaunchedEffect(doctorId) {
        Log.d(TAG, "Loading past appointments for doctor: $doctorId")
        try {
            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("doctor_id", doctorId) // Match your Firestore field name
                .whereIn("status", listOf("FINISHED", "CANCELLED"))
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d(TAG, "Past appointments query completed")
                    if (snapshot.isEmpty) {
                        Log.d(TAG, "No past appointments found")
                    } else {
                        Log.d(TAG, "Found ${snapshot.size()} past appointments")
                        pastAppointments.clear()
                        for (document in snapshot.documents) {
                            try {
                                val appointment = document.toObject(Appointment::class.java)
                                appointment?.let {
                                    pastAppointments.add(it)
                                    if (!patientsCache.containsKey(it.user_id)) { // Match your Firestore field name
                                        patientsLoading = true
                                        fetchPatientInfo(it.user_id, patientsCache) {
                                            patientsLoading = false
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing past appointment: ${e.message}")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading past appointments: ${e.message}")
                    error = "Failed to load past appointments: ${e.localizedMessage}"
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
            Log.e(TAG, "Exception loading past appointments: ${e.message}")
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton(
                    text = "Upcoming (${upcomingAppointments.size})",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    text = "Past (${pastAppointments.size})",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            when (selectedTab) {
                0 -> DoctorAppointmentList(
                    appointments = upcomingAppointments,
                    patientsCache = patientsCache,
                    emptyMessage = "No upcoming appointments",
                    showComplete = true,
                    onComplete = ::completeAppointment,
                    onStartChat = ::openChatWithPatient
                )
                1 -> DoctorAppointmentList(
                    appointments = pastAppointments,
                    patientsCache = patientsCache,
                    emptyMessage = "No past appointments"
                )
            }
        }

        if (isLoading || patientsLoading) {
            Log.d(TAG, "Showing loading indicator")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading appointments...")
                    }
                }
            }
        }

        if (completingAppointmentId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Completing appointment...")
                    }
                }
            }
        }

        error?.let {
            Log.e(TAG, "Showing error: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            error = null
        }
    }
}

// Rest of the composables remain the same (TabButton, DoctorAppointmentList, AppointmentCard)

private fun fetchPatientInfo(patientId: String, cache: MutableMap<String, User>, onComplete: () -> Unit) {
    Log.d(TAG, "Fetching patient info for: $patientId")
    FirebaseFirestore.getInstance()
        .collection("patients")
        .document(patientId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                user?.let {
                    Log.d(TAG, "Found patient: ${it.name}")
                    cache[patientId] = it
                } ?: run {
                    Log.e(TAG, "Patient document exists but couldn't be parsed")
                }
            } else {
                Log.e(TAG, "Patient document doesn't exist")
            }
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error fetching patient: ${e.message}")
            onComplete()
        }
}
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun DoctorAppointmentList(
    appointments: List<Appointment>,
    patientsCache: Map<String, User>,
    emptyMessage: String,
    showComplete: Boolean = false,
    onComplete: (Appointment) -> Unit = {},
    onStartChat: (Appointment) -> Unit = {}
) {
    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(emptyMessage)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    patient = patientsCache[appointment.user_id],
                    showComplete = showComplete,
                    onComplete = onComplete,
                    onStartChat = onStartChat
                )
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    patient: User?,
    showComplete: Boolean,
    onComplete: (Appointment) -> Unit,
    onStartChat: (Appointment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Patient",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = patient?.name ?: "Unknown Patient",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Format the date and time properly
            val dateTime = appointment.date?.toDate() ?: Date()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            Text(text = "Date: ${dateFormat.format(dateTime)}")
            Text(text = "Time: ${timeFormat.format(dateTime)}")


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (showComplete) {
                    Button(
                        onClick = { onComplete(appointment) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Complete")
                    }
                }
                Button(
                    onClick = { onStartChat(appointment) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("Chat")
                }
            }
        }
    }
}

