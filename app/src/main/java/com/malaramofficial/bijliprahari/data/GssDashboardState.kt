package com.malaramofficial.bijliprahari.data

data class GssDashboardState(
    val feeder: Feeder,
    val powerOn: Boolean,
    val outageStartedAt: Long? = null,
    val todayOnMinutes: Int = 0,
    val todayOffMinutes: Int = 0,
    val deviceOnline: Boolean = false,
    val batteryPercent: Int = 0,
    val signalLabel: String = "अज्ञात"
)
