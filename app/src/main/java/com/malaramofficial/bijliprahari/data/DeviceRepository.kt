package com.malaramofficial.bijliprahari.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DeviceRepository(context: Context) {
    private val db: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    suspend fun getDevice(deviceId: String): Device? {
        val firestore = db ?: return null
        return firestore.collection("devices").document(deviceId).get().await().toObject(Device::class.java)?.copy(id = deviceId)
    }
}
