package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val auth: FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }.getOrNull()
    private val db: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    fun currentUid(): String? = auth?.currentUser?.uid

    suspend fun getCurrentRole(): UserRole {
        val uid = currentUid() ?: return UserRole.UNKNOWN
        val data = db?.collection("users")?.document(uid)?.get()?.await()?.data ?: return UserRole.UNKNOWN
        return when ((data["role"] as? String)?.uppercase()) {
            "ADMIN" -> UserRole.ADMIN
            "GSS" -> UserRole.GSS
            "FARMER" -> UserRole.FARMER
            else -> UserRole.UNKNOWN
        }
    }
}
