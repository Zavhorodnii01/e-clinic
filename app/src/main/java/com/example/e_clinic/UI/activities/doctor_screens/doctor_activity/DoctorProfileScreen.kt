package com.example.e_clinic.UI.activities.doctor_screens.doctor_activity

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.Repositories.DoctorRepository
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.UI.activities.user_screens.user_activity.ProfileInfoItem
//import com.example.e_clinic.ui.activities.user_screens.user_activity.ProfileInfoItem
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.yalantis.ucrop.UCrop
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.e_clinic.Firebase.Storage.uploadProfilePicture
import com.example.e_clinic.UI.activities.doctor_screens.ChangeDoctorPinActivity
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.PasswordUpdateScreen
import com.example.e_clinic.UI.activities.user_screens.ChangePinScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen() {
    val context = LocalContext.current
    val doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var doctor by remember { mutableStateOf<Doctor?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    
    // Load doctor data
    LaunchedEffect(doctorId) {
        DoctorRepository().getDoctorById(doctorId) { loadedDoctor ->
            doctor = loadedDoctor
        }
    }

    // Crop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultUri = UCrop.getOutput(result.data!!)
        resultUri?.let {
            uploadProfilePicture(context as Activity, doctorId, it) { downloadUrl ->
                FirebaseFirestore.getInstance()
                    .collection("doctors")
                    .document(doctorId)
                    .update("profilePicture", downloadUrl)
                    .addOnSuccessListener {
                        doctor = doctor?.copy(profilePicture = downloadUrl)
                    }
            }
        }
    }

    // Pick image launcher
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val destUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
            val cropIntent = UCrop.of(it, destUri)
                .withAspectRatio(1f, 1f)
                .getIntent(context)
            cropLauncher.launch(cropIntent)
        }
    }



        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
        ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!doctor?.profilePicture.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = doctor?.profilePicture,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dr. ${doctor?.name ?: "Loading..."} ${doctor?.surname ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = doctor?.specialization ?: "Specialization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { pickLauncher.launch("image/*") }) {
                Text("Change Profile Picture")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoItem(Icons.Default.Home, "Address", doctor?.address ?: "Loading...")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Default.Email, "Email", doctor?.email ?: "Loading...")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Default.Phone, "Phone", doctor?.phone ?: "Loading...")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Filled.Work, "Specialization", doctor?.specialization ?: "Loading...")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Filled.School, "Education", doctor?.education ?: "Loading...")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Default.Star, "Experience", doctor?.experience ?: "Loading...")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Change Password")
                    }
                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Change PIN")
                    }
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            val intent = android.content.Intent(context, com.example.e_clinic.UI.activities.doctor_screens.DoctorLogInActivity::class.java)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                }
            }
        }
            if (showPasswordDialog) {
                PasswordUpdateScreen()
            }
            if(showPinDialog){
                val intent = android.content.Intent(context, ChangeDoctorPinActivity::class.java)
                context.startActivity(intent)
                showPinDialog = false
            }



    }


}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}