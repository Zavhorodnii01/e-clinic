package com.example.e_clinic.ZEGOCloud

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_clinic.BuildConfig
import com.example.e_clinic.R
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageFragment
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var targetUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        targetUserId = intent.getStringExtra("targetUserId") ?: return

        // Add chat fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ZIMKitMessageFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", targetUserId)
                }
            })
            .commit()

        // Set up call button
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.call_button).setOnClickListener {
            startCall(targetUserId)
        }
    }

    private fun startCall(targetUserId: String) {
        val appID = BuildConfig.APP_ID.toLong()
        val appSign = BuildConfig.APP_SIGN

        val user = FirebaseAuth.getInstance().currentUser
        val userID = user?.uid ?: "defaultUser"
        val userName = user?.displayName ?: user?.email ?: userID
        val callID = UUID.randomUUID().toString()

        val config = ZegoUIKitPrebuiltCallConfig().apply {
            turnOnCameraWhenJoining = true
            turnOnMicrophoneWhenJoining = true
            // Use enum value directly
            layout = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().layout
        }

        // Create and show call fragment
        val callFragment = ZegoUIKitPrebuiltCallFragment.newInstance(
            appID,
            appSign,
            callID,
            userID,
            userName,
            config
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.call_container, callFragment)
            .commitNow()
    }
}