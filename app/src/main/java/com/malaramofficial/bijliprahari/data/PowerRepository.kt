package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Firebase is optional until google-services.json is added. */
class PowerRepository(context: Context) {
    private val firestore: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    suspend fun savePowerEvent(feederId: String, state: String, atMillis: Long, source: String = "device") {
        firestore?.collection("power_events")?.add(
            mapOf("feederId" to feederId, "state" to state, "atMillis" to atMillis, "source" to source)
        )?.await()
    }

    suspend fun getRecentEvents(feederId: String, limit: Long = 50): List<Map<String, Any?>> {
        val db = firestore ?: return emptyList()
        return db.collection("power_events").whereEqualTo("feederId", feederId)
            .orderBy("atMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit).get().await().documents.map { it.data ?: emptyMap() }
    }
}
