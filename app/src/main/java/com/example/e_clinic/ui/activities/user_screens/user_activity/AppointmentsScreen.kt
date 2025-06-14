package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
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
import com.example.e_clinic.Firebase.collections.TimeSlot
import com.example.e_clinic.Firebase.collections.specializations.DoctorSpecialization
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import im.zego.connection.internal.ZegoConnectionImpl.context


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentBookingForm(
    userId: String,
    onAppointmentBooked: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val specializations = remember { mutableStateListOf<String>() }
    val doctors = remember { mutableStateListOf<Pair<String, String>>() }
    val availableTimeSlots = remember { mutableStateListOf<Timestamp>() }

    var selectedSpecialization by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDoctor by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTimeSlot by rememberSaveable { mutableStateOf<Timestamp?>(null) }

    var specializationDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var doctorDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var timeSlotDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    // Load specializations
    LaunchedEffect(Unit) {
        specializations.clear()
        DoctorSpecialization.values().forEach { specialization ->
            specializations.add(specialization.name)
        }
    }

    // Load doctors filtered by specialization from timeslots
    LaunchedEffect(selectedSpecialization) {
        if (selectedSpecialization != null) {
            isLoading = true
            db.collection("timeslots")
                .whereEqualTo("specialization", selectedSpecialization)
                .get()
                .addOnSuccessListener { result ->
                    val doctorIds = result.documents.mapNotNull { it.getString("doctor_id") }.distinct()
                    doctors.clear()
                    availableTimeSlots.clear()
                    selectedDoctor = null
                    selectedTimeSlot = null

                    if (doctorIds.isNotEmpty()) {
                        db.collection("doctors")
                            .whereIn(FieldPath.documentId(), doctorIds)
                            .get()
                            .addOnSuccessListener { docs ->
                                doctors.addAll(docs.map {
                                    val name = "${it.getString("name")} ${it.getString("surname")}"
                                    val specialization = it.getString("specialization") ?: "General"
                                    it.id to "$name ($specialization)"
                                })
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to load doctors details", Toast.LENGTH_SHORT).show()
                            }
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                }
        } else {
            doctors.clear()
            selectedDoctor = null
            availableTimeSlots.clear()
            selectedTimeSlot = null
        }
    }

    // Load available time slots for selected doctor
    LaunchedEffect(selectedDoctor) {
        selectedTimeSlot = null
        availableTimeSlots.clear()

        if (selectedDoctor != null) {
            isLoading = true
            val doctorId = selectedDoctor!!.first

            db.collection("timeslots")
                .whereEqualTo("doctor_id", doctorId)
                .get()
                .addOnSuccessListener { result ->
                    val timeSlots = result.toObjects(TimeSlot::class.java)
                    val slots = timeSlots.flatMap { it.available_slots ?: emptyList() }.sortedBy { it.seconds }

                    availableTimeSlots.addAll(slots)

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Specialization dropdown
        ExposedDropdownMenuBox(
            expanded = specializationDropdownExpanded,
            onExpandedChange = { specializationDropdownExpanded = !specializationDropdownExpanded }
        ) {
            TextField(
                value = selectedSpecialization?.let {
                    DoctorSpecialization.valueOf(it).displayName
                } ?: "Select Specialization",
                onValueChange = {},
                readOnly = true,
                label = { Text("Specialization") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationDropdownExpanded)
                }
            )
            ExposedDropdownMenu(
                expanded = specializationDropdownExpanded,
                onDismissRequest = { specializationDropdownExpanded = false }
            ) {
                specializations.forEach { specialization ->
                    DropdownMenuItem(
                        text = { Text(DoctorSpecialization.valueOf(specialization).displayName) },
                        onClick = {
                            selectedSpecialization = specialization
                            specializationDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Doctor dropdown
        ExposedDropdownMenuBox(
            expanded = doctorDropdownExpanded,
            onExpandedChange = { doctorDropdownExpanded = it && doctors.isNotEmpty() }
        ) {
            TextField(
                value = selectedDoctor?.second ?: "Select Doctor",
                onValueChange = {},
                readOnly = true,
                label = { Text("Doctor") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded)
                },
                enabled = doctors.isNotEmpty()
            )
            ExposedDropdownMenu(
                expanded = doctorDropdownExpanded,
                onDismissRequest = { doctorDropdownExpanded = false }
            ) {
                doctors.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedDoctor = id to name
                            doctorDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Time slot dropdown
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = timeSlotDropdownExpanded,
                onExpandedChange = { timeSlotDropdownExpanded = it && availableTimeSlots.isNotEmpty() }
            ) {
                TextField(
                    value = selectedTimeSlot?.toDate()?.formatTime() ?: "Select Time Slot",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Available Time Slots") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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

        // Book button
        Button(
            onClick = {
                if (selectedDoctor != null && selectedTimeSlot != null) {
                    val (doctorId, _) = selectedDoctor!!
                    val selectedSlot = selectedTimeSlot!!
                    isLoading = true

                    val timeslotsRef = db.collection("timeslots")
                    val appointmentsRef = db.collection("appointments")

                    // Сначала получаем документ timeslot, который содержит выбранный слот
                    timeslotsRef
                        .whereEqualTo("doctor_id", doctorId)
                        .whereArrayContains("available_slots", selectedSlot)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                isLoading = false
                                Toast.makeText(context, "Selected time slot is no longer available", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            val timeslotDoc = querySnapshot.documents[0]

                            // TODO START OF TRANSACTION
                            db.runTransaction { transaction ->

                                val docSnapshot = transaction.get(timeslotDoc.reference)

                                val availableSlots = docSnapshot.get("available_slots") as? List<Timestamp> ?: emptyList()

                                if (!availableSlots.contains(selectedSlot)) {
                                    throw Exception("Selected time slot is no longer available in transaction")
                                }

                                val updatedSlots = availableSlots.toMutableList().apply {
                                    remove(selectedSlot)
                                }

                                transaction.update(timeslotDoc.reference, "available_slots", updatedSlots)

                                val newAppointment = hashMapOf(
                                    "date" to selectedSlot,
                                    "doctor_id" to doctorId,
                                    "user_id" to userId,
                                    "status" to "NOT_FINISHED"
                                )
                                transaction.set(appointmentsRef.document(), newAppointment)
                                // TODO END OF TRANSACTION
                            }.addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Appointment booked!", Toast.LENGTH_SHORT).show()
                                onAppointmentBooked()
                            }.addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Failed to book appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Failed to load time slot document", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Please select all required fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            enabled = selectedDoctor != null && selectedTimeSlot != null && !isLoading
        ) {
            Text("Book Appointment")
        }
    }
}


fun Date.formatTime(): String {
    val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return format.format(this)
}


@Composable
fun AppointmentsScreen(userId: String, onAppointmentMade: () -> Unit) {
    val context = LocalContext.current
    var showBookingForm by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val upcomingAppointments = remember { mutableStateListOf<Appointment>() }
    val pastAppointments = remember { mutableStateListOf<Appointment>() }
    val doctorsCache = remember { mutableStateMapOf<String, Doctor>() }
    var doctorsLoading by remember { mutableStateOf(false) }

    var cancelingAppointmentId by remember { mutableStateOf<String?>(null) }

    // Function to open chat with the doctor
    fun openChatWithDoctor(appointment: Appointment) {
        val doctorId = appointment.doctor_id
        if (doctorId.isBlank()) {
            Toast.makeText(context, "Invalid doctor ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Assuming userId is the current logged-in user
        ZIMKitRouter.toMessageActivity(
            context,
            doctorId, // This should be the recipient (doctor) ID
            ZIMKitConversationType.ZIMKitConversationTypePeer
        )
    }

    // Function to cancel the appointment
    fun cancel(appointment: Appointment) {
        cancelingAppointmentId = appointment.id

        FirebaseFirestore.getInstance()
            .collection("appointments")
            .document(appointment.id)
            .update("status", "CANCELLED")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cancelingAppointmentId = null
                    Toast.makeText(context, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    cancelingAppointmentId = null
                    Toast.makeText(context, "Failed to cancel appointment", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Loading upcoming appointments
    LaunchedEffect(userId) {
        isLoading = true
        upcomingAppointments.clear()
        try {
            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", "NOT_FINISHED")
                .addSnapshotListener { snapshot, firebaseError ->
                    firebaseError?.let {
                        error = "Firestore error: ${it.message}"
                        isLoading = false
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        upcomingAppointments.clear()

                        for (document in it.documents) {
                            document.toObject(Appointment::class.java)?.let { appointment ->
                                upcomingAppointments.add(appointment)

                                if (!doctorsCache.containsKey(appointment.doctor_id)) {
                                    doctorsLoading = true
                                    fetchDoctorInfo(appointment.doctor_id, doctorsCache) {
                                        doctorsLoading = false
                                    }
                                }
                            }
                        }

                        isLoading = false
                    }
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
            isLoading = false
        }
    }

    // Loading past appointments
    LaunchedEffect(userId) {
        pastAppointments.clear()
        try {
            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("user_id", userId)
                .whereIn("status", listOf("FINISHED", "CANCELLED"))
                .addSnapshotListener { snapshot, firebaseError ->
                    firebaseError?.let {
                        error = "Firestore error: ${it.message}"
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        pastAppointments.clear()

                        for (document in it.documents) {
                            document.toObject(Appointment::class.java)?.let { appointment ->
                                pastAppointments.add(appointment)

                                if (!doctorsCache.containsKey(appointment.doctor_id)) {
                                    doctorsLoading = true
                                    fetchDoctorInfo(appointment.doctor_id, doctorsCache) {
                                        doctorsLoading = false
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            error = "Exception: ${e.localizedMessage}"
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { showBookingForm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("Make New Appointment")
            }
            Spacer(modifier = Modifier.height(16.dp))

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
            // Sort past appointments with most recent first
            pastAppointments.sortByDescending { it.date } // Replace with actual datetime field
            when (selectedTab) {
                0 -> AppointmentList(
                    appointments = upcomingAppointments,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No upcoming appointments",
                    showCancel = true,
                    onCancel = { appointment -> cancel(appointment) },
                    onStartChat = { appointment -> openChatWithDoctor(appointment) }
                )
                1 -> AppointmentList(
                    appointments = pastAppointments,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No past appointments"
                )
            }
        }

        if (cancelingAppointmentId != null) {
            Box(
                Modifier
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
                        Text("Cancelling appointment...")
                    }
                }
            }
        }
    }

    if (showBookingForm) {
        AlertDialog(
            onDismissRequest = { showBookingForm = false },
            title = { Text("Book New Appointment") },
            text = {
                AppointmentBookingForm(
                    userId = userId,
                    onAppointmentBooked = {
                        onAppointmentMade()
                        showBookingForm = false
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBookingForm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
fun AppointmentList(
    appointments: List<Appointment>,
    doctorsCache: Map<String, Doctor>,
    emptyMessage: String,
    showCancel: Boolean = false,
    onCancel: (Appointment) -> Unit = {},
    onStartChat: (Appointment) -> Unit = {},
) {
    if (appointments.isEmpty()) {
        Text(
            emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(appointments, key = { it.id }) { appointment ->
            val doctor = doctorsCache[appointment.doctor_id]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doctor?.name ?: "Loading doctor…",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Date: ${appointment.date?.toDate()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Status: ${appointment.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (appointment.status == "CANCELLED") Color.Red else Color.Unspecified
                        )
                    }

                    if (appointment.status == "NOT_FINISHED") {
                        Row {
                            if (showCancel) {
                                IconButton(
                                    onClick = { onCancel(appointment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Cancel Appointment",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onStartChat(appointment) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Start Chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Logs in to Zego (if not already) and opens the peer chat with this doctor. */
fun openChatWithDoctor(appt: Appointment) {
    val doctorId = appt.doctor_id
    if (doctorId.isBlank()) {
        Toast.makeText(context, "Invalid doctor ID", Toast.LENGTH_SHORT).show()
        return
    }

    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    if (firebaseUser == null) {
        Toast.makeText(context, "You must be logged in first.", Toast.LENGTH_SHORT).show()
        return
    }

    val selfId      = firebaseUser.uid
    val selfName    = firebaseUser.displayName ?: firebaseUser.email ?: selfId
    val selfAvatar  = ""   // optional avatar URL

    /* If we’re already the same Zego user, skip connectUser. */
    val localUser = com.zegocloud.zimkit.services.ZIMKit.getLocalUser()
    val readyBlock = {
        // ✨ jump directly into the 1-on-1 chat
        ZIMKitRouter.toMessageActivity(
            context,
            doctorId,
            ZIMKitConversationType.ZIMKitConversationTypePeer
        )
    }

    if (localUser != null && localUser.id == selfId) {
        readyBlock()
    } else {
        com.zegocloud.zimkit.services.ZIMKit.connectUser(selfId, selfName, selfAvatar) { err ->
            if (err == null || err.code.value() == 0) {
                readyBlock()
            } else {
                Toast.makeText(context, "Chat login failed: ${err.message}", Toast.LENGTH_SHORT).show()
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
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}
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
}


/**
 * Cancels an appointment in a single Firestore transaction:
 *  1. Verifies the appointment is still active.
 *  2. Marks the appointment document as "CANCELLED".
 *  3. Adds the timeslot back to the doctor's available_slots array
 *     (only if it is not already there).
 */
fun cancelAppointment(
    appointment: Appointment,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val appointmentsRef = db.collection("appointments").document(appointment.id)

    // STEP 1 – find the timeslot document for this doctor *outside* the transaction
    db.collection("timeslots")
        .whereEqualTo("doctor_id", appointment.doctor_id)
        .limit(1)
        .get()
        .addOnSuccessListener { qs ->
            if (qs.isEmpty) {
                onFailure("Timeslot document not found for doctor")
                return@addOnSuccessListener
            }

            val timeslotRef = qs.documents[0].reference

            // STEP 2 – run the actual transaction
            // TODO START OF TRANSACTION
            db.runTransaction { txn ->
                /* ----- 2a. validate / update appointment  ----- */
                val apptSnap   = txn.get(appointmentsRef)
                val status     = apptSnap.getString("status")

                if (status == null || status == "CANCELLED") {
                    throw Exception("Appointment already cancelled or not found")
                }
                txn.update(appointmentsRef, "status", "CANCELLED")

                /* ----- 2b. return slot to available_slots ----- */
                val slotSnap        = txn.get(timeslotRef)
                val currentSlots    =
                    (slotSnap.get("available_slots") as? List<Timestamp>)?.toMutableList()
                        ?: mutableListOf()

                if (!currentSlots.contains(appointment.date)) {
                    currentSlots.add(appointment.date as Timestamp)   // put it back
                }

                txn.update(timeslotRef, "available_slots", currentSlots)
            }
                // TODO END OF TRANSACTION
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
        }
        .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
}
