package com.malaramofficial.bijliprahari.data

object PowerEventValidator {
    private const val MIN_STATE_LENGTH = 2

    fun normalizeState(state: String): String? {
        val normalized = state.trim().uppercase()
        return if (normalized.length >= MIN_STATE_LENGTH && normalized in setOf("ON", "OFF")) normalized else null
    }

    fun isMeaningfulTransition(previous: String?, current: String): Boolean =
        previous == null || previous != current
}
