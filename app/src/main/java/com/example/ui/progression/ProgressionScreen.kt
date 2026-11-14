package com.example.ui.progression

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.audio.SoundManager
import com.example.data.CasinoRepository
import com.example.model.BlackjackVariant
import com.example.model.CardBackTheme
import com.example.model.FeltTheme
import com.example.model.LevelMilestone
import com.example.model.PlayerProfile
import com.example.model.PokerVariant
import com.example.model.ProgressionManager
import com.example.model.RouletteVariant
import com.example.model.SlotsVariant
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
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
fun ProgressionScreen(
    repository: CasinoRepository,
    soundManager: SoundManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by repository.profile.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("LEVEL ROAD", "WARDROBE", "VARIANTS & LIMITS")

    val nextReq = ProgressionManager.xpRequiredForNextLevel(profile.level)
    val currentProgress = profile.xpProgressInLevel

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FireKernEmberDark, FireKernObsidian, FireKernVoid)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        soundManager.playButtonClick()
                        onBackClick()
                    },
                    modifier = Modifier.testTag("progression_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GoldLight
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "VIP PROGRESSION & CLUB",
                        color = GoldPrimary,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Level up to unlock game variants, betting limits & cosmetics",
                        color = FireKernAsh,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(FireKernCardElevated, FireKernCard)
                        )
                    )
                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(profile.vipTier.badgeColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${profile.level}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LEVEL ${profile.level}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = profile.vipTier.title,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Max Bet Limit Tag
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TABLE LIMIT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$${profile.maxBetAllowed}",
                                color = GoldPrimary,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // XP Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL XP: ${profile.xp}",
                            color = FireKernAsh,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(currentProgress * 100).toInt()}% to Level ${profile.level + 1}",
                            color = GoldLight,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldPrimary,
                        trackColor = FireKernCardBorder,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = GoldPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            soundManager.playButtonClick()
                            selectedTab = index
                        },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) GoldPrimary else FireKernAsh.copy(alpha = 0.7f),
                                fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> MilestoneRoadView(profile = profile)
                    1 -> WardrobeCosmeticsView(
                        profile = profile,
                        onSelectFelt = {
                            soundManager.playChipClick()
                            repository.selectFelt(it)
                        },
                        onSelectCardBack = {
                            soundManager.playCardFlip()
                            repository.selectCardBack(it)
                        }
                    )
                    2 -> VariantsAndLimitsView(
                        profile = profile,
                        onSelectPoker = {
                            soundManager.playButtonClick()
                            repository.selectPokerVariant(it)
                        },
                        onSelectSlots = {
                            soundManager.playButtonClick()
                            repository.selectSlotsVariant(it)
                        },
                        onSelectBlackjack = {
                            soundManager.playButtonClick()
                            repository.selectBlackjackVariant(it)
                        },
                        onSelectRoulette = {
                            soundManager.playButtonClick()
                            repository.selectRouletteVariant(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneRoadView(profile: PlayerProfile) {
    val milestones = ProgressionManager.MILESTONES

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        milestones.forEach { m ->
            val isUnlocked = profile.level >= m.level
            val isNext = profile.level + 1 == m.level

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isUnlocked -> Color(0xFF064E3B).copy(alpha = 0.45f)
                            isNext -> Color(0xFF78350F).copy(alpha = 0.45f)
                            else -> FireKernCard
                        }
                    )
                    .border(
                        width = if (isNext) 2.dp else 1.dp,
                        color = when {
                            isUnlocked -> Color(0xFF10B981)
                            isNext -> GoldPrimary
                            else -> FireKernCardBorder
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Circle Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isUnlocked -> Color(0xFF10B981)
                                isNext -> GoldPrimary
                                else -> Color(0xFF334155)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L${m.level}",
                        color = if (isUnlocked || isNext) Color.Black else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Milestone Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = m.rewardIconEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = m.title,
                            color = if (isUnlocked) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = m.description,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Icon / Pill
                if (isUnlocked) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Unlocked",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WardrobeCosmeticsView(
    profile: PlayerProfile,
    onSelectFelt: (FeltTheme) -> Unit,
    onSelectCardBack: (CardBackTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Table Felt Themes Section
        Text(
            text = "TABLE FELT THEMES",
            color = GoldPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )

        FeltTheme.values().forEach { theme ->
            val isUnlocked = profile.isFeltUnlocked(theme)
            val isEquipped = profile.selectedFelt == theme

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FireKernCard)
                    .border(
                        width = if (isEquipped) 2.dp else 1.dp,
                        color = if (isEquipped) GoldPrimary else FireKernCardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(theme.primaryColor, theme.mediumColor)
                            )
                        )
                        .border(1.dp, GoldLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = theme.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "Unlocks at Level ${theme.requiredLevel}",
                            color = NeonYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSelectFelt(theme) },
                    enabled = isUnlocked && !isEquipped,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEquipped) GoldPrimary else Color(0xFF1E293B),
                        contentColor = if (isEquipped) Color.Black else GoldLight,
                        disabledContainerColor = if (isEquipped) GoldPrimary else Color(0xFF1E293B).copy(alpha = 0.5f),
                        disabledContentColor = if (isEquipped) Color.Black else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("equip_felt_${theme.id}")
                ) {
                    Text(
                        text = if (isEquipped) "EQUIPPED" else if (isUnlocked) "EQUIP" else "LOCKED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card Back Designs Section
        Text(
            text = "PLAYING CARD DECKS",
            color = GoldPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )

        CardBackTheme.values().forEach { theme ->
            val isUnlocked = profile.isCardBackUnlocked(theme)
            val isEquipped = profile.selectedCardBack == theme

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FireKernCard)
                    .border(
                        width = if (isEquipped) 2.dp else 1.dp,
                        color = if (isEquipped) GoldPrimary else FireKernCardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature card back preview
                PlayingCardView(
                    card = null,
                    width = 36.dp,
                    cardBackTheme = theme
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = theme.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "Unlocks at Level ${theme.requiredLevel}",
                            color = NeonYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSelectCardBack(theme) },
                    enabled = isUnlocked && !isEquipped,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEquipped) GoldPrimary else Color(0xFF1E293B),
                        contentColor = if (isEquipped) Color.Black else GoldLight,
                        disabledContainerColor = if (isEquipped) GoldPrimary else Color(0xFF1E293B).copy(alpha = 0.5f),
                        disabledContentColor = if (isEquipped) Color.Black else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("equip_card_${theme.id}")
                ) {
                    Text(
                        text = if (isEquipped) "EQUIPPED" else if (isUnlocked) "EQUIP" else "LOCKED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun VariantsAndLimitsView(
    profile: PlayerProfile,
    onSelectPoker: (PokerVariant) -> Unit,
    onSelectSlots: (SlotsVariant) -> Unit,
    onSelectBlackjack: (BlackjackVariant) -> Unit,
    onSelectRoulette: (RouletteVariant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Roller Limits Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FireKernCardElevated)
                .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "ACTIVE BETTING LIMITS",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Maximum Single Bet: $${profile.maxBetAllowed}\n• Highest Available Chip: $${if (profile.level >= 15) "25,000" else if (profile.level >= 7) "5,000" else if (profile.level >= 4) "2,500" else "1,000"}\n• Daily Wheel Multiplier: ${profile.vipTier.bonusMultiplier}x",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Text(
            text = "VIDEO POKER VARIANTS",
            color = GoldLight,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        PokerVariant.values().forEach { variant ->
            val unlocked = profile.isPokerVariantUnlocked(variant)
            val selected = profile.selectedPokerVariant == variant
            VariantCard(
                title = variant.displayName,
                description = variant.description,
                requiredLevel = variant.requiredLevel,
                isUnlocked = unlocked,
                isSelected = selected,
                onSelect = { onSelectPoker(variant) },
                testTag = "var_poker_${variant.id}"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "ROYALE SLOTS VARIANTS",
            color = GoldLight,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        SlotsVariant.values().forEach { variant ->
            val unlocked = profile.isSlotsVariantUnlocked(variant)
            val selected = profile.selectedSlotsVariant == variant
            VariantCard(
                title = variant.displayName,
                description = variant.description,
                requiredLevel = variant.requiredLevel,
                isUnlocked = unlocked,
                isSelected = selected,
                onSelect = { onSelectSlots(variant) },
                testTag = "var_slots_${variant.id}"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "BLACKJACK VARIANTS",
            color = GoldLight,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        BlackjackVariant.values().forEach { variant ->
            val unlocked = profile.isBlackjackVariantUnlocked(variant)
            val selected = profile.selectedBlackjackVariant == variant
            VariantCard(
                title = variant.displayName,
                description = variant.description,
                requiredLevel = variant.requiredLevel,
                isUnlocked = unlocked,
                isSelected = selected,
                onSelect = { onSelectBlackjack(variant) },
                testTag = "var_bj_${variant.id}"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "ROULETTE VARIANTS",
            color = GoldLight,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        RouletteVariant.values().forEach { variant ->
            val unlocked = profile.isRouletteVariantUnlocked(variant)
            val selected = profile.selectedRouletteVariant == variant
            VariantCard(
                title = variant.displayName,
                description = variant.description,
                requiredLevel = variant.requiredLevel,
                isUnlocked = unlocked,
                isSelected = selected,
                onSelect = { onSelectRoulette(variant) },
                testTag = "var_rl_${variant.id}"
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun VariantCard(
    title: String,
    description: String,
    requiredLevel: Int,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FireKernCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldPrimary else FireKernCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            if (!isUnlocked) {
                Text(
                    text = "Requires Level $requiredLevel",
                    color = NeonYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSelect,
            enabled = isUnlocked && !isSelected,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) GoldPrimary else Color(0xFF1E293B),
                contentColor = if (isSelected) Color.Black else GoldLight,
                disabledContainerColor = if (isSelected) GoldPrimary else Color(0xFF1E293B).copy(alpha = 0.5f),
                disabledContentColor = if (isSelected) Color.Black else Color.Gray
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag(testTag)
        ) {
            Text(
                text = if (isSelected) "ACTIVE" else if (isUnlocked) "ACTIVATE" else "LOCKED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
