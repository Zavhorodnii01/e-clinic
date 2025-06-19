package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging


class DoctorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EClinicTheme {
                MainScreen()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        // Update token if needed
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                val userId = Firebase.auth.currentUser?.uid
                if (userId != null) {
                    Firebase.firestore.collection("doctors")
                        .document(userId)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d("DoctorActivity", "Token updated")
                        }
                }
            }
        }

        // Check if opened via notification
        val appointmentId = intent.getStringExtra("appointmentId")
        appointmentId?.let {
            Log.d("DoctorActivity", "Opened from FCM with appointmentId = $it")
            // TODO: navigate to appointment detail screen
        }
    }}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("Loading...") }
    val doctor = Doctor()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Fetch doctor name
    val user = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(user) {
        user?.email?.let { email ->
            FirebaseFirestore.getInstance()
                .collection("doctors")
                .whereEqualTo("e-mail", email)
                .get()
                .addOnSuccessListener { documents ->
                    doctorName = documents.documents.firstOrNull()?.getString("name") ?: "Unknown Doctor"
                }
                .addOnFailureListener {
                    doctorName = "Unknown Doctor"
                }
        }
    }

    Log.d("DoctorIDDebug", "Doctor ID received: ${FirebaseAuth.getInstance().currentUser?.uid}")

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
                    IconButton(onClick = {FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, DoctorLogInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)}) {
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

    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
    }
}

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier, doctor: Doctor, userId: String) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }
        composable("appointments") { AppointmentsScreen(userId) }
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

// Other existing composables (ServiceListItem, AppointmentItem, etc.) remain in this file
@Composable
fun AppointmentItem(title: String, description: String) {
    var checked by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
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
