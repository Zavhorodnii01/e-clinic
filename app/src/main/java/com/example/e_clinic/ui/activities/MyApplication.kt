package com.example.e_clinic.ui.activities // Your package name

import android.app.Application  // Correct import
import com.google.firebase.FirebaseApp

// TODO don't delete it
class MyApplication : Application() { // Extends Application
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}