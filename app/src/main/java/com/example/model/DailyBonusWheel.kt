package com.example.model

import kotlin.random.Random

enum class WheelPrizeType {
    VIRTUAL_CHIPS,
    ROYALE_POINTS
}

data class WheelSlice(
    val id: String,
    val wheelLabel: String,
    val rewardTitle: String,
    val prizeType: WheelPrizeType,
    val amount: Long,
    val colorHex: Long,
    val accentTextColorHex: Long = 0xFFFFFFFF,
    val weight: Int
)

data class DailyWheelStatus(
    val canSpin: Boolean,
    val lastSpinTimestampMs: Long,
    val nextSpinAvailableAtMs: Long,
    val millisUntilNextSpin: Long
) {
    val formattedCountdown: String
        get() = DailyBonusWheelCatalog.formatCountdown(millisUntilNextSpin)

    val compactCountdown: String
        get() = DailyBonusWheelCatalog.formatCompactCountdown(millisUntilNextSpin)
}

data class DailyWheelSpinResult(
    val slice: WheelSlice,
    val chipsAwarded: Long,
    val royalePointsAwarded: Long,
    val newBankroll: Long,
    val newRoyalePoints: Long,
    val newLifetimeRoyalePoints: Long,
    val claimedAtMs: Long
)

object DailyBonusWheelCatalog {
    const val COOLDOWN_HOURS = 24L
    const val COOLDOWN_MS = COOLDOWN_HOURS * 60L * 60L * 1000L

    val slices: List<WheelSlice> = listOf(
        WheelSlice(
            id = "chips_1500",
            wheelLabel = "$1.5K",
            rewardTitle = "+$1,500 Virtual Chips",
            prizeType = WheelPrizeType.VIRTUAL_CHIPS,
            amount = 1_500L,
            colorHex = 0xFFB91C1C,
            weight = 20
        ),
        WheelSlice(
            id = "rp_75",
            wheelLabel = "75 RP",
            rewardTitle = "+75 Royale Points",
            prizeType = WheelPrizeType.ROYALE_POINTS,
            amount = 75L,
            colorHex = 0xFF1D4ED8,
            weight = 18
        ),
        WheelSlice(
            id = "chips_3500",
            wheelLabel = "$3.5K",
            rewardTitle = "+$3,500 Virtual Chips",
            prizeType = WheelPrizeType.VIRTUAL_CHIPS,
            amount = 3_500L,
            colorHex = 0xFF15803D,
            weight = 16
        ),
        WheelSlice(
            id = "rp_150",
            wheelLabel = "150 RP",
            rewardTitle = "+150 Royale Points",
            prizeType = WheelPrizeType.ROYALE_POINTS,
            amount = 150L,
            colorHex = 0xFF7E22CE,
            weight = 14
        ),
        WheelSlice(
            id = "chips_7500",
            wheelLabel = "$7.5K",
            rewardTitle = "+$7,500 Virtual Chips",
            prizeType = WheelPrizeType.VIRTUAL_CHIPS,
            amount = 7_500L,
            colorHex = 0xFFD97706,
            accentTextColorHex = 0xFF0A0606,
            weight = 12
        ),
        WheelSlice(
            id = "rp_300",
            wheelLabel = "300 RP",
            rewardTitle = "+300 Royale Points",
            prizeType = WheelPrizeType.ROYALE_POINTS,
            amount = 300L,
            colorHex = 0xFF0E7490,
            weight = 9
        ),
        WheelSlice(
            id = "chips_25000",
            wheelLabel = "$25K",
            rewardTitle = "+$25,000 Jackpot Chips",
            prizeType = WheelPrizeType.VIRTUAL_CHIPS,
            amount = 25_000L,
            colorHex = 0xFFEA580C,
            accentTextColorHex = 0xFF0A0606,
            weight = 7
        ),
        WheelSlice(
            id = "rp_500",
            wheelLabel = "500 RP",
            rewardTitle = "+500 Royale Points",
            prizeType = WheelPrizeType.ROYALE_POINTS,
            amount = 500L,
            colorHex = 0xFF14F195,
            accentTextColorHex = 0xFF0A0606,
            weight = 4
        )
    )

    fun evaluateStatus(
        lastSpinTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DailyWheelStatus {
        if (lastSpinTimestampMs <= 0L) {
            return DailyWheelStatus(
                canSpin = true,
                lastSpinTimestampMs = 0L,
                nextSpinAvailableAtMs = 0L,
                millisUntilNextSpin = 0L
            )
        }

        val elapsedMs = (currentTimeMs - lastSpinTimestampMs).coerceAtLeast(0L)
        val remainingMs = (COOLDOWN_MS - elapsedMs).coerceAtLeast(0L)
        return DailyWheelStatus(
            canSpin = remainingMs == 0L,
            lastSpinTimestampMs = lastSpinTimestampMs,
            nextSpinAvailableAtMs = lastSpinTimestampMs + COOLDOWN_MS,
            millisUntilNextSpin = remainingMs
        )
    }

    fun pickWeightedSliceIndex(random: Random = Random.Default): Int {
        val totalWeight = slices.sumOf { it.weight.coerceAtLeast(1) }
        var roll = random.nextInt(totalWeight)
        for (i in slices.indices) {
            roll -= slices[i].weight.coerceAtLeast(1)
            if (roll < 0) return i
        }
        return 0
    }

    /**
     * Computes target clockwise rotation in degrees so that [targetSliceIndex]
     * lands squarely under the top 12 o'clock pointer when slices are drawn
     * starting at -90 degrees (12 o'clock) clockwise.
     */
    fun computeTargetRotationDegrees(
        currentRotationDeg: Float,
        targetSliceIndex: Int,
        sliceCount: Int = slices.size,
        extraFullSpins: Int = 6
    ): Float {
        val safeCount = sliceCount.coerceAtLeast(1)
        val safeIndex = targetSliceIndex.coerceIn(0, safeCount - 1)
        val sweep = 360f / safeCount
        val centerOffsetFromTop = safeIndex * sweep + sweep / 2f
        val desiredMod = ((360f - (centerOffsetFromTop % 360f)) + 360f) % 360f
        val currentMod = ((currentRotationDeg % 360f) + 360f) % 360f
        val forwardDelta = (desiredMod - currentMod + 360f) % 360f
        return currentRotationDeg + (extraFullSpins.coerceAtLeast(1) * 360f) + forwardDelta
    }

    fun formatCountdown(millis: Long): String {
        if (millis <= 0L) return "Ready to Spin!"
        val totalSeconds = millis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    fun formatCompactCountdown(millis: Long): String {
        if (millis <= 0L) return "READY"
        val totalSeconds = millis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        return if (hours > 0L) {
            String.format("%02dh %02dm", hours, minutes)
        } else {
            val seconds = totalSeconds % 60L
            String.format("%02dm %02ds", minutes, seconds)
        }
    }
}
