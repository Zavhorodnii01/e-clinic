package com.example.e_clinic.UI.activities // Your package name

import android.app.Application
import com.example.e_clinic.BuildConfig
import com.google.firebase.FirebaseApp
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zpns.ZPNsManager
import im.zego.zpns.util.ZPNsConfig


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

        ZIMKit.enableFCMPush()
        val zpnsConfig = ZPNsConfig().apply {
            enableFCMPush() // Enable FCM push notifications
            //setFCMNotificationIcon(R.drawable.ic_notification) // Set your notification icon
        }
        ZPNsManager.setPushConfig(zpnsConfig)


        ZPNsManager.getInstance().registerPush(this);


        //ZPNsManager.getInstance().unregisterPush();


        /*




        val zpnsConfig = ZPNsConfig()
        zpnsConfig.enableFCMPush()

        ZPNsManager.setPushConfig(zpnsConfig)// Enable the Google push notification channel. After it is enabled, the notification channels of other vendors won't be available.

*/


    }

}
