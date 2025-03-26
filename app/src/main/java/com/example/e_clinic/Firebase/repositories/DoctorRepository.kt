package com.example.e_clinic.Firebase.repositories

import com.example.e_clinic.Firebase.collections.Doctor
import com.google.firebase.firestore.FirebaseFirestore

class DoctorRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("doctors")

    fun updateDoctorProfile(doctorId: String, newData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        collection.document(doctorId).update(newData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getDoctorsBySpecialization(specialization: String, onSuccess: (List<Doctor>) -> Unit) {
        collection.whereEqualTo("specialization", specialization).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Doctor::class.java))
            }
    }
}