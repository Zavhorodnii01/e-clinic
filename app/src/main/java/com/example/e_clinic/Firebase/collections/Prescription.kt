package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Prescription(
    @DocumentId
    val id: String = "",
    val doctor_id: String = "",
    var user_id: String = "",
    val issued_date: Timestamp? = null,
    var link_to_storage: String = "",
    var appointment_id: String = "",
    val doctor_comment: String = "",
)
