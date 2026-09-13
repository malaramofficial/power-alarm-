package com.malaramofficial.bijliprahari.data

data class Village(
    val id: String,
    val name: String,
    val feederId: String,
    val farmerCount: Int = 0,
    val active: Boolean = true
)
