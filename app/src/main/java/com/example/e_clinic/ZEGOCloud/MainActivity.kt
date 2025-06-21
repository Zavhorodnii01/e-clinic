package com.example.e_clinic.ZEGOCloud

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_clinic.R

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appID = 2013180826L
        val appSign = "7f25f426b54a6fb21587414c4ac9174adb144d9d43353c41df876afcb43062df"

        val ownIdEditText = findViewById<EditText>(R.id.own_id)
        val targetIdEditText = findViewById<EditText>(R.id.target_id)
        val joinButton = findViewById<Button>(R.id.join_btn)

        joinButton.setOnClickListener {
            val ownID = ownIdEditText.text.toString().trim()
            val targetID = targetIdEditText.text.toString().trim()

            if (ownID.isEmpty() || targetID.isEmpty()) {
                Toast.makeText(this, "Please enter both IDs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("appID", appID)
            intent.putExtra("appSign", appSign)
            intent.putExtra("userID", ownID)
            intent.putExtra("userName", "${ownID}_Name")
            intent.putExtra("callID", targetID) // the person you're calling
            startActivity(intent)
        }
    }
}
