package com.example.ui.missions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundManager
import com.example.data.CasinoRepository
import com.example.model.MissionCategory
import com.example.model.MissionClaimResult
import com.example.model.MissionProgressState
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MissionsScreen(
    repository: CasinoRepository,
    soundManager: SoundManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val missionStates by repository.missionsRepository.missionStates.collectAsStateWithLifecycle()
    val claimableCount by repository.missionsRepository.claimableCount.collectAsStateWithLifecycle()
    var lastClaimBanner by remember { mutableStateOf<MissionClaimResult?>(null) }

    val dailyMissions = remember(missionStates) {
        missionStates.filter { it.mission.category == MissionCategory.DAILY }
    }
    val careerMissions = remember(missionStates) {
        missionStates.filter { it.mission.category == MissionCategory.CAREER }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FireKernEmberDark, FireKernObsidian, FireKernVoid)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = FireKernVoid.copy(alpha = 0.96f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            soundManager.playButtonClick()
                            onBackClick()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("missions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Lobby",
                            tint = GoldLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ROYALE QUESTS & TROPHIES",
                            color = GoldPrimary,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (claimableCount > 0) "$claimableCount REWARDS READY TO CLAIM" else "COMPLETE FLOOR GOALS TO EARN REWARDS",
                            color = if (claimableCount > 0) Color(0xFF14F195) else FireKernAsh,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = lastClaimBanner != null) {
                lastClaimBanner?.let { claimed ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .border(1.5.dp, Color(0xFF14F195), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2922))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF14F195),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CLAIMED: ${claimed.mission.title.uppercase()}",
                                    color = Color(0xFF14F195),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                val rewardSummary = buildString {
                                    append("+$${NumberFormat.getNumberInstance(Locale.US).format(claimed.chipsAwarded)} Chips")
                                    append(" • +${claimed.xpAwarded} XP")
                                    if (claimed.voidAwarded > 0L) append(" • +${claimed.voidAwarded} \$VOID")
                                }
                                Text(
                                    text = rewardSummary,
                                    color = GoldLight,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            MissionSectionHeader(
                title = "DAILY FLOOR QUESTS",
                subtitle = "Reset daily at midnight • Play games to progress",
                icon = Icons.Default.Whatshot,
                accentColor = FireKernEmber
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dailyMissions.forEach { state ->
                    MissionCardItem(
                        state = state,
                        onClaim = {
                            val result = repository.claimMissionReward(state.mission.id)
                            if (result != null) {
                                soundManager.playWin()
                                lastClaimBanner = result
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            MissionSectionHeader(
                title = "CAREER HALL TROPHIES",
                subtitle = "Lifetime milestones across all 5 floor games",
                icon = Icons.Default.EmojiEvents,
                accentColor = GoldPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                careerMissions.forEach { state ->
                    MissionCardItem(
                        state = state,
                        onClaim = {
                            val result = repository.claimMissionReward(state.mission.id)
                            if (result != null) {
                                soundManager.playBigWin()
                                lastClaimBanner = result
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MissionSectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = GoldLight,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp
            )
            Text(
                text = subtitle,
                color = FireKernAsh.copy(alpha = 0.78f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MissionCardItem(
    state: MissionProgressState,
    onClaim: () -> Unit
) {
    val mission = state.mission
    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
    val borderColor = when {
        state.isClaimable -> Color(0xFF14F195)
        state.isClaimed -> FireKernCardBorder.copy(alpha = 0.5f)
        else -> FireKernCardBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (state.isClaimable) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("mission_card_${mission.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isClaimable) FireKernCardElevated else FireKernCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FireKernEmberDark)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mission.badgeEmoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        color = GoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = mission.description,
                        color = FireKernAsh,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (state.isClaimed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF14F195).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF14F195).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "CLAIMED",
                            color = Color(0xFF14F195),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                } else if (state.isClaimable) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF14F195),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("claim_mission_${mission.id}")
                    ) {
                        Text("CLAIM", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (state.isCompleted) Color(0xFF14F195) else GoldPrimary,
                trackColor = FireKernVoid
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${numberFmt.format(state.clampedProgress)} / ${numberFmt.format(mission.targetValue)}",
                    color = FireKernAsh,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RewardBadgeChip(
                        text = "+$${numberFmt.format(mission.rewardChips)}",
                        color = GoldPrimary
                    )
                    RewardBadgeChip(
                        text = "+${mission.rewardXp} XP",
                        color = NeonCyan
                    )
                    if (mission.rewardVoid > 0L) {
                        RewardBadgeChip(
                            text = "+${mission.rewardVoid} \$VOID",
                            color = Color(0xFFC084FC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardBadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
