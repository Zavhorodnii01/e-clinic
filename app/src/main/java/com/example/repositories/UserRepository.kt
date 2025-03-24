package com.example.repositories

import com.example.collections.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("users")

    fun updateUserMedicalHistory(userId: String, historyId: String, onComplete: (Boolean) -> Unit) {
        collection.document(userId).update("medical_history_id", historyId)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getUserById(userId: String, onSuccess: (User?) -> Unit) {
        collection.document(userId).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(User::class.java))
            }
    }
}