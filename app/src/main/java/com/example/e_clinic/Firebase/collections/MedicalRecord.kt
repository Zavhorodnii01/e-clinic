package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class MedicalRecord(
    @DocumentId val id: String = "",
    val appointment_id: Timestamp? = null,
    val user_id: String = "",
    val doctor_id: String = "",
    val date: String = "",
    val prescription_id: String = "",
    val doctors_notes: String = ""
)