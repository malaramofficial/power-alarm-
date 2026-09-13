package com.malaramofficial.bijliprahari.data

data class PowerEvent(
    val state: String,
    val timestamp: Long,
    val durationMinutes: Int? = null,
    val feeder: String = "मीठी बेरी फीडर",
    val feederId: String = "meethi-beri",
    val source: String = "device",
    val verified: Boolean = false,
    val verifiedBy: String? = null
)
