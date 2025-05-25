package com.example.e_clinic.Firebase.repositories

import com.example.e_clinic.Firebase.collections.MedicalRecord
import com.google.firebase.firestore.FirebaseFirestore

class MedicalRecords {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("medical_records")

    fun addMedicalHistory(history: MedicalRecord, onComplete: (Boolean) -> Unit) {
        collection.add(history)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getHistoryByUser(userId: String, onSuccess: (List<MedicalRecord>) -> Unit) {
        collection.whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(MedicalRecord::class.java))
            }
    }
}