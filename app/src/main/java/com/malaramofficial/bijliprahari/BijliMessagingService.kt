package com.malaramofficial.bijliprahari

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BijliMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .addOnFailureListener { /* User may be signed out/offline; next token refresh can retry. */ }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val state = message.data["state"]?.uppercase()
        val feederId = message.data["feederId"].orEmpty()
        val title = message.notification?.title ?: when (state) {
            "ON" -> "🟢 बिजली आ गई"
            "OFF" -> "🔴 बिजली चली गई"
            else -> "बिजली प्रहरी"
        }
        val body = message.notification?.body ?: when (state) {
            "ON" -> "फीडर की लाइन फिर से उपलब्ध है।"
            "OFF" -> "फीडर की लाइन बंद हो गई है।"
            else -> "नया power event प्राप्त हुआ है।"
        }

        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (feederId.isNotBlank()) putExtra("feederId", feederId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, feederId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        if (Build.VERSION.SDK_INT < 33 || NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "बिजली अलर्ट", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "फीडर बिजली ON/OFF notifications" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "power_alerts"
    }
}
