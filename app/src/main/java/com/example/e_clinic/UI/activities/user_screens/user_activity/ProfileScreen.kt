package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.User
import com.example.e_clinic.Firebase.repositories.UserRepository
import com.example.e_clinic.ui.activities.user_screens.ChangePinActivity
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var user by remember { mutableStateOf<User?>(null) }
    var visitsCount by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        UserRepository().getUserById(userId) { loadedUser ->
            user = loadedUser
            visitsCount = 4 // Placeholder
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Profile") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProfileHeader(user?.name, user?.surname, visitsCount)
            Spacer(modifier = Modifier.height(24.dp))
            ProfileInfoSection(user)
            Spacer(modifier = Modifier.height(24.dp))
            SecurityOptions()
        }
    }
}

@Composable
fun ProfileHeader(name: String?, surname: String?, visits: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Profile Photo",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${name ?: "Loading..."} ${surname ?: ""}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$visits visits this month",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
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
                    // TODO: implement change password
                    Toast.makeText(context, "Change password not implemented yet", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Change Password")
            }

            Button(
                onClick = {
                    val intent = Intent(context, ChangePinActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Change PIN Code")
            }
        }
    }
}
