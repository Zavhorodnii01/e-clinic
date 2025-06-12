package com.example.e_clinic.Firebase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName


data class Doctor(
    @DocumentId
    var id: String = "",
    val address: String = "",
    val dob: Timestamp? = null,
    @PropertyName("e-mail")
    val email: String = "",
    val education: String = "",
    val experience: String = "",
    val gender: String = "",
    val name: String = "",
    val phone: String = "",
    val specialization: String = "",
    val surname: String = "",

)