package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MonthlySpendingLimitState
import com.example.data.SpendingLimitAlertLevel
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SpendingLimitConfirmationDialog(
    limitState: MonthlySpendingLimitState,
    onConfirmContinue: () -> Unit,
    onAdjustLimit: () -> Unit,
    onTakeBreak: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val isReached = limitState.pendingAlert == SpendingLimitAlertLevel.LIMIT_REACHED
    val accentColor = if (isReached) CasinoRed else FireKernFlame
    val badgeBg = if (isReached) Color(0xFF3A1010) else Color(0xFF331B09)

    Dialog(
        onDismissRequest = onConfirmContinue,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(accentColor, GoldPrimary, FireKernCardBorder)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("spending_limit_confirmation_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = FireKernCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Monthly Spending Limit Alert",
                            tint = accentColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReached) "MONTHLY LIMIT REACHED" else "APPROACHING MONTHLY LIMIT",
                            fontFamily = DisplayFontFamily,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${limitState.monthKey} • ${limitState.usagePercent}% of $${numberFormatter.format(limitState.monthlyLimit)} limit",
                            fontFamily = MonoFontFamily,
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Meter Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FireKernCardElevated)
                        .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY VIRTUAL WAGERS",
                            color = GoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$${numberFormatter.format(limitState.monthlySpent)} / $${numberFormatter.format(limitState.monthlyLimit)}",
                            fontFamily = MonoFontFamily,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { limitState.usageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = FireKernCard
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isReached) {
                                "Threshold exceeded by $${numberFormatter.format((limitState.monthlySpent - limitState.monthlyLimit).coerceAtLeast(0L))}"
                            } else {
                                "Remaining before limit: $${numberFormatter.format(limitState.remainingBeforeLimit)}"
                            },
                            color = FireKernAsh,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Alert @ ${limitState.warningThresholdPercent}%",
                            fontFamily = MonoFontFamily,
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Safe Play Commitment Guidance Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FireKernEmberDark.copy(alpha = 0.7f))
                        .border(1.dp, GoldDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isReached) {
                                "You have reached 100% of your self-set monthly virtual credit limit. Confirming a pause or lowering wager stakes supports mindful, healthy habits. All Fire Kern Royale credits remain strictly simulated entertainment with zero real-money stakes."
                            } else {
                                "You have passed ${limitState.warningThresholdPercent}% of your self-set monthly virtual credit limit. Please take a moment to review your pace and confirm whether you want to pause for a break or continue playing mindfully."
                            },
                            color = FireKernAsh,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = FireKernCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Primary Healthy Pause Action
                Button(
                    onClick = onTakeBreak,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = FireKernVoid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spending_limit_take_break_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TAKE A BREAK (RETURN TO LOBBY)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Explicit Confirmation to Continue
                Button(
                    onClick = onConfirmContinue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReached) FireKernEmber else GoldPrimary,
                        contentColor = if (isReached) Color.White else FireKernVoid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spending_limit_confirm_continue_button")
                ) {
                    Text(
                        text = if (isReached) "CONFIRM LIMIT NOTICE & CONTINUE" else "CONFIRM & CONTINUE PLAYING",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Settings & Support Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAdjustLimit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("spending_limit_adjust_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADJUST LIMIT",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenSupport,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("spending_limit_support_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SUPPORT HELP",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
