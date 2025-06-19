package com.example.e_clinic.Firebase.Repositories

import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Appointment
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

    fun getAppointmentsForDoctor(doctorId: String, onSuccess: (List<Appointment>) -> Unit) {
        collection.whereEqualTo("doctor_id", doctorId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Appointment::class.java))
            }
    }
}