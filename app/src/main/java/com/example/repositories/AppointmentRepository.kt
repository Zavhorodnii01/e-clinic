package com.example.repositories

import com.example.collections.Appointment
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("appointments")

    fun bookAppointment(appointment: Appointment, onComplete: (Boolean) -> Unit) {
        collection.add(appointment)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getAppointmentsForUser(userId: String, onSuccess: (List<Appointment>) -> Unit) {
        collection.whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Appointment::class.java))
            }
    }
}