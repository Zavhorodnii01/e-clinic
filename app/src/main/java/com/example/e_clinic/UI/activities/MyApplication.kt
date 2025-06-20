package com.example.e_clinic.UI.activities // Your package name

import android.app.Application
import com.google.firebase.FirebaseApp

import com.example.e_clinic.BuildConfig
import com.zegocloud.zimkit.services.ZIMKit

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initZegocloud()  // Initialize ZEGOCLOUD
    }

    private fun initZegocloud() {
        val appId = BuildConfig.APP_ID  // Make sure APP_ID is long
        val appSign = BuildConfig.APP_SIGN  // Ensure appSign is a String

        // Initialize ZEGOCLOUD using appId and appSign
        ZIMKit.initWith(this, appId.toLong(), appSign)
        ZIMKit.initNotifications()

    }

}
