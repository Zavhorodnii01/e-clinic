package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentBookingForm(
    userId: String,
    onAppointmentBooked: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // State management
    val doctors = remember { mutableStateListOf<Pair<String, String>>() }
    val availableTimeSlots = remember { mutableStateListOf<Timestamp>() }

    var selectedDoctor by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var selectedDate by rememberSaveable { mutableStateOf<Timestamp?>(null) }
    var selectedTimeSlot by rememberSaveable { mutableStateOf<Timestamp?>(null) }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var timeSlotDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch doctors
    LaunchedEffect(Unit) {
        db.collection("doctors").get().addOnSuccessListener { result ->
            doctors.clear()
            result.documents.forEach { document ->
                val name = "${document.getString("name")} ${document.getString("surname")}"
                val specialization = document.getString("specialization") ?: "General"
                doctors.add(document.id to "$name ($specialization)")
            }
        }
    }

    // Fetch available time slots when doctor or date changes
    LaunchedEffect(selectedDoctor, selectedDate) {
        selectedTimeSlot = null
        availableTimeSlots.clear()

        if (selectedDoctor != null && selectedDate != null) {
            isLoading = true
            val (doctorId, _) = selectedDoctor!!

            // Calculate start and end of selected day
            val calendar = Calendar.getInstance().apply {
                time = selectedDate!!.toDate()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startOfDay = Timestamp(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = Timestamp(calendar.time)

            db.collection("timeSlots")
                .whereEqualTo("doctor_id", doctorId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .addOnSuccessListener { result ->
                    result.documents.forEach { doc ->
                        val slots = doc.get("available_slots") as? List<Timestamp> ?: emptyList()
                        availableTimeSlots.addAll(slots.sortedBy { it.seconds })
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Failed to load time slots", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Doctor selection dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            TextField(
                value = selectedDoctor?.second ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Doctor") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                }
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                doctors.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedDoctor = id to name
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Date selection
        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        selectedDate = Timestamp(selectedCalendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.toDate()?.formatDate() ?: "Select Date")
        }

        // Time slot selection
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            ExposedDropdownMenuBox(
                expanded = timeSlotDropdownExpanded,
                onExpandedChange = { timeSlotDropdownExpanded = it && availableTimeSlots.isNotEmpty() }
            ) {
                TextField(
                    value = selectedTimeSlot?.toDate()?.formatTime() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Available Time Slots") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeSlotDropdownExpanded)
                    },
                    enabled = availableTimeSlots.isNotEmpty()
                )
                ExposedDropdownMenu(
                    expanded = timeSlotDropdownExpanded,
                    onDismissRequest = { timeSlotDropdownExpanded = false }
                ) {
                    availableTimeSlots.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slot.toDate().formatTime()) },
                            onClick = {
                                selectedTimeSlot = slot
                                timeSlotDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Book appointment button
        Button(
            onClick = {
                if (selectedDoctor != null && selectedTimeSlot != null) {
                    val (doctorId, _) = selectedDoctor!!

                    val appointment = Appointment(
                        date = selectedTimeSlot!!,
                        doctor_id = doctorId,
                        user_id = userId,
                        status = "NOT_FINISHED"
                    )

                    // First book the appointment
                    AppointmentRepository().bookAppointment(appointment) { success ->
                        if (success) {
                            // Then remove the time slot from availability
                            db.collection("timeSlots")
                                .whereEqualTo("doctor_id", doctorId)
                                .whereArrayContains("available_slots", selectedTimeSlot!!)
                                .get()
                                .addOnSuccessListener { docs ->
                                    docs.forEach { doc ->
                                        val updatedSlots =
                                            (doc.get("available_slots") as List<Timestamp>)
                                                .minus(selectedTimeSlot!!)

                                        doc.reference.update("available_slots", updatedSlots)
                                    }
                                    Toast.makeText(context, "Appointment booked!", Toast.LENGTH_SHORT).show()
                                    onAppointmentBooked()
                                }
                        } else {
                            Toast.makeText(context, "Failed to book appointment", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please select all required fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDoctor != null && selectedTimeSlot != null
        ) {
            Text("Book Appointment")
        }
    }
}

// Date formatting extensions
fun Date.formatDate(): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)
fun Date.formatTime(): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(this)





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(userId: String, onAppointmentMade: () -> Unit) {
    val context = LocalContext.current
    var showBookingForm by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val allAppointments = remember { mutableStateListOf<Appointment>() }
    val doctorsCache = remember { mutableMapOf<String, Doctor>() }
    var doctorsLoading by remember { mutableStateOf(false) }

    // Debugging state
    var debugInfo by remember { mutableStateOf("") }
    val upcomingAppointments = remember{ mutableStateListOf<Appointment>()}
   /* val upcomingAppointments = remember(allAppointments) {
        allAppointments.filter { appointment ->
            // Upcoming: NOT_FINISHED and date is in future (if date exists)

                    (appointment.date?.toDate()?.after(Date()) ?: true)
        }.sortedBy { it.date?.toDate() }
    }*/


    val pastAppointments = remember {  mutableStateListOf<Appointment>()}

    LaunchedEffect(userId) {
        try {
            debugInfo = "Starting data fetch..."

            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", "NOT_FINISHED")
                .addSnapshotListener { snapshot, firebaseError ->
                    firebaseError?.let {
                        error = "Firestore error: ${it.message}"
                        isLoading = false
                        debugInfo = "Error: ${it.message}"
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        error = "No data returned from Firestore"
                        isLoading = false
                        debugInfo = "Snapshot is null"
                        return@addSnapshotListener
                    }

                    debugInfo = "Received ${snapshot.documents.size} documents"

                    snapshot.documents.forEach { document ->
                        document.toObject(Appointment::class.java)?.let { appointment ->
                            if (upcomingAppointments.none { it.id == appointment.id }) {
                                upcomingAppointments.add(appointment)
                                if (!doctorsCache.containsKey(appointment.doctor_id)) {
                                    doctorsLoading = true
                                    fetchDoctorInfo(appointment.doctor_id, doctorsCache) {
                                        doctorsLoading = doctorsCache.size <
                                                upcomingAppointments.distinctBy { it.doctor_id }.size
                                    }
                                }
                            }
                        }
                    }

                    if (upcomingAppointments.isNotEmpty()) {
                        isLoading = false
                        debugInfo = "Loaded ${upcomingAppointments.size} appointments"

                    } else {
                        debugInfo = "No appointments found for user $userId"
                    }
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
            isLoading = false
            debugInfo = "Caught exception: ${e.stackTraceToString()}"
        }
    }

    LaunchedEffect(userId) {
        try {
            debugInfo = "Starting data fetch..."

            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("user_id", userId)
                .whereIn("status", listOf("FINISHED", "CANCELLED"))
                .addSnapshotListener { snapshot, firebaseError ->
                    firebaseError?.let {
                        error = "Firestore error: ${it.message}"
                        isLoading = false
                        debugInfo = "Error: ${it.message}"
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        error = "No data returned from Firestore"
                        isLoading = false
                        debugInfo = "Snapshot is null"
                        return@addSnapshotListener
                    }

                    debugInfo = "Received ${snapshot.documents.size} documents"

                    snapshot.documents.forEach { document ->
                        document.toObject(Appointment::class.java)?.let { appointment ->
                            if (pastAppointments.none { it.id == appointment.id }) {
                                pastAppointments.add(appointment)
                                if (!doctorsCache.containsKey(appointment.doctor_id)) {
                                    doctorsLoading = true
                                    fetchDoctorInfo(appointment.doctor_id, doctorsCache) {
                                        doctorsLoading = doctorsCache.size <
                                                pastAppointments.distinctBy { it.doctor_id }.size
                                    }
                                }
                            }
                        }
                    }

                    if (pastAppointments.isNotEmpty()) {
                        isLoading = false
                        debugInfo = "Loaded ${upcomingAppointments.size} appointments"

                    } else {
                        debugInfo = "No appointments found for user $userId"
                    }
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
            isLoading = false
            debugInfo = "Caught exception: ${e.stackTraceToString()}"
        }
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Debug info (remove in production)
        Text(
            text = debugInfo,
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading || doctorsLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (doctorsLoading) {
                        Text("Loading doctor information...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if (error != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error loading appointments", color = MaterialTheme.colorScheme.error)
                Text(error!!, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { isLoading = true }) {
                    Text("Retry")
                }
            }
        } else {
            // Tab selection
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

            // Content
            when (selectedTab) {
                0 -> AppointmentList(
                    appointments = upcomingAppointments,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No upcoming appointments",
                    showCancel = true,
                    onCancel = { showCancelDialog(context, it) }
                )
                1 -> AppointmentList(
                    appointments = pastAppointments,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No past appointments"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showBookingForm = !showBookingForm }) {
            Text(if (showBookingForm) "Hide Booking Form" else "Make New Appointment")
        }

        if (showBookingForm) {
            AppointmentBookingForm(userId = userId) {
                onAppointmentMade()
                showBookingForm = false
            }
        }

        /*Button(
            onClick = {
                context.startActivity(Intent(context, UserActivity::class.java))
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Return")
        }*/
    }
}

// Helper function for fetching doctor info
private fun fetchDoctorInfo(
    doctorId: String,
    doctorsCache: MutableMap<String, Doctor>,
    onComplete: () -> Unit = {}
) {
    FirebaseFirestore.getInstance()
        .collection("doctors")
        .document(doctorId)
        .get()
        .addOnSuccessListener { document ->
            document.toObject(Doctor::class.java)?.let {
                doctorsCache[doctorId] = it
            }
            onComplete()
        }
        .addOnFailureListener {
            onComplete()
        }
}@Composable
private fun AppointmentList(
    appointments: List<Appointment>,
    doctorsCache: Map<String, Doctor>,
    emptyMessage: String,
    showCancel: Boolean = false,
    onCancel: ((String) -> Unit)? = null
) {
    if (appointments.isEmpty()) {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
    } else {
        LazyColumn {
            items(appointments) { appointment ->
                AppointmentItem(
                    appointment = appointment,
                    doctor = doctorsCache[appointment.doctor_id],
                    onCancel = if (showCancel && appointment.status == "NOT_FINISHED") {
                        { onCancel?.invoke(appointment.id) }
                    } else null
                )
            }
        }
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
            else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun AppointmentItem(
    appointment: Appointment,
    doctor: Doctor? = null,
    onCancel: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (appointment.status) {
                "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                "FINISHED" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            doctor?.let {
                Text(
                    text = "Dr. ${it.name} ${it.surname} (${it.specialization})",
                    style = MaterialTheme.typography.titleMedium
                )
            } ?: Text(
                text = "Loading doctor info...",
                style = MaterialTheme.typography.titleMedium
            )

            appointment.date?.let {
                Text(
                    text = dateFormat.format(it.toDate()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status: ", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = appointment.status
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = when (appointment.status) {
                            "CANCELLED" -> MaterialTheme.colorScheme.error
                            "FINISHED" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }

            onCancel?.let {
                Button(
                    onClick = it,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun fetchDoctorInfo(
    doctorId: String,
    doctorsCache: MutableMap<String, Doctor>
) {
    FirebaseFirestore.getInstance()
        .collection("doctors")
        .document(doctorId)
        .get()
        .addOnSuccessListener { document ->
            document.toObject(Doctor::class.java)?.let {
                doctorsCache[doctorId] = it
            }
        }
}

private fun showCancelDialog(context: Context, appointmentId: String) {
    AlertDialog.Builder(context)
        .setTitle("Cancel Appointment")
        .setMessage("Are you sure you want to cancel this appointment?")
        .setPositiveButton("Yes") { _, _ ->
            cancelAppointment(appointmentId, context)
        }
        .setNegativeButton("No", null)
        .show()
}

private fun cancelAppointment(appointmentId: String, context: Context) {
    FirebaseFirestore.getInstance()
        .collection("appointments")
        .document(appointmentId)
        .update("status", "CANCELLED")
        .addOnSuccessListener {
            Toast.makeText(context, "Appointment cancelled", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

@Preview(showBackground = true)
@Composable
fun AppointmentsScreenPreview() {
    AppointmentsScreen(userId = "test_user") {}
}