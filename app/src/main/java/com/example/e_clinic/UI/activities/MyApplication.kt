package com.example.e_clinic.UI.activities // Your package name

import android.app.Application
import com.google.firebase.FirebaseApp

import com.example.e_clinic.BuildConfig
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
