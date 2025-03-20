package com.example.collections

import com.google.firebase.firestore.DocumentId

data class MedDoc(
    @DocumentId val id: String = "",
    val doctor_id: String = "",
    val documents: String = "", // Zakładam, że to URL do pliku w Firebase Storage
    val user_id: String = ""
)