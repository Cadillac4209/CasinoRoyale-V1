package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.audio.SoundProfilePreset
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

@Composable
fun SoundSettingsDialog(
    soundManager: SoundManager,
    onDismiss: () -> Unit
) {
    var masterVol by remember { mutableFloatStateOf(soundManager.masterVolume) }
    var sfxVol by remember { mutableFloatStateOf(soundManager.sfxVolume) }
    var musicVol by remember { mutableFloatStateOf(soundManager.musicVolume) }
    var soundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var musicEnabled by remember { mutableStateOf(soundManager.isMusicEnabled) }
    var hapticsEnabled by remember { mutableStateOf(soundManager.isHapticsEnabled) }
    var activePreset by remember { mutableStateOf(soundManager.activePreset) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(GoldPrimary, FireKernCardBorder)),
                    RoundedCornerShape(20.dp)
                )
                .testTag("sound_settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FireKernCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
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
                                .background(FireKernEmberDark)
                                .border(1.dp, GoldDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CASINO AUDIO SUITE",
                                fontFamily = DisplayFontFamily,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Classic Assets & Tactile Haptic Feedback",
                                color = FireKernAsh,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_sound_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = FireKernCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Sound On/Off Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = null,
                            tint = if (soundEnabled) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Game Sound Effects",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            soundManager.updateSettings(soundEnabled = it)
                            if (it) soundManager.playButtonClick()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FireKernVoid,
                            checkedTrackColor = GoldPrimary
                        ),
                        modifier = Modifier.testTag("toggle_sound_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Music On/Off Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = if (musicEnabled) Color(0xFF38BDF8) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lounge Background Music",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = musicEnabled,
                        onCheckedChange = {
                            musicEnabled = it
                            soundManager.updateSettings(musicEnabled = it)
                            soundManager.playButtonClick()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FireKernVoid,
                            checkedTrackColor = Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.testTag("toggle_music_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Haptics Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (hapticsEnabled) FireKernFlame else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tactile Haptic Feedback",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            soundManager.updateSettings(hapticsEnabled = it)
                            if (it) soundManager.playChipClick(100L)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FireKernVoid,
                            checkedTrackColor = FireKernFlame
                        ),
                        modifier = Modifier.testTag("toggle_haptics_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Volume Sliders
                Text(
                    text = "MASTER VOLUME: ${(masterVol * 100).toInt()}%",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Slider(
                    value = masterVol,
                    onValueChange = {
                        masterVol = it
                        soundManager.updateSettings(masterVol = it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = FireKernCardElevated
                    ),
                    modifier = Modifier.testTag("master_volume_slider")
                )

                Text(
                    text = "SFX DYNAMICS: ${(sfxVol * 100).toInt()}%",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Slider(
                    value = sfxVol,
                    onValueChange = {
                        sfxVol = it
                        soundManager.updateSettings(sfxVol = it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = FireKernFlame,
                        activeTrackColor = FireKernFlame,
                        inactiveTrackColor = FireKernCardElevated
                    ),
                    modifier = Modifier.testTag("sfx_volume_slider")
                )

                Text(
                    text = "LOUNGE MUSIC: ${(musicVol * 100).toInt()}%",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Slider(
                    value = musicVol,
                    onValueChange = {
                        musicVol = it
                        soundManager.updateSettings(musicVol = it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = FireKernCardElevated
                    ),
                    modifier = Modifier.testTag("music_volume_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets Selection
                Text(
                    text = "SOUND PROFILE PRESET",
                    color = GoldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SoundProfilePreset.values().forEach { preset ->
                        val isSelected = activePreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldPrimary else FireKernCardElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) GoldLight else FireKernCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    activePreset = preset
                                    soundManager.updateSettings(preset = preset)
                                    soundManager.playChipClick()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.displayName,
                                color = if (isSelected) FireKernVoid else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // INTERACTIVE AUDIO FEEDBACK TESTERS
                Text(
                    text = "SAMPLE EVENT FEEDBACK",
                    color = GoldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Chip stack test
                    Button(
                        onClick = { soundManager.playChipCollect() },
                        colors = ButtonDefaults.buttonColors(containerColor = FireKernCardElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_sound_chip")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                            Text("Chips", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Card shuffle test
                    Button(
                        onClick = { soundManager.playShuffleCards() },
                        colors = ButtonDefaults.buttonColors(containerColor = FireKernCardElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_sound_shuffle")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Style, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Text("Shuffle", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Slot spin test
                    Button(
                        onClick = {
                            soundManager.playSpinReels()
                            soundManager.playReelStop(2)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireKernCardElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_sound_slot")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = FireKernFlame, modifier = Modifier.size(16.dp))
                            Text("Slots", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Win streak test
                    Button(
                        onClick = { soundManager.playWinStreak(3) },
                        colors = ButtonDefaults.buttonColors(containerColor = FireKernCardElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_sound_streak")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = FireKernEmber, modifier = Modifier.size(16.dp))
                            Text("Streak", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = FireKernVoid),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sound_settings_done_button")
                ) {
                    Text("SAVE & RESUME PLAY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
