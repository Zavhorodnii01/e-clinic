package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TimeSlot(
    @DocumentId
    var id: String = "",
    val doctor_id: String = "",
    val specializations: String = "",
//    val specializations: List<String> = emptyList(),
    val available_slots: List<Timestamp> = emptyList(),

    val startTime: String = "",
    val endTime: String = "",
    val date: Timestamp? = null,
    val isBooked: Boolean = false
)