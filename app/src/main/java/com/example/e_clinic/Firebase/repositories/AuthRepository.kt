package com.example.e_clinic.Firebase.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    fun registerUser(email: String, password: String, onComplete: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // NEW: Facebook Authentication
    fun signInWithFacebook(token: String, onComplete: (Boolean, String?) -> Unit) {
        val credential = FacebookAuthProvider.getCredential(token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }
    }

