package com.example.collections

import com.google.firebase.firestore.DocumentId

data class Timeslot(
    @DocumentId val id: String = "",
    val doctor_id: String = "",
    val first_day_of_week: String = "",
    val specializations: List<String> = emptyList(),
    val user_id: String = "",
    val appointments: List<Appointment> = emptyList() // Zagnieżdżona struktura
)