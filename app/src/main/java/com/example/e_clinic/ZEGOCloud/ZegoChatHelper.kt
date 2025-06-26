package com.example.e_clinic.ZEGOCloud

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zim.enums.ZIMErrorCode

fun launchZegoChat(context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
        return
    }

    val userId = user.uid
    Log.d("UserProfile", "Current user ID: $userId")

    val db = FirebaseFirestore.getInstance()

    // First, check the "users" collection
    db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
        if (userDoc.exists()) {
            // User is a regular "user"
            val name = userDoc.getString("name") ?: "Unknown"
            val surname = userDoc.getString("surname") ?: "Unknown"
            val userName = "$name $surname"
            connectToZIMKit(context, userId, userName)
            //Log.d("UserProfile2",  userName)
            return@addOnSuccessListener
        }

        // If not found in "users", check the "doctors" collection
        db.collection("doctors").document(userId).get().addOnSuccessListener { doctorDoc ->
            if (doctorDoc.exists()) {
                // User is a "doctor"
                val name = doctorDoc.getString("name") ?: "Dr. Unknown"
                val surname = doctorDoc.getString("surname") ?: ""
                val userName = "$name $surname"
                connectToZIMKit(context, userId, userName)
                Log.d("UserProfile2",  userName)
            } else {
                // Not found in either collection
                Log.d("UserProfile", "User not found in 'users' or 'doctors'")
                Toast.makeText(context, "Profile not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("UserProfile", "Error fetching doctor data", e)
            Toast.makeText(context, "Error fetching profile", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { e ->
        Log.e("UserProfile", "Error fetching user data", e)
        Toast.makeText(context, "Error fetching profile", Toast.LENGTH_SHORT).show()
    }
}

// Helper function to connect to ZIMKit
private fun connectToZIMKit(context: Context, userId: String, userName: String) {
    ZIMKit.connectUser(userId, userName, "") { errorInfo ->
        if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {
            val intent = Intent(context, ConversationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Chat login failed: ${errorInfo.message}", Toast.LENGTH_SHORT).show()
        }
    }
}