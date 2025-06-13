package com.example.e_clinic.ZEGOCloud

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zim.enums.ZIMErrorCode

fun launchZegoChat(context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val name = document.getString("name") ?: "Unknown"
                val surname = document.getString("surname") ?: "Unknown"
                val userName = "$name $surname"
                val userAvatar = "" // Optional avatar URL

        ZIMKit.connectUser(userId, userName, userAvatar) { errorInfo ->
            if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {

                val intent = Intent(context, ConversationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // ADD THIS LINE
                context.startActivity(intent)

            } else {
                Toast.makeText(context, "Chat login failed: ${errorInfo.message}", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}