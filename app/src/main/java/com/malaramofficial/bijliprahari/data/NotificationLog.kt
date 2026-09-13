package com.malaramofficial.bijliprahari.data

data class NotificationLog(
    val eventId: String,
    val feederId: String,
    val state: String,
    val recipientCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val createdAt: Long = 0L
)
