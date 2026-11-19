package com.example.ui.lobby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.model.CheckInDayReward
import com.example.model.CheckInStreakStatus
import com.example.model.DailyCheckInClaimResult
import com.example.model.DailyCheckInSchedule
import com.example.model.DailyLoginRecord
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoFeltMedium
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.CasinoSurfaceDark
import com.example.ui.theme.ChipPurple
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DailyCheckInDialog(
    status: CheckInStreakStatus,
    onClaimReward: () -> DailyCheckInClaimResult,
    onSimulateNextDay: () -> Unit,
    onResetStreak: () -> Unit,
    onDismiss: () -> Unit,
    soundManager: SoundManager,
    dailyLoginRecord: DailyLoginRecord? = null,
    isCloudSyncing: Boolean = false
) {
    var latestClaimResult by remember { mutableStateOf<DailyCheckInClaimResult?>(null) }
    var showClaimCelebration by remember { mutableStateOf(false) }

    // Live countdown timer ticker
    var remainingMillis by remember(status.millisUntilNextClaim) {
        mutableLongStateOf(status.millisUntilNextClaim)
    }

    LaunchedEffect(status.millisUntilNextClaim) {
        remainingMillis = status.millisUntilNextClaim
        while (remainingMillis > 0) {
            delay(1000L)
            remainingMillis = maxOf(0L, remainingMillis - 1000L)
        }
    }

    val countdownDisplay = remember(remainingMillis) {
        if (remainingMillis <= 0) "Ready to Claim!"
        else {
            val totalSeconds = remainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_gold")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, Brush.verticalGradient(listOf(GoldPrimary, CasinoSurfaceBorder)), RoundedCornerShape(22.dp))
                .testTag("daily_checkin_dialog"),
            color = CasinoSurfaceDark
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFFEA580C), CasinoRed))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CONSECUTIVE LOGIN BONUS",
                                    color = GoldPrimary,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Fire Kern VIP Check-In Calendar",
                                    color = FireKernAsh,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                soundManager.playButtonClick()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("daily_checkin_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = FireKernAsh
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Streak Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CasinoFeltDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CasinoSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${status.currentStreak}-DAY STREAK",
                                        color = if (status.currentStreak > 0) Color(0xFFF97316) else Color.Gray,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp
                                    )
                                    if (status.currentStreak >= 3) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🔥 ON FIRE",
                                            color = NeonYellow,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = if (status.isStreakBroken) "Streak reset! Check in today to start a new streak."
                                    else if (status.isClaimAvailableToday) "Check-in reward available today! Keep your streak alive."
                                    else "You checked in today! Streak secured.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "BEST RECORD",
                                    color = GoldDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${status.bestStreak} Days",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Firestore Last Login Timestamp & Cloud Sync Row
                    val lastLoginText = remember(dailyLoginRecord?.lastLoginAt, isCloudSyncing) {
                        val ts = dailyLoginRecord?.lastLoginAt
                        when {
                            ts != null -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
                                .format(ts.toDate())
                            isCloudSyncing -> "Syncing to Firestore..."
                            else -> "Awaiting Firestore sync"
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CasinoSurfaceCard)
                            .border(1.dp, CasinoSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("daily_checkin_firestore_sync_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIRESTORE LAST LOGIN",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Text(
                            text = lastLoginText,
                            color = GoldLight,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7-Day Rewards List (Days 1 to 6 in 2 columns, Day 7 as full-width Grand Prize)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Days 1-6 displayed in pairs of rows
                        val standardDays = DailyCheckInSchedule.days.take(6)
                        items(standardDays.chunked(2).size) { rowIndex ->
                            val pair = standardDays.chunked(2)[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (day in pair) {
                                    val isClaimed = if (!status.isClaimAvailableToday) {
                                        day.dayNumber <= status.targetDayInCycle
                                    } else {
                                        day.dayNumber < status.targetDayInCycle
                                    }
                                    val isReadyToday = status.isClaimAvailableToday && day.dayNumber == status.targetDayInCycle

                                    Box(modifier = Modifier.weight(1f)) {
                                        DayRewardCard(
                                            dayReward = day,
                                            isClaimed = isClaimed,
                                            isReadyToday = isReadyToday,
                                            pulseScale = if (isReadyToday) pulseScale else 1f
                                        )
                                    }
                                }
                            }
                        }

                        // Day 7 Grand Prize Card (full width)
                        item {
                            val day7 = DailyCheckInSchedule.days.last()
                            val isClaimed = if (!status.isClaimAvailableToday) {
                                day7.dayNumber <= status.targetDayInCycle
                            } else {
                                day7.dayNumber < status.targetDayInCycle
                            }
                            val isReadyToday = status.isClaimAvailableToday && day7.dayNumber == status.targetDayInCycle

                            Day7JackpotCard(
                                dayReward = day7,
                                isClaimed = isClaimed,
                                isReadyToday = isReadyToday,
                                pulseScale = if (isReadyToday) pulseScale else 1f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button or Countdown Indicator
                    if (status.isClaimAvailableToday) {
                        Button(
                            onClick = {
                                val result = onClaimReward()
                                latestClaimResult = result
                                showClaimCelebration = true
                                soundManager.playCheckInClaim(isJackpot = result.dayNumber == 7)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .scale(pulseScale)
                                .testTag("daily_checkin_claim_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLAIM DAY ${status.targetDayInCycle} REWARD",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Already claimed today banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CasinoSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CasinoSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "NEXT REWARD UNLOCKS IN",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            text = countdownDisplay,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Today Claimed",
                                        color = Color(0xFF22C55E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Developer Simulation Tools (Allows instantaneous multi-day testing of consecutive streaks)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEV PREVIEW SIMULATOR",
                            color = Color.DarkGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    soundManager.playButtonClick()
                                    onSimulateNextDay()
                                },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("sim_next_day_checkin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+24h (Next Day)", color = GoldLight, fontSize = 10.sp)
                            }

                            TextButton(
                                onClick = {
                                    soundManager.playButtonClick()
                                    onResetStreak()
                                },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("sim_reset_checkin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = CasinoRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reset", color = CasinoRed, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Reward Claim Celebration Overlay
                AnimatedVisibility(
                    visible = showClaimCelebration && latestClaimResult != null,
                    enter = fadeIn() + scaleIn(),
                    modifier = Modifier.matchParentSize()
                ) {
                    val result = latestClaimResult!!
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.92f))
                            .clickable {
                                showClaimCelebration = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldPrimary, GoldDark))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (result.dayNumber == 7) "GRAND JACKPOT CRATE!" else "DAY ${result.dayNumber} CLAIMED!",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )

                            Text(
                                text = "${result.streak}-Day Login Streak Active!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Claimed Rewards Breakdown Card
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CasinoSurfaceCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Free Casino Chips:", color = Color.LightGray, fontSize = 13.sp)
                                        Text(
                                            "+${NumberFormat.getNumberInstance(Locale.US).format(result.chipsAwarded)}",
                                            color = GoldLight,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Player XP:", color = Color.LightGray, fontSize = 13.sp)
                                        Text(
                                            "+${NumberFormat.getNumberInstance(Locale.US).format(result.xpAwarded)} XP",
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }

                                    if (result.voidTokensAwarded > 0L) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Void Token Boost:", color = Color.LightGray, fontSize = 13.sp)
                                            Text(
                                                "+${result.voidTokensAwarded} \$VOID",
                                                color = ChipPurple,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    if (result.isNewBestStreak) {
                                        Text(
                                            text = "🏆 NEW PERSONAL BEST STREAK!",
                                            color = NeonYellow,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    soundManager.playChipCollect()
                                    showClaimCelebration = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(44.dp)
                                    .testTag("daily_checkin_celebration_ok_button")
                            ) {
                                Text("COLLECT & PLAY", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRewardCard(
    dayReward: CheckInDayReward,
    isClaimed: Boolean,
    isReadyToday: Boolean,
    pulseScale: Float
) {
    val borderColor = when {
        isReadyToday -> GoldPrimary
        isClaimed -> Color(0xFF166534)
        else -> CasinoSurfaceBorder
    }

    val backgroundColor = when {
        isReadyToday -> CasinoFeltMedium
        isClaimed -> CasinoFeltDark.copy(alpha = 0.6f)
        else -> CasinoSurfaceCard
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isReadyToday) pulseScale else 1f)
            .testTag("day_reward_card_${dayReward.dayNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(if (isReadyToday) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day Header with Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAY ${dayReward.dayNumber}",
                    color = if (isReadyToday) GoldPrimary else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )

                if (isClaimed) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Claimed",
                            tint = Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                } else if (isReadyToday) {
                    Text(
                        text = "READY",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chips Reward
            Text(
                text = "+${NumberFormat.getNumberInstance(Locale.US).format(dayReward.chipsBonus)}",
                color = if (isClaimed) Color.Gray else GoldLight,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(
                text = "CHIPS",
                color = if (isClaimed) Color.DarkGray else GoldDark,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // XP and Void Tokens Pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${dayReward.xpBonus} XP",
                    color = if (isClaimed) Color.Gray else NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )

                if (dayReward.voidTokens > 0L) {
                    Text(
                        text = "+${dayReward.voidTokens} \$V",
                        color = if (isClaimed) Color.Gray else ChipPurple,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Day7JackpotCard(
    dayReward: CheckInDayReward,
    isClaimed: Boolean,
    isReadyToday: Boolean,
    pulseScale: Float
) {
    val borderColor = when {
        isReadyToday -> GoldLight
        isClaimed -> Color(0xFF166534)
        else -> GoldDark
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isReadyToday) pulseScale else 1f)
            .testTag("day_reward_card_7"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CasinoFeltMedium),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(GoldPrimary, GoldDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DAY 7 GRAND JACKPOT",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "👑 VIP CRATE",
                            color = NeonYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+50,000 CHIPS",
                            color = GoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "+6,000 XP",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "+5 \$VOID",
                            color = ChipPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isClaimed) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Claimed",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isReadyToday) {
                Text(
                    text = "READY!",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
