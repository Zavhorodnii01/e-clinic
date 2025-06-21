package com.example.e_clinic.ZEGOCloud

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.e_clinic.R
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class CallActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            addCallFragment()
        } else {
            Toast.makeText(
                this,
                "Camera & microphone permissions are required for calls",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        if (hasRequiredPermissions()) {
            addCallFragment()
        } else {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    private fun addCallFragment() {
        val appID = intent.getLongExtra("appID", 0L)
        val appSign = intent.getStringExtra("appSign") ?: run {
            Toast.makeText(this, "Invalid call configuration", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val userID = intent.getStringExtra("userID") ?: run {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val userName = intent.getStringExtra("userName") ?: userID
        val callID = intent.getStringExtra("callID") ?: run {
            Toast.makeText(this, "Call ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
            turnOnCameraWhenJoining = true
            turnOnMicrophoneWhenJoining = true
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                ZegoUIKitPrebuiltCallFragment.newInstance(
                    appID,
                    appSign,
                    userID,
                    userName,
                    callID,
                    config
                )
            )
            .commitNow()
    }
}