package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Appointment(
    @DocumentId val id: String = "",
    val date: Timestamp? = null ,
    val doctor_id: String = "",
    val user_id: String = "",
    val status: String = ""
)