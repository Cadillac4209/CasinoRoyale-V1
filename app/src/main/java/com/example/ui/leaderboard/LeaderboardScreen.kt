package com.example.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundManager
import com.example.auth.signOutUser
import com.example.model.LeaderboardEntry
import com.example.model.PlayerProfile
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    profile: PlayerProfile,
    playerDisplayName: String,
    soundManager: SoundManager,
    onUpdateDisplayName: (String) -> Unit,
    onSyncNow: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val rankSummary by viewModel.playerRankSummary.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var draftName by remember(playerDisplayName) { mutableStateOf(playerDisplayName) }
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }

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
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fire Kern Header Top Bar
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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
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
                            .testTag("leaderboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Lobby",
                            tint = GoldLight
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GLOBAL HALL OF FLAME",
                            fontFamily = DisplayFontFamily,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "TOP 100 PLAYERS BY TOTAL XP",
                            color = FireKernFlame,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                soundManager.playButtonClick()
                                onSyncNow()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("leaderboard_sync_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = GoldPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync XP Rank",
                                    tint = GoldPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                soundManager.playButtonClick()
                                signOutUser(credentialManager, scope)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("leaderboard_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = FireKernAsh
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // YOUR GLOBAL RANK HERO CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp))
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(GoldPrimary, FireKernEmber, GoldDark)),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("leaderboard_player_rank_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = FireKernCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(FireKernEmberDark.copy(alpha = 0.85f), FireKernCard)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(listOf(GoldLight, GoldPrimary, FireKernEmber))
                                        )
                                        .border(2.dp, GoldLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rankSummary.rank?.let { "#$it" } ?: "--",
                                        fontFamily = MonoFontFamily,
                                        color = FireKernVoid,
                                        fontWeight = FontWeight.Black,
                                        fontSize = if ((rankSummary.rank ?: 0) >= 100) 14.sp else 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = playerDisplayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        IconButton(
                                            onClick = {
                                                soundManager.playButtonClick()
                                                draftName = playerDisplayName
                                                showEditNameDialog = true
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("leaderboard_edit_name_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Player Name",
                                                tint = GoldLight,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "LVL ${profile.level} • ${profile.vipTier.title.uppercase()}",
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "YOUR TOTAL XP",
                                    color = FireKernAsh,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${numberFormatter.format(profile.xp)} XP",
                                    fontFamily = MonoFontFamily,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FireKernVoid.copy(alpha = 0.7f))
                                .border(1.dp, FireKernCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (rankSummary.rank == 1) Icons.Default.EmojiEvents else Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = if (rankSummary.rank == 1) GoldPrimary else FireKernFlame,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val gapText = when {
                                    rankSummary.rank == 1 -> "You hold #1 Throne on the Fire Kern Leaderboard!"
                                    rankSummary.xpToNextRank != null && rankSummary.nextPlayerName != null ->
                                        "${numberFormatter.format(rankSummary.xpToNextRank)} XP needed to pass ${rankSummary.nextPlayerName}"
                                    rankSummary.rank != null -> "Play any casino game to climb higher than #${rankSummary.rank}"
                                    else -> "Syncing your XP rank with Firestore..."
                                }
                                Text(
                                    text = gapText,
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    soundManager.playButtonClick()
                                    onSyncNow()
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = FireKernVoid
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("leaderboard_sync_now_button")
                            ) {
                                Text(
                                    text = if (isSyncing) "SYNCING" else "SYNC NOW",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!syncMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = syncMessage!!,
                                color = FireKernEmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TOP 100 LEADERBOARD LIST
                when (val state = uiState) {
                    is LeaderboardUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GoldPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Fetching Global Top 100...",
                                    color = GoldLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    is LeaderboardUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(FireKernCard)
                                    .border(1.dp, FireKernEmber, RoundedCornerShape(16.dp))
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "LEADERBOARD UNAVAILABLE",
                                    fontFamily = DisplayFontFamily,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    color = FireKernAsh,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        soundManager.playButtonClick()
                                        onSyncNow()
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("RETRY SYNC", color = GoldLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is LeaderboardUiState.Success -> {
                        if (state.players.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No contenders yet — tap SYNC NOW to claim #1!",
                                    color = GoldLight,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("leaderboard_list"),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                itemsIndexed(
                                    items = state.players,
                                    key = { _, entry -> entry.userId }
                                ) { index, entry ->
                                    LeaderboardRowCard(
                                        rank = index + 1,
                                        entry = entry,
                                        isCurrentUser = entry.userId == viewModel.currentUserId,
                                        numberFormatter = numberFormatter
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = FireKernCard,
            title = {
                Text(
                    text = "PLAYER MONIKER",
                    fontFamily = DisplayFontFamily,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose how your name appears on the Global Top 100 Leaderboard (max 32 characters).",
                        color = FireKernAsh,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { if (it.length <= 32) draftName = it },
                        singleLine = true,
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = FireKernCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = FireKernAsh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_display_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = draftName.trim()
                        if (cleaned.isNotEmpty()) {
                            soundManager.playButtonClick()
                            onUpdateDisplayName(cleaned)
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = FireKernVoid
                    ),
                    modifier = Modifier.testTag("save_display_name_button")
                ) {
                    Text("SAVE & SYNC", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL", color = GoldLight)
                }
            }
        )
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    numberFormatter: NumberFormat
) {
    val podiumColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFCD7F32)
        else -> null
    }

    val cardBg = when {
        isCurrentUser -> FireKernEmberDark
        rank <= 3 -> FireKernCardElevated
        else -> FireKernCard
    }

    val borderColor = when {
        isCurrentUser -> GoldPrimary
        podiumColor != null -> podiumColor.copy(alpha = 0.7f)
        else -> FireKernCardBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isCurrentUser || rank <= 3) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("leaderboard_row_$rank"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        podiumColor ?: if (isCurrentUser) FireKernCrimson else FireKernVoid
                    )
                    .border(
                        1.dp,
                        podiumColor ?: if (isCurrentUser) GoldPrimary else FireKernCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (rank <= 3) "#$rank" else "$rank",
                    fontFamily = MonoFontFamily,
                    color = if (podiumColor != null) FireKernVoid else GoldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = if (rank >= 100) 11.sp else 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldPrimary)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = FireKernVoid,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                    if (rank == 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Rank 1 Leader",
                            tint = FireKernFlame,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "LVL ${entry.level} • ${entry.vipTitle} • ${entry.totalRoundsPlayed} Rounds",
                    color = FireKernAsh,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${numberFormatter.format(entry.totalXp)} XP",
                    fontFamily = MonoFontFamily,
                    color = podiumColor ?: GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (entry.bestWinStreak > 0) {
                    Text(
                        text = "${entry.bestWinStreak}x Streak",
                        color = FireKernFlame,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
