package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.specializations.DoctorSpecialization
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.sharp.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.MedicalRecord
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Prescription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.zegocloud.zimkit.services.ZIMKit


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

    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    val types = listOf("ONLINE", "IN_CLINIC")
    var typeDropdownExpanded by rememberSaveable { mutableStateOf(false) }

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

        // Type dropdown
        ExposedDropdownMenuBox(
            expanded = typeDropdownExpanded,
            onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
        ) {
            TextField(
                value = selectedType ?: "Select Type",
                onValueChange = {},
                readOnly = true,
                label = { Text("Appointment Type") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                }
            )
            ExposedDropdownMenu(
                expanded = typeDropdownExpanded,
                onDismissRequest = { typeDropdownExpanded = false }
            ) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(formatEnumString(type)) },
                        onClick = {
                            selectedType = type
                            typeDropdownExpanded = false
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
                                    "status" to "NOT_FINISHED",
                                    "type" to selectedType,
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
    val medicalRecords = remember { mutableStateListOf<MedicalRecord>() }
    val doctorsCache = remember { mutableStateMapOf<String, Doctor>() }
    var doctorsLoading by remember { mutableStateOf(false) }

    var cancelingAppointmentId by remember { mutableStateOf<String?>(null) }
    var showingPrescription by remember { mutableStateOf<Prescription?>(null) }
    var showingNotes by remember { mutableStateOf<String?>(null) }

    // Function to open chat with the doctor
    fun openChatWithDoctor(appt: Appointment) {
        Log.d("ChatDebug", "Starting openChatWithPatient()")

        // 1. Validate appointment
        if (appt == null) {
            Log.e("ChatDebug", "ERROR: Null appointment object")
            Toast.makeText(context, "Invalid appointment data", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Validate patient ID
        val doctorId = appt.doctor_id?.trim() ?: ""
        if (doctorId.isBlank()) {
            Log.e("ChatDebug", "ERROR: Blank patient ID in appointment: ${appt.toString()}")
            Toast.makeText(context, "Invalid patient ID", Toast.LENGTH_LONG).show()
            return
        }
        Log.d("ChatDebug", "Patient ID validated: ${doctorId.take(4)}...")

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
                Log.d("ChatDebug", "Navigating to chat with patient: ${doctorId.take(4)}...")
                try {
                    ZIMKitRouter.toMessageActivity(
                        context,
                        doctorId,
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
    // Function to cancel the appointment
    fun cancel(appointment: Appointment) {
        cancelingAppointmentId = appointment.id

        FirebaseFirestore.getInstance()
            .collection("appointments")
            .document(appointment.id)
            .update("status", "CANCELED")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cancelingAppointmentId = null
                    Toast.makeText(context, "Appointment canceled", Toast.LENGTH_SHORT).show()
                } else {
                    cancelingAppointmentId = null
                    Toast.makeText(context, "Failed to cancel appointment", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to show prescription
    fun showPrescription(prescriptionId: String) {
        FirebaseFirestore.getInstance()
            .collection("prescriptions")
            .document(prescriptionId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Prescription::class.java)?.let {
                    showingPrescription = it
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load prescription", Toast.LENGTH_SHORT).show()
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

    // Loading medical records (past appointments)
    LaunchedEffect(userId) {
        medicalRecords.clear()
        try {
            FirebaseFirestore.getInstance()
                .collection("medical_records")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener { snapshot, firebaseError ->
                    firebaseError?.let {
                        error = "Firestore error: ${it.message}"
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        medicalRecords.clear()

                        for (document in it.documents) {
                            document.toObject(MedicalRecord::class.java)?.let { medicalRecord ->
                                medicalRecords.add(medicalRecord)

                                if (!doctorsCache.containsKey(medicalRecord.doctor_id)) {
                                    doctorsLoading = true
                                    fetchDoctorInfo(medicalRecord.doctor_id, doctorsCache) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
    )  {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Appointments",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                    text = "Medical Records (${medicalRecords.size})",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            medicalRecords.sortByDescending { it.date }
            when (selectedTab) {
                0 -> AppointmentList(
                    appointments = upcomingAppointments,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No upcoming appointments",
                    showCancel = true,
                    onCancel = { appointment -> cancel(appointment) },
                    onStartChat = { appointment -> openChatWithDoctor(appointment) }
                )
                1 -> MedicalRecordsList(
                    medicalRecords = medicalRecords,
                    doctorsCache = doctorsCache,
                    emptyMessage = "No medical records found",
                    onShowPrescription = { prescriptionId -> showPrescription(prescriptionId) },
                    onShowNotes = { notes -> showingNotes = notes }
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

        showingPrescription?.let { prescription ->
            PrescriptionDialog(
                prescription = prescription,
                onDismiss = { showingPrescription = null }
            )
        }

        showingNotes?.let { notes ->
            AlertDialog(
                onDismissRequest = { showingNotes = null },
                title = { Text("Doctor's Notes") },
                text = {
                    Text(notes)
                },
                confirmButton = {
                    TextButton(onClick = { showingNotes = null }) {
                        Text("Close")
                    }
                }
            )
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
fun PrescriptionDialog(
    prescription: Prescription,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val doctorName = remember { mutableStateOf("Loading...") }
    var imageUrl by remember { mutableStateOf("") }
    var showQRCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(prescription.doctor_id) {
        if (prescription.doctor_id.isNotEmpty()) {
            db.collection("doctors").document(prescription.doctor_id).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "Unknown"
                    val surname = document.getString("surname") ?: "User"
                    doctorName.value = "$name $surname"
                }
                .addOnFailureListener {
                    doctorName.value = "Unknown User"
                }
        } else {
            doctorName.value = "Unknown User"
        }

        // Load the image URL
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(prescription.link_to_storage)
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            imageUrl = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prescription from ${doctorName.value}") },
        text = {
            Column {
                Text("Date of Issue: ${prescription.issued_date?.toDate()}",
                    style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                if (imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Prescription Image",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = { showQRCodeDialog = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Show QR Code")
                }
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showQRCodeDialog) {
        showQRCodeDialog(prescription.link_to_storage) {
            showQRCodeDialog = false
        }
    }
}

@Composable
fun MedicalRecordsList(
    medicalRecords: List<MedicalRecord>,
    doctorsCache: Map<String, Doctor>,
    emptyMessage: String,
    onShowPrescription: (String) -> Unit,
    onShowNotes: (String) -> Unit
) {
    if (medicalRecords.isEmpty()) {
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
        items(medicalRecords, key = { it.id }) { record ->
            val doctor = doctorsCache[record.doctor_id]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Doctor Information Section
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "Dr. ${doctor?.name ?: "Unknown"} ${doctor?.surname ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        doctor?.specialization?.let { specialization ->
                            if (specialization.isNotEmpty()) {
                                Text(
                                    text = "Specialization: ${formatEnumString(specialization).uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Divider
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )

                    // Appointment Information Section
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "Date: ${record.date?.toDate()?.formatDateTime() ?: "Unknown date"}",
                            style = MaterialTheme.typography.bodyMedium
                        )


                        Spacer(modifier = Modifier.height(4.dp))

                        if (record.doctors_notes.isNotEmpty()) {
                            Text(
                                text = "Notes: ${record.doctors_notes.take(50)}...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { onShowPrescription(record.prescription_id) },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Star,
                                contentDescription = "Prescription",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prescription")
                        }

                        Button(
                            onClick = { onShowNotes(record.doctors_notes) },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Notes",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notes")
                        }
                    }
                }
            }
        }
    }
}

// Extension function to format date (add this somewhere in your utilities)
fun Date.formatDateTime(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return dateFormat.format(this)
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
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!doctor?.profilePicture.isNullOrEmpty()) {
                            AsyncImage(
                                model = doctor?.profilePicture,
                                contentDescription = "Doctor Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Doctor",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dr. ${doctor?.name ?: "Unknown"} ${doctor?.surname ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                            Text(
                                text = "Date: ${appointment.date?.toDate()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Status: ${formatEnumString(appointment.status)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (appointment.status == "CANCELED") Color.Red else Color.Unspecified
                            )
                            Text(
                                text = "Type: ${formatEnumString(appointment.type)}",
                                style = MaterialTheme.typography.bodySmall
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
 *  2. Marks the appointment document as "CANCELED".
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

                if (status == null || status == "CANCELED") {
                    throw Exception("Appointment already canceled or not found")
                }
                txn.update(appointmentsRef, "status", "CANCELED")

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

