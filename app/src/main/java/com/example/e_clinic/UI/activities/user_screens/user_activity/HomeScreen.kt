package com.example.e_clinic.UI.activities.user_screens.user_activity

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.AIAssistant.getDailyHealthTip
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.Repositories.AppointmentRepository
import com.example.e_clinic.Firebase.Repositories.DoctorRepository
import com.example.e_clinic.Services.functions.appServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight

import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.services.ZIMKit
import kotlinx.coroutines.tasks.await
import kotlin.compareTo
import kotlin.div
import kotlin.rem
import kotlin.text.format
import kotlin.text.get
import kotlin.text.toInt
import kotlin.times

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val appointmentRepository = AppointmentRepository()
    val appointments = remember { mutableStateOf<List<Appointment>>(emptyList()) }
    val dailyHealthTip = remember { mutableStateOf("Loading health tip...") }
    val isLoadingTip = remember { mutableStateOf(false) }

    // Load initial health tip - SAFE VERSION
    LaunchedEffect(Unit) {
        try {
            isLoadingTip.value = true
            dailyHealthTip.value = getDailyHealthTip()
        } finally {
            isLoadingTip.value = false
        }
    }

    // Original appointment loading logic (unchanged)
    LaunchedEffect(user) {
        user?.let {
            appointmentRepository.getAppointmentsForUser(it.uid) { loadedAppointments ->
                appointments.value = loadedAppointments.sortedBy { it.date?.toDate()?.time ?: 0 }
            }
        }
    }

    LaunchedEffect(user) {
        user?.let {
            appointmentRepository.getAppointmentsForUser(it.uid) { loadedAppointments ->
                appointments.value = loadedAppointments
                    .filter { it.status != "CANCELED" }
                    .sortedBy { it.date?.toDate()?.time ?: 0 }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        // Original health tip card (with safe refresh)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable {
                    if (!isLoadingTip.value) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                isLoadingTip.value = true
                                dailyHealthTip.value = getDailyHealthTip()
                            } finally {
                                isLoadingTip.value = false
                            }
                        }
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            if (isLoadingTip.value) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading new tip...")
                }
            } else {
                Text(
                    text = "💡 Health Tip: ${dailyHealthTip.value} (Tap to refresh)",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Rest of your original content (completely unchanged)
        Text(
            text = "Your Next Appointment: ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        appointments.value.firstOrNull()?.let { appointment ->
            NextAppointmentItem(appointment = appointment)
        } ?: Text("No upcoming appointments")

        Spacer(modifier = Modifier.height(16.dp))

        CalendarWidget(appointments.value)
    }
}


// TODO dont delete
@Composable
fun HealthTipCard(
    dailyHealthTip: MutableState<String>,
    isLoadingTip: MutableState<Boolean>,
    showTipError: MutableState<Boolean>,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (isLoadingTip.value) null else rememberRipple(),
                onClick = onRefresh
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (showTipError.value) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoadingTip.value -> LoadingTipIndicator()
                showTipError.value -> ErrorTipIndicator()
                else -> DisplayTip(dailyHealthTip.value)
            }
        }
    }
}

// TODO dont delete
@Composable
private fun LoadingTipIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Generating new tip...")
    }
}
// TODO dont delete
@Composable
private fun ErrorTipIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tap to retry",
            color = MaterialTheme.colorScheme.error
        )
    }
}
// TODO dont delete
@Composable
private fun DisplayTip(tip: String) {
    Column {
        Text(
            text = "💡 Daily Health Tip",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tip,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap for another tip",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
// TODO dont delete
private suspend fun loadHealthTip(
    dailyHealthTip: MutableState<String>,
    isLoadingTip: MutableState<Boolean>,
    showError: MutableState<Boolean>
) {
    if (isLoadingTip.value) return

    isLoadingTip.value = true
    showError.value = false

    try {
        dailyHealthTip.value = getDailyHealthTip()
    } catch (e: Exception) {
        showError.value = true
        Log.e("Error", "Error loading health tip", e)
    } finally {
        isLoadingTip.value = false
    }
}



@Composable
fun CalendarWidget(allAppointments: List<Appointment>) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val appointments = allAppointments.filter { it.user_id == currentUserId && it.status != "CANCELLED" }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
    val selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    val colorScheme = MaterialTheme.colorScheme


    val coroutineScope = rememberCoroutineScope()

    var selectedAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var currentAppointmentIndex by remember { mutableStateOf(0) }

    val appointmentDays = appointments.mapNotNull { appointment ->
        appointment.date?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()
            ?.takeIf { it.year == currentMonth.year && it.month == currentMonth.month }
    }.map { it.dayOfMonth }

    fun openChatWithDoctor(appt: Appointment) {
        Log.d("ChatDebug", "Starting openChatWithPatient()")

        if (appt == null) {
            Log.e("ChatDebug", "ERROR: Null appointment object")
            Toast.makeText(context, "Invalid appointment data", Toast.LENGTH_LONG).show()
            return
        }

        val doctorId = appt.doctor_id?.trim() ?: ""
        if (doctorId.isBlank()) {
            Log.e("ChatDebug", "ERROR: Blank patient ID in appointment: ${appt.toString()}")
            Toast.makeText(context, "Invalid patient ID", Toast.LENGTH_LONG).show()
            return
        }
        Log.d("ChatDebug", "Patient ID validated: ${doctorId.take(4)}...")

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e("ChatDebug", "ERROR: No authenticated Firebase user")
            Toast.makeText(context, "Please sign in first", Toast.LENGTH_LONG).show()
            return
        }

        val selfId = firebaseUser.uid
        val selfName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "User_${selfId.takeLast(4)}"

        Log.d("ChatDebug", "User Details - ID: ${selfId.take(4)}... | Name: $selfName")

        try {
            val localUser = ZIMKit.getLocalUser()
            Log.d("ChatDebug", "ZIMKit LocalUser: ${localUser?.id?.take(4)}...")

            val readyBlock = {
                Log.d("ChatDebug", "Navigating to chat with patient: ${doctorId.take(4)}...")
                try {
                    ZIMKitRouter.toMessageActivity(
                        context,
                        doctorId,
                        ZIMKitConversationType.ZIMKitConversationTypePeer
                    )
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Failed to start chat activity: ${e.message}")
                    Toast.makeText(context, "Failed to open chat", Toast.LENGTH_LONG).show()
                }
            }

            if (localUser != null && localUser.id == selfId) {
                Log.d("ChatDebug", "Already connected to ZIMKit")
                readyBlock()
            } else {
                Log.d("ChatDebug", "Initiating ZIMKit connection...")
                ZIMKit.connectUser(selfId, selfName, "") { err ->
                    when {
                        err == null || err.code.value() == 0 -> {
                            Log.d("ChatDebug", "ZIMKit connection successful")
                            readyBlock()
                        }
                        err.code.value() == 6000011 -> { // PARAM_INVALID
                            Log.e("ChatDebug", "Invalid parameters - Name: '$selfName'")
                            Toast.makeText(
                                context,
                                "Please set your display name in profile settings",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Log.e(
                                "ChatDebug",
                                "Connection failed (${err.code}): ${err.message}\n" +
                                        "Common fixes:\n" +
                                        "1. Verify ZIMKit.init() was called\n" +
                                        "2. Check Zego AppID/Signature\n" +
                                        "3. Ensure network connectivity"
                            )

                            Toast.makeText(
                                context,
                                "Chat service unavailable (${err.code})",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDebug", "Critical error: ${e.javaClass.simpleName}\n${e.stackTraceToString()}")
            Toast.makeText(context, "Chat system error occurred", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentMonth.year}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .pointerInput(currentMonth, appointments) {
                            detectTapGestures { offset ->
                                val dayWidth = size.width / 7
                                val dayHeight = size.height / 6
                                val column = (offset.x / dayWidth).toInt()
                                val row = (offset.y / dayHeight).toInt()
                                val day = row * 7 + column - firstDayOfMonth + 1

                                if (day in 1..daysInMonth) {
                                    val clickedDate = currentMonth.atDay(day)
                                    selectedDate.value = clickedDate

                                    val appointmentsForDay = appointments.filter { appointment ->
                                        appointment.date?.toDate()?.toInstant()
                                            ?.atZone(ZoneId.systemDefault())
                                            ?.toLocalDate() == clickedDate
                                    }

                                    if (appointmentsForDay.isNotEmpty()) {
                                        selectedAppointments = appointmentsForDay
                                        currentAppointmentIndex = 0
                                        showDialog = true

                                        coroutineScope.launch {
                                            val doctorId = appointmentsForDay[0].doctor_id
                                            try {
                                                val docSnap = FirebaseFirestore.getInstance()
                                                    .collection("doctors")
                                                    .document(doctorId)
                                                    .get()
                                                    .await()
                                                selectedDoctor = docSnap.toObject(Doctor::class.java)?.apply {
                                                    id = docSnap.id
                                                }
                                            } catch (e: Exception) {
                                                Log.e("CalendarWidget", "Error fetching doctor", e)
                                                selectedDoctor = null
                                            }
                                        }
                                    } else {
                                        selectedAppointments = emptyList()
                                        selectedDoctor = null
                                        showDialog = true
                                    }
                                }
                            }
                        }
                ) {
                    val dayWidth = size.width / 7
                    val dayHeight = size.height / 6

                    for (day in 1..daysInMonth) {
                        val column = (day + firstDayOfMonth - 1) % 7
                        val row = (day + firstDayOfMonth - 1) / 7
                        val x = column * dayWidth + dayWidth / 2
                        val y = row * dayHeight + dayHeight / 2

                        if (day in appointmentDays) {
                            drawCircle(
                                color = colorScheme.primaryContainer, // Subtle color
                                radius = minOf(dayWidth, dayHeight) / 3,
                                center = Offset(x, y)
                            )
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            day.toString(),
                            x,
                            y + 15f,
                            android.graphics.Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 40f
                                color = colorScheme.onSurface.toArgb()
                            }
                        )
                    }
                }

                // ... (Dialog code unchanged)
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Appointment Details") },
                        text = {
                            if (selectedAppointments.isEmpty()) {
                                Text("No appointment found on ${selectedDate.value}")
                            } else {
                                val appointment = selectedAppointments[currentAppointmentIndex]
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Date: ${selectedDate.value}")
                                    if (selectedDoctor != null) {
                                        Text("Doctor: Dr. ${selectedDoctor!!.name} ${selectedDoctor!!.surname}")
                                        Text("Specialization: ${formatEnumString(selectedDoctor!!.specialization)}")
                                    } else {
                                        Text("Loading doctor info...")
                                    }
                                    Text("Status: ${formatEnumString(appointment.status)}")

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = {
                                                currentAppointmentIndex =
                                                    if (currentAppointmentIndex == 0) selectedAppointments.size - 1
                                                    else currentAppointmentIndex - 1
                                            },
                                            enabled = selectedAppointments.size > 1
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Appointment")
                                        }
                                        IconButton(
                                            onClick = {
                                                currentAppointmentIndex =
                                                    if (currentAppointmentIndex == selectedAppointments.size - 1) 0
                                                    else currentAppointmentIndex + 1
                                            },
                                            enabled = selectedAppointments.size > 1
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Appointment")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { openChatWithDoctor(appointment) },
                                        enabled = selectedDoctor != null
                                    ) {
                                        Text("Chat with Doctor")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NextAppointmentItem(appointment: Appointment) {
    if (appointment.status == "CANCELED" || appointment.status == "FINISHED") {Text("No upcoming appointments"); return}
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val doctor = remember { mutableStateOf<Doctor?>(null) }

    // Fetch doctor info when appointment changes
    LaunchedEffect(appointment.doctor_id) {
        appointment.doctor_id?.let { doctorId ->
            FirebaseFirestore.getInstance().collection("doctors").document(doctorId)
                .get()
                .addOnSuccessListener { doc ->
                    doctor.value = doc.toObject(Doctor::class.java)?.apply { id = doc.id }
                }
                .addOnFailureListener { doctor.value = null }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            doctor.value?.let {
                Text(
                    text = "Dr. ${it.name} ${it.surname}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatEnumString(it.specialization).uppercase(),
                    style = MaterialTheme.typography.bodySmall
                )
            } ?: Text(
                text = "Loading doctor info...",
                style = MaterialTheme.typography.titleMedium
            )
            appointment.date?.let {
                Column {
                    Text(
                        text = dateFormat.format(it.toDate()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = timeFormat.format(it.toDate()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

fun formatEnumString(enumString: String): String =
    enumString.lowercase()
        .split('_')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }