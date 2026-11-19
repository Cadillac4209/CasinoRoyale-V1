package com.example.model

enum class MissionCategory {
    DAILY,
    CAREER
}

enum class MissionMetric {
    DAILY_ROUNDS_ANY,
    DAILY_CARD_ROUNDS,
    DAILY_WHEEL_REEL_ROUNDS,
    DAILY_WINS_ANY,
    CAREER_WINS_ANY,
    CAREER_BEST_STREAK,
    CAREER_TOTAL_WAGERED,
    CAREER_GAMES_SAMPLED,
    CAREER_BIGGEST_WIN
}

data class RoyaleMission(
    val id: String,
    val title: String,
    val description: String,
    val category: MissionCategory,
    val metric: MissionMetric,
    val targetValue: Long,
    val rewardChips: Long,
    val rewardXp: Long,
    val rewardVoid: Long = 0L,
    val badgeEmoji: String = "🔥"
)

data class MissionProgressState(
    val mission: RoyaleMission,
    val currentValue: Long,
    val isClaimed: Boolean
) {
    val clampedProgress: Long get() = currentValue.coerceIn(0L, mission.targetValue)
    val progressFraction: Float
        get() = if (mission.targetValue <= 0L) 1f else (clampedProgress.toFloat() / mission.targetValue.toFloat()).coerceIn(0f, 1f)
    val isCompleted: Boolean get() = currentValue >= mission.targetValue
    val isClaimable: Boolean get() = isCompleted && !isClaimed
}

data class MissionClaimResult(
    val mission: RoyaleMission,
    val chipsAwarded: Long,
    val xpAwarded: Long,
    val voidAwarded: Long
)

object RoyaleMissionCatalog {
    val missions: List<RoyaleMission> = listOf(
        // Daily Rotating Floor Quests
        RoyaleMission(
            id = "daily_warmup",
            title = "Ember Warmup",
            description = "Play 5 rounds in any Fire Kern floor game today.",
            category = MissionCategory.DAILY,
            metric = MissionMetric.DAILY_ROUNDS_ANY,
            targetValue = 5L,
            rewardChips = 1500L,
            rewardXp = 200L,
            rewardVoid = 0L,
            badgeEmoji = "🔥"
        ),
        RoyaleMission(
            id = "daily_card_strategist",
            title = "Table Strategist",
            description = "Play 3 hands in Blackjack, Video Poker, or Royal Baccarat today.",
            category = MissionCategory.DAILY,
            metric = MissionMetric.DAILY_CARD_ROUNDS,
            targetValue = 3L,
            rewardChips = 2000L,
            rewardXp = 250L,
            rewardVoid = 0L,
            badgeEmoji = "🃏"
        ),
        RoyaleMission(
            id = "daily_wheel_reels",
            title = "Reels & Wheel",
            description = "Complete 4 spins in Royale Slots or Roulette Royale today.",
            category = MissionCategory.DAILY,
            metric = MissionMetric.DAILY_WHEEL_REEL_ROUNDS,
            targetValue = 4L,
            rewardChips = 2000L,
            rewardXp = 250L,
            rewardVoid = 0L,
            badgeEmoji = "🎡"
        ),
        RoyaleMission(
            id = "daily_hot_hand",
            title = "Hot Hand",
            description = "Score 3 winning rounds on the floor today.",
            category = MissionCategory.DAILY,
            metric = MissionMetric.DAILY_WINS_ANY,
            targetValue = 3L,
            rewardChips = 3500L,
            rewardXp = 400L,
            rewardVoid = 1L,
            badgeEmoji = "⚡"
        ),

        // Permanent Career Trophies
        RoyaleMission(
            id = "career_first_flame",
            title = "First Flame",
            description = "Win your first casino round in any floor game.",
            category = MissionCategory.CAREER,
            metric = MissionMetric.CAREER_WINS_ANY,
            targetValue = 1L,
            rewardChips = 1000L,
            rewardXp = 150L,
            rewardVoid = 0L,
            badgeEmoji = "🏅"
        ),
        RoyaleMission(
            id = "career_molten_streak",
            title = "Molten Streak",
            description = "Reach a 4-win streak across the casino floor.",
            category = MissionCategory.CAREER,
            metric = MissionMetric.CAREER_BEST_STREAK,
            targetValue = 4L,
            rewardChips = 4000L,
            rewardXp = 500L,
            rewardVoid = 2L,
            badgeEmoji = "🔥"
        ),
        RoyaleMission(
            id = "career_high_roller",
            title = "High Roller",
            description = "Wager $25,000+ in total virtual chips.",
            category = MissionCategory.CAREER,
            metric = MissionMetric.CAREER_TOTAL_WAGERED,
            targetValue = 25000L,
            rewardChips = 7500L,
            rewardXp = 900L,
            rewardVoid = 3L,
            badgeEmoji = "💎"
        ),
        RoyaleMission(
            id = "career_grand_tour",
            title = "Grand Floor Tour",
            description = "Sample all 5 casino games (Poker, Slots, Blackjack, Roulette, Baccarat).",
            category = MissionCategory.CAREER,
            metric = MissionMetric.CAREER_GAMES_SAMPLED,
            targetValue = 5L,
            rewardChips = 5000L,
            rewardXp = 750L,
            rewardVoid = 3L,
            badgeEmoji = "👑"
        ),
        RoyaleMission(
            id = "career_crown_jewel",
            title = "Crown Jewel",
            description = "Land a single winning payout of $2,500+ virtual chips.",
            category = MissionCategory.CAREER,
            metric = MissionMetric.CAREER_BIGGEST_WIN,
            targetValue = 2500L,
            rewardChips = 10000L,
            rewardXp = 1500L,
            rewardVoid = 5L,
            badgeEmoji = "🏆"
        )
    )

    fun findById(id: String): RoyaleMission? = missions.firstOrNull { it.id == id }
}
