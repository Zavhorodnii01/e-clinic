package com.example.collections

import com.google.firebase.firestore.DocumentId
//The class represents the administrator in Firestore.
// It stores contact information and allows management of the system.
data class Administrator(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val surname: String = ""
)
