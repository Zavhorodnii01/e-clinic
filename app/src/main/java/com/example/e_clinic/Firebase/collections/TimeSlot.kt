package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TimeSlot(
    @DocumentId
    val id: String = "",
    val doctor_id: String = "",
    val specializations: List<String> = emptyList(),
    val available_slots: List<Timestamp> = emptyList()
)