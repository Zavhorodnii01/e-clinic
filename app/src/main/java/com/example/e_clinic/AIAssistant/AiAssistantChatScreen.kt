package com.example.e_clinic.ui.activities.user_screens.user_activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_clinic.AIAssistant.BookingStep
import com.example.e_clinic.BuildConfig
import com.example.e_clinic.Firebase.collections.TimeSlot
import com.example.e_clinic.Firebase.collections.specializations.DoctorSpecialization
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import java.util.*

@Composable
fun AiAssistantChatScreen(
    userId: String,
    onAppointmentBooked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }

    var step by remember { mutableStateOf(BookingStep.WAIT_SYMPTOMS) }
    var specialization by remember { mutableStateOf<String?>(null) }
    var doctors by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedDoctorId by remember { mutableStateOf<String?>(null) }
    var slots by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) messages.add(false to "👋 Hi! I'm your assistant. What symptoms are you experiencing today?")
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.asReversed()) { (isUser, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(text, Modifier.padding(12.dp), color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type here…") },
                singleLine = true
            )
            IconButton(onClick = {
                val prompt = input.trim()
                if (prompt.isEmpty()) return@IconButton
                messages.add(true to prompt)
                input = ""
                scope.launch {
                    when (step) {
                        BookingStep.WAIT_SYMPTOMS -> {
                            val suggestions = generateSpecializationSuggestions(prompt)
                            specialization = suggestions.firstOrNull()?.substringAfter(".")?.trim()
                            messages.add(false to buildString {
                                appendLine("Thanks! Based on your symptoms, these specializations might help:")
                                suggestions.forEach { appendLine("- $it") }
                                append("Would you like to see doctors in this field?")
                            })
                            step = BookingStep.CONFIRM_BOOKING
                        }

                        BookingStep.CONFIRM_BOOKING -> {
                            val userMeansYes = interpretUserIntentWithAI(prompt)
                            if (userMeansYes && specialization != null) {
                                messages.add(false to "Got it! Let me find available doctors in $specialization…")
                                doctors = fetchDoctorsBySpecialization(specialization)
                                if (doctors.isEmpty()) {
                                    messages.add(false to "😕 Sorry, no doctors available in $specialization right now.")
                                    step = BookingStep.NONE
                                    return@launch
                                }
                                doctors.forEachIndexed { i, (_, name) -> messages.add(false to "${i + 1}. $name") }
                                messages.add(false to "Please choose a doctor by number.")
                                step = BookingStep.CHOOSE_DOCTOR
                            } else {
                                messages.add(false to "Okay, no problem! Let me know if you need anything else.")
                                step = BookingStep.NONE
                            }
                        }

                        BookingStep.CHOOSE_DOCTOR -> {
                            val idx = prompt.toIntOrNull()?.minus(1)
                            val doc = idx?.let { doctors.getOrNull(it) }
                            if (doc == null) {
                                messages.add(false to "⚠️ Please select a doctor using a valid number.")
                                return@launch
                            }
                            selectedDoctorId = doc.first
                            messages.add(false to "Checking time slots for Dr. ${doc.second}…")
                            slots = fetchAvailableSlots(doc.first)
                            if (slots.isEmpty()) {
                                messages.add(false to "No time slots available for Dr. ${doc.second}. Try another doctor.")
                                step = BookingStep.CHOOSE_DOCTOR
                                return@launch
                            }
                            slots.forEachIndexed { i, slot -> messages.add(false to "${i + 1}. $slot") }
                            messages.add(false to "Pick a time slot by number.")
                            step = BookingStep.CHOOSE_SLOT
                        }

                        BookingStep.CHOOSE_SLOT -> {
                            val idx = prompt.toIntOrNull()?.minus(1)
                            val slot = idx?.let { slots.getOrNull(it) }
                            if (slot == null || selectedDoctorId == null) {
                                messages.add(false to "⚠️ Invalid time slot. Please try again.")
                                return@launch
                            }
                            messages.add(false to "Booking your appointment for $slot…")
                            val success = bookAppointment(userId, selectedDoctorId!!, slot)
                            if (success) {
                                messages.add(false to "✅ Appointment confirmed for $slot. See you then!")
                                onAppointmentBooked()
                                step = BookingStep.CONFIRMATION
                            } else {
                                messages.add(false to "❌ Couldn't book that slot. Try a different one.")
                                step = BookingStep.CHOOSE_SLOT
                            }
                        }

                        else -> {
                            messages.add(false to "Let's start over. What symptoms are you experiencing?")
                            step = BookingStep.WAIT_SYMPTOMS
                        }
                    }
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

suspend fun interpretUserIntentWithAI(input: String): Boolean {
    val modelPrompt = """
        You are an intelligent assistant helping schedule medical appointments.
        The user said: "$input"
        Based on typical human expression, does the user agree to proceed? Just answer "yes" or "no".
    """.trimIndent()

    return try {
        val model = GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY)
        val response = model.generateContent(modelPrompt).text.orEmpty().lowercase(Locale.getDefault()).trim()
        response.contains("yes")
    } catch (e: Exception) {
        false
    }
}

private suspend fun generateSpecializationSuggestions(prompt: String): List<String> {
    val modelPrompt = """
        You are an AI medical assistant.
        Suggest up to three relevant specialisations (with short reason) from: ${DoctorSpecialization.values().joinToString { it.displayName }}.
        Patient: $prompt
    """.trimIndent()

    return try {
        val model = GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY)
        model.generateContent(modelPrompt).text.orEmpty()
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("^\\*\\s+", RegexOption.MULTILINE), "")
            .trim()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
    } catch (e: Exception) {
        listOf("Error: ${e.message}")
    }
}

suspend fun fetchDoctorsBySpecialization(selectedSpecialization: String?): List<Pair<String, String>> {
    if (selectedSpecialization.isNullOrEmpty()) return emptyList()
    val db = FirebaseFirestore.getInstance()
    return try {
        val timeSlotDocs = db.collection("timeslots")
            .whereEqualTo("specialization", selectedSpecialization)
            .get().await()

        val doctorIds = timeSlotDocs.documents.mapNotNull { it.getString("doctor_id") }.distinct()
        if (doctorIds.isEmpty()) return emptyList()

        val doctorDocs = db.collection("doctors")
            .whereIn(FieldPath.documentId(), doctorIds)
            .get().await()

        doctorDocs.map { doc ->
            val name = doc.getString("name") ?: ""
            val surname = doc.getString("surname") ?: ""
            val spec = doc.getString("specialization") ?: "General"
            doc.id to "$name $surname ($spec)"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun fetchAvailableSlots(doctorId: String): List<String> {
    val db = FirebaseFirestore.getInstance()
    val fmt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    return try {
        val slotDocs = db.collection("timeslots")
            .whereEqualTo("doctor_id", doctorId)
            .get().await()

        val allSlots = slotDocs.toObjects(TimeSlot::class.java)
            .flatMap { it.available_slots }
            .distinct()
            .sortedBy { it.seconds }

        allSlots.map { fmt.format(it.toDate()) }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun bookAppointment(userId: String, doctorId: String, slot: String): Boolean {
    val db = FirebaseFirestore.getInstance()
    val fmt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    val date = fmt.parse(slot) ?: return false
    val ts = Timestamp(date)

    return try {
        db.runTransaction { txn: Transaction ->
            val timeslotQuery = db.collection("timeslots")
                .whereEqualTo("doctor_id", doctorId)
                .whereArrayContains("available_slots", ts)
                .limit(1)
                .get()
                .getResult()

            if (timeslotQuery.isEmpty) throw Exception("Slot no longer available")

            val timeslotDocRef = timeslotQuery.documents[0].reference
            val currentSlots = (timeslotQuery.documents[0]["available_slots"] as? List<Timestamp>)?.toMutableList() ?: mutableListOf()

            if (!currentSlots.contains(ts)) throw Exception("Slot taken")

            currentSlots.remove(ts)
            txn.update(timeslotDocRef, "available_slots", currentSlots)

            val appointmentData = mapOf(
                "user_id" to userId,
                "doctor_id" to doctorId,
                "date" to ts,
                "status" to "NOT_FINISHED"
            )

            txn.set(db.collection("appointments").document(), appointmentData)
        }.await()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
