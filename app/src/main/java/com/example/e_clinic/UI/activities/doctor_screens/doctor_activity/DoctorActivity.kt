package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.Services.Service
import com.example.e_clinic.UI.activities.doctor_screens.DoctorLogInActivity
import com.example.e_clinic.UI.activities.user_screens.user_activity.SettingsScreen
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.app.Activity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import android.app.AlertDialog
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.MedicalRecord
import com.example.e_clinic.Firebase.Repositories.AppointmentRepository
import com.google.firebase.Timestamp


class DoctorActivity : ComponentActivity() {
    private lateinit var appointmentRepository: AppointmentRepository
    private val urgentAppointmentId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appointmentRepository = AppointmentRepository()

        // Handle notification permission and FCM token
        setupNotifications()

        setContent {
            EClinicTheme {
                MaterialTheme {
                    MainScreen()
                }
            }
        }

        checkIntent(intent)
    }

    private fun setupNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                val userId = Firebase.auth.currentUser?.uid
                if (userId != null) {
                    Firebase.firestore.collection("doctors")
                        .document(userId)
                        .update("fcmToken", token)
                }
            }
        }
    }

    private fun checkIntent(intent: Intent?) {
        intent?.getStringExtra("appointmentId")?.let { id ->
            urgentAppointmentId.value = id
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("Loading...") }
    val doctor = Doctor()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val appointments = remember { mutableStateListOf<Appointment>() }
    val urgentAppointmentId = remember { mutableStateOf<String?>(null) }
    val doctorState = remember { mutableStateOf<String?>(null) }
    val currentAppointment = remember { mutableStateOf<Appointment?>(null) }
    val medicalRecordId = remember { mutableStateOf<String?>(null) }
    val appointmentRepository = remember { AppointmentRepository() }
    val showPrescriptionChoice = remember { mutableStateOf(false) }
    val showPrescribeScreen = remember { mutableStateOf(false) }
    val prescribePatientId = remember { mutableStateOf<String?>(null) }
    val prescribeAppointmentId = remember { mutableStateOf<String?>(null) }


    val intent = (context as? Activity)?.intent
    val showDialog = intent?.getBooleanExtra("showFinishDialog", false) == true
    val incomingAppointmentId = intent?.getStringExtra("appointmentId")

    LaunchedEffect(showDialog, incomingAppointmentId) {
        if (showDialog && incomingAppointmentId != null) {
            urgentAppointmentId.value = incomingAppointmentId

            // Clear the extras so dialog doesn't show again on recomposition
            intent.removeExtra("showFinishDialog")
            intent.removeExtra("appointmentId")
        }
    }

    // Fetch doctor name and state
    val user = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(user) {
        user?.email?.let { email ->
            FirebaseFirestore.getInstance()
                .collection("doctors")
                .whereEqualTo("e-mail", email)
                .get()
                .addOnSuccessListener { documents ->
                    val doctorDoc = documents.documents.firstOrNull()
                    doctorName = doctorDoc?.getString("name") ?: "Unknown Doctor"
                    doctorState.value = doctorDoc?.id
                }
                .addOnFailureListener {
                    doctorName = "Unknown Doctor"
                }
        }
    }

    fun finishAppointment(appointment: Appointment) {
        val currentTime = Timestamp.now()
        val appointmentTime = appointment.date ?: run {
            Toast.makeText(context, "Invalid appointment time", Toast.LENGTH_SHORT).show()
            return
        }

        if (appointmentTime.seconds > currentTime.seconds ||
            (appointmentTime.seconds == currentTime.seconds && appointmentTime.nanoseconds > currentTime.nanoseconds)) {
            Toast.makeText(
                context,
                "Cannot finish future appointments",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

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

    // Handle urgent appointments
    val pendingAppointmentToFinish = remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(urgentAppointmentId.value) {
        urgentAppointmentId.value?.let { appointmentId ->
            val localAppointment = appointments.find { it.id == appointmentId }
            if (localAppointment != null) {
                pendingAppointmentToFinish.value = localAppointment
            } else {
                FirebaseFirestore.getInstance()
                    .collection("appointments")
                    .document(appointmentId)
                    .get()
                    .addOnSuccessListener { doc ->
                        doc.toObject(Appointment::class.java)?.let { appt ->
                            pendingAppointmentToFinish.value = appt
                        }
                    }
            }
        }
    }

// Separate Composable-side check for the dialog
    pendingAppointmentToFinish.value?.let { appointment ->
        AlertDialog.Builder(context)
            .setTitle("Please mark this appointment as finished")
            .setMessage("The appointment should have status as finished. Would you like to finish it now?")
            .setCancelable(false)
            .setPositiveButton("Yes, finish now") { _, _ ->
                currentAppointment.value = appointment
                showPrescriptionChoice.value = true
                pendingAppointmentToFinish.value = null
            }
            .setNegativeButton("I'll do it later") { dialog, _ ->
                dialog.dismiss()
                pendingAppointmentToFinish.value = null
            }
            .show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome $doctorName!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, DoctorLogInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavigationHost(navController, Modifier.padding(innerPadding), doctor, userId)
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
                    currentAppointment.value = null
                }) {
                    Text("No, Finish Without")
                }
            }
        )
    }

    if (
        showPrescribeScreen.value &&
        prescribePatientId.value != null &&
        prescribeAppointmentId.value != null &&
        medicalRecordId.value != null
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



    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
    }
}



@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier, doctor: Doctor, userId: String) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }

        // Unified appointments route
        composable("appointments/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: userId
            AppointmentsScreen(doctorId = doctorId)
        }

        // Fallback route if no ID needed
        composable("appointments") {
            AppointmentsScreen(doctorId = userId)
        }

        composable("services") { ServicesScreen(navController) }
        composable("prescriptions") { PrescribeScreen() }
        composable("profile") { ProfileScreen() }
        composable("settings") { SettingsScreen(onClose = {}) }

        composable("prescribe/{fromCalendar}/{patientId}") { backStackEntry ->
            val fromCalendar = backStackEntry.arguments?.getString("fromCalendar")?.toBoolean() ?: false
            val patientId = backStackEntry.arguments?.getString("patientId")
            PrescribeScreen(fromCalendar = fromCalendar, patientId = patientId)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = currentDestination == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Check, null) },
            label = { Text("Appointments") },
            selected = currentDestination == "appointments",
            onClick = { navController.navigate("appointments") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBox, null) },
            label = { Text("Services") },
            selected = currentDestination == "services",
            onClick = { navController.navigate("services") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBox, null) },
            label = { Text("Chat") },
            selected = false,
            onClick = {

                launchZegoChat(context)
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") },
            selected = currentDestination == "profile",
            onClick = { navController.navigate("profile") }
        )
    }
}


@Composable
fun ServiceListItem(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.displayedName,
                style = MaterialTheme.typography.titleMedium
            )
            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}