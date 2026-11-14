package com.example.ui.slots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.WinCelebrationOverlay
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow

@Composable
fun SlotsScreen(
    viewModel: SlotsViewModel,
    profile: PlayerProfile,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grid by viewModel.grid.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val reelsSpinning by viewModel.reelsSpinning.collectAsState()
    val activeLines by viewModel.activeLines.collectAsState()
    val betPerLine by viewModel.betPerLine.collectAsState()
    val winningLines by viewModel.winningLines.collectAsState()
    val lastWin by viewModel.lastWin.collectAsState()
    val freeSpinsRemaining by viewModel.freeSpinsRemaining.collectAsState()
    val isAutoSpin by viewModel.isAutoSpin.collectAsState()
    val celebrationWin by viewModel.celebrationWin.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    var showPaytableDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "jackpot_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(profile.selectedFelt.darkColor, FireKernObsidian, FireKernVoid)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CasinoTopBar(
                profile = profile,
                title = profile.selectedSlotsVariant.displayName.uppercase(),
                isSoundEnabled = isSoundEnabled,
                onToggleSound = onToggleSound,
                onBackClick = onBackClick,
                onEmergencyRefill = onEmergencyRefill
            )

            // Free Spins Banner if active
            AnimatedVisibility(visible = freeSpinsRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CasinoRed, Color(0xFFE11D48), CasinoRed)
                            )
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FREE SPINS ACTIVE: $freeSpinsRemaining LEFT (2X MULTIPLIER)!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Jackpot Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(FireKernEmberDark, FireKernCrimson, FireKernEmberDark)
                        )
                    )
                    .border(
                        2.dp,
                        GoldPrimary.copy(alpha = glowAlpha),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FIRE KERN MEGA JACKPOT",
                            color = GoldLight,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$250,000",
                            color = GoldPrimary,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(
                        onClick = { showPaytableDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Paytable Info",
                            tint = GoldLight
                        )
                    }
                }
            }

            // Status Marquee Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FireKernCard)
                    .border(1.dp, FireKernCardBorder, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    color = if (lastWin > 0) GoldPrimary else GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5 REELS CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FireKernVoid)
                    .border(2.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 5) {
                        val isReelSpinning = reelsSpinning.getOrNull(col) == true
                        val reelSymbols = grid.getOrNull(col) ?: emptyList()

                        // Single Reel Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FireKernObsidian)
                                .border(1.dp, FireKernCardBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (row in 0 until 3) {
                                val symbol = reelSymbols.getOrNull(row) ?: SlotSymbol.CHERRY
                                val isHighlighted = winningLines.any { line ->
                                    val pattern = viewModel.paylinePatterns[line.lineIndex - 1]
                                    pattern[col] == row && col < line.matchCount
                                }

                                SlotSymbolTile(
                                    symbol = symbol,
                                    isSpinning = isReelSpinning,
                                    isWinning = isHighlighted
                                )
                            }
                        }
                    }
                }
            }

            // Winning lines report if any
            if (winningLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${winningLines.size} PAYLINE WINS!",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "TOTAL: $${lastWin}",
                        color = NeonCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls: Bet Per Line & Lines Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lines selector
                Column {
                    Text("LINES: $activeLines", color = FireKernAsh, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 3, 5, 9).forEach { lineCount ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (activeLines == lineCount) GoldPrimary else FireKernCardElevated)
                                    .clickable(enabled = !isSpinning) { viewModel.setLines(lineCount) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$lineCount",
                                    color = if (activeLines == lineCount) Color.Black else GoldLight,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Bet per line selector
                Column(horizontalAlignment = Alignment.End) {
                    Text("BET/LINE: $$betPerLine", color = FireKernAsh, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5L, 10L, 25L, 50L, 100L).forEach { betAmount ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (betPerLine == betAmount) GoldPrimary else FireKernCardElevated)
                                    .clickable(enabled = !isSpinning) { viewModel.setBetPerLine(betAmount) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$$betAmount",
                                    color = if (betPerLine == betAmount) Color.Black else GoldLight,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Bet & Spin Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Bet info
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FireKernCard)
                        .border(1.dp, FireKernCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("TOTAL BET", color = FireKernAsh, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${viewModel.totalBet}",
                        color = GoldPrimary,
                        fontFamily = MonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Auto-spin toggle button
                Button(
                    onClick = { viewModel.toggleAutoSpin() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoSpin) CasinoRed else FireKernCardElevated,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("auto_spin_button")
                ) {
                    Icon(
                        imageVector = if (isAutoSpin) Icons.Default.Stop else Icons.Default.Refresh,
                        contentDescription = "Auto Spin",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAutoSpin) "STOP" else "AUTO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Main SPIN Button
                Button(
                    onClick = { viewModel.spin() },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = GoldDark.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("spin_button")
                ) {
                    Text(
                        text = if (isSpinning) "SPINNING" else "SPIN",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Paytable dialog
        if (showPaytableDialog) {
            SlotPaytableDialog(onDismiss = { showPaytableDialog = false })
        }

        // Big Win Celebration Overlay
        celebrationWin?.let { win ->
            WinCelebrationOverlay(
                winAmount = win,
                title = if (win >= viewModel.totalBet * 50) "MEGA JACKPOT!" else "BIG WIN!",
                onDismiss = { viewModel.dismissCelebration() }
            )
        }
    }
}

@Composable
private fun SlotSymbolTile(
    symbol: SlotSymbol,
    isSpinning: Boolean,
    isWinning: Boolean
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isWinning -> GoldPrimary.copy(alpha = 0.35f)
                    isSpinning -> FireKernCardElevated.copy(alpha = 0.8f)
                    else -> FireKernCard
                }
            )
            .border(
                width = if (isWinning) 2.dp else 1.dp,
                color = when {
                    isWinning -> GoldPrimary
                    symbol.isWild -> NeonCyan
                    symbol.isScatter -> NeonYellow
                    else -> FireKernCardBorder
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol.iconEmoji,
                fontSize = if (isSpinning) 20.sp else 24.sp
            )
            if (!isSpinning && (symbol.isWild || symbol.isScatter)) {
                Text(
                    text = if (symbol.isWild) "WILD" else "SCATTER",
                    color = if (symbol.isWild) NeonCyan else NeonYellow,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun SlotPaytableDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SLOT PAYTABLE & RULES",
                fontWeight = FontWeight.Black,
                color = GoldPrimary,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• 💎 WILD substitutes for any symbol except Scatter.",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = "• ⭐ 3+ SCATTERS award 10 Free Spins with 2x multiplier!",
                    color = GoldLight,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                SlotSymbol.values().forEach { sym ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${sym.iconEmoji} ${sym.displayName}", color = Color.White, fontSize = 12.sp)
                        Text(
                            "3: ${sym.payout3}x | 4: ${sym.payout4}x | 5: ${sym.payout5}x",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = FireKernObsidian
    )
}
