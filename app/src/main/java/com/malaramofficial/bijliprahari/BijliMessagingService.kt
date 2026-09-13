package com.malaramofficial.bijliprahari

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BijliMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Notification payloads are displayed by FCM when the app is backgrounded.
        // Data payloads are intentionally kept small for future in-app event handling.
    }
}
