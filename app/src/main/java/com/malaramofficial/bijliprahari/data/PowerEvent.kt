package com.malaramofficial.bijliprahari.data

data class PowerEvent(
    val state: String,
    val timestamp: Long,
    val durationMinutes: Int? = null,
    val feeder: String = "मीठी बेरी फीडर"
)
