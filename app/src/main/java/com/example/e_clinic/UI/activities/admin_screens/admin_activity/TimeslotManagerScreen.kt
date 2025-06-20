package com.example.e_clinic.UI.activities.admin_screens.admin_activity


import android.app.TimePickerDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import java.time.temporal.TemporalAdjusters
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import java.util.Date
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.example.e_clinic.Firebase.Repositories.TimeSlotRepository
import com.google.firebase.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.text.get


@Composable
fun TimeslotManagerScreen(selectedDoctor: Doctor) {
    val daysOfWeek = listOf("M", "T", "W", "Th", "F", "S", "Sd")
    val dayMap = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    val selectedDays = remember { mutableStateListOf<Int>() }
    var startHour by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endHour by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }
    var generatedSlots by remember { mutableStateOf(listOf<LocalDateTime>()) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }


    Column(  Modifier
        .fillMaxSize()
        .background(Color.White)
        .padding(16.dp)) {
        Text("Doctor: ${selectedDoctor.name} ${selectedDoctor.surname}", style = MaterialTheme.typography.titleMedium)
        Text("Specialization: ${selectedDoctor.specialization}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text("Select Days:", style = MaterialTheme.typography.titleLarge)
        LazyRow {
            items(daysOfWeek.size) { idx ->
                val label = daysOfWeek[idx]
                val selected = selectedDays.contains(idx)
                Button(
                    onClick = {
                        if (selected) selectedDays.remove(idx) else selectedDays.add(idx)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color.Blue else Color.LightGray,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp) // Make buttons round
                        .clip(RoundedCornerShape(20.dp)) // Apply rounded shape
                ) {
                    Text(label)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Select Working Hours:", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("From: ${startHour}")
            Spacer(Modifier.width(8.dp))
            Button(onClick = { showTimePicker = true; pickingStart = true }) { Text("Pick") }
            Spacer(Modifier.width(16.dp))
            Text("To: ${endHour}")
            Spacer(Modifier.width(8.dp))
            Button(onClick = { showTimePicker = true; pickingStart = false }) { Text("Pick") }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val repository = TimeSlotRepository()
            val slots = mutableListOf<LocalDateTime>()
            val today = LocalDate.now()

            selectedDays.forEach { dayIdx ->
                val dayOfWeek = dayMap[dayIdx]
                val nextOrSameDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                val startDateTime = nextOrSameDate.atTime(startHour)
                val endDateTime = nextOrSameDate.atTime(endHour)

                var currentSlot = startDateTime
                while (currentSlot.isBefore(endDateTime)) {
                    slots.add(currentSlot)
                    currentSlot = currentSlot.plus(1, ChronoUnit.HOURS) // Example: 1-hour slots
                }
            }

            generatedSlots = slots

            slots.forEach { slot ->
                val instant = slot.atZone(ZoneId.systemDefault()).toInstant()
                val timeslot = TimeSlot(
                    id = "",
                    doctor_id = selectedDoctor.id,
                    specialization = selectedDoctor.specialization,
                    available_slots = listOf(Timestamp(Date.from(instant)))
                )
                repository.getCollection().add(timeslot)
            }
        }) {
            Text("Generate & Save Timeslots")
        }

        Spacer(Modifier.height(16.dp))
        if (generatedSlots.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Timeslots Generated") },
                text = { Text("Generated ${generatedSlots.size} slots") },
                confirmButton = {
                    Button(onClick = { }) {
                        Text("OK")
                    }
                }
            )
    }
    if (showTimePicker) {
        val context = LocalContext.current
        val timePickerDialog = TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute ->
                if (pickingStart) {
                    startHour = LocalTime.of(pickedHour, pickedMinute)
                } else {
                    endHour = LocalTime.of(pickedHour, pickedMinute)
                }
                showTimePicker = false
            },
            if (pickingStart) startHour.hour else endHour.hour,
            if (pickingStart) startHour.minute else endHour.minute,
            true
        )
        timePickerDialog.show()
    }
    }
}