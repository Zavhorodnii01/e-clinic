package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.example.e_clinic.Firebase.Repositories.UserRepository
import com.example.e_clinic.Firebase.Storage.uploadProfilePicture
import com.example.e_clinic.UI.activities.LogInActivity
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.PasswordUpdateScreen
import com.example.e_clinic.UI.activities.user_screens.ChangePinActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlin.printStackTrace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(userId) {
        UserRepository().getUserById(userId) { loadedUser ->
            user = loadedUser
        }
    }
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                uploadProfilePicture(context as Activity, userId, it) { downloadUrl ->
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("profilePicture", downloadUrl)
                        .addOnSuccessListener {
                            user = user?.copy(profilePicture = downloadUrl)
                        }
                }
            }
        }
        if (result.resultCode == UCrop.RESULT_ERROR) return@rememberLauncherForActivityResult

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )  {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            ProfileHeader(user)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { pickLauncher.launch("image/*") }) {
                Text("Change Profile Picture")
            }
            Spacer(modifier = Modifier.height(24.dp))
            ProfileInfoSection(user)
            Spacer(modifier = Modifier.height(24.dp))
            SecurityOptions()
        }
    }
}

@Composable
fun ProfileHeader(user: User?) {
    if (!user?.profilePicture.isNullOrEmpty()) {
        coil.compose.AsyncImage(
            model = user?.profilePicture,
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
        text = "${user?.name ?: "Loading..."} ${user?.surname ?: ""}",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun ProfileInfoSection(user: User?) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoItem(Icons.Default.Home, "Address", user?.address ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Default.Email, "Email", user?.email ?: "Loading...")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(Icons.Default.Phone, "Phone", user?.phone ?: "Loading...")
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SecurityOptions() {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPINDialog by remember { mutableStateOf(false) }
    var updateUserData by remember { mutableStateOf(false) }

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
                onClick = {
                    showPasswordDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Change Password")
            }

            Button(
                onClick = {
                    showPINDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Change PIN Code")
            }
            Button(
                onClick = {
                    updateUserData = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Update Your Data")
            }


            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, LogInActivity::class.java)
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
        if (showPasswordDialog) {
            PasswordUpdateScreen()
        }
        if(showPINDialog){
            val intent = Intent(context, ChangePinActivity()::class.java)
            context.startActivity(intent)
            showPINDialog = false
        }
        if (updateUserData) {
            val intent = Intent(context, UpdateUserDataActivity::class.java)
            context.startActivity(intent)
            updateUserData = false
        }
    }
}
