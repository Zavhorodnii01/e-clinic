package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.e_clinic.ZEGOCloud.launchZegoChat
import com.example.e_clinic.services.Service
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth


import com.example.e_clinic.Firebase.collections.specializations.DoctorSpecialization
import com.google.ai.client.generativeai.GenerativeModel   // adjust import if SDK path differs

@Composable
fun ServicesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val services: List<Service> = appServices()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(services) { service ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (service.name) {
                            "My Appointments" -> navController.navigate("appointment_screen/$userId") // Navigate to AppointmentsScreen

                            "Chat with Doctor" -> launchZegoChat(context) // Launch Zego chat
                            "Chat with AI Assistant" -> navController.navigate("ai_chat")
                            //else -> {} // No action for other services
                        }
                    }
            ) {
                ListItem(
                    headlineContent = { Text(service.displayedName) },
                    supportingContent = { Text(service.description) }
                )
            }
        }
    }
}




/**
 * Given a free-text symptom/problem description, returns 1-3 suitable
 * doctor specializations from DoctorSpecialization.values().
 */
suspend fun generateSpecializationSuggestions(prompt: String): String {
    // Build a comma-separated list of all allowed specializations.
    val availableSpecs = DoctorSpecialization.values()
        .joinToString(", ") { it.displayName }

    val finalPrompt = """
        You are an AI medical assistant.
        Choose the most relevant doctor specialization(s) for the patient's problem.
        Only pick from this list: $availableSpecs.
        Return up to three suggestions, each with a brief reason (max 25 words each).
        
        Patient description: $prompt
    """.trimIndent()

    val apiKey = ""  // TODO: insert your key

    val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    return try {
        val raw = model.generateContent(finalPrompt).text.orEmpty()
        raw
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")        // strip **bold**
            .replace(Regex("^\\*\\s+", RegexOption.MULTILINE),"") // strip bullets
            .trim()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

