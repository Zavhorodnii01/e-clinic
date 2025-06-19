package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class MedicalRecord(
    @DocumentId val id: String = "",
    val appointment_id: String = "",
    val user_id: String = "",
    val doctor_id: String = "",
    val date: Timestamp? = null,
    val prescription_id: String = "",
    val doctors_notes: String = ""
)