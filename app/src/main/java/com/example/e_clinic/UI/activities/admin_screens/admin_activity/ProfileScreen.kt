@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.yalantis.ucrop.UCrop
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Administrator
import com.example.e_clinic.Firebase.Storage.uploadProfilePicture
import com.example.e_clinic.UI.activities.LogInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File


@Composable
fun ProfileScreen(admin: Administrator, navController: NavController, onProfilePictureChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var profilePicUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }


    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                uploadProfilePicture(context as Activity, admin.id, it) { downloadUrl ->
                    FirebaseFirestore.getInstance()
                        .collection("administrators")
                        .document(admin.id)
                        .update("profilePicture", downloadUrl)
                        .addOnSuccessListener {
                            onProfilePictureChanged()
                        }
                }
            }
        }
        if (result.resultCode == UCrop.RESULT_ERROR) return@rememberLauncherForActivityResult

    }

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

    Scaffold{ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (admin.profilePicture.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = admin.profilePicture,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(300.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(100.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${admin.name} ${admin.surname}",
                style = MaterialTheme.typography.headlineMedium
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
                    ProfileInfoItem(Icons.Default.Phone, "Phone", admin.phone)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(Icons.Default.Email, "Email", admin.email)
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
                            navController.navigate("admin_data_update/${admin.id}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Change Your Data")
                    }
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            //Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
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
            }
        }
        if (showPasswordDialog) {
            PasswordUpdateScreen(
            )
        }
    }
}
@Composable
fun ProfileInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
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