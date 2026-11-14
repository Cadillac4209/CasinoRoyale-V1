package com.example.model

enum class GameSpeed(
    val id: String,
    val label: String,
    val badge: String,
    val description: String,
    val delayMultiplier: Float
) {
    RELAXED(
        id = "relaxed",
        label = "Relaxed",
        badge = "1.3x",
        description = "Unhurried classic table & reel pacing",
        delayMultiplier = 1.3f
    ),
    NORMAL(
        id = "normal",
        label = "Normal",
        badge = "1.0x",
        description = "Standard Fire Kern casino timing",
        delayMultiplier = 1.0f
    ),
    FAST(
        id = "fast",
        label = "Fast",
        badge = "0.55x",
        description = "Snappy card deals & brisk reel stops",
        delayMultiplier = 0.55f
    ),
    TURBO(
        id = "turbo",
        label = "Turbo",
        badge = "0.25x",
        description = "Instant high-roller action velocity",
        delayMultiplier = 0.25f
    );

    fun scaleDelay(baseMs: Long): Long {
        if (baseMs <= 0L) return 0L
        return (baseMs * delayMultiplier).toLong().coerceAtLeast(15L)
    }

    companion object {
        fun fromId(id: String?): GameSpeed =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
