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
        val config = ZegoUIKitPrebuiltCallConfig().apply {
            turnOnCameraWhenJoining = true
            turnOnMicrophoneWhenJoining = true
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                ZegoUIKitPrebuiltCallFragment.newInstance(
                    intent.getLongExtra("appID", 0L),
                    intent.getStringExtra("appSign") ?: "",
                    intent.getStringExtra("userID") ?: "",
                    intent.getStringExtra("userName") ?: "",
                    intent.getStringExtra("callID") ?: "",
                    config
                )
            )
            .commitNow()
    }
}