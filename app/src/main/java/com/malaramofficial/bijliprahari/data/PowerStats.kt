package com.malaramofficial.bijliprahari.data

object PowerStats {
    fun totalDuration(events: List<PowerEvent>, state: String): Long {
        return events.filter { it.state == state }.sumOf { (it.durationMinutes ?: 0).toLong() }
    }

    fun outageCount(events: List<PowerEvent>): Int = events.count { it.state == "OFF" }
}
