package com.malaramofficial.bijliprahari.data

import kotlin.math.max

/** Derived device health. No secrets or network calls belong here. */
data class DeviceHealth(
    val online: Boolean,
    val batteryPercent: Int,
    val signalPercent: Int,
    val lastHeartbeatAt: Long?,
    val staleAfterMillis: Long = 5 * 60 * 1000L
) {
    val isStale: Boolean
        get() = lastHeartbeatAt == null || System.currentTimeMillis() - lastHeartbeatAt > staleAfterMillis

    val normalizedBattery: Int get() = batteryPercent.coerceIn(0, 100)
    val normalizedSignal: Int get() = max(0, signalPercent.coerceIn(0, 100))
}
