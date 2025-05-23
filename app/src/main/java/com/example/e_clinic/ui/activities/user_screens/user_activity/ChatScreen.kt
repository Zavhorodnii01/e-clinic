package com.example.e_clinic.ui.activities.user_screens.user_activity

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.e_clinic.ZEGOCloud.launchZegoChat

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    launchZegoChat(context)
    //val context = LocalContext.current
    Text("Chat Screen")
}