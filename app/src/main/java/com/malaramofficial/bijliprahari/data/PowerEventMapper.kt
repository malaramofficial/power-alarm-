package com.malaramofficial.bijliprahari.data

/** Converts Firestore maps into a stable UI model without crashing on missing fields. */
object PowerEventMapper {
    fun fromMap(data: Map<String, Any?>): PowerEvent? {
        val state = data["state"] as? String ?: return null
        val atMillis = (data["atMillis"] as? Number)?.toLong() ?: return null
        return PowerEvent(
            state = state,
            timestamp = atMillis,
            durationMinutes = (data["durationMinutes"] as? Number)?.toInt(),
            feeder = data["feeder"] as? String ?: "मीठी बेरी फीडर",
            feederId = data["feederId"] as? String ?: "",
            source = data["source"] as? String ?: "device",
            verified = data["verified"] as? Boolean ?: false,
            verifiedBy = data["verifiedBy"] as? String
        )
    }
}
