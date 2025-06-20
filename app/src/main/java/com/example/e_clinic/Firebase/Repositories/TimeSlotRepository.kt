package com.example.e_clinic.Firebase.Repositories

import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class TimeSlotRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("timeslots")

    fun updateTimeslotAvailability(timeslotId: String, isFree: Boolean, onComplete: (Boolean) -> Unit) {
        collection.document(timeslotId).update(mapOf(
            "appointments.is_free" to isFree
        )).addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getTimeslotsForDoctor(doctorId: String, onSuccess: (List<TimeSlot>) -> Unit) {
        collection.whereEqualTo("doctor_id", doctorId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(TimeSlot::class.java))
            }
    }
    fun getCollection(): CollectionReference {
        return collection
    }
}