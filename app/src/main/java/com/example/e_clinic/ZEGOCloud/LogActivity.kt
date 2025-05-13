package com.example.e_clinic.ZEGOCloud

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.e_clinic.R
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zim.entity.ZIMError
import im.zego.zim.enums.ZIMErrorCode

class LogActivity : AppCompatActivity() {

    private lateinit var userIdInput: EditText
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log2)



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userIdInput = findViewById(R.id.userid_input)
        loginBtn = findViewById(R.id.login_btn)

        loginBtn.setOnClickListener {
            val userId = userIdInput.text.toString()
            connectUser(userId, userId, "")
        }
    }

    private fun connectUser(userId: String, userName: String, userAvatar: String) {
        ZIMKit.connectUser(userId, userName, userAvatar) { errorInfo: ZIMError? ->
            if (errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS) {
                toConversationActivity()
            } else {
                Toast.makeText(this, "Login failed: ${errorInfo?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toConversationActivity() {
        val intent = Intent(this, ConversationActivity::class.java)
        startActivity(intent)
        finish() // Optional: closes the login screen
    }
}
