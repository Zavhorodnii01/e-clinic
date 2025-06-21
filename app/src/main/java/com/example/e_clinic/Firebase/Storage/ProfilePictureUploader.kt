package com.example.e_clinic.Firebase.Storage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage


fun uploadProfilePicture(
    activity: Activity,
    id: String,
    imageUri: Uri,
    onSuccess: (String) -> Unit = {}
) {
    val storageRef = FirebaseStorage.getInstance().reference.child("profiles/$id.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
                Toast.makeText(activity, "Profile picture uploaded!", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(activity, "Failed to upload profile picture: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}