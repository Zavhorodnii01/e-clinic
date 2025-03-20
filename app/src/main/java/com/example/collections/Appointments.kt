package com.example.collections

import com.google.firebase.firestore.DocumentId

data class Appointment(
    @DocumentId val id: String = "",
    val date_and_time: String = "",
    val doctor_id: String = "",
    val user_id: String = ""
)