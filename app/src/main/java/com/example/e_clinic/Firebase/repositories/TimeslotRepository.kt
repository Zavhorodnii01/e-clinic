package com.example.e_clinic.Firebase.repositories

import com.example.e_clinic.Firebase.collections.Timeslot
import com.google.firebase.firestore.FirebaseFirestore

class TimeslotRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("timeslots")

    fun updateTimeslotAvailability(timeslotId: String, isFree: Boolean, onComplete: (Boolean) -> Unit) {
        collection.document(timeslotId).update(mapOf(
            "appointments.is_free" to isFree
        )).addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getTimeslotsForDoctor(doctorId: String, onSuccess: (List<Timeslot>) -> Unit) {
        collection.whereEqualTo("doctor_id", doctorId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Timeslot::class.java))
            }
    }
}