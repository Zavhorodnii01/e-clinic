package com.example.e_clinic.ui.activities.doctor_screens.doctor_activity

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.e_clinic.ui.theme.EClinicTheme
import androidx.navigation.compose.rememberNavController
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import com.example.e_clinic.ui.activities.doctor_screens.DoctorProfileScreen


//TODO: UserActivity and functions

class DoctorActivity : ComponentActivity() {
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
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    val doctor = Doctor()


    val user = FirebaseAuth.getInstance().currentUser
    user?.let {
        val db = FirebaseFirestore.getInstance()
        db.collection("doctors").whereEqualTo("e-mail", it.email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doctor = documents.documents[0]
                    doctorName = doctor.getString("name") ?: "Unknown Doctor"
                } else {
                    doctorName = "Unknown Doctor"
                }
            }
            .addOnFailureListener {
                doctorName = "Unknown Doctor"
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome $doctorName!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding), doctor = doctor)
    }
    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
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

    Column {
        Column {
            Text(
                text = "Your Services",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(services) { service ->
                    ServiceListItem(service = service, onClick = {
                        // Handle service click
                    })
                }
            }
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


//@Composable
//fun DocumentsScreen() {
//    val scrollState = rememberScrollState()
//    var userId by remember { mutableStateOf("") }
//    var medication by remember { mutableStateOf("") }
//    var doctorComment by remember { mutableStateOf("") }
//
//    Column(modifier = Modifier.verticalScroll(scrollState)) {
//        Text(
//            text = "Your Documents",
//            style = MaterialTheme.typography.headlineSmall,
//            modifier = Modifier.padding(16.dp)
//        )
//
//        OutlinedTextField(
//            value = userId,
//            onValueChange = { userId = it },
//            label = { Text("Enter User ID") },
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth()
//        )
//        OutlinedTextField(
//            value = medication,
//            onValueChange = { medication = it },
//            label = { Text("Enter Medication") },
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth()
//        )
//        OutlinedTextField(
//            value = doctorComment,
//            onValueChange = { doctorComment = it },
//            label = { Text("Write Comment") },
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth()
//        )
//
//        Button(
//            onClick = {
//                val doctorId = FirebaseAuth.getInstance().currentUser?.uid
//                val prescription = Prescription(doctor_id=doctorId.toString(), user_id=userId,issued_date= Timestamp.now(), link_to_storage="link", appointment_id = "appointmentid" , doctor_comment = doctorComment)
//                addPrescription(prescription, medication)
//                      },
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text("Load Document")
//        }
//    }
//}

//fun addPrescription(prescription: Prescription, medication : String) {
//    uploadPrescriptionToStorage(prescription, medication)
//}


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

        LazyRow(
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
            .height(80.dp)
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
fun NavigationHost(navController: NavHostController, modifier: Modifier, doctor: Doctor) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.HomeScreen() }
        composable("chat") { com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.ChatScreen() }
        composable("prescriptions") { com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.PrescribeScreen()}
        composable("documents") { com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.DocumentScreen(
        ) }
        composable("profile") { com.example.e_clinic.ui.activities.doctor_screens.DoctorProfileScreen() }
        composable("settings") {
            com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.SettingsScreen(
                onClose = {})
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = false, onClick = { navController.navigate("home") })
        NavigationBarItem(icon = { Icon(Icons.Default.AccountBox, null) }, label = { Text("Chat") }, selected = false, onClick = { navController.navigate("chat") })
        NavigationBarItem(icon = { Icon(Icons.Default.Check, null) }, label = { Text("Prescriptions") }, selected = false, onClick = { navController.navigate("prescriptions") })
        NavigationBarItem(icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Documents") }, selected = false, onClick = { navController.navigate("documents") })
        NavigationBarItem(icon = { Icon(Icons.Default.Menu, null) }, label = { Text("Profile") }, selected = false, onClick = { navController.navigate("profile") })
    }
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    launchZegoChat(context)
    //val context = LocalContext.current
    Text("Chat Screen")
}

