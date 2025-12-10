package com.devstormtech.toe3skins

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "New Skin Alert!"
        val body = remoteMessage.notification?.body ?: "Check out the latest skins."
        val truckFilter = remoteMessage.data["truck_filter"]
        showNotification(title, body, truckFilter)
    }

    private fun showNotification(title: String, body: String, truckFilter: String?) {
        val channelId = "toe3_skins_channel"

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (truckFilter != null) {
            intent.putExtra("TARGET_TAB", truckFilter)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- FIX: Looking in the correct 'drawable' folder now ---
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_large_notification)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // White silhouette for status bar
            .setLargeIcon(largeIcon)                  // Round logo for notification shade
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Skin Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(0, builder.build())
    }
}