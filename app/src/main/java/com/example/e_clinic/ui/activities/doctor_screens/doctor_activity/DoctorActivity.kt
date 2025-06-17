package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.services.Service
import com.example.e_clinic.services.functions.doctorServices
import com.example.e_clinic.ui.activities.user_screens.user_activity.SettingsScreen
import com.example.e_clinic.ui.theme.EClinicTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import im.zego.connection.internal.ZegoConnectionImpl.context


class DoctorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EClinicTheme {
                MainScreen()
            }
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
            icon = { Icon(Icons.Default.Call, null) },
            label = { Text("Chat") },
            selected = false,
            onClick = { launchZegoChat(context) }
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
