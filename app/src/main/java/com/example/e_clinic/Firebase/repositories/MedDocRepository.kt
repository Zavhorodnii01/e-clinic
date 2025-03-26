package com.example.e_clinic.Firebase.repositories

import android.net.Uri
import com.example.e_clinic.Firebase.collections.MedDoc
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
//MedDocRepository connects Firestore to Firebase Storage.
class MedDocRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val collection = db.collection("med_docs")

    fun uploadMedDoc(medDoc: MedDoc, fileUri: Uri, onComplete: (Boolean) -> Unit) {
        val fileRef = storage.reference.child("med_docs/${medDoc.id}")
        fileRef.putFile(fileUri).addOnSuccessListener {
            collection.add(medDoc)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun getMedDocsForUser(userId: String, onSuccess: (List<MedDoc>) -> Unit) {
        collection.whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(MedDoc::class.java))
            }
    }
}