package com.example.model

import java.util.Calendar

data class CheckInDayReward(
    val dayNumber: Int,
    val chipsBonus: Long,
    val xpBonus: Long,
    val voidTokens: Long = 0L,
    val title: String,
    val isJackpotDay: Boolean = false
)

data class CheckInStreakStatus(
    val currentStreak: Int,
    val bestStreak: Int,
    val isClaimAvailableToday: Boolean,
    val targetDayInCycle: Int, // 1 to 7
    val millisUntilNextClaim: Long,
    val isStreakBroken: Boolean,
    val totalCheckIns: Int
) {
    val formattedCountdown: String
        get() {
            if (isClaimAvailableToday) return "Ready to Claim!"
            if (millisUntilNextClaim <= 0) return "Ready soon"
            val totalSeconds = millisUntilNextClaim / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        }
}

data class DailyCheckInClaimResult(
    val dayNumber: Int,
    val streak: Int,
    val chipsAwarded: Long,
    val xpAwarded: Long,
    val voidTokensAwarded: Long,
    val isNewBestStreak: Boolean
)

object DailyCheckInSchedule {
    val days: List<CheckInDayReward> = listOf(
        CheckInDayReward(
            dayNumber = 1,
            chipsBonus = 1500L,
            xpBonus = 200L,
            voidTokens = 0L,
            title = "Day 1 Welcome"
        ),
        CheckInDayReward(
            dayNumber = 2,
            chipsBonus = 3000L,
            xpBonus = 400L,
            voidTokens = 0L,
            title = "Day 2 Double Roll"
        ),
        CheckInDayReward(
            dayNumber = 3,
            chipsBonus = 5000L,
            xpBonus = 650L,
            voidTokens = 1L,
            title = "Day 3 Void Inflow"
        ),
        CheckInDayReward(
            dayNumber = 4,
            chipsBonus = 8000L,
            xpBonus = 1000L,
            voidTokens = 0L,
            title = "Day 4 High Roller"
        ),
        CheckInDayReward(
            dayNumber = 5,
            chipsBonus = 12000L,
            xpBonus = 1500L,
            voidTokens = 2L,
            title = "Day 5 Quantum Boost"
        ),
        CheckInDayReward(
            dayNumber = 6,
            chipsBonus = 20000L,
            xpBonus = 2500L,
            voidTokens = 3L,
            title = "Day 6 High Stakes"
        ),
        CheckInDayReward(
            dayNumber = 7,
            chipsBonus = 50000L,
            xpBonus = 6000L,
            voidTokens = 5L,
            title = "Day 7 Grand Jackpot",
            isJackpotDay = true
        )
    )

    fun getRewardForDay(dayNumber: Int): CheckInDayReward {
        val normalized = ((dayNumber - 1) % 7) + 1
        return days.firstOrNull { it.dayNumber == normalized } ?: days.first()
    }

    /**
     * Calculates consecutive day difference based on calendar days or 20-hour window.
     */
    fun evaluateStreak(
        lastCheckInTimestamp: Long,
        savedStreak: Int,
        bestStreak: Int,
        totalCheckIns: Int,
        currentTime: Long = System.currentTimeMillis()
    ): CheckInStreakStatus {
        if (lastCheckInTimestamp == 0L) {
            return CheckInStreakStatus(
                currentStreak = 0,
                bestStreak = bestStreak,
                isClaimAvailableToday = true,
                targetDayInCycle = 1,
                millisUntilNextClaim = 0L,
                isStreakBroken = false,
                totalCheckIns = totalCheckIns
            )
        }

        val lastCal = Calendar.getInstance().apply { timeInMillis = lastCheckInTimestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTime }

        val lastYear = lastCal.get(Calendar.YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val lastDayOfYear = lastCal.get(Calendar.DAY_OF_YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val isSameDay = (lastYear == nowYear && lastDayOfYear == nowDayOfYear)

        // Calculate calendar days passed
        val daysPassed: Int = if (nowYear == lastYear) {
            nowDayOfYear - lastDayOfYear
        } else if (nowYear == lastYear + 1) {
            val daysInPrevYear = lastCal.getActualMaximum(Calendar.DAY_OF_YEAR)
            (daysInPrevYear - lastDayOfYear) + nowDayOfYear
        } else {
            // Multiple years passed
            365
        }

        return when {
            isSameDay -> {
                // Already claimed today. Calculate time until midnight tonight.
                val midnightCal = Calendar.getInstance().apply {
                    timeInMillis = currentTime
                    set(Calendar.HOUR_OF_DAY, 24)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val millisUntilMidnight = maxOf(0L, midnightCal.timeInMillis - currentTime)
                val targetDay = if (savedStreak <= 0) 1 else (((savedStreak - 1) % 7) + 1)
                CheckInStreakStatus(
                    currentStreak = savedStreak,
                    bestStreak = maxOf(bestStreak, savedStreak),
                    isClaimAvailableToday = false,
                    targetDayInCycle = targetDay,
                    millisUntilNextClaim = millisUntilMidnight,
                    isStreakBroken = false,
                    totalCheckIns = totalCheckIns
                )
            }
            daysPassed == 1 -> {
                // Consecutive next day!
                val nextStreak = savedStreak + 1
                val targetDay = (((nextStreak - 1) % 7) + 1)
                CheckInStreakStatus(
                    currentStreak = savedStreak,
                    bestStreak = maxOf(bestStreak, savedStreak),
                    isClaimAvailableToday = true,
                    targetDayInCycle = targetDay,
                    millisUntilNextClaim = 0L,
                    isStreakBroken = false,
                    totalCheckIns = totalCheckIns
                )
            }
            else -> {
                // More than 1 calendar day missed: streak broke!
                CheckInStreakStatus(
                    currentStreak = 0,
                    bestStreak = bestStreak,
                    isClaimAvailableToday = true,
                    targetDayInCycle = 1,
                    millisUntilNextClaim = 0L,
                    isStreakBroken = true,
                    totalCheckIns = totalCheckIns
                )
            }
        }
    }
}
