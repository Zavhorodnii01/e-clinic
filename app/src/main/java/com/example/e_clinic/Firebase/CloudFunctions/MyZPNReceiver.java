package com.example.e_clinic.Firebase.CloudFunctions;

import static com.zegocloud.zimkit.components.message.utils.notification.NotificationsUtils.showNotification;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import im.zego.zpns.ZPNsMessageReceiver;
import im.zego.zpns.entity.ZPNsMessage;
import im.zego.zpns.entity.ZPNsRegisterMessage;

public class MyZPNReceiver extends ZPNsMessageReceiver {
    // Callback for passing the notifications directly.
    @Override
    protected void onThroughMessageReceived(Context context, ZPNsMessage message) {
        Log.e("MyZPNReceiver2", "onThroughMessageReceived message: " + message.toString());

        // Try to extract content from the message (payload, title, etc.)
        String title = "New Message";
        String content = "You received a message.";

        try {
            if (message != null && message.getPayload() != null) {
                JSONObject payload = new JSONObject(message.getPayload());
                title = payload.optString("title", "New Message");
                content = payload.optString("content", "You received a message.");
            }
        } catch (Exception e) {
            Log.e("MyZPNReceiver3", "Error parsing payload", e);
        }

        Log.e("MyZPNReceiver4", "title: " + title + ", content: " + content);

    }

    @Override
    protected void onNotificationClicked(Context context, ZPNsMessage message) {
        Log.e("MyZPNReceiver", "onNotificationClicked message: " + message.toString());
    }

    @Override
    protected void onNotificationArrived(Context context, ZPNsMessage message) {

        Log.e("MyZPNReceiver", "onNotificationArrived message: " + message.toString());

    }

    // Callback for the results of set up the offline push notifications, this can be used to get the Push ID.
    @Override
    protected void onRegistered(Context context, ZPNsRegisterMessage message) {
        Log.e("MyZPNReceiver", "onRegistered: message:" + message.getCommandResult());
    }




}
