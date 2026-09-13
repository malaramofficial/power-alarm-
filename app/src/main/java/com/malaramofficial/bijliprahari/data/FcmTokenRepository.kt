package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmTokenRepository(context: Context) {
    private val db: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()
    private val messaging: FirebaseMessaging? = runCatching { FirebaseMessaging.getInstance() }.getOrNull()

    suspend fun registerCurrentToken(userId: String): Boolean {
        val firestore = db ?: return false
        val token = messaging?.token?.await() ?: return false
        firestore.collection("users").document(userId).update(
            "fcmTokens", FieldValue.arrayUnion(token)
        ).await()
        return true
    }
}
