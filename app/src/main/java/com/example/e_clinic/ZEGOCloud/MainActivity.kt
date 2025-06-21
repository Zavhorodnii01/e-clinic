package com.example.e_clinic.ZEGOCloud

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_clinic.R
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        val ownIdEditText = findViewById<EditText>(R.id.own_id)
        val targetIdEditText = findViewById<EditText>(R.id.target_id)
        val yourUserID = findViewById<TextView>(R.id.your_user_id)

        // Set your app credentials
        val appID = 2013180826L
        val appSign = "7f25f426b54a6fb21587414c4ac9174adb144d9d43353c41df876afcb43062df"

        // Initialize call invitation buttons
        val videoCallBtn = findViewById<ZegoSendCallInvitationButton>(R.id.video_call_btn)
        val voiceCallBtn = findViewById<ZegoSendCallInvitationButton>(R.id.voice_call_btn)

        // When user enters their ID, initialize the call service
        ownIdEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && ownIdEditText.text.isNotEmpty()) {
                val userID = ownIdEditText.text.toString()
                yourUserID.text = "Your ID: $userID"

                // Initialize ZegoUIKit call service
                ZegoUIKitPrebuiltCallService.init(
                    application,
                    appID,
                    appSign,
                    userID,
                    userID, // Using ID as name for simplicity
                    ZegoUIKitPrebuiltCallInvitationConfig()
                )
            }
        }

        // Set up video call button
        videoCallBtn.setOnClickListener(View.OnClickListener {
            makeCall(targetIdEditText, isVideoCall = true)
        })

        // Set up voice call button
        voiceCallBtn.setOnClickListener(View.OnClickListener {
            makeCall(targetIdEditText, isVideoCall = false)
        })

        // Logout button
        findViewById<Button>(R.id.logout_btn).setOnClickListener {
            ZegoUIKitPrebuiltCallService.unInit()
            finish()
        }
    }

    private fun makeCall(targetIdEditText: EditText, isVideoCall: Boolean) {
        val targetID = targetIdEditText.text.toString().trim()
        if (targetID.isEmpty()) {
            Toast.makeText(this, "Please enter target ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Create user list with explicit type
        val invitees: List<ZegoUIKitUser> = listOf(ZegoUIKitUser(targetID, targetID))

        // Get the appropriate button
        val button = if (isVideoCall) {
            findViewById<ZegoSendCallInvitationButton>(R.id.video_call_btn)
        } else {
            findViewById<ZegoSendCallInvitationButton>(R.id.voice_call_btn)
        }

        // Configure the button
        button.apply {
            setIsVideoCall(isVideoCall)
            setInvitees(invitees) // Explicitly typed to avoid ambiguity
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallService.unInit()
    }
}