package com.example.e_clinic.ZEGOCloud

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_clinic.BuildConfig
import com.example.e_clinic.R
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class VideoCallActivity : AppCompatActivity() {

    // You can set these dynamically or pass via Intent extras
    private val userID = "user123"     // Replace with actual userID
    private val userName = "User Name" // Replace with actual userName
    private val callID = "call123"     // Replace with actual call ID (random UUID etc)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        addCallFragment()
    }

    private fun addCallFragment() {
        // Load appID and appSign from BuildConfig (which you set from local.properties)
        val appID = BuildConfig.APP_ID.toLong()  // Make sure APP_ID in local.properties is numeric string
        val appSign = BuildConfig.APP_SIGN

        // Use Zego's prebuilt one-on-one video call config
        val config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()

        val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
            appID,
            appSign,
            userID,
            userName,
            callID,
            config
        )

        /*supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()*/
    }
}
