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
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType


class ConversationActivity : AppCompatActivity() {

    private lateinit var actionButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conversation)

        actionButton = findViewById(R.id.floating_btn)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        actionButton.setOnClickListener {
            showPopupMenu()
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, actionButton)
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.new_chat -> {
                    showNewChatDialog()
                    true
                }
                R.id.logout -> {
                    // TODO: Handle logout logic here
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showNewChatDialog()
    {
        val editText = EditText(this).apply {
            hint = "UserID"
        }

        AlertDialog.Builder(this)
            .setTitle("New chat")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val userId = editText.text.toString()
                ZIMKitRouter.toMessageActivity(
                    this,
                    userId,
                    ZIMKitConversationType.ZIMKitConversationTypePeer
                )
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()


    }
}
