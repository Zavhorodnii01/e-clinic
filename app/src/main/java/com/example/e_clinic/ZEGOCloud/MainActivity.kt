package com.example.e_clinic.ZEGOCloud

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.e_clinic.R
import java.util.Random

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appID = 2013180826L
        val appSign = "7f25f426b54a6fb21587414c4ac9174adb144d9d43353c41df876afcb43062df"

        val userID = generateUserID()
        val userName = userID + "_Name"
        val callID = "test_call_id"

        findViewById<View>(R.id.join_btn).setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                CallActivity::class.java
            )
            intent.putExtra("appID", appID)
            intent.putExtra("appSign", appSign)
            intent.putExtra("userID", userID)
            intent.putExtra("userName", userName)
            intent.putExtra("callID", callID)
            startActivity(intent)
        }
    }

    private fun generateUserID(): String {
        val builder = StringBuilder()
        val random = Random()
        while (builder.length < 5) {
            val nextInt = random.nextInt(10)
            if (builder.length == 0 && nextInt == 0) {
                continue
            }
            builder.append(nextInt)
        }
        return builder.toString()
    }
}