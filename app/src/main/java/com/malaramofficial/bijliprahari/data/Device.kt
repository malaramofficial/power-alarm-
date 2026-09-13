package com.malaramofficial.bijliprahari.data

data class Device(
    val id: String = "",
    val feederId: String = "",
    val online: Boolean = false,
    val batteryPercent: Int = 0,
    val signalPercent: Int = 0,
    val lastHeartbeatAt: Long? = null,
    val firmwareVersion: String = ""
)
