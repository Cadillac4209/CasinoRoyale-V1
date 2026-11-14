package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoFeltGreen
import com.example.ui.theme.CasinoFeltMedium
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernVoid

enum class FeltTheme(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val primaryColor: Color,
    val mediumColor: Color,
    val darkColor: Color,
    val description: String
) {
    FIRE_KERN_FORGE(
        "felt_fire_kern",
        "Fire Kern Ember",
        1,
        FireKernCrimson,
        FireKernEmberDark,
        FireKernVoid,
        "Signature volcanic crimson velvet with molten gold trim."
    ),
    EMERALD_GREEN(
        "felt_emerald",
        "Classic Emerald",
        1,
        CasinoFeltGreen,
        CasinoFeltMedium,
        CasinoFeltDark,
        "Authentic Las Vegas green velvet felt."
    ),
    ROYAL_NAVY(
        "felt_navy",
        "Royal Sapphire Navy",
        2,
        Color(0xFF1E3A8A),
        Color(0xFF172554),
        Color(0xFF0F172A),
        "Monte Carlo midnight blue luxury felt."
    ),
    CRIMSON_RUBY(
        "felt_crimson",
        "High Roller Crimson",
        6,
        Color(0xFF881337),
        Color(0xFF4C0519),
        Color(0xFF1F020A),
        "Exclusive Macau high-limit salon burgundy."
    ),
    OBSIDIAN_GOLD(
        "felt_obsidian",
        "VIP Obsidian & Gold",
        10,
        Color(0xFF1C1917),
        Color(0xFF0C0A09),
        Color(0xFF000000),
        "Ultra-exclusive carbon black with gold accents."
    );

    companion object {
        fun fromId(id: String): FeltTheme = values().firstOrNull { it.id == id } ?: FIRE_KERN_FORGE
    }
}

enum class CardBackTheme(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val color1: Color,
    val color2: Color,
    val accentColor: Color,
    val emblem: String,
    val description: String
) {
    ROYAL_RED(
        "card_royal_red",
        "Burgundy Royale",
        1,
        Color(0xFF881337),
        Color(0xFF4C0519),
        Color(0xFFFFD700),
        "R",
        "Classic casino diamond-weave with golden seal."
    ),
    GOLDEN_IMPERIAL(
        "card_golden_imperial",
        "Imperial Dragon Gold",
        4,
        Color(0xFFB45309),
        Color(0xFF78350F),
        Color(0xFFFFE066),
        "龍",
        "Gilded dragon scales with metallic sheen."
    ),
    NEON_CYBER(
        "card_neon_cyber",
        "Cyberpunk Vegas",
        9,
        Color(0xFF0284C7),
        Color(0xFF0369A1),
        Color(0xFF00F5D4),
        "⚡",
        "Glowing holographic circuits with cyan accents."
    ),
    PLATINUM_ACE(
        "card_platinum_ace",
        "Platinum Ace",
        14,
        Color(0xFF334155),
        Color(0xFF0F172A),
        Color(0xFFE2E8F0),
        "♠",
        "Brushed platinum finish with embossed ace crest."
    );

    companion object {
        fun fromId(id: String): CardBackTheme = values().firstOrNull { it.id == id } ?: ROYAL_RED
    }
}

enum class PokerVariant(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val description: String
) {
    JACKS_OR_BETTER("poker_jacks", "Jacks or Better", 1, "Classic 9/6 full-pay machine. Pair of Jacks or better wins."),
    DEUCES_WILD("poker_deuces", "Deuces Wild", 3, "All 2s are WILD! 5 of a Kind, Wild Royals, and 4 Deuces pays big.")
}

enum class SlotsVariant(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val description: String
) {
    CLASSIC_ROYALE("slots_classic", "Royale 5-Reel Classic", 1, "Standard 9-line slot with 10 Free Spins scatter bonus."),
    NEON_FRENZY("slots_neon", "Neon Cyber Frenzy", 5, "High volatility mode: 3x Wild Multipliers on all paylines!")
}

enum class BlackjackVariant(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val description: String
) {
    VEGAS_STRIP("bj_vegas", "Vegas Strip Rules", 1, "Standard 3:2 payout, dealer stands on all 17s."),
    HIGH_ROLLER_VIP("bj_high_roller", "VIP High Roller 21", 8, "Higher betting limits + 2:1 bonus for Suited Blackjacks!")
}

enum class RouletteVariant(
    val id: String,
    val displayName: String,
    val requiredLevel: Int,
    val description: String
) {
    EUROPEAN("rl_european", "European Single Zero (0)", 1, "Classic 37-pocket wheel with 2.7% house edge."),
    FRENCH_LA_PARTAGE("rl_french", "French (La Partage)", 12, "When 0 hits, 50% of Even-Money bets are refunded!")
}

data class LevelMilestone(
    val level: Int,
    val title: String,
    val description: String,
    val rewardIconEmoji: String,
    val isVariantUnlock: Boolean = false,
    val isLimitUnlock: Boolean = false,
    val isCosmeticUnlock: Boolean = false
)

object ProgressionManager {
    val MILESTONES = listOf(
        LevelMilestone(2, "Royal Sapphire Navy Felt", "Unlock deep blue velvet table felt cosmetic.", "🎨", isCosmeticUnlock = true),
        LevelMilestone(3, "Deuces Wild Video Poker", "All 2s become Wild cards! 5 of a Kind unlocked.", "🃏", isVariantUnlock = true),
        LevelMilestone(4, "Imperial Gold Card Back & $2.5K Chip", "Unlock dragon scales card back & $2,500 chip.", "🪙", isCosmeticUnlock = true, isLimitUnlock = true),
        LevelMilestone(5, "Neon Cyber Frenzy Slots", "High-volatility slot variant with 3x Wild Multipliers!", "🎰", isVariantUnlock = true),
        LevelMilestone(6, "High Roller Crimson Felt", "Unlock Macau salon ruby velvet felt cosmetic.", "🎨", isCosmeticUnlock = true),
        LevelMilestone(7, "$5,000 Onyx Chip", "Unlocks $5,000 chip denomination for high rollers.", "🪙", isLimitUnlock = true),
        LevelMilestone(8, "VIP High Roller Blackjack", "Unlock 2:1 Suited Blackjack payouts and $10K limit.", "♠️", isVariantUnlock = true, isLimitUnlock = true),
        LevelMilestone(9, "Cyberpunk Vegas Card Back", "Unlock futuristic electric neon cyan card deck.", "✨", isCosmeticUnlock = true),
        LevelMilestone(10, "VIP Obsidian Felt & $10K Limits", "Unlock carbon black felt and $10,000 maximum bet.", "👑", isCosmeticUnlock = true, isLimitUnlock = true),
        LevelMilestone(12, "French Roulette (La Partage)", "Unlock French rules: get 50% refund when 0 hits!", "🎡", isVariantUnlock = true),
        LevelMilestone(14, "Platinum Ace Card Back", "Unlock brushed platinum and embossed ace design.", "💎", isCosmeticUnlock = true),
        LevelMilestone(15, "$25,000 Whale Chip", "Unlock the legendary $25,000 diamond chip & $50K limit.", "🐋", isLimitUnlock = true),
        LevelMilestone(20, "Diamond VIP Whale Trophy", "Earn maximum 3.0x Daily Wheel multiplier and prestige badge.", "🏆")
    )

    // Progressive XP Curve:
    // Level 1 -> 2: 500 XP
    // Level 2 -> 3: 750 XP
    // Level 3 -> 4: 1,000 XP
    // Level L requires: 500 + (L - 1) * 250 XP to reach Level L+1
    fun xpRequiredForNextLevel(currentLevel: Int): Long {
        return 500L + (currentLevel - 1) * 250L
    }

    fun calculateLevelAndProgress(totalXp: Long): Pair<Int, Float> {
        var level = 1
        var remainingXp = totalXp
        while (true) {
            val req = xpRequiredForNextLevel(level)
            if (remainingXp >= req) {
                remainingXp -= req
                level++
            } else {
                val progress = remainingXp.toFloat() / req.toFloat()
                return Pair(level, progress.coerceIn(0f, 1f))
            }
        }
    }

    fun getMilestoneForLevel(level: Int): LevelMilestone? {
        return MILESTONES.firstOrNull { it.level == level }
    }
}
