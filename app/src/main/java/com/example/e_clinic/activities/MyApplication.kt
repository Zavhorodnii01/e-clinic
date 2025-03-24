package com.example.e_clinic.activities // Your package name

import android.app.Application  // Correct import
import com.google.firebase.FirebaseApp

class MyApplication : Application() { // Extends Application
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}