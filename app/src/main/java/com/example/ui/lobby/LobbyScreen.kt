package com.example.ui.lobby

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.SoundManager
import com.example.data.CasinoRepository
import com.example.data.CryptoRatesUiState
import com.example.model.CryptoAsset
import com.example.model.DailyBonusWheelCatalog
import com.example.model.DailyCheckInClaimResult
import com.example.model.DailyLoginRecord
import com.example.model.DailyWheelSpinResult
import com.example.model.GameType
import com.example.model.PlayerProfile
import com.example.model.RoyaleLoyaltyCatalog
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.SoundSettingsDialog
import com.example.ui.converter.DualBalanceVisualizerCard
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoFeltGreen
import com.example.ui.theme.CasinoFeltMedium
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.ChipPurple
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LobbyScreen(
    repository: CasinoRepository,
    soundManager: SoundManager,
    onSelectGame: (GameType) -> Unit,
    onOpenProgression: () -> Unit,
    onOpenVoidWallet: () -> Unit,
    onOpenLeaderboard: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenCryptoConverter: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenLoyaltyRewards: () -> Unit = {},
    cryptoRatesState: CryptoRatesUiState = CryptoRatesUiState.Loading,
    selectedCryptoAsset: CryptoAsset = CryptoAsset.BTC,
    onSelectCryptoAsset: (CryptoAsset) -> Unit = {},
    onRefreshCryptoRates: () -> Unit = {},
    playerRank: Int? = null,
    dailyLoginRecord: DailyLoginRecord? = null,
    isDailyLoginSyncing: Boolean = false,
    onDailyCheckInClaimed: (DailyCheckInClaimResult) -> Unit = {},
    onDailyWheelSpun: (DailyWheelSpinResult) -> Unit = {},
    onResetDailyCheckInCloud: () -> Unit = {},
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by repository.profile.collectAsState()
    val gameSpeed by repository.gameSpeed.collectAsStateWithLifecycle()
    val missionsClaimableCount by repository.missionsRepository.claimableCount.collectAsStateWithLifecycle()
    val resumableGames by repository.activeGameRepository.resumableGameTypes
        .collectAsStateWithLifecycle(initialValue = emptySet())
    var showBonusWheel by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSoundSettings by remember { mutableStateOf(false) }
    var showDailyCheckIn by remember { mutableStateOf(false) }
    var hasPromptedDailyCheckIn by remember { mutableStateOf(false) }
    var wheelNowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val checkInStatus = remember(profile) { repository.getDailyCheckInStatus() }
    val wheelStatus = remember(profile.lastDailyBonusTime, wheelNowMs) {
        DailyBonusWheelCatalog.evaluateStatus(profile.lastDailyBonusTime, wheelNowMs)
    }

    LaunchedEffect(profile.lastDailyBonusTime) {
        wheelNowMs = System.currentTimeMillis()
        while (!DailyBonusWheelCatalog.evaluateStatus(profile.lastDailyBonusTime, wheelNowMs).canSpin) {
            kotlinx.coroutines.delay(1000L)
            wheelNowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(checkInStatus.isClaimAvailableToday) {
        if (checkInStatus.isClaimAvailableToday && !hasPromptedDailyCheckIn) {
            hasPromptedDailyCheckIn = true
            showDailyCheckIn = true
        }
    }

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
                title = "FIRE KERN ROYALE",
                isSoundEnabled = soundManager.isSoundEnabled,
                onToggleSound = {
                    soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                },
                onBackClick = null,
                onOpenBonusWheel = { showBonusWheel = true },
                onEmergencyRefill = onEmergencyRefill,
                onOpenVoidWallet = onOpenVoidWallet,
                onOpenSoundSettings = { showSoundSettings = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // FIRE KERN GRAND CASINO HERO BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .shadow(14.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, GoldPrimary, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_fire_casino_banner),
                    contentDescription = "Fire Kern Royale Grand Floor",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    FireKernVoid.copy(alpha = 0.72f),
                                    FireKernObsidian.copy(alpha = 0.88f),
                                    FireKernVoid.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FIRE KERN ROYALE",
                                color = GoldPrimary,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Classic High-Stakes Ember Floor",
                                color = FireKernAsh,
                                fontSize = 12.sp
                            )
                        }

                        // VIP Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(profile.vipTier.badgeColorHex))
                                .border(1.dp, GoldLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = profile.vipTier.title.uppercase(),
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // VIP Progression Road Banner Button
                    Button(
                        onClick = {
                            soundManager.playButtonClick()
                            onOpenProgression()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FireKernCardElevated.copy(alpha = 0.92f),
                            contentColor = GoldLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .height(48.dp)
                            .testTag("lobby_progression_road_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VIP ROAD & UNLOCKS (LVL ${profile.level})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = GoldPrimary
                                )
                            }
                            Text(
                                text = "VIEW REWARDS >",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Consecutive Daily Check-In Streak Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.5.dp,
                                if (checkInStatus.isClaimAvailableToday) GoldPrimary else FireKernCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                soundManager.playButtonClick()
                                showDailyCheckIn = true
                            }
                            .testTag("lobby_daily_checkin_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checkInStatus.isClaimAvailableToday) FireKernEmberDark else FireKernCard
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (checkInStatus.isClaimAvailableToday)
                                                Brush.radialGradient(listOf(Color(0xFFEA580C), CasinoRed))
                                            else
                                                Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = if (checkInStatus.isClaimAvailableToday) GoldLight else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "LOGIN STREAK",
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🔥 ${checkInStatus.currentStreak} DAYS",
                                            color = if (checkInStatus.currentStreak > 0) Color(0xFFF97316) else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = if (checkInStatus.isClaimAvailableToday)
                                            "Day ${checkInStatus.targetDayInCycle} reward READY to claim!"
                                        else
                                            "Next reward in: ${checkInStatus.formattedCountdown}",
                                        color = if (checkInStatus.isClaimAvailableToday) NeonCyan else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (checkInStatus.isClaimAvailableToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    soundManager.playButtonClick()
                                    showDailyCheckIn = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (checkInStatus.isClaimAvailableToday) GoldPrimary else FireKernCardBorder,
                                    contentColor = if (checkInStatus.isClaimAvailableToday) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("lobby_daily_checkin_button")
                            ) {
                                Text(
                                    text = if (checkInStatus.isClaimAvailableToday) "CLAIM" else "VIEW",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: Daily Wheel, Career Stats, Audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                showBonusWheel = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wheelStatus.canSpin) GoldPrimary else FireKernCardElevated,
                                contentColor = if (wheelStatus.canSpin) Color.Black else GoldLight
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .then(
                                    if (!wheelStatus.canSpin) {
                                        Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("lobby_daily_wheel_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WHEEL", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                Text(
                                    text = if (wheelStatus.canSpin) "SPIN READY" else wheelStatus.compactCountdown,
                                    color = if (wheelStatus.canSpin) FireKernVoid else NeonCyan,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                soundManager.playButtonClick()
                                showStatsDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("lobby_stats_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STATS", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                soundManager.playButtonClick()
                                showSoundSettings = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("lobby_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = FireKernFlame,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AUDIO", color = FireKernFlame, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // GLOBAL TOP 100 LEADERBOARD ENTRY CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(GoldPrimary, FireKernEmber)),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        soundManager.playButtonClick()
                        onOpenLeaderboard()
                    }
                    .testTag("lobby_leaderboard_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireKernCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldPrimary, FireKernCrimson))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Global Top 100 Leaderboard",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HALL OF FLAME • TOP 100",
                            color = GoldPrimary,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedXp = NumberFormat.getNumberInstance(Locale.US).format(profile.xp)
                        val rankText = playerRank?.let { "Global Rank #$it" } ?: "Rank Pending"
                        Text(
                            text = "$rankText • $formattedXp Total XP",
                            color = GoldLight,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(FireKernEmberDark)
                            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "RANKS >",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SOLANA VOID POWER VAULT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF9945FF), Color(0xFF14F195))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        soundManager.playButtonClick()
                        onOpenVoidWallet()
                    }
                    .testTag("lobby_void_vault_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120E2C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF9945FF), Color(0xFF14F195))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SOLANA VOID VAULT",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(profile.voidWallet.activePowerTier.badgeColorHex))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = profile.voidWallet.activePowerTier.title.uppercase(),
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${profile.voidWallet.balance} \$VOID Available • ${profile.voidWallet.stakedAmount} Staked (${((profile.voidWallet.activePowerTier.xpMultiplier - 1f) * 100).toInt()}% XP Boost)",
                            color = Color(0xFF14F195),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF231B50))
                            .border(1.dp, Color(0xFF9945FF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "VAULT >",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DUAL VIRTUAL CREDITS & LIVE CRYPTO EXCHANGE RATE VISUALIZER CARD
            DualBalanceVisualizerCard(
                virtualCredits = profile.bankroll,
                selectedAsset = selectedCryptoAsset,
                ratesState = cryptoRatesState,
                onSelectAsset = { asset ->
                    soundManager.playChipClick()
                    onSelectCryptoAsset(asset)
                },
                onRefreshRates = {
                    soundManager.playButtonClick()
                    onRefreshCryptoRates()
                },
                onOpenFullConverter = {
                    soundManager.playButtonClick()
                    onOpenCryptoConverter()
                },
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ROYALE QUESTS & TROPHIES ENTRY CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (missionsClaimableCount > 0) 1.5.dp else 1.dp,
                        color = if (missionsClaimableCount > 0) Color(0xFF14F195) else GoldPrimary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        soundManager.playButtonClick()
                        onOpenMissions()
                    }
                    .testTag("lobby_missions_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireKernCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldPrimary, FireKernEmberDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Royale Quests and Trophies",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ROYALE QUESTS & TROPHIES",
                            color = GoldLight,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (missionsClaimableCount > 0) {
                                "$missionsClaimableCount Completed Rewards Ready to Claim!"
                            } else {
                                "Daily floor quests & career milestones for Chips + XP"
                            },
                            color = if (missionsClaimableCount > 0) Color(0xFF14F195) else FireKernAsh,
                            fontSize = 11.sp,
                            fontWeight = if (missionsClaimableCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (missionsClaimableCount > 0) Color(0xFF14F195) else FireKernEmberDark
                            )
                            .border(
                                1.dp,
                                if (missionsClaimableCount > 0) Color(0xFF14F195) else GoldPrimary,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (missionsClaimableCount > 0) "$missionsClaimableCount READY" else "QUESTS >",
                            color = if (missionsClaimableCount > 0) Color.Black else GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROYALE POINTS LOYALTY CLUB ENTRY CARD
            val loyaltyUnlockableCount = remember(profile) {
                RoyaleLoyaltyCatalog.countUnlockableRewards(profile)
            }
            val loyaltyTierColor = Color(profile.loyaltyTier.colorHex)
            val rpFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (loyaltyUnlockableCount > 0) 1.5.dp else 1.dp,
                        color = if (loyaltyUnlockableCount > 0) GoldPrimary else loyaltyTierColor.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        soundManager.playButtonClick()
                        onOpenLoyaltyRewards()
                    }
                    .testTag("lobby_loyalty_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireKernCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(loyaltyTierColor, FireKernEmberDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Royale Points Loyalty Club",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ROYALE POINTS • ${profile.loyaltyTier.title.uppercase()}",
                            color = loyaltyTierColor,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${rpFormatter.format(profile.royalePoints)} RP Available • Earn ${profile.loyaltyTier.earnMultiplier}x on play volume",
                            color = if (loyaltyUnlockableCount > 0) NeonCyan else FireKernAsh,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (loyaltyUnlockableCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (loyaltyUnlockableCount > 0) GoldPrimary else FireKernEmberDark)
                            .border(
                                1.dp,
                                if (loyaltyUnlockableCount > 0) GoldLight else loyaltyTierColor,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (loyaltyUnlockableCount > 0) "$loyaltyUnlockableCount UNLOCK" else "TIERS >",
                            color = if (loyaltyUnlockableCount > 0) FireKernVoid else loyaltyTierColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SETTINGS & ACCOUNT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        FireKernCardBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        soundManager.playButtonClick()
                        onOpenSettings()
                    }
                    .testTag("lobby_settings_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireKernCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FireKernEmberDark)
                            .border(1.dp, GoldDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Casino Settings",
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SETTINGS & ACCOUNT",
                            color = GoldLight,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val musicStatus = if (soundManager.isMusicEnabled) "Music On" else "Music Off"
                        val sfxStatus = if (soundManager.isSoundEnabled) "SFX On" else "SFX Off"
                        Text(
                            text = "$musicStatus • $sfxStatus • Speed: ${gameSpeed.label} (${gameSpeed.badge})",
                            color = FireKernAsh,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(FireKernCardElevated)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "OPEN >",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION TITLE: SELECT GAME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FIRE KERN FLOOR GAMES",
                    color = GoldLight,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4 GAME CARDS WITH CLASSIC CASINO ARTWORK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. VIDEO POKER CARD
                GameLobbyCard(
                    title = "VIDEO POKER",
                    subtitle = "Jacks or Better • 5-Card Draw • Double Up Gamble",
                    badge = "RTP 99.5%",
                    badgeColor = NeonCyan,
                    artRes = R.drawable.img_fire_poker_art,
                    gradientColors = listOf(Color(0xFF33110C), FireKernCard),
                    accentColor = GoldPrimary,
                    highlights = "Royal Flush 800:1 • Beat Dealer to Double Win",
                    isResumable = resumableGames.contains(GameType.VIDEO_POKER),
                    onClick = {
                        soundManager.playCardDeal()
                        onSelectGame(GameType.VIDEO_POKER)
                    },
                    testTag = "card_game_video_poker"
                )

                // 2. ROYALE SLOTS CARD
                GameLobbyCard(
                    title = "ROYALE SLOTS",
                    subtitle = "5-Reels • 9 Paylines • 10 Free Spins (2x Multiplier)",
                    badge = "MEGA JACKPOT",
                    badgeColor = GoldPrimary,
                    artRes = R.drawable.img_fire_slots_art,
                    gradientColors = listOf(Color(0xFF470C07), FireKernCard),
                    accentColor = FireKernEmber,
                    highlights = "Wild Diamonds • Scatters • Auto-Spin Turbo",
                    isResumable = resumableGames.contains(GameType.SLOTS),
                    onClick = {
                        soundManager.playSpinReels()
                        onSelectGame(GameType.SLOTS)
                    },
                    testTag = "card_game_slots"
                )

                // 3. BLACKJACK 21 CARD
                GameLobbyCard(
                    title = "BLACKJACK 21",
                    subtitle = "Classic Vegas Rules • 3:2 Natural Blackjack",
                    badge = "TABLE CLASSIC",
                    badgeColor = Color(0xFF22C55E),
                    artRes = R.drawable.img_fire_blackjack_art,
                    gradientColors = listOf(Color(0xFF2B100A), FireKernCard),
                    accentColor = GoldPrimary,
                    highlights = "Double Down • Split Pairs • Surrender • Dealer stands on 17",
                    isResumable = resumableGames.contains(GameType.BLACKJACK),
                    onClick = {
                        soundManager.playCardDeal()
                        onSelectGame(GameType.BLACKJACK)
                    },
                    testTag = "card_game_blackjack"
                )

                // 4. ROULETTE ROYALE CARD
                GameLobbyCard(
                    title = "ROULETTE ROYALE",
                    subtitle = "European 37-Pocket Wheel • Single Zero (0)",
                    badge = "35:1 PAYOUT",
                    badgeColor = NeonYellow,
                    artRes = R.drawable.img_fire_roulette_art,
                    gradientColors = listOf(Color(0xFF3D1408), FireKernCard),
                    accentColor = GoldLight,
                    highlights = "Interactive Felt • Straight & Outside Bets • Spin History",
                    isResumable = resumableGames.contains(GameType.ROULETTE),
                    onClick = {
                        soundManager.playBallBounce()
                        onSelectGame(GameType.ROULETTE)
                    },
                    testTag = "card_game_roulette"
                )

                // 5. ROYAL BACCARAT CARD
                GameLobbyCard(
                    title = "ROYAL BACCARAT",
                    subtitle = "Punto Banco • Player vs Banker • Tie 8:1",
                    badge = "PAIR 11:1",
                    badgeColor = Color(0xFF14F195),
                    artRes = null,
                    customCrestText = "9",
                    gradientColors = listOf(Color(0xFF0A2E22), FireKernCard),
                    accentColor = Color(0xFF14F195),
                    highlights = "Dragon Pair Side Bet • Macau Bead Road • Auto-Resume",
                    isResumable = resumableGames.contains(GameType.BACCARAT),
                    onClick = {
                        soundManager.playCardDeal()
                        onSelectGame(GameType.BACCARAT)
                    },
                    testTag = "card_game_baccarat"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Fair Play notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fire Kern Royale is a simulated social casino for adult entertainment only. No real money or prizes can be won or purchased. Practice or success at social gaming does not imply future success at real money gambling.",
                    color = FireKernAsh.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Daily Bonus Modal
        if (showBonusWheel) {
            DailyBonusWheelModal(
                profile = profile,
                dailyLoginRecord = dailyLoginRecord,
                onSpinAndClaim = { sliceIndex ->
                    val result = repository.claimDailyWheelSpin(sliceIndex)
                    if (result != null) {
                        onDailyWheelSpun(result)
                    }
                    result
                },
                onDismiss = { showBonusWheel = false },
                soundManager = soundManager
            )
        }

        // Stats Modal
        if (showStatsDialog) {
            StatsDialog(
                profile = profile,
                onResetStats = {
                    repository.resetStats()
                    showStatsDialog = false
                },
                onDismiss = { showStatsDialog = false }
            )
        }

        // Sound Settings Modal
        if (showSoundSettings) {
            SoundSettingsDialog(
                soundManager = soundManager,
                onDismiss = { showSoundSettings = false }
            )
        }

        // Daily Check-In Streak Modal
        if (showDailyCheckIn) {
            DailyCheckInDialog(
                status = repository.getDailyCheckInStatus(),
                onClaimReward = {
                    val result = repository.claimDailyCheckIn()
                    onDailyCheckInClaimed(result)
                    result
                },
                onSimulateNextDay = { repository.simulateNextDayForTesting() },
                onResetStreak = {
                    repository.resetCheckInForTesting()
                    onResetDailyCheckInCloud()
                },
                onDismiss = { showDailyCheckIn = false },
                soundManager = soundManager,
                dailyLoginRecord = dailyLoginRecord,
                isCloudSyncing = isDailyLoginSyncing
            )
        }
    }
}

@Composable
private fun GameLobbyCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    @DrawableRes artRes: Int?,
    gradientColors: List<Color>,
    accentColor: Color,
    highlights: String,
    isResumable: Boolean = false,
    customCrestText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (artRes != null) {
                    Image(
                        painter = painterResource(id = artRes),
                        contentDescription = "$title artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(listOf(accentColor.copy(alpha = 0.35f), FireKernObsidian))
                            )
                            .border(1.5.dp, accentColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customCrestText ?: "♠",
                            color = accentColor,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            color = GoldLight,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                color = badgeColor,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        if (isResumable) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FireKernFlame)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("${testTag}_resume_badge")
                            ) {
                                Text(
                                    text = "RESUME",
                                    color = Color.Black,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = FireKernAsh,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = highlights,
                        color = GoldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play Arrow
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play $title",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
