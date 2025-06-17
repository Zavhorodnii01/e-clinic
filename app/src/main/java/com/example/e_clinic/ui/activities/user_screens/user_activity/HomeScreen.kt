package com.example.e_clinic.ui.activities.user_screens.user_activity

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.collections.Appointment
import com.example.e_clinic.Firebase.collections.Doctor
import com.example.e_clinic.Firebase.repositories.AppointmentRepository
import com.example.e_clinic.Firebase.repositories.DoctorRepository
import com.example.e_clinic.services.functions.appServices
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val appointmentRepository = AppointmentRepository()
    val appointments = remember { mutableStateListOf<Appointment>() }


    LaunchedEffect(user) {
        user?.let {
            appointmentRepository.getAppointmentsForUser(it.uid) { loadedAppointments ->
                appointments.clear()
                loadedAppointments.forEach { appointment ->
                    val doctorRepository = DoctorRepository()
                    doctorRepository.getDoctorById(appointment.doctor_id) { doctor ->
                        val doctorId = doctor?.id ?: "Unknown ID"
                        appointments.add(appointment.copy(doctor_id = doctorId))

                    }
                }
            }
        }
    }

    val services = appServices()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp))
    {
        Text(
            text = "Your Next Appointment: ",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        NextAppointmentItem(appointment = appointments.firstOrNull() ?: return, doctor = null)
        CalendarWidget(appointments)
    }




/*
    Column {
        Text(text = "Upcoming Appointments", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(appointments) { appointment ->
                AppointmentItem(title = "Doctor: ${appointment.doctor_id}", description = "Date & Time: ${appointment.date}")
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        ServicesSection(services = services, onServiceClick = { service ->
            """when (service.name) {
                "appointment" -> {
                    val intent = Intent(context, AppointmentActivity::class.java)
                    intent.putExtra("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    context.startActivity(intent)
                }
            }"""
        })
    }
*/
}

@Composable
fun CalendarWidget(appointments: List<Appointment>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
    val selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    // Extract appointment days for the current month
    val appointmentDays = appointments.mapNotNull { appointment ->
        appointment.date?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()
            ?.takeIf { it.year == currentMonth.year && it.month == currentMonth.month }
    }.map { it.dayOfMonth }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .border(1.dp, colorScheme.onSurface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val dayWidth = size.width / 7
                            val dayHeight = size.height / 6
                            val column = (offset.x / dayWidth).toInt()
                            val row = (offset.y / dayHeight).toInt()
                            val day = row * 7 + column - firstDayOfMonth + 1
                            if (day in 1..daysInMonth) {
                                selectedDate.value = currentMonth.atDay(day)
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
                            color = Color.Yellow,
                            radius = minOf(dayWidth, dayHeight) / 3,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
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
            }
        }
    }
}

@Composable
fun NextAppointmentItem(
    appointment: Appointment,
    doctor: Doctor? = null
){
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    if (appointment == null) {
        Text(
            text = "No upcoming appointments",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
    else{
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ){
            Column(Modifier.padding(16.dp)) {
                doctor?.let {
                    Text(
                        text = "Dr. ${it.name} ${it.surname}/n ${it.specialization}",
                        style = MaterialTheme.typography.titleMedium
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
}

