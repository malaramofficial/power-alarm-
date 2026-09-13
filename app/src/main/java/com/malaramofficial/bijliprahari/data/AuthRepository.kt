package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    fun isSignedIn(): Boolean = auth?.currentUser != null
    fun signOut() { auth?.signOut() }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = runCatching {
        val firebaseAuth = auth ?: error("Firebase Auth उपलब्ध नहीं है")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        firebaseAuth.currentUser?.uid ?: error("लॉगिन UID नहीं मिला")
    }

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

    suspend fun getAssignedFeederIds(): List<String> {
        val uid = currentUid() ?: return emptyList()
        val data = db?.collection("users")?.document(uid)?.get()?.await()?.data ?: return emptyList()
        return (data["feederIds"] as? List<*>)?.filterIsInstance<String>().orEmpty()
    }
}
