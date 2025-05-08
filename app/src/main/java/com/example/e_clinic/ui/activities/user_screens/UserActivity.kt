package com.example.e_clinic.ui.activities.user_screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.e_clinic.services.Service
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.e_clinic.ui.theme.EClinicTheme
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope


//TODO: UserActivity and functions

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent  {
            EClinicTheme{
                MainScreen()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }


    //TODO: Fetch user info from Firebase

    val user = FirebaseAuth.getInstance().currentUser
    user?.let {
        val userId = it.uid
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userName = document.getString("name") ?: "Unknown User"
                }
            }
            .addOnFailureListener {
                userName = "Unknown User"
            }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome $userName !") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Background color
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer// Title color
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) {innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
    }

}

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier) {
    val coroutineScope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }
        composable("services") { ServicesScreen(coroutineScope = coroutineScope)}
        composable("documents") { DocumentsScreen() }
        composable("settings") { SettingsScreen(onClose = {}) }
        // TODO: Handling the services lists
    }
}

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    var visibility by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visibility = true
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onClose() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServicesScreen(coroutineScope: CoroutineScope) {
    //TODO: Actual Settings Screen UI
    val scrollState = rememberScrollState()
    val services = appServices()
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(services) { service ->
            ServiceListItem(service = service, onClick = {
                when (service.name) {
                    "appointment" -> {
                        val intent = Intent(context, AppointmentActivity::class.java)
                        intent.putExtra("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        context.startActivity(intent)
                    }
                    // other services...
                }
            })
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


@Composable
fun DocumentsScreen() {
    //TODO: Implement Documents Screen UI with Firebase operations
    val scrollState = rememberScrollState()

    Column {
        Text(
            text = "Your Documents",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
    }

}

@Composable
fun HomeScreen() {
    //TODO: Actual Home Screen UI
    val scrollState = rememberScrollState()
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val appointmentRepository = AppointmentRepository()
    val appointments = remember { mutableStateListOf<Appointment>() }
    LaunchedEffect(user) {
        user?.let {
            appointmentRepository.getAppointmentsForUser(it.uid) { loadedAppointments ->
                val doctorRepository = DoctorRepository()
                appointments.clear()
                loadedAppointments.forEach { appointment ->
                    doctorRepository.getDoctorById(appointment.doctor_id) { doctor ->
                        val doctorName = doctor?.name ?: "Unknown Doctor"
                        appointments.add(appointment.copy(doctor_id = doctorName))
                    }
                }
            }
        }
    }


    val services = appServices()
    Column {
        Text(
            text = "Upcoming Appointments",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(appointments) { appointment ->
                AppointmentItem(
                    title = "Doctor: ${appointment.doctor_id}",
                    description = "Date & Time: ${appointment.date_and_time}"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        ServicesSection(
            services = services,
            onServiceClick = { service ->
                when (service.name) {
                    "appointment" -> {
                        val intent = Intent(context, AppointmentActivity::class.java)
                        intent.putExtra("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        context.startActivity(intent)
                    }
                    // Handle other services here
                }
            }
        )
    }
}



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
fun ServicesSection(
    services: List<Service>,
    onServiceClick: (Service) -> Unit = {}
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Services",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            IconButton(onClick = { /* Handle view all services click */ }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View all services"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(services) { service ->
                ServiceCard(
                    service = service,
                    onClick = { onServiceClick(service) }
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ServiceCard(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = service.displayedName)
        }
    }
}



@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Services,
        BottomNavItem.Documents
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    when (item) {
                        BottomNavItem.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                        BottomNavItem.Services -> Icon(Icons.Default.List, contentDescription = "S")
                        BottomNavItem.Documents -> Icon(Icons.Default.AccountBox, contentDescription = "2")
                    }
                },
                label = { Text(text = item.title) }
            )
        }
    }
}

// Sealed class to define different bottom navigation items
sealed class BottomNavItem(val route: String, val title: String) {
    object Home : BottomNavItem("home", "Home")
    object Services : BottomNavItem("services", "Services")
    object Documents : BottomNavItem("documents", "Documents")
}


@Preview
@Composable
fun PreviewUserActivity() {
    EClinicTheme {
        MainScreen()
    }
}