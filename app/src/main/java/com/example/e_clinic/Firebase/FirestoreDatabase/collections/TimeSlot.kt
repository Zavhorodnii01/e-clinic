package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TimeSlot(
    @DocumentId
    var id: String = "",
    val doctor_id: String = "",
    val specialization: String = "",
    val available_slots: List<Timestamp> = emptyList()
)