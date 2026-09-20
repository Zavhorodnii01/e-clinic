package com.example.e_clinic.ZEGOCloud

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.e_clinic.BuildConfig

import com.example.e_clinic.R
import com.zegocloud.zimkit.services.ZIMKit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        initZegocloud() // Initialize ZEGOCLOUD

        setContentView(R.layout.activity_main2)
        startActivity(Intent(this, LogActivity::class.java))
        finish()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initZegocloud() {
        val appId = BuildConfig.APP_ID
        val appSign = BuildConfig.APP_SIGN

        ZIMKit.initWith(application, appId.toLong(), appSign)
        ZIMKit.initNotifications()
    }
}
