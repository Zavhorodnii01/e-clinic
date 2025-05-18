package com.example.e_clinic.Firebase.collections

data class Prescription(
    val doctor_id: String = "",
    val user_id: String = "",
    val issued_date: String = "",
    val medications: String = "",
    val appointment_id: String = ""
)
