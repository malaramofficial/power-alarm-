package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val app: FirebaseApp? = runCatching {
        FirebaseApp.getApps(context).firstOrNull()
    }.getOrNull()
    private val auth: FirebaseAuth? = runCatching {
        app?.let { FirebaseAuth.getInstance(it) }
    }.getOrNull()
    private val db: FirebaseFirestore? = runCatching {
        app?.let { FirebaseFirestore.getInstance(it) }
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

    private suspend fun firestoreForServerRead(): FirebaseFirestore {
        val firestore = db ?: error(firebaseDiagnostic("Firebase Firestore instance नहीं बना"))
        try {
            firestore.enableNetwork().await()
            return firestore
        } catch (e: Exception) {
            throw IllegalStateException(firebaseDiagnostic("Firestore network enable विफल"), e)
        }
    }

    private fun firebaseDiagnostic(prefix: String): String {
        val appInfo = app?.options?.let {
            "project=${it.projectId ?: "null"}, appId=${it.applicationId}"
        } ?: "FirebaseApp=null"
        return "$prefix | $appInfo"
    }

    private fun describe(error: Throwable): String {
        val parts = mutableListOf<String>()
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 4) {
            parts += "${current::class.java.simpleName}: ${current.message ?: "no-message"}"
            current = current.cause
            depth++
        }
        return parts.joinToString(" <- ")
    }

    suspend fun getCurrentRole(): UserRole {
        val uid = currentUid() ?: return UserRole.UNKNOWN
        val firestore = firestoreForServerRead()
        return try {
            val data = firestore.collection("users").document(uid).get(Source.SERVER).await().data ?: return UserRole.UNKNOWN
            when ((data["role"] as? String)?.uppercase()) {
                "ADMIN" -> UserRole.ADMIN
                "GSS" -> UserRole.GSS
                "FARMER" -> UserRole.FARMER
                else -> UserRole.UNKNOWN
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                firebaseDiagnostic("users/$uid read विफल | ${describe(e)}"),
                e
            )
        }
    }

    suspend fun getAssignedFeederIds(): List<String> {
        val uid = currentUid() ?: return emptyList()
        val firestore = firestoreForServerRead()
        return try {
            val data = firestore.collection("users").document(uid).get(Source.SERVER).await().data ?: return emptyList()
            (data["feederIds"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        } catch (e: Exception) {
            throw IllegalStateException(
                firebaseDiagnostic("users/$uid feederIds read विफल | ${describe(e)}"),
                e
            )
        }
    }
}
