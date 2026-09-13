package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FeederRepository(context: Context) {
    private val db: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    suspend fun getAssignedFeeders(feederIds: List<String>): List<Feeder> {
        val firestore = db ?: return emptyList()
        if (feederIds.isEmpty()) return emptyList()
        return feederIds.mapNotNull { id ->
            firestore.collection("feeders").document(id).get().await().toObject(Feeder::class.java)?.copy(id = id)
        }
    }
}
