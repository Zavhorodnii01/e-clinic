package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.e_clinic.Services.Service
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.e_clinic.UI.theme.EClinicTheme
import androidx.navigation.compose.rememberNavController
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.ServiceListItem
import com.example.e_clinic.UI.activities.user_screens.UserLogInActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

//import com.example.e_clinic.ui.activities.doctor_screens.doctor_activity.ServiceListItem


class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EClinicTheme {
                MainScreen()
            }
        }

        setupNotifications()

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
                    Firebase.firestore.collection("users")
                        .document(userId)
                        .update("fcmToken", token)
                }
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
    var userName by remember { mutableStateOf("") }

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
                title = { Text("Welcome $userName!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                    IconButton(onClick = {FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, UserLogInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)}) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
    }
}


@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {

        composable("home") { HomeScreen() }

        composable("services") {
            ServicesScreen(navController = navController)
        }

        composable("documents") {
            DocumentScreen()
        }

        // Uncomment this if ChatScreen is implemented
        // composable("chat") { ChatScreen() }

        composable("appointments") {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            AppointmentsScreen(userId = userId) {
                // Handle optional post-appointment logic here
            }
        }

        composable("profile") {
            ProfileScreen()
        }

        composable("settings") {
            SettingsScreen(onClose = {
                navController.popBackStack() // Optional: closes settings
            })
        }

        composable("appointment_screen/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "unknown"
            AppointmentsScreen(userId = userId) {
                navController.navigate("home")
            }
        }

        composable("ai_chat") {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@composable
            AiAssistantChatScreen(
                userId = userId,
                onAppointmentBooked = {
                    // Optionally navigate somewhere
                    // navController.navigate("appointments")
                }
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    EClinicTheme {
        MainScreen()
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val context = LocalContext.current

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = false,
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBox, null) },
            label = { Text("Chat") },
            selected = false,
            onClick = {
                // Directly launch chat here
                launchZegoChat(context)
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Check, null) },
            label = { Text("Appointments") },
            selected = false,
            onClick = { navController.navigate("appointments") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Check, null) },
            label = { Text("Services") },
            selected = false,
            onClick = { navController.navigate("services") }
        )

        /*NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, null) },
            label = { Text("Documents") },
            selected = false,
            onClick = { navController.navigate("documents") }
        )*/
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, null) },
            label = { Text("Profile") },
            selected = false,
            onClick = { navController.navigate("profile") }
        )
    }
}

/*@Composable
fun ChatScreen() {
    val context = LocalContext.current
    launchZegoChat(context)
    Text("Chat Screen")
}*/
