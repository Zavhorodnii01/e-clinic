package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.e_clinic.Services.Service
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.e_clinic.UI.theme.EClinicTheme
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.e_clinic.ZEGOCloud.launchZegoChat
//import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.ServiceListItem
import com.example.e_clinic.UI.activities.user_screens.UserLogInActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText
import kotlin.text.get

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
    private val updateUserDataLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Optionally refresh user data here
    }
    override fun onStart() {
        super.onStart()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val dob = doc.getTimestamp("dob")
                val address = doc.getString("address")
                val gender = doc.getString("gender")
                val phone = doc.getString("phone")
                if (dob == null || address.isNullOrBlank() || gender.isNullOrBlank() || phone.isNullOrBlank()) {
                    val intent = Intent(this, UpdateUserDataActivity::class.java)
                    updateUserDataLauncher.launch(intent)
                }
            }
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


private fun createCallConfig(): ZegoUIKitPrebuiltCallInvitationConfig {
    return ZegoUIKitPrebuiltCallInvitationConfig().apply {
        //notifyWhenAppRunningInBackgroundOrQuit = true
        translationText = ZegoTranslationText().apply {
            //callInvitationDialogTitle = "Incoming Call"
            //callInvitationDialogMessage = "is calling you"
            incomingCallPageAcceptButton = "Accept"
            incomingCallPageDeclineButton = "Decline"
        }
    }}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var userName by remember { mutableStateOf("") }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var userID : String = ""
    val user = FirebaseAuth.getInstance().currentUser
    user?.let {
        val userId = it.uid
        userID = userId
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userName = document.getString("name") ?: "Unknown User"
                    profilePictureUrl = document.getString("profilePicture")
                }
            }
            .addOnFailureListener {
                userName = "Unknown User"
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("eClinic Patient", fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        if (!profilePictureUrl.isNullOrEmpty()) {
                            // Use Coil or similar for async image loading
                            AsyncImage(
                                model = profilePictureUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }

    val application = context.applicationContext as Application

    ZegoUIKitPrebuiltCallService.init(
        application,
        951562290L,
        "f94c807ab100d261b21dd7dfb8e7fef479ff64946fd73e07001c373080bc8986",
        userID,  // Actual user ID
        userName, // Actual user name
        createCallConfig() // Same config as before
    )

}


@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {

        composable("home") { HomeScreen() }

        composable("services") {
            ServicesScreen(navController = navController)
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

        composable("documents") {
            DocumentScreenForm(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            )
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
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(20.dp, RoundedCornerShape(32.dp), clip = false)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Home") },
                selected = currentDestination == "home",
                onClick = { navController.navigate("home") },
                alwaysShowLabel = true
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.CalendarToday, null) },
                label = { Text("Appointments") },
                selected = currentDestination == "appointments",
                onClick = { navController.navigate("appointments") },
                alwaysShowLabel = true
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Build, null) },
                label = { Text("Services") },
                selected = currentDestination == "services",
                onClick = { navController.navigate("services") },
                alwaysShowLabel = true
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Forum, null) },
                label = { Text("Chat") },
                selected = false,
                onClick = { launchZegoChat(context) },
                alwaysShowLabel = true
            )
        }
    }
}
