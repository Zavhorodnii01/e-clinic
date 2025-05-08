package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId


data class Doctor(
    @DocumentId
    val id: String = "",
    val address: String = "",
    val dob: Timestamp? = null,
    val e_mail: String = "",
    val education: String = "",
    val experience: String = "",
    val gender: String = "",
    val name: String = "",
    val phone: String = "",
    val specialization: String = "",
    val surname: String = ""
)