package com.example.ui.slots

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow

enum class SlotSymbol(
    val displayName: String,
    val iconEmoji: String,
    val tintColor: Color,
    val payout3: Int,
    val payout4: Int,
    val payout5: Int,
    val isWild: Boolean = false,
    val isScatter: Boolean = false
) {
    WILD("DIAMOND", "💎", NeonCyan, 50, 200, 1000, isWild = true),
    SEVEN("777", "7️⃣", CasinoRed, 40, 150, 500),
    BELL("BELL", "🔔", GoldPrimary, 25, 80, 250),
    BAR("BAR", "🍫", GoldLight, 15, 50, 150),
    HORSESHOE("HORSESHOE", "🧲", Color(0xFF94A3B8), 10, 30, 100),
    CLOVER("CLOVER", "🍀", Color(0xFF22C55E), 8, 20, 60),
    CHERRY("CHERRY", "🍒", Color(0xFFEF4444), 5, 15, 40),
    SCATTER("FREE SPINS", "⭐", NeonYellow, 2, 5, 20, isScatter = true);

    fun payoutForCount(count: Int): Int = when (count) {
        3 -> payout3
        4 -> payout4
        5 -> payout5
        else -> 0
    }
}

data class WinningLine(
    val lineIndex: Int,
    val symbol: SlotSymbol,
    val matchCount: Int,
    val winAmount: Long
)
