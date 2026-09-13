package com.malaramofficial.bijliprahari.data

data class DeviceHeartbeat(
    val deviceId: String,
    val feederId: String,
    val online: Boolean = true,
    val batteryPercent: Int = 0,
    val signalDbm: Int? = null,
    val receivedAt: Long
)
