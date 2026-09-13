package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FeederListeners(context: Context) {
    private val db: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    fun listen(feederId: String, onChange: (Feeder?) -> Unit): ListenerRegistration? {
        return db?.collection("feeders")?.document(feederId)?.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                onChange(null)
                return@addSnapshotListener
            }
            onChange(snapshot.toObject(Feeder::class.java)?.copy(id = snapshot.id))
        }
    }

    fun listenEvents(feederId: String, onChange: (List<PowerEvent>) -> Unit): ListenerRegistration? {
        return db?.collection("power_events")?.whereEqualTo("feederId", feederId)
            ?.orderBy("atMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            ?.limit(50)?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                onChange(snapshot.documents.mapNotNull { PowerEventMapper.fromMap(it.data ?: emptyMap()) })
            }
    }
}
