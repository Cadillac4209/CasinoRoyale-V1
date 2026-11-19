package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatsDialog(
    profile: PlayerProfile,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    val stats = profile.stats

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "FIRE KERN CAREER STATS",
                    color = GoldPrimary,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Rank: ${profile.vipTier.title} (Level ${profile.level})",
                    color = GoldLight,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCardRow("Current Bankroll", formatter.format(profile.bankroll), GoldPrimary)
                StatCardRow("Total Wagered", formatter.format(stats.totalWagered), Color.White)
                StatCardRow("Total Won", formatter.format(stats.totalWon), NeonCyan)
                StatCardRow("Biggest Win", formatter.format(stats.biggestWin), GoldLight)
                StatCardRow("Current Win Streak", "${stats.currentWinStreak} 🔥", if (stats.currentWinStreak > 0) Color(0xFFF43F5E) else Color.LightGray)
                StatCardRow("Best Win Streak", "${stats.bestWinStreak} ⚡", GoldPrimary)
                StatCardRow("Login Streak", "${stats.checkInStreak} Days 🔥", Color(0xFFF97316))
                StatCardRow("Best Login Streak", "${stats.bestCheckInStreak} Days 🏆", GoldLight)
                StatCardRow("Total Check-Ins", "${stats.totalCheckIns} Days", NeonCyan)
                StatCardRow("Total Rounds", "${stats.totalRoundsPlayed}", Color.White)
                StatCardRow("Persistence Engine", "Room SQLite Database", Color(0xFF14F195))

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GAMES BREAKDOWN",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                StatCardRow("Video Poker Hands", "${stats.videoPokerHands}", FireKernAsh)
                StatCardRow("Slots Spins", "${stats.slotsSpins}", FireKernAsh)
                StatCardRow("Blackjack Hands", "${stats.blackjackHands}", FireKernAsh)
                StatCardRow("Roulette Spins", "${stats.rouletteSpins}", FireKernAsh)
                StatCardRow("Baccarat Hands", "${stats.baccaratHands}", FireKernAsh)

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FireKernCardElevated)
                        .border(1.dp, CasinoSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "VIP Tier Perks:\n• Daily Bonus Multiplier: ${profile.vipTier.bonusMultiplier}x\n• Instant Chip Refills whenever bankroll < $200\n• Pure simulated entertainment — no real money involved.",
                        color = FireKernAsh,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onResetStats) {
                Text("RESET STATS", color = CasinoRed, fontSize = 11.sp)
            }
        },
        containerColor = FireKernObsidian
    )
}

@Composable
private fun StatCardRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(FireKernCard)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = FireKernAsh, fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
