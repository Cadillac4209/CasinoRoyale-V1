package com.example.model

enum class VipTier(val title: String, val badgeColorHex: Long, val bonusMultiplier: Double) {
    BRONZE("Bronze VIP", 0xFFCD7F32, 1.0),
    SILVER("Silver VIP", 0xFFC0C0C0, 1.25),
    GOLD("Gold VIP", 0xFFFFD700, 1.5),
    PLATINUM("Platinum VIP", 0xFFE5E4E2, 2.0),
    DIAMOND("Diamond VIP", 0xFF00E5FF, 3.0)
}

data class PlayerStats(
    val totalWagered: Long = 0L,
    val totalWon: Long = 0L,
    val biggestWin: Long = 0L,
    val totalRoundsPlayed: Int = 0,
    val videoPokerHands: Int = 0,
    val slotsSpins: Int = 0,
    val blackjackHands: Int = 0,
    val rouletteSpins: Int = 0,
    val totalJackpots: Int = 0,
    val totalLevelUps: Int = 0,
    val totalVoidEarned: Long = 10L,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val checkInStreak: Int = 0,
    val bestCheckInStreak: Int = 0,
    val lastCheckInTimestamp: Long = 0L,
    val totalCheckIns: Int = 0
) {
    val netProfit: Long get() = totalWon - totalWagered
}

data class PlayerProfile(
    val bankroll: Long = 10000L,
    val xp: Long = 0L,
    val lastDailyBonusTime: Long = 0L,
    val stats: PlayerStats = PlayerStats(),
    val selectedFelt: FeltTheme = FeltTheme.FIRE_KERN_FORGE,
    val selectedCardBack: CardBackTheme = CardBackTheme.ROYAL_RED,
    val selectedPokerVariant: PokerVariant = PokerVariant.JACKS_OR_BETTER,
    val selectedSlotsVariant: SlotsVariant = SlotsVariant.CLASSIC_ROYALE,
    val selectedBlackjackVariant: BlackjackVariant = BlackjackVariant.VEGAS_STRIP,
    val selectedRouletteVariant: RouletteVariant = RouletteVariant.EUROPEAN,
    val voidWallet: VoidWallet = VoidWallet()
) {
    private val levelData: Pair<Int, Float> get() = ProgressionManager.calculateLevelAndProgress(xp)

    val level: Int get() = levelData.first
    val xpProgressInLevel: Float get() = levelData.second

    val maxBetAllowed: Long
        get() = when {
            level >= 15 -> 50000L
            level >= 10 -> 10000L
            level >= 8 -> 5000L
            level >= 4 -> 2500L
            else -> 1000L
        }

    val vipTier: VipTier
        get() = when {
            level >= 20 -> VipTier.DIAMOND
            level >= 15 -> VipTier.PLATINUM
            level >= 10 -> VipTier.GOLD
            level >= 5 -> VipTier.SILVER
            else -> VipTier.BRONZE
        }

    fun isFeltUnlocked(theme: FeltTheme): Boolean = level >= theme.requiredLevel
    fun isCardBackUnlocked(theme: CardBackTheme): Boolean = level >= theme.requiredLevel
    fun isPokerVariantUnlocked(variant: PokerVariant): Boolean = level >= variant.requiredLevel
    fun isSlotsVariantUnlocked(variant: SlotsVariant): Boolean = level >= variant.requiredLevel
    fun isBlackjackVariantUnlocked(variant: BlackjackVariant): Boolean = level >= variant.requiredLevel
    fun isRouletteVariantUnlocked(variant: RouletteVariant): Boolean = level >= variant.requiredLevel
}
