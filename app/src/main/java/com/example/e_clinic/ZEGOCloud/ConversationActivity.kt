package com.example.e_clinic.ZEGOCloud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.e_clinic.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType

class ConversationActivity : AppCompatActivity() {

    private lateinit var actionButton: FloatingActionButton
    private val appID = 2013180826L
    private val appSign = "7f25f426b54a6fb21587414c4ac9174adb144d9d43353c41df876afcb43062df"
    private lateinit var currentUserID: String
    private var targetUserIDForCall: String? = null

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            targetUserIDForCall?.let { startCall(it) }
        } else {
            Toast.makeText(
                this,
                "Camera & microphone permissions are required for calls",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conversation)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            // Handle redirection to login activity
            finish()
            return
        }
        currentUserID = firebaseUser.uid

        actionButton = findViewById(R.id.floating_btn)
        actionButton.setOnClickListener { showPopupMenu() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, actionButton)
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.new_chat -> { showNewChatDialog(); true }
                R.id.start_call -> { showCallDialog(); true }
                R.id.logout -> { handleLogout(); true }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showNewChatDialog() {
        val editText = EditText(this).apply { hint = "User ID" }

        AlertDialog.Builder(this)
            .setTitle("New Chat")
            .setView(editText)
            .setPositiveButton("Chat") { _, _ ->
                editText.text.toString().takeIf { it.isNotEmpty() }?.let { startChat(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startChat(targetUserID: String) {
        ZIMKitRouter.toMessageActivity(this, targetUserID, ZIMKitConversationType.ZIMKitConversationTypePeer)
    }

    private fun showCallDialog() {
        val editText = EditText(this).apply { hint = "Target User ID" }

        AlertDialog.Builder(this)
            .setTitle("Start a Call")
            .setView(editText)
            .setPositiveButton("Call") { _, _ ->
                editText.text.toString().takeIf { it.isNotEmpty() }?.let { initiateCall(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateCall(targetUserID: String) {
        this.targetUserIDForCall = targetUserID
        if (hasRequiredPermissions()) {
            startCall(targetUserID)
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

    private fun startCall(targetUserID: String) {
        val callID = "call_${currentUserID}_$targetUserID"

        Intent(this, CallActivity::class.java).apply {
            putExtra("appID", appID)
            putExtra("appSign", appSign)
            putExtra("userID", currentUserID)
            putExtra("userName", FirebaseAuth.getInstance().currentUser?.displayName ?: currentUserID)
            putExtra("callID", callID)
            putExtra("targetUserID", targetUserID)
            startActivity(this)
        }
    }

    private fun handleLogout() {
        FirebaseAuth.getInstance().signOut()
        // Handle redirection to login activity
        finish()
    }
}