package com.example.e_clinic.ui.activities.logIn_screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.e_clinic.R
import com.example.e_clinic.ui.activities.admin_screens.AdminLogInActivity
import com.example.e_clinic.ui.activities.doctor_screens.DoctorLogInActivity
import com.example.e_clinic.ui.activities.user_screens.UserLogInActivity
import com.example.e_clinic.ui.theme.EClinicTheme

class LogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                StartScreen(this)
            }
        }
    }
}

@Composable
fun StartScreen(activity: Activity) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Your Role",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navigateToActivity(activity, UserLogInActivity::class.java) },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("User")
        }

        Button(
            onClick = { navigateToActivity(activity, DoctorLogInActivity::class.java) },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Doctor")
        }

        Button(
            onClick = { navigateToActivity(activity, AdminLogInActivity::class.java) },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Admin")
        }
    }
}

fun navigateToActivity(activity: Activity, target: Class<*>) {
    activity.startActivity(Intent(activity, target))
}