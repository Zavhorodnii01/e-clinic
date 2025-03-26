package com.example.e_clinic.Firebase.collections

import com.google.firebase.firestore.DocumentId

data class MedDoc(
    @DocumentId val id: String = "",
    val doctor_id: String = "",
    val documents: String = "", // Zakładam, że to URL do pliku w Firebase Storage
    val user_id: String = ""
)