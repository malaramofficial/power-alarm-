package com.malaramofficial.bijliprahari.data

data class DeviceStatus(
    val deviceId: String,
    val feederId: String,
    val online: Boolean,
    val batteryPercent: Int,
    val signalLabel: String,
    val lastSeenAt: Long
)
