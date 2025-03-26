package com.example.e_clinic.Firebase.collections

import com.google.firebase.firestore.DocumentId

data class MedicalHistory(
    @DocumentId val id: String = "",
    val appointment_id: String = "",
    val med_doc_id: String = "",
    val user_id: String = ""
)