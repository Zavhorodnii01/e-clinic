package com.example.e_clinic.ZEGOCloud

import android.os.Bundle
import android.widget.EditText
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.e_clinic.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zegocloud.zimkit.common.CustomZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType

class ConversationActivity : AppCompatActivity() {

    private lateinit var actionButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_conversation)

        //actionButton = findViewById(R.id.floating_btn)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

       /* actionButton.setOnClickListener {
            showPopupMenu()
        }*/
    }

    /*private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, actionButton)
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.new_chat -> {
                    showNewChatDialog()
                    true
                }
                R.id.logout -> {
                    // Handle logout
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showNewChatDialog() {
        val editText = EditText(this).apply {
            hint = "UserID"
        }

        AlertDialog.Builder(this)
            .setTitle("New chat")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val targetUserId = editText.text.toString()
                startMessageActivityWithCall(targetUserId)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun startMessageActivityWithCall(targetUserId: String) {
        // Get current user info - replace with your actual user management
        val currentUserId = getCurrentUserId()
        val currentUserName = getCurrentUserName()

        CustomZIMKitRouter.toMessageActivityWithCall(
            this,
            targetUserId,
            currentUserId,
            currentUserName,
            ZIMKitConversationType.ZIMKitConversationTypePeer
        )
    }

    private fun getCurrentUserId(): String {
        // Replace with your actual user ID retrieval
        return "current_user_id" // Example: from SharedPreferences or Auth system
    }

    private fun getCurrentUserName(): String {
        // Replace with your actual user name retrieval
        return "Current User" // Example: from SharedPreferences or Auth system
    }*/
}