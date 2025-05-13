package com.example.e_clinic.ZEGOCloud

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zim.enums.ZIMErrorCode

fun launchZegoChat(context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val userId = user.uid
        val userName = user.displayName ?: user.email ?: user.uid
        val userAvatar = "" // Optional avatar URL

        ZIMKit.connectUser(userId, userName, userAvatar) { errorInfo ->
            if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {

                val intent = Intent(context, ConversationActivity::class.java)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Chat login failed: ${errorInfo.message}", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
    }
}