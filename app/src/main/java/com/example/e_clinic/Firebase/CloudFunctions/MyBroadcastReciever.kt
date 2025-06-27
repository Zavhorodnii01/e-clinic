package com.example.e_clinic.Firebase.CloudFunctions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MyBroadcastReceiver", "onReceive called")

        val message = intent?.getStringExtra("message")
        Log.d("MyBroadcastReceiver", "Received message: $message")
    }
}
