package com.malaramofficial.bijliprahari.data

data class Subscription(
    val userId: String,
    val feederId: String,
    val villageId: String? = null,
    val powerOffAlerts: Boolean = true,
    val powerOnAlerts: Boolean = true
)
