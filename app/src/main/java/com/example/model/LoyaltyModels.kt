package com.example.model

import kotlin.math.roundToLong

enum class RoyaleLoyaltyTier(
    val id: String,
    val title: String,
    val minLifetimePoints: Long,
    val earnMultiplier: Double,
    val colorHex: Long,
    val perkSummary: String
) {
    EMBER(
        id = "ember",
        title = "Ember Member",
        minLifetimePoints = 0L,
        earnMultiplier = 1.0,
        colorHex = 0xFFD6C2B5,
        perkSummary = "Earn 1 Royale Point per $25 wagered (min 5 RP/bet)"
    ),
    CRIMSON(
        id = "crimson",
        title = "Crimson Preferred",
        minLifetimePoints = 250L,
        earnMultiplier = 1.10,
        colorHex = 0xFFFF5722,
        perkSummary = "+10% Royale Points bonus on all gameplay volume"
    ),
    MOLTEN_GOLD(
        id = "molten_gold",
        title = "Molten Gold",
        minLifetimePoints = 1_000L,
        earnMultiplier = 1.25,
        colorHex = 0xFFFFC13B,
        perkSummary = "+25% Royale Points bonus + Molten Gold reward vault"
    ),
    OBSIDIAN_PLATINUM(
        id = "obsidian_platinum",
        title = "Obsidian Platinum",
        minLifetimePoints = 3_500L,
        earnMultiplier = 1.50,
        colorHex = 0xFF38BDF8,
        perkSummary = "+50% Royale Points bonus + Platinum Void Reactor"
    ),
    IMPERIAL_CROWN(
        id = "imperial_crown",
        title = "Imperial Kern Crown",
        minLifetimePoints = 10_000L,
        earnMultiplier = 2.00,
        colorHex = 0xFF14F195,
        perkSummary = "2.0x Royale Points overdrive + Crown Treasury"
    );

    companion object {
        fun tierForPoints(lifetimePoints: Long): RoyaleLoyaltyTier {
            val safe = lifetimePoints.coerceAtLeast(0L)
            return values()
                .filter { safe >= it.minLifetimePoints }
                .maxByOrNull { it.minLifetimePoints }
                ?: EMBER
        }

        fun nextTierAfter(tier: RoyaleLoyaltyTier): RoyaleLoyaltyTier? {
            val ordered = values().sortedBy { it.minLifetimePoints }
            val idx = ordered.indexOf(tier)
            return if (idx >= 0 && idx + 1 < ordered.size) ordered[idx + 1] else null
        }
    }
}

data class LoyaltyReward(
    val id: String,
    val title: String,
    val description: String,
    val requiredTier: RoyaleLoyaltyTier,
    val pointsCost: Long,
    val chipsReward: Long,
    val xpReward: Long,
    val voidReward: Long,
    val badgeEmoji: String
)

data class LoyaltyRewardUnlockResult(
    val reward: LoyaltyReward,
    val chipsAwarded: Long,
    val xpAwarded: Long,
    val voidAwarded: Long,
    val remainingRoyalePoints: Long
)

object RoyaleLoyaltyCatalog {
    const val CHIPS_PER_BASE_POINT = 25L
    const val MIN_POINTS_PER_WAGER = 5L

    val rewards: List<LoyaltyReward> = listOf(
        LoyaltyReward(
            id = "ember_chips_starter",
            title = "Ember Lounge Stipend",
            description = "Redeem your early floor play volume for bonus virtual chips & VIP XP.",
            requiredTier = RoyaleLoyaltyTier.EMBER,
            pointsCost = 100L,
            chipsReward = 3_500L,
            xpReward = 300L,
            voidReward = 0L,
            badgeEmoji = "🔥"
        ),
        LoyaltyReward(
            id = "crimson_chip_vault",
            title = "Crimson Preferred Coffers",
            description = "Tier 2 loyalty chest granting simulated chips and minted \$VOID tokens.",
            requiredTier = RoyaleLoyaltyTier.CRIMSON,
            pointsCost = 250L,
            chipsReward = 10_000L,
            xpReward = 750L,
            voidReward = 2L,
            badgeEmoji = "♦️"
        ),
        LoyaltyReward(
            id = "crimson_xp_surge",
            title = "Fire Kern Mastery Tome",
            description = "Accelerates your VIP club level progression with a heavy XP infusion.",
            requiredTier = RoyaleLoyaltyTier.CRIMSON,
            pointsCost = 300L,
            chipsReward = 5_000L,
            xpReward = 2_000L,
            voidReward = 1L,
            badgeEmoji = "📜"
        ),
        LoyaltyReward(
            id = "gold_high_roller_pack",
            title = "Molten Gold High-Roller Pack",
            description = "Tier 3 prestige bundle for dedicated table and slot regulars.",
            requiredTier = RoyaleLoyaltyTier.MOLTEN_GOLD,
            pointsCost = 650L,
            chipsReward = 30_000L,
            xpReward = 3_000L,
            voidReward = 5L,
            badgeEmoji = "👑"
        ),
        LoyaltyReward(
            id = "platinum_void_infusion",
            title = "Obsidian Platinum Void Reactor",
            description = "Tier 4 vault release packed with high-limit chips, XP, and \$VOID.",
            requiredTier = RoyaleLoyaltyTier.OBSIDIAN_PLATINUM,
            pointsCost = 1_400L,
            chipsReward = 75_000L,
            xpReward = 6_000L,
            voidReward = 15L,
            badgeEmoji = "💎"
        ),
        LoyaltyReward(
            id = "crown_imperial_jackpot",
            title = "Imperial Kern Crown Treasury",
            description = "Apex Tier 5 loyalty milestone unlocking the grand simulated treasury.",
            requiredTier = RoyaleLoyaltyTier.IMPERIAL_CROWN,
            pointsCost = 3_000L,
            chipsReward = 200_000L,
            xpReward = 15_000L,
            voidReward = 40L,
            badgeEmoji = "🏆"
        )
    )

    fun calculatePointsEarned(wagerAmount: Long, currentTier: RoyaleLoyaltyTier): Long {
        if (wagerAmount <= 0L) return 0L
        val basePoints = maxOf(MIN_POINTS_PER_WAGER, wagerAmount / CHIPS_PER_BASE_POINT)
        return (basePoints.toDouble() * currentTier.earnMultiplier)
            .roundToLong()
            .coerceAtLeast(MIN_POINTS_PER_WAGER)
    }

    fun progressToNextTier(lifetimePoints: Long): Float {
        val current = RoyaleLoyaltyTier.tierForPoints(lifetimePoints)
        val next = RoyaleLoyaltyTier.nextTierAfter(current) ?: return 1f
        val span = (next.minLifetimePoints - current.minLifetimePoints).coerceAtLeast(1L)
        val earnedInTier = (lifetimePoints - current.minLifetimePoints).coerceAtLeast(0L)
        return (earnedInTier.toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    fun pointsRemainingToNextTier(lifetimePoints: Long): Long {
        val current = RoyaleLoyaltyTier.tierForPoints(lifetimePoints)
        val next = RoyaleLoyaltyTier.nextTierAfter(current) ?: return 0L
        return (next.minLifetimePoints - lifetimePoints).coerceAtLeast(0L)
    }

    fun isRewardEligibleToUnlock(profile: PlayerProfile, reward: LoyaltyReward): Boolean {
        if (profile.unlockedLoyaltyRewardIds.contains(reward.id)) return false
        val tierMet = profile.lifetimeRoyalePoints >= reward.requiredTier.minLifetimePoints
        val pointsMet = profile.royalePoints >= reward.pointsCost
        return tierMet && pointsMet
    }

    fun countUnlockableRewards(profile: PlayerProfile): Int {
        return rewards.count { isRewardEligibleToUnlock(profile, it) }
    }
}
