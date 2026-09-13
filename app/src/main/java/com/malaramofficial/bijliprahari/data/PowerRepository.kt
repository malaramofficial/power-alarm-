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

    suspend fun savePowerEvent(
        feederId: String,
        state: String,
        atMillis: Long,
        source: String = "device"
    ) {
        firestore?.collection("power_events")?.add(
            mapOf(
                "feederId" to feederId,
                "state" to state,
                "atMillis" to atMillis,
                "source" to source
            )
        )?.await()
    }

    suspend fun getRecentEvents(feederId: String, limit: Long = 50): List<Map<String, Any?>> {
        val db = firestore ?: return emptyList()
        return db.collection("power_events")
            .whereEqualTo("feederId", feederId)
            .orderBy("atMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .map { it.data ?: emptyMap() }
    }

    suspend fun getAssignedFeeders(userId: String): List<Feeder> {
        val db = firestore ?: return emptyList()
        val user = db.collection("users").document(userId).get().await()
        val ids = (user.get("feederIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        if (ids.isEmpty()) return emptyList()

        return ids.mapNotNull { id ->
            db.collection("feeders").document(id).get().await().data?.let { data ->
                Feeder(
                    id = id,
                    name = data["name"] as? String ?: id,
                    gssName = data["gssName"] as? String ?: "GSS",
                    villageCount = (data["villageCount"] as? Long)?.toInt() ?: 0,
                    active = data["active"] as? Boolean ?: true
                )
            }
        }
    }

    suspend fun getFeeder(feederId: String): Feeder? {
        val db = firestore ?: return null
        val data = db.collection("feeders").document(feederId).get().await().data ?: return null
        return Feeder(
            id = feederId,
            name = data["name"] as? String ?: feederId,
            gssName = data["gssName"] as? String ?: "GSS",
            villageCount = (data["villageCount"] as? Long)?.toInt() ?: 0,
            active = data["active"] as? Boolean ?: true
        )
    }

    suspend fun updateFeederDeviceStatus(
        feederId: String,
        online: Boolean,
        batteryPercent: Int,
        signalLabel: String,
        atMillis: Long
    ) {
        firestore?.collection("devices")?.document(feederId)?.set(
            mapOf(
                "feederId" to feederId,
                "online" to online,
                "batteryPercent" to batteryPercent,
                "signalLabel" to signalLabel,
                "lastSeenAt" to atMillis
            )
        )?.await()
    }
}
