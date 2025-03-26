package com.example.e_clinic.Firebase.repositories

import com.example.e_clinic.Firebase.collections.Administrator
import com.google.firebase.firestore.FirebaseFirestore

class AdministratorRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("administrators")

    fun addAdministrator(admin: Administrator, onComplete: (Boolean) -> Unit) {
        collection.document(admin.id).set(admin)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteAdministrator(adminId: String, onComplete: (Boolean) -> Unit) {
        collection.document(adminId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}