package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

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
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.MedicalRecord
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Prescription
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.example.e_clinic.Firebase.Repositories.AppointmentRepository
import com.example.e_clinic.UI.activities.user_screens.user_activity.PrescriptionDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.services.ZIMKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


// Assuming PrescribeScreen is defined elsewhere and imported correctly
// Make sure you have this import. Example:
// import com.example.e_clinic.ui.activities.doctor_screens.PrescribeScreen


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

    // State variables for the finish/prescription flow
    var appointmentToComplete by remember { mutableStateOf<Appointment?>(null) } // Stores the appointment when "Complete" is clicked
    var showPrescriptionChoiceDialog by remember { mutableStateOf(false) } // Controls the visibility of the "Add Prescription?" dialog
    var showPrescribeScreen by remember { mutableStateOf(false) } // Controls the visibility of the PrescribeScreen
    var medicalRecordIdForPrescription by remember { mutableStateOf<String?>(null) } // Stores the ID of the created medical record

    val doctorState = remember { mutableStateOf<String?>(null) } // Keeping this as per original script
    val auth = FirebaseAuth.getInstance()
    val doctorEmail = auth.currentUser?.email ?: ""
    val db = FirebaseFirestore.getInstance()
    val appointmentRepository = AppointmentRepository()
    // val appointments = remember { mutableStateListOf<Appointment>() } // This seems unused in the original script, focusing on upcoming/past


    // LaunchedEffect to get doctorId from email if needed (from original script)
    // Keep this as requested, assuming doctorId param might be empty in some cases.
    LaunchedEffect(doctorEmail) {
        if (doctorId.isEmpty() && doctorEmail.isNotEmpty()) {
            db.collection("doctors").whereEqualTo("e-mail", doctorEmail).get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doctor = documents.documents[0]
                    doctorState.value = doctor.id
                    // Use doctorState.value going forward for data loading
                } else {
                    Log.w(TAG, "Doctor document not found for email: $doctorEmail")
                    // Consider setting an error state or handling this case
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error fetching doctor by email", e)
                error = "Failed to load doctor data."
            }
        } else if (doctorId.isNotEmpty()){
            doctorState.value = doctorId // Use passed doctorId if available
        }
    }


    // Debug log for initial state
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing appointments screen")
    }

    // Keep existing openChatWithPatient function as is
    fun openChatWithPatient(appt: Appointment) {
        // ... (Your existing openChatWithPatient function logic) ...
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
                // Ensure AppID and Signature are set correctly via ZIMKit.init in your Application class
               /* if (BuildConfig.ZEGO_APP_ID.toLongOrNull() == null || BuildConfig.ZEGO_APP_SIGN.isBlank()) {
                    Log.e("ChatDebug", "Zego AppID or Signature is invalid/missing in BuildConfig")
                    Toast.makeText(context, "Chat service configuration error", Toast.LENGTH_LONG).show()
                    return
                }*/
                ZIMKit.connectUser(selfId, selfName, "") { err ->
                    when {
                        err == null || err.code.value() == 0 -> {
                            Log.d("ChatDebug", "ZIMKit connection successful")
                            readyBlock()
                        }
                        err.code.value() == 6000011 -> { // PARAM_INVALID
                            Log.e("ChatDebug", "Invalid parameters - Name: '$selfName' (code: ${err.code})")
                            Toast.makeText(context,
                                "Please set your display name in profile settings",
                                Toast.LENGTH_LONG).show()
                        }
                        err.code.value() == 1000003 -> { // AUTH_FAILED
                            Log.e("ChatDebug", "ZIMKit Authentication Failed (code: ${err.code}) - Check AppID/Signature")
                            Toast.makeText(context,
                                "Chat authentication failed. Contact support.",
                                Toast.LENGTH_LONG).show()
                        }
                        err.code.value() == 1000004 -> { // TOKEN_EXPIRED
                            Log.e("ChatDebug", "ZIMKit Token Expired (code: ${err.code}) - Needs token generation logic")
                            // In a real app, you'd re-generate and reconnect here
                            Toast.makeText(context,
                                "Chat token expired. Please try again.",
                                Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.e("ChatDebug",
                                "Connection failed (${err.code}): ${err.message}")

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

    // Function to complete the appointment (renamed slightly for clarity)
    // This function performs the actual Firestore transaction to finish the appointment
    fun performFinishAppointmentTransaction(appointment: Appointment, comments: String = "") {
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

        // Create a MedicalRecord object with comments
        val record = MedicalRecord(
            appointment_id = appointment.id,
            user_id = appointment.user_id,
            doctor_id = doctorState.value ?: doctorId,
            date = appointment.date ?: Timestamp.now(),
            prescription_id = "", // Placeholder
            doctors_notes = comments // Add comments here
        )

        val recordRef = db.collection("medical_records").document()
        val appointmentRef = db.collection("appointments").document(appointment.id)
        medicalRecordIdForPrescription = recordRef.id

        db.runTransaction { transaction ->
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
            Log.d(TAG, "Appointment finish transaction successful.")
            medicalRecordIdForPrescription = recordRef.id
            Toast.makeText(context, "Appointment finished.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Appointment finish transaction failed: ${e.message}", e)
            error = "Failed to finish appointment: ${e.localizedMessage}"
            appointmentToComplete = null
            showPrescriptionChoiceDialog = false
            showPrescribeScreen = false
            medicalRecordIdForPrescription = null
        }
    }
    // Loading upcoming appointments (using SnapshotListener as per original script)
    LaunchedEffect(doctorState.value) {
        val currentDoctorId = doctorState.value ?: doctorId
        if (currentDoctorId.isNotEmpty()) {
            Log.d(TAG, "Setting up upcoming appointments listener for doctor: $currentDoctorId")
            isLoading = true // Only show full screen loader on initial load

            val listenerRegistration = FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("doctor_id", currentDoctorId)
                .whereEqualTo("status", "NOT_FINISHED")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed for upcoming appointments.", e)
                        error = "Failed to load upcoming appointments: ${e.localizedMessage}"
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Upcoming appointments update received: ${snapshot.documents.size} documents")
                        val fetchedList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Appointment::class.java)?.copy(id = doc.id) // Add document ID
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error parsing upcoming appointment document: ${ex.message}", ex)
                                null
                            }
                        }/*.filter {
                            // Optional: filter by date if needed, though status filter usually handles this
                            // Example: Check if date is today or in the future
                            it.date?.toDate()?.after(Date()) ?: true // Assuming future appointments
                        }*/

                        upcomingAppointments.clear()
                        upcomingAppointments.addAll(fetchedList)

                        // Fetch patient info for new appointments
                        val patientIdsToFetch = fetchedList.mapNotNull { it.user_id }.filter { !patientsCache.containsKey(it) }.toSet()
                        if (patientIdsToFetch.isNotEmpty()) {
                            patientsLoading = true // Indicate patient data is being fetched
                            Log.d(TAG, "Fetching ${patientIdsToFetch.size} new patients for upcoming appointments")
                            patientIdsToFetch.forEach { patientId ->
                                fetchPatientInfo(patientId, patientsCache) {
                                    // Check if all new patients are fetched
                                    // Note: This simple check might not be perfect if fetches fail or are slow
                                    // A counter or more robust tracking might be needed for complex scenarios
                                    if (patientIdsToFetch.all { patientsCache.containsKey(it) }) {
                                        patientsLoading = false
                                        Log.d(TAG, "All new patients for upcoming appointments fetched.")
                                    }
                                }
                            }
                        } else {
                            patientsLoading = false // No new patients to fetch
                        }

                        isLoading = false
                    } else {
                        Log.d(TAG, "Upcoming appointments snapshot is null")
                        isLoading = false // If snapshot is null, stop loading indicator
                    }
                }

            // Clean up listener when the effect leaves composition
            /*onDispose {
                Log.d(TAG, "Removing upcoming appointments listener.")
                listenerRegistration.remove()
            }*/
        } else {
            isLoading = false // If no doctorId, stop loading
            Log.w(TAG, "Cannot load upcoming appointments: doctorId is empty.")
        }
    }


    // Loading past appointments (using SnapshotListener as per original script)
    LaunchedEffect(doctorState.value) {
        val currentDoctorId = doctorState.value ?: doctorId
        if (currentDoctorId.isNotEmpty()) {
            Log.d(TAG, "Fetching past appointments for doctor: $currentDoctorId")

            FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("doctor_id", currentDoctorId)
                .whereIn("status", listOf("FINISHED", "CANCELED")) // Match exact spelling
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        error = "Failed to load past appointments."
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Raw documents: ${snapshot.documents.size}")
                        val fetchedList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error parsing doc ${doc.id}", ex)
                                null
                            }
                        }/*.filter { appointment ->
                            // Log dates for debugging
                            Log.d(TAG, "Checking date: ${appointment.date?.toDate()}")
                            // Only keep past appointments (exclude null dates)
                            appointment.date?.toDate()?.before(Date()) ?: false
                        }*/

                        Log.d(TAG, "Filtered appointments: ${fetchedList.size}")
                        pastAppointments.clear()
                        pastAppointments.addAll(fetchedList)
                    }
                }
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
                    showComplete = true, // Show Complete button for upcoming
                    // **MODIFIED:** On complete, trigger the choice dialog flow
                    onComplete = { appt ->
                        appointmentToComplete = appt // Store the appointment
                        showPrescriptionChoiceDialog = true // Show the choice dialog
                    },
                    onStartChat = ::openChatWithPatient // Keep existing chat logic
                )
                1 -> DoctorAppointmentList(
                    appointments = pastAppointments,
                    patientsCache = patientsCache,
                    emptyMessage = "No past appointments",
                    showComplete = false, // Don't show Complete button for past
                    onStartChat = ::openChatWithPatient
                )
            }
        }

        // --- Loading Indicators ---
        // Show loading if initial load is happening, or patients are being fetched
        // Note: completingAppointmentId state seems unused in the original script, removed its check here.
        if (doctorId.isEmpty() && doctorState.value == null || isLoading || patientsLoading) {
            Log.d(TAG, "Showing loading indicator (isLoading=$isLoading, patientsLoading=$patientsLoading)")
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
                        Text("Loading...") // Generic loading text
                    }
                }
            }
        }
        // --- End Loading Indicators ---


        // --- Prescription Choice Dialog ---
        // Show this dialog when an appointment is ready to be completed and the state is set
        if (showPrescriptionChoiceDialog && appointmentToComplete != null) {
            var comments by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    showPrescriptionChoiceDialog = false
                    appointmentToComplete = null
                },
                title = { Text("Finish Appointment") },
                text = {
                    Column {
                        // Show patient name if available
                        val patientName = patientsCache[appointmentToComplete?.user_id]?.name ?: "this patient"
                        Text("Would you like to add a prescription now for $patientName?")
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
                        val appt = appointmentToComplete!!
                        // Update the transaction function to include comments
                        performFinishAppointmentTransaction(appt, comments)
                        showPrescriptionChoiceDialog = false
                        showPrescribeScreen = true
                    }) {
                        Text("Yes, Add Prescription")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        val appt = appointmentToComplete!!
                        performFinishAppointmentTransaction(appt, comments)
                        showPrescriptionChoiceDialog = false
                    }) {
                        Text("No, Finish Without")
                    }
                }
            )
        }        // --- End Prescription Choice Dialog ---


        // --- Prescribe Screen Integration ---
        // This block conditionally displays the PrescribeScreen.
        // It appears when 'showPrescribeScreen' is true AND the necessary IDs are available.
        // medicalRecordIdForPrescription is set *after* the transaction succeeds in performFinishAppointmentTransaction.
        if (showPrescribeScreen && medicalRecordIdForPrescription != null && appointmentToComplete != null) {
            Log.d(TAG, "Attempting to show PrescribeScreen for MR ID: $medicalRecordIdForPrescription, Appt ID: ${appointmentToComplete!!.id}, Patient ID: ${appointmentToComplete!!.user_id}")

            // Ensure user_id and id are not null before passing (should be handled by logic)
            if (appointmentToComplete!!.user_id != null && appointmentToComplete!!.id != null) {
                PrescribeScreen(
                    // Pass necessary data to PrescribeScreen
                    medicalRecordId = medicalRecordIdForPrescription!!,
                    patientId = appointmentToComplete!!.user_id!!,
                    appointmentId = appointmentToComplete!!.id!!,
                    // Provide a lambda to close the screen and reset state when done
                    onDismiss = {
                        Log.d(TAG, "PrescribeScreen dismissed.")
                        showPrescribeScreen = false
                        medicalRecordIdForPrescription = null // Clear ID after prescription is done/canceled
                        appointmentToComplete = null // Clear the completed appointment state
                    }
                )
            } else {
                Log.e(TAG, "Cannot show PrescribeScreen: missing patientId or appointmentId in appointmentToComplete state.")
                error = "Error preparing prescription screen."
                // Reset state
                showPrescribeScreen = false
                medicalRecordIdForPrescription = null
                appointmentToComplete = null
            }
        }
        // --- End Prescribe Screen Integration ---


        // --- Error Display ---
        error?.let {
            Log.e(TAG, "Showing error: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            error = null // Consume the error
        }
        // --- End Error Display ---
    }
}

// Helper function to fetch patient info (keep as is)
private fun fetchPatientInfo(patientId: String, cache: MutableMap<String, User>, onComplete: () -> Unit) {
    Log.d(TAG, "Fetching patient info for: $patientId")
    // Check if already in cache to prevent redundant fetches
    if (cache.containsKey(patientId)) {
        Log.d(TAG, "Patient $patientId already in cache.")
        onComplete()
        return
    }
    FirebaseFirestore.getInstance()
        .collection("users") // Assuming patients are in 'users' collection
        .document(patientId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                user?.let {
                    Log.d(TAG, "Found patient: ${it.name}")
                    cache[patientId] = it
                } ?: run {
                    Log.e(TAG, "Patient document exists but couldn't be parsed: $patientId")
                }
            } else {
                Log.w(TAG, "Patient document doesn't exist for ID: $patientId")
                // Optionally add a placeholder user or mark as failed fetch
            }
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error fetching patient $patientId: ${e.message}", e)
            // Optionally add a placeholder user or mark as failed fetch in cache
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
        // Sort appointments by date/time for better readability
        val sortedAppointments = remember(appointments) {
            appointments.sortedBy { it.date?.toDate() }
        }
        val scope = rememberCoroutineScope()
        val patientMap = remember { mutableStateMapOf<String, User>() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val db = FirebaseFirestore.getInstance()


            items(sortedAppointments, key = { it.id }) { appointment ->
                val patientState = remember { mutableStateOf<User?>(null) }

                // Fetch patient data when this item is first composed
                LaunchedEffect(appointment.user_id) {
                    scope.launch {
                        try {
                            val patientRef = db.collection("users").document(appointment.user_id)
                            val document = withContext(Dispatchers.IO) {
                                patientRef.get().await()
                            }
                            if (document.exists()) {
                                patientState.value = document.toObject(User::class.java)
                                patientMap[appointment.user_id] = patientState.value!!
                            }
                        } catch (e: Exception) {
                            // Handle error (e.g., log it)
                            Log.e("AppointmentList", "Error fetching patient: ${e.message}")
                        }
                    }
                }

                // Use cached patient data if available
                val currentPatient = patientMap[appointment.user_id] ?: patientState.value

                if (currentPatient != null) {
                    AppointmentCard(
                        appointment = appointment,
                        patient = currentPatient,
                        showComplete = showComplete,
                        onComplete = onComplete,
                        onStartChat = onStartChat
                    )
                } else {
                    // Show loading/placeholder while patient data is being fetched
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    patient: User,
    showComplete: Boolean,
    onComplete: (Appointment) -> Unit,
    onStartChat: (Appointment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Add some elevation
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
                    text = "${patient.name ?: ""} ${patient.surname ?: ""}".trim().takeIf { it.isNotBlank() } ?: "Unknown Patient",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Format the date and time properly
            val dateTime = appointment.date?.toDate() ?: Date() // Use current date if Timestamp is null
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            Text(text = "Date: ${dateFormat.format(dateTime)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Time: ${timeFormat.format(dateTime)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Status: ${appointment.status ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp), // Add some space above buttons
                horizontalArrangement = Arrangement.End // Align buttons to the end
            ) {
                // Only show complete button if showComplete is true
                if (showComplete) {
                    OutlinedButton( // Using OutlinedButton might look better next to primary Chat button
                        onClick = { onComplete(appointment) }, // This now triggers the dialog
                        // Optionally adjust colors if needed
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        //border = ButtonDefaults.outlinedShape(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Finish")
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

                var showingPrescription by remember { mutableStateOf<Prescription?>(null) }
                val context = LocalContext.current

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


                if (appointment.status == "FINISHED") {
                    Button(
                        onClick = {
                            FirebaseFirestore.getInstance()
                                .collection("medical_records")
                                .whereEqualTo("appointment_id", appointment.id)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        val medicalRecord = result.documents[0]
                                        val prescriptionId =
                                            medicalRecord.getString("prescription_id")
                                        if (!prescriptionId.isNullOrEmpty()) {
                                            showPrescription(prescriptionId)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "No prescription found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No medical record found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Failed to load medical record",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("See Prescription")
                    }
                }

                showingPrescription?.let { prescription ->
                    PrescriptionDialog(
                        prescription = prescription,
                        onDismiss = { showingPrescription = null }
                    )
                }
                }
            }
        }
    }