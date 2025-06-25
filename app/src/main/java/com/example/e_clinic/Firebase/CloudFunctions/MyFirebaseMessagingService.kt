package com.example.e_clinic.Firebase.CloudFunctions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.e_clinic.R
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.DoctorActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.e_clinic.UI.activities.user_screens.user_activity.UserActivity // Assuming this is where the chat is
import com.example.e_clinic.ZEGOCloud.ConversationActivity // Or the correct activity for Zego chat UI

// Assume constants for the data payload keys you will send from your backend
const val NOTIFICATION_TYPE = "type"
const val TYPE_CHAT_MESSAGE = "chat_message" // New type for chat
const val TYPE_FINISH_REMINDER = "finish_reminder"
const val TYPE_CANCEL_NOTIFICATION = "cancel_notification"
const val TYPE_USER_APPOINTMENT_REMINDER = "user_appointment_reminder"

// Keys for chat message data (decide on these with your backend)
const val DATA_KEY_SENDER_ID = "sender_user_id"
const val DATA_KEY_SENDER_NAME = "sender_user_name" // Or just "sender_name"
const val DATA_KEY_CONVERSATION_ID = "conversation_id"
const val DATA_KEY_MESSAGE_CONTENT = "message_content" // Snippet of the message

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Notification Channel ID for chat messages
    private val CHAT_NOTIFICATION_CHANNEL_ID = "chat_notifications"
    private val APPOINTMENT_NOTIFICATION_CHANNEL_ID = "appointment_reminders"


    override fun onMessageReceived(remoteMessage: RemoteMessage) {


        Log.d("FCM_RECEIVE", "onMessageReceived called") // Add this line
        Log.d("FCM_RECEIVE", "From: ${remoteMessage.from}")
        Log.d("FCM_RECEIVE", "Data payload: ${remoteMessage.data}")
        Log.d("FCM_RECEIVE", "Notification payload: ${remoteMessage.notification}")
        // Handle data payload messages first
        remoteMessage.data.let { data ->
            when (data[NOTIFICATION_TYPE]) {
                TYPE_CHAT_MESSAGE -> handleChatMessageNotification(data)
                TYPE_FINISH_REMINDER -> handleFinishReminder(data)
                TYPE_CANCEL_NOTIFICATION -> handleCancelNotification(data)
                TYPE_USER_APPOINTMENT_REMINDER -> handleUserAppointmentReminder(data)
                else -> {
                    // If data type is not recognized, maybe handle notification payload if present
                    remoteMessage.notification?.let {
                        createGenericNotification(it)
                    }
                }
            }
        }

        // Note: If the FCM message contains *both* notification and data payload,
        // onMessageReceived is called *only* when the app is in the foreground.
        // When the app is in the background/killed, the system handles the notification payload.
        // To ensure handling in all states, send *only* data payload from the server
        // and build the notification yourself in onMessageReceived.

        // ZegoCloud's onReceivePeerMessage callback will still fire when the app is running.
        // This FCM notification is for when the app is *not* running or not in the relevant chat screen.
    }

    // --- New handler for chat messages ---
    private fun handleChatMessageNotification(data: Map<String, String>) {
        val senderName = data[DATA_KEY_SENDER_NAME] ?: "New Message"
        val messageContent = data[DATA_KEY_MESSAGE_CONTENT] ?: "..."
        val conversationId = data[DATA_KEY_CONVERSATION_ID] ?: return // Conversation ID is crucial

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the chat notification channel
        createNotificationChannel(
            notificationManager,
            CHAT_NOTIFICATION_CHANNEL_ID,
            "Chat Messages", // User visible name for the channel
            "Notifications for new chat messages." // User visible description
        )

        // Intent to open the relevant chat screen when notification is tapped
        // You need to replace ConversationActivity::class.java with your actual chat activity
        // And pass the conversationId so the activity knows which chat to open.
        val intent = Intent(this, ConversationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear stack
            putExtra("conversationID", conversationId) // Pass the conversation ID
            // You might also need conversation type (PEER, GROUP) if your chat activity needs it
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(), // Use conversation ID hash for unique pending intent per conversation
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHAT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Use an appropriate icon
            .setContentTitle(senderName) // Sender's name as title
            .setContentText(messageContent) // Message snippet as text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismisses notification when tapped
            .setContentIntent(pendingIntent) // What happens when tapped
            // Optional: Add summary for multiple messages from the same conversation
            // .setGroup(conversationId) // Group notifications by conversation
            // .setGroupSummary(false) // This notification is not the summary
            .build()

        // Use conversationId.hashCode() as the notification ID
        // This way, new messages in the same conversation update the existing notification.
        notificationManager.notify(conversationId.hashCode(), notification)
    }
    // --- End of new handler ---


    // Your existing handlers for appointment reminders
    private fun handleUserAppointmentReminder(data: Map<String, String>) {
        val appointmentDate = data["appointmentDate"] ?: "Unknown time"
        val appointmentId = data["appointmentId"] ?: return
        val dayType = data["dayType"] ?: "Today"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(
            notificationManager,
            APPOINTMENT_NOTIFICATION_CHANNEL_ID,
            "Appointment Reminders",
            "Notifications for upcoming and past appointments."
        )

        val notification = NotificationCompat.Builder(this, APPOINTMENT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🩺 Reminder: $dayType Appointment")
            .setContentText("Your appointment is scheduled on $appointmentDate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }

    private fun handleFinishReminder(data: Map<String, String>) {
        val appointmentId = data["appointmentId"] ?: return

        val intent = Intent(this, DoctorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showFinishDialog", true)
            putExtra("appointmentId", appointmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(
            notificationManager,
            APPOINTMENT_NOTIFICATION_CHANNEL_ID,
            "Appointment Reminders",
            "Notifications for upcoming and past appointments."
        )

        val notification = NotificationCompat.Builder(this, APPOINTMENT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDCCB✍\uFE0F✅\uD83D\uDE0E\uD83D\uDCCB\uD83D\uDD25 Appointment Not Finished")
            .setContentText("Tap to finish the appointment.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }

    private fun handleCancelNotification(data: Map<String, String>) {
        val appointmentId = data["appointmentId"] ?: return
        val patientName = data["patientName"] ?: "Unknown"
        val appointmentDate = data["appointmentDate"] ?: "Unknown time"

        val intent = Intent(this, DoctorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("appointmentId", appointmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(
            notificationManager,
            APPOINTMENT_NOTIFICATION_CHANNEL_ID,
            "Appointment Reminders",
            "Notifications for upcoming and past appointments."
        )

        val notification = NotificationCompat.Builder(this, APPOINTMENT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83C\uDF7E \uD83D\uDED1\uD83E\uDE7A\uD83D\uDE0CAppointment Canceled")
            .setContentText("Patient: $patientName\nDate: $appointmentDate")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Patient: $patientName\nDate: $appointmentDate"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }

    // Generic notification handler for messages without a specific type
    private fun createGenericNotification(notification: RemoteMessage.Notification) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(
            notificationManager,
            "general_notifications", // Use a different channel for generic notifications
            "General Notifications",
            "General application notifications."
        )

        val notificationBuilder = NotificationCompat.Builder(this, "general_notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title ?: "Notification")
            .setContentText(notification.body ?: "You have a new notification.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Default priority for generic
            .setAutoCancel(true)

        // You might want to add a default intent here too, e.g., opening the main activity
        val defaultIntent = Intent(this, UserActivity::class.java).apply { // Or your main activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val defaultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            defaultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(defaultPendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }


    // Helper function to create notification channels (to avoid repetition)
    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // Use HIGH for chat/urgent notifications
            ).apply {
                description = channelDescription
            }
            // Check if channel already exists before creating
            if (notificationManager.getNotificationChannel(channelId) == null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }


    // Important: This is called when the FCM token is generated or updated.
    // You MUST send this token to your backend and associate it with the logged-in user ID.
    override fun onNewToken(token: String) {
        // Log the new token
        println("FCM Token: $token")

        // Send the token to your backend service
        // Your backend needs to know which user ID this token belongs to
        // and store it in your database for sending push notifications.
        // Example: sendRegistrationToServer(token)
        // This usually involves an API call to your backend.
        // You'll also need to handle the case where a user logs out
        // and remove the token association on the backend.
    }


}