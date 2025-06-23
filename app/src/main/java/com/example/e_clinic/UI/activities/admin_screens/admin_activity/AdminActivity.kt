@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Administrator
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.BottomNavigationBar
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.*

import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlin.inc
import kotlin.text.get

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var admin by remember { mutableStateOf<Administrator?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(uid, reloadTrigger) {
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("administrators")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    admin = doc.toObject(Administrator::class.java)
                }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("eClinic Management", fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        if (admin?.profilePicture?.isNotEmpty() == true) {
                            AsyncImage(
                                model = admin?.profilePicture,
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AdminBottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        SetupNavHost(navController, Modifier.padding(innerPadding), onProfilePictureChanged = { reloadTrigger++ })
    }
}

@Composable
fun AdminBottomNavigationBar(navController: NavController) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(20.dp, RoundedCornerShape(32.dp), clip = false)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ){
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = "Doctors"
                    )
                },
                label = { Text("Doctors") },
                selected = navController.currentDestination?.route == "doctors",
                onClick = { navController.navigate("doctors") },
                alwaysShowLabel = true
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = "Patients"
                    )
                },
                label = { Text("Patients") },
                selected = navController.currentDestination?.route == "patients",
                onClick = { navController.navigate("patients") },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun SetupNavHost(navController: NavHostController, modifier: Modifier = Modifier, onProfilePictureChanged: () -> Unit = {}) {
    NavHost(
        navController = navController,
        startDestination = "doctors",
        modifier = modifier
    ) {
        composable("doctors") { DoctorsScreen() }
        composable("patients") { UsersScreen() }
        composable("profile") { AdminProfileLoader(onProfilePictureChanged, navController) }
        composable("admin_data_update/{adminId}") { backStackEntry ->
            val adminId = backStackEntry.arguments?.getString("adminId") ?: ""
            AdminDataUpdateScreen(id = adminId)
        }
    }
}


@Composable
fun AdminProfileLoader(onProfilePictureChanged: () -> Unit, navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var admin by remember { mutableStateOf<Administrator?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("administrators")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    admin = doc.toObject(Administrator::class.java)
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    error = e.message
                    isLoading = false
                }
        } else {
            error = "No UID found"
            isLoading = false
        }
    }

    when {
        isLoading -> CircularProgressIndicator()
        error != null -> Text("Error: $error")
        admin != null -> ProfileScreen(admin!!, onProfilePictureChanged = onProfilePictureChanged, navController = navController)
        else -> Text("No administrator data found")
    }
    if (admin != null) {
        ProfileScreen(admin!!, navController, onProfilePictureChanged)
    }
}