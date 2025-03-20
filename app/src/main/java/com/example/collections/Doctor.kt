package com.example.collections

import com.google.firebase.firestore.DocumentId


data class Doctor(
    @DocumentId
    val id: String = "",
    val address: String = "",
    val dob: String = "",
    val e_mail: String = "",
    val education: String = "",
    val experience: String = "",
    val gender: String = "",
    val name: String = "",
    val phone: String = "",
    val specialization: String = "",
    val surname: String = ""
)