package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ResponsiblePlayManager
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan

data class GamblingSupportResource(
    val id: String,
    val name: String,
    val summary: String,
    val helplineNumber: String? = null,
    val dialUri: String? = null,
    val websiteUrl: String
)

object SupportResourcesCatalog {
    val resources: List<GamblingSupportResource> = listOf(
        GamblingSupportResource(
            id = "ncpg",
            name = "National Problem Gambling Helpline",
            summary = "24/7 confidential call, text & chat support via NCPG",
            helplineNumber = "1-800-522-4700",
            dialUri = "tel:18005224700",
            websiteUrl = "https://www.ncpgambling.org/help-treatment/"
        ),
        GamblingSupportResource(
            id = "samhsa",
            name = "SAMHSA National Helpline",
            summary = "Free, confidential 24/7 treatment referral and information",
            helplineNumber = "1-800-662-4357",
            dialUri = "tel:18006624357",
            websiteUrl = "https://www.samhsa.gov/find-help/national-helpline"
        ),
        GamblingSupportResource(
            id = "gamblers_anonymous",
            name = "Gamblers Anonymous",
            summary = "Peer fellowship meetings, recovery tools & local groups",
            websiteUrl = "https://www.gamblersanonymous.org/ga/"
        ),
        GamblingSupportResource(
            id = "gamble_aware",
            name = "GambleAware Support",
            summary = "Free advice, tools & self-assessment for safer play",
            websiteUrl = "https://www.gambleaware.org/"
        )
    )
}

@Composable
fun BreakReminderDialog(
    continuousPlayMs: Long,
    isThresholdReached: Boolean,
    onTakeBreak: () -> Unit,
    onContinuePlaying: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onContinuePlaying,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(NeonCyan, GoldPrimary, FireKernCardBorder)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("break_reminder_dialog"),
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F2E2A))
                            .border(1.5.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Responsible Play Break",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isThresholdReached) "TIME FOR A BREAK" else "RESPONSIBLE PLAY SUPPORT",
                            fontFamily = DisplayFontFamily,
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 0.8.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Continuous session: ${ResponsiblePlayManager.formatDuration(continuousPlayMs)}",
                                fontFamily = MonoFontFamily,
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FireKernEmberDark.copy(alpha = 0.7f))
                        .border(1.dp, GoldDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isThresholdReached) {
                            "You have been playing continuously for 60 minutes. Taking a short break helps keep play fun, mindful, and balanced. Step away for a few minutes, stretch, or check the confidential support resources below."
                        } else {
                            "Mindful play matters. Fire Kern Royale is a simulated social casino with no real-money stakes. If you or someone you know is struggling with gambling urges, free confidential help is available 24/7."
                        },
                        color = FireKernAsh,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = FireKernCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = FireKernFlame,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GAMBLING ADDICTION SUPPORT RESOURCES",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("break_support_resources_list")
                ) {
                    SupportResourcesCatalog.resources.forEach { resource ->
                        SupportResourceCard(
                            resource = resource,
                            onCallClick = { dialUri -> launchExternalIntent(context, Intent.ACTION_DIAL, dialUri) },
                            onWebsiteClick = { url -> launchExternalIntent(context, Intent.ACTION_VIEW, url) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        .testTag("break_dialog_take_break_button")
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

                OutlinedButton(
                    onClick = onContinuePlaying,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("break_dialog_continue_button")
                ) {
                    Text(
                        text = if (isThresholdReached) "RESET 60-MIN TIMER & RESUME" else "CLOSE & RETURN",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportResourceCard(
    resource: GamblingSupportResource,
    onCallClick: (String) -> Unit,
    onWebsiteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FireKernCardElevated)
            .border(1.dp, FireKernCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("support_resource_${resource.id}")
    ) {
        Text(
            text = resource.name,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = resource.summary,
            color = FireKernAsh,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        if (!resource.helplineNumber.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Helpline: ${resource.helplineNumber}",
                fontFamily = MonoFontFamily,
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (resource.dialUri != null) {
                Button(
                    onClick = { onCallClick(resource.dialUri) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = FireKernVoid
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("support_call_${resource.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${resource.name}",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CALL", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }

            OutlinedButton(
                onClick = { onWebsiteClick(resource.websiteUrl) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("support_web_${resource.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open ${resource.name} website",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("WEBSITE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

private fun launchExternalIntent(context: Context, action: String, uriString: String) {
    try {
        val intent = Intent(action, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore safely if no handler exists on the device/emulator
    }
}
