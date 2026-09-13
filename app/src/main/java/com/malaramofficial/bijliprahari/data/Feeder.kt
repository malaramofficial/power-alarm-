package com.malaramofficial.bijliprahari.data

data class Feeder(
    val id: String,
    val name: String,
    val gssName: String,
    val villageCount: Int = 0,
    val farmerCount: Int = 0,
    val active: Boolean = true,
    val currentState: String = "UNKNOWN",
    val lastEventAt: Long? = null
)
