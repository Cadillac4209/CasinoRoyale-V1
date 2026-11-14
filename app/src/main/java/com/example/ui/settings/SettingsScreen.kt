package com.example.ui.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundManager
import com.example.audio.SoundProfilePreset
import com.example.auth.signOutUser
import com.example.data.CasinoRepository
import com.example.model.GameSpeed
import com.example.model.LeaderboardEntry
import com.example.model.PlayerProfile
import com.example.ui.leaderboard.LeaderboardViewModel
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: CasinoRepository,
    soundManager: SoundManager,
    leaderboardViewModel: LeaderboardViewModel,
    profile: PlayerProfile,
    playerDisplayName: String,
    onUpdateDisplayName: (String) -> Unit,
    onSyncNow: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameSpeed by repository.gameSpeed.collectAsStateWithLifecycle()
    val currentPlayerEntry by leaderboardViewModel.currentPlayerEntry.collectAsStateWithLifecycle()
    val currentEntryError by leaderboardViewModel.currentEntryError.collectAsStateWithLifecycle()
    val rankSummary by leaderboardViewModel.playerRankSummary.collectAsStateWithLifecycle()
    val isSyncing by leaderboardViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by leaderboardViewModel.syncMessage.collectAsStateWithLifecycle()

    var musicEnabled by remember { mutableStateOf(soundManager.isMusicEnabled) }
    var musicVolume by remember { mutableFloatStateOf(soundManager.musicVolume) }
    var soundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var sfxVolume by remember { mutableFloatStateOf(soundManager.sfxVolume) }
    var masterVolume by remember { mutableFloatStateOf(soundManager.masterVolume) }
    var hapticsEnabled by remember { mutableStateOf(soundManager.isHapticsEnabled) }
    var activePreset by remember { mutableStateOf(soundManager.activePreset) }

    var draftDisplayName by remember(playerDisplayName) { mutableStateOf(playerDisplayName) }
    var nameSavedNotice by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseUser = remember { Firebase.auth.currentUser }
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
            // Top Bar
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
                            .testTag("settings_back_button")
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
                            text = "CASINO SETTINGS",
                            fontFamily = DisplayFontFamily,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "AUDIO • SPEED • FIRESTORE ACCOUNT",
                            color = FireKernFlame,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Scrollable Settings Body
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: AUDIO & MUSIC
                SettingsSectionCard(
                    title = "AUDIO & LOUNGE MUSIC",
                    subtitle = "Background ambient loop, classic SFX, and tactile feedback",
                    icon = Icons.Default.Tune,
                    accentColor = GoldPrimary,
                    modifier = Modifier.testTag("settings_audio_section")
                ) {
                    // Music Toggle
                    SettingsToggleRow(
                        icon = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        iconTint = if (musicEnabled) NeonCyan else Color.Gray,
                        title = "Lounge Background Music",
                        subtitle = "Warm Fire Kern ambient chord progression during play",
                        checked = musicEnabled,
                        onCheckedChange = { enabled ->
                            musicEnabled = enabled
                            soundManager.updateSettings(musicEnabled = enabled)
                            soundManager.playButtonClick()
                        },
                        trackColor = NeonCyan,
                        switchTag = "settings_music_toggle"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "MUSIC VOLUME: ${(musicVolume * 100).toInt()}%",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = musicVolume,
                        onValueChange = { vol ->
                            musicVolume = vol
                            soundManager.updateSettings(musicVol = vol)
                        },
                        enabled = musicEnabled && activePreset != SoundProfilePreset.MUTED,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = FireKernCardElevated
                        ),
                        modifier = Modifier.testTag("settings_music_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = FireKernCardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // SFX Toggle
                    SettingsToggleRow(
                        icon = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        iconTint = if (soundEnabled) GoldPrimary else Color.Gray,
                        title = "Game Sound Effects",
                        subtitle = "Chips, card shuffles, slot reels, and win streak fanfares",
                        checked = soundEnabled,
                        onCheckedChange = { enabled ->
                            soundEnabled = enabled
                            soundManager.updateSettings(soundEnabled = enabled)
                            if (enabled) soundManager.playChipClick(100L)
                        },
                        trackColor = GoldPrimary,
                        switchTag = "settings_sfx_toggle"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "SFX VOLUME: ${(sfxVolume * 100).toInt()}%",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = sfxVolume,
                        onValueChange = { vol ->
                            sfxVolume = vol
                            soundManager.updateSettings(sfxVol = vol)
                        },
                        enabled = soundEnabled && activePreset != SoundProfilePreset.MUTED,
                        colors = SliderDefaults.colors(
                            thumbColor = FireKernFlame,
                            activeTrackColor = FireKernFlame,
                            inactiveTrackColor = FireKernCardElevated
                        ),
                        modifier = Modifier.testTag("settings_sfx_volume_slider")
                    )

                    Text(
                        text = "MASTER OUTPUT: ${(masterVolume * 100).toInt()}%",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = masterVolume,
                        onValueChange = { vol ->
                            masterVolume = vol
                            soundManager.updateSettings(masterVol = vol)
                        },
                        enabled = activePreset != SoundProfilePreset.MUTED,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldPrimary,
                            activeTrackColor = GoldPrimary,
                            inactiveTrackColor = FireKernCardElevated
                        ),
                        modifier = Modifier.testTag("settings_master_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = FireKernCardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Haptics Toggle
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        iconTint = if (hapticsEnabled) FireKernEmber else Color.Gray,
                        title = "Tactile Haptics",
                        subtitle = "Vibration pulses for chip clicks, deals, and jackpots",
                        checked = hapticsEnabled,
                        onCheckedChange = { enabled ->
                            hapticsEnabled = enabled
                            soundManager.updateSettings(hapticsEnabled = enabled)
                            if (enabled) soundManager.playChipClick(100L)
                        },
                        trackColor = FireKernFlame,
                        switchTag = "settings_haptics_toggle"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound Profile Presets
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SOUND PROFILE PRESET",
                            color = GoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoundProfilePreset.values().toList().chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { preset ->
                                    val isSelected = activePreset == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) GoldPrimary else FireKernCardElevated)
                                            .border(
                                                1.dp,
                                                if (isSelected) GoldLight else FireKernCardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                activePreset = preset
                                                soundManager.updateSettings(preset = preset)
                                                soundManager.playChipClick()
                                            }
                                            .testTag("settings_preset_${preset.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            color = if (isSelected) FireKernVoid else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: GAMEPLAY SPEED
                SettingsSectionCard(
                    title = "GAMEPLAY SPEED",
                    subtitle = "Applies to Blackjack deals, Slots spins, Video Poker draws & Roulette wheels",
                    icon = Icons.Default.Speed,
                    accentColor = FireKernFlame,
                    modifier = Modifier.testTag("settings_speed_section")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameSpeed.entries.forEach { speed ->
                            val isSelected = gameSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) FireKernEmberDark else FireKernCardElevated
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) FireKernFlame else FireKernCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        repository.setGameSpeed(speed)
                                        soundManager.playChipClick(100L)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("settings_speed_${speed.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) FireKernFlame else FireKernCard)
                                            .border(
                                                1.dp,
                                                if (isSelected) GoldLight else FireKernCardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = speed.badge,
                                            fontFamily = MonoFontFamily,
                                            color = if (isSelected) FireKernVoid else GoldLight,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = speed.label.uppercase(),
                                            color = if (isSelected) GoldPrimary else Color.White,
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = speed.description,
                                            color = FireKernAsh,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "${speed.label} Selected",
                                        tint = FireKernFlame,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = FireKernCardBorder,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: FIRESTORE ACCOUNT & PROFILE
                SettingsSectionCard(
                    title = "FIRESTORE ACCOUNT PROFILE",
                    subtitle = "Google Sign-In identity & live cloud leaderboard document",
                    icon = Icons.Default.AccountCircle,
                    accentColor = NeonCyan,
                    modifier = Modifier.testTag("settings_account_section")
                ) {
                    val syncedEntry: LeaderboardEntry? = currentPlayerEntry
                    val activeDisplayName = syncedEntry?.displayName ?: playerDisplayName
                    val userEmail = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: "Google Authenticated Account"
                    val userUid = firebaseUser?.uid ?: "Unavailable"

                    // Auth Identity Summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FireKernCardElevated)
                            .border(1.dp, FireKernCardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            .background(
                                                Brush.radialGradient(listOf(GoldPrimary, FireKernCrimson))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeDisplayName.take(1).uppercase(),
                                            color = FireKernVoid,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = activeDisplayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = userEmail,
                                            color = FireKernAsh,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F2E2A))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (syncedEntry != null) "SYNCED" else "PENDING",
                                            color = NeonCyan,
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = FireKernCardBorder)

                            AccountMetaRow(label = "Sign-In Provider", value = "Google Sign-In")
                            AccountMetaRow(label = "Account UID", value = userUid, monospace = true)
                            AccountMetaRow(
                                label = "Global Rank",
                                value = rankSummary.rank?.let { "#$it" } ?: "Calculating..."
                            )
                            AccountMetaRow(
                                label = "VIP Standing",
                                value = "Level ${syncedEntry?.level ?: profile.level} • ${syncedEntry?.vipTitle ?: profile.vipTier.title}"
                            )
                            AccountMetaRow(
                                label = "Firestore Total XP",
                                value = "${numberFormatter.format(syncedEntry?.totalXp ?: profile.xp)} XP",
                                monospace = true
                            )
                            AccountMetaRow(
                                label = "Cloud Best Streak / Win",
                                value = "${syncedEntry?.bestWinStreak ?: profile.stats.bestWinStreak} streak • $${numberFormatter.format(syncedEntry?.biggestWin ?: profile.stats.biggestWin)}",
                                monospace = true
                            )
                            AccountMetaRow(
                                label = "Total Rounds Synced",
                                value = numberFormatter.format(syncedEntry?.totalRoundsPlayed ?: profile.stats.totalRoundsPlayed),
                                monospace = true
                            )
                            AccountMetaRow(
                                label = "Last Cloud Update",
                                value = formatFirestoreTimestamp(syncedEntry?.updatedAt)
                            )
                        }
                    }

                    val errorText = currentEntryError ?: syncMessage
                    if (!errorText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorText,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inline Display Name Editor
                    Text(
                        text = "LEADERBOARD DISPLAY NAME",
                        color = GoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftDisplayName,
                            onValueChange = {
                                draftDisplayName = it.take(30)
                                nameSavedNotice = false
                            },
                            singleLine = true,
                            placeholder = { Text("Enter casino alias", color = FireKernAsh) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = FireKernCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = GoldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_display_name_input")
                        )

                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                val cleaned = draftDisplayName.trim().ifBlank { "Fire Kern Player" }
                                onUpdateDisplayName(cleaned)
                                nameSavedNotice = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = FireKernVoid
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("settings_save_display_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    if (nameSavedNotice) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Display name updated and queued for Firestore sync.",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloud Sync & Sign Out Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                soundManager.playButtonClick()
                                onSyncNow()
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireKernCardElevated,
                                contentColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .testTag("settings_sync_profile_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = GoldPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "SYNCING..." else "SYNC NOW",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                soundManager.playButtonClick()
                                signOutUser(credentialManager, scope)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = FireKernEmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SIGN OUT",
                                color = FireKernEmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.85f), FireKernCardBorder)),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FireKernCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FireKernEmberDark)
                        .border(1.dp, GoldDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = DisplayFontFamily,
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = subtitle,
                        color = FireKernAsh,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FireKernCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trackColor: Color,
    switchTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = FireKernAsh,
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FireKernVoid,
                checkedTrackColor = trackColor
            ),
            modifier = Modifier.testTag(switchTag)
        )
    }
}

@Composable
private fun AccountMetaRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = FireKernAsh,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            color = GoldLight,
            fontFamily = if (monospace) MonoFontFamily else null,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp)
        )
    }
}

private fun formatFirestoreTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Awaiting initial sync"
    return try {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
            .format(timestamp.toDate())
    } catch (_: Exception) {
        "Synced"
    }
}
