package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val address: String = "",
    val dob: Timestamp? = null,
    val email: String = "",
    val gender: String = "",
    val name: String = "",
    val phone: String = "",
    val surname: String = "",
    val profilePicture: String = ""
)