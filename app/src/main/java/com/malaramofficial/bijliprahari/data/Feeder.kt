package com.malaramofficial.bijliprahari.data

data class Feeder(
    val id: String,
    val name: String,
    val gssName: String,
    val villageCount: Int = 0,
    val active: Boolean = true
)
