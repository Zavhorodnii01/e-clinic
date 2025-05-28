package com.example.e_clinic.Firebase.collections

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val address: String = "",
    val dob: String = "",
    val email: String = "",
    val gender: String = "",
    val name: String = "",
    val phone: String = "",
    val surname: String = "",
    val pinCode: String? = null,
    val hasSetPin: Boolean = false // Dodane pole do śledzenia, czy użytkownik ustawił PIN
)
