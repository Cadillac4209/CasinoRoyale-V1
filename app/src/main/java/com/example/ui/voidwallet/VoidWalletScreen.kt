package com.example.ui.voidwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.CasinoRepository
import com.example.model.PlayerProfile
import com.example.model.VoidPowerTier
import com.example.model.VoidTransaction
import com.example.model.VoidTxType
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.ChipPurple
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoidWalletScreen(
    repository: CasinoRepository,
    soundManager: SoundManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by repository.profile.collectAsState()
    val wallet = profile.voidWallet
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val solanaGradient = listOf(
        Color(0xFF9945FF), // Solana Purple
        Color(0xFF14F195), // Solana Green/Cyan
        Color(0xFF00C2FF)  // Cyan
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF160B24),
                        FireKernObsidian,
                        FireKernVoid
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color(0xFF0B091E).copy(alpha = 0.95f),
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
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("void_wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SOLANA VOID WALLET",
                            color = GoldPrimary,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14F195))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOLANA MAINNET // ACTIVE POWER VAULT",
                                color = Color(0xFF14F195),
                                fontFamily = MonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { soundManager.isSoundEnabled = !soundManager.isSoundEnabled },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (soundManager.isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (soundManager.isSoundEnabled) GoldLight else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SOLANA HARDWARE-STYLE CYBER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(solanaGradient),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120E2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header row: Solana badge + Address copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E174A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "◎ SOLANA",
                                color = Color(0xFF14F195),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\$VOID",
                                color = Color(0xFF9945FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }

                        // Copyable Solana Address Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B1936))
                                .clickable {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Solana Void Wallet",
                                        wallet.solanaAddress
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    soundManager.playChipClick()
                                    Toast
                                        .makeText(
                                            context,
                                            "Solana Address Copied!",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("copy_solana_address_button")
                        ) {
                            Text(
                                text = "${wallet.solanaAddress.take(4)}...${wallet.solanaAddress.takeLast(4)}",
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Balance Display
                    Text(
                        text = "TOTAL VOID POWER ASSETS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${wallet.totalVoid}",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "\$VOID",
                            color = Color(0xFF9945FF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // Chip Value Equivalent
                        val chipsEquivalent = wallet.totalVoid * 1000L
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "WORTH",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\$${NumberFormat.getNumberInstance(Locale.US).format(chipsEquivalent)} Chips",
                                color = GoldLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Split Balance: Available vs Staked
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C0A1F))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE FOR USE",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${wallet.balance} \$VOID",
                                color = NeonCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "STAKED IN POWER VAULT",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${wallet.stakedAmount} \$VOID",
                                color = Color(0xFF14F195),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Power Tier Badge
                    val activeTier = wallet.activePowerTier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(activeTier.badgeColorHex).copy(alpha = 0.25f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Color(activeTier.badgeColorHex).copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(activeTier.badgeColorHex),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeTier.title.uppercase(),
                                    color = Color(activeTier.badgeColorHex),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${((activeTier.xpMultiplier - 1f) * 100).toInt()}% XP BOOST",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = activeTier.perkDescription,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB SELECTOR: STAKING VAULT | CHIP FORGE | TRANSACTIONS
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = Color(0xFF110E2A),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF14F195),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        soundManager.playButtonClick()
                        selectedTab = 0
                    },
                    modifier = Modifier.testTag("void_tab_stake"),
                    text = {
                        Text(
                            "POWER STAKE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedTab == 0) Color(0xFF14F195) else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        soundManager.playButtonClick()
                        selectedTab = 1
                    },
                    modifier = Modifier.testTag("void_tab_forge"),
                    text = {
                        Text(
                            "CHIP FORGE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedTab == 1) Color(0xFF14F195) else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        soundManager.playButtonClick()
                        selectedTab = 2
                    },
                    modifier = Modifier.testTag("void_tab_txs"),
                    text = {
                        Text(
                            "SOLANA TXS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedTab == 2) Color(0xFF14F195) else Color.Gray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB CONTENT
            when (selectedTab) {
                0 -> PowerStakingSection(
                    wallet = wallet,
                    soundManager = soundManager,
                    onStake = { amount ->
                        if (repository.stakeVoid(amount)) {
                            soundManager.playWin()
                        }
                    },
                    onUnstake = { amount ->
                        if (repository.unstakeVoid(amount)) {
                            soundManager.playChipClick()
                        }
                    }
                )

                1 -> ChipForgeSection(
                    profile = profile,
                    wallet = wallet,
                    soundManager = soundManager,
                    onSwapChipsToVoid = { chips ->
                        if (repository.swapChipsToVoid(chips)) {
                            soundManager.playBigWin()
                        }
                    },
                    onSwapVoidToChips = { voidTokens ->
                        if (repository.swapVoidToChips(voidTokens)) {
                            soundManager.playWin()
                        }
                    }
                )

                2 -> SolanaTransactionLedgerSection(
                    transactions = wallet.transactions
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // HOW TO EARN $VOID BANNER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121428))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOW TO MINT & EARN \$VOID TOKENS",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Level Up Milestone: +5 \$VOID automatically credited on each level-up.\n" +
                                "• Floor Big Wins: Wins of \$300+ drop +1 \$VOID; \$1,000+ drop +3 \$VOID.\n" +
                                "• Slot Jackpots & Royal Flush: Massive +10 \$VOID jackpot drops.\n" +
                                "• High-Roller Forge: Convert excess casino chips into \$VOID at 1,000:1 ratio.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PowerStakingSection(
    wallet: com.example.model.VoidWallet,
    soundManager: SoundManager,
    onStake: (Long) -> Unit,
    onUnstake: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SOLANA POWER VAULT TIERS",
            color = GoldLight,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Stake your \$VOID to permanently unlock XP multipliers and daily wheel boosts across all 4 casino games. Unstake anytime without penalty.",
            color = Color.Gray,
            fontSize = 11.sp
        )

        // Tiers list
        val tiers = listOf(
            VoidPowerTier.QUANTUM,
            VoidPowerTier.SUPERNOVA,
            VoidPowerTier.SINGULARITY
        )

        tiers.forEach { tier ->
            val isCurrentOrHigher = wallet.stakedAmount >= tier.requiredStake
            val isExact = wallet.activePowerTier == tier

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isExact) 2.dp else 1.dp,
                        color = if (isExact) Color(tier.badgeColorHex) else Color(tier.badgeColorHex).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExact) Color(0xFF1D173D) else Color(0xFF0F1224)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(tier.badgeColorHex).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentOrHigher) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color(tier.badgeColorHex),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tier.title,
                                color = Color(tier.badgeColorHex),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            if (isExact) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(tier.badgeColorHex))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Requires ${tier.requiredStake} \$VOID Staked",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = tier.perkDescription,
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Stake to this tier button
                    if (wallet.stakedAmount < tier.requiredStake) {
                        val needed = tier.requiredStake - wallet.stakedAmount
                        val canAfford = wallet.balance >= needed
                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                onStake(needed)
                            },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(tier.badgeColorHex),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("stake_to_${tier.name.lowercase()}")
                        ) {
                            Text("+$needed", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Quick Staking Action Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13112E))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "VAULT CONTROLS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            soundManager.playButtonClick()
                            onStake(5L)
                        },
                        enabled = wallet.balance >= 5L,
                        colors = ButtonDefaults.buttonColors(containerColor = ChipPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stake_quick_5")
                    ) {
                        Text("STAKE 5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            soundManager.playButtonClick()
                            onStake(wallet.balance)
                        },
                        enabled = wallet.balance > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14F195)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stake_all_button")
                    ) {
                        Text("STAKE ALL", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            soundManager.playButtonClick()
                            onUnstake(wallet.stakedAmount)
                        },
                        enabled = wallet.stakedAmount > 0L,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("unstake_all_button")
                    ) {
                        Text("UNSTAKE", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipForgeSection(
    profile: PlayerProfile,
    wallet: com.example.model.VoidWallet,
    soundManager: SoundManager,
    onSwapChipsToVoid: (Long) -> Unit,
    onSwapVoidToChips: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "LIQUIDITY FORGE (1 \$VOID = \$1,000 CHIPS)",
            color = GoldLight,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )

        // CHIPS TO VOID CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2E))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FORGE CHIPS → \$VOID TOKENS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Convert high-limit chip earnings into permanent \$VOID crypto tokens to stake for game power.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val swapAmounts = listOf(1000L, 5000L, 10000L)
                    swapAmounts.forEach { chips ->
                        val voidTokens = chips / 1000L
                        val canAfford = profile.bankroll >= chips
                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                onSwapChipsToVoid(chips)
                            },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) NeonCyan else Color.DarkGray,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("swap_chips_${chips}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\$$chips", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("+$voidTokens \$VOID", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // VOID TO CHIPS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1226))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONVERT \$VOID → CASINO CHIPS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cash out available \$VOID tokens for immediate high-roller table chips if you're running low.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val voidAmounts = listOf(1L, 5L, 10L)
                    voidAmounts.forEach { voidAmount ->
                        val chipsToReceive = voidAmount * 1000L
                        val canAfford = wallet.balance >= voidAmount
                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                onSwapVoidToChips(voidAmount)
                            },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) GoldPrimary else Color.DarkGray,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("swap_void_${voidAmount}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("-$voidAmount \$VOID", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("+\$$chipsToReceive", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolanaTransactionLedgerSection(
    transactions: List<VoidTransaction>
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOLANA ON-CHAIN LEDGER",
                color = GoldLight,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "${transactions.size} TRANSACTIONS",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (transactions.isEmpty()) {
            Text(
                text = "No recorded transactions yet. Play games or forge tokens to initiate Solana ledger events.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            transactions.forEach { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1221))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tx.type.isCredit) Color(0xFF14F195).copy(alpha = 0.2f)
                                    else Color(0xFF9945FF).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.type.isCredit) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (tx.type.isCredit) Color(0xFF14F195) else Color(0xFF9945FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.type.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = tx.note,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Sig: ${tx.txHash.take(6)}...${tx.txHash.takeLast(6)} • ${dateFormat.format(Date(tx.timestamp))}",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${if (tx.type.isCredit) "+" else "-"}${tx.amount} \$VOID",
                            color = if (tx.type.isCredit) Color(0xFF14F195) else Color(0xFF9945FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
