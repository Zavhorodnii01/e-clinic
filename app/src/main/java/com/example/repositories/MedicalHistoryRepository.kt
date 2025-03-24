package com.example.repositories

import com.example.collections.MedicalHistory
import com.google.firebase.firestore.FirebaseFirestore

class MedicalHistoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("medical_histories")

    fun addMedicalHistory(history: MedicalHistory, onComplete: (Boolean) -> Unit) {
        collection.add(history)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getHistoryByUser(userId: String, onSuccess: (List<MedicalHistory>) -> Unit) {
        collection.whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(MedicalHistory::class.java))
            }
    }
}