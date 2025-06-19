package com.example.e_clinic.Firebase.Repositories

import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
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

    fun getDoctorById(doctorId: String, onSuccess: (Doctor?) -> Unit) {
        collection.document(doctorId).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(Doctor::class.java))
            }
    }
}