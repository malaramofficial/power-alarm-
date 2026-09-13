package com.malaramofficial.bijliprahari.data

object NotificationTopics {
    fun feeder(feederId: String): String = "feeder_${feederId.replace(Regex("[^A-Za-z0-9_-]"), "_")}"
    fun village(villageId: String): String = "village_${villageId.replace(Regex("[^A-Za-z0-9_-]"), "_")}"
}
