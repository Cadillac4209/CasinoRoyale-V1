package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.PlayerProfile
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CasinoTopBar(
    profile: PlayerProfile,
    title: String? = null,
    isSoundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onOpenBonusWheel: () -> Unit = {},
    onEmergencyRefill: () -> Unit = {},
    onOpenVoidWallet: () -> Unit = {},
    onOpenSoundSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = FireKernVoid.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button or VIP level pill
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Lobby",
                            tint = GoldLight
                        )
                    }
                } else {
                    // Level & VIP Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FireKernCard)
                            .border(1.dp, FireKernCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(profile.vipTier.badgeColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${profile.level}",
                                fontFamily = MonoFontFamily,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.vipTier.title,
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center: Title
                if (title != null) {
                    Text(
                        text = title,
                        fontFamily = DisplayFontFamily,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 6.dp)
                    )
                }

                // Right: Balance pill, refill, sound toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Void Token pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2E1065), FireKernObsidian)
                                )
                            )
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .clickable { onOpenVoidWallet() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("topbar_void_token_pill")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14F195)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◎", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.voidWallet.totalVoid}",
                            fontFamily = MonoFontFamily,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "VOID",
                            color = Color(0xFFC084FC),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bankroll pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(FireKernEmberDark, FireKernCard)
                                )
                            )
                            .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$", color = FireKernVoid, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currencyFormatter.format(profile.bankroll),
                            fontFamily = MonoFontFamily,
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Low funds refill button
                    if (profile.bankroll < 200L) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(GoldPrimary)
                                .clickable { onEmergencyRefill() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("emergency_refill_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Refill Chips",
                                    tint = FireKernVoid,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "+$2.5K",
                                    color = FireKernVoid,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Sound Toggle
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSoundEnabled) "Mute Sound" else "Unmute Sound",
                            tint = if (isSoundEnabled) GoldLight else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Audio Settings dialog opener
                    if (onOpenSoundSettings != null) {
                        IconButton(
                            onClick = onOpenSoundSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sound_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Audio Suite Settings",
                                tint = FireKernFlame,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Bottom subtle molten XP progress bar
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { profile.xpProgressInLevel },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = GoldPrimary,
                trackColor = FireKernCardBorder,
            )
        }
    }
}
