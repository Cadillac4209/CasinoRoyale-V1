package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.SoundManager
import com.example.auth.authStateFlow
import com.example.data.CasinoRepository
import com.example.data.LeaderboardRepository
import com.example.model.GameType
import com.example.ui.auth.AuthScreen
import com.example.ui.blackjack.BlackjackScreen
import com.example.ui.blackjack.BlackjackViewModel
import com.example.ui.components.LevelUpCelebrationDialog
import com.example.ui.components.LevelUpParticleOverlay
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.leaderboard.LeaderboardViewModel
import com.example.ui.lobby.LobbyScreen
import com.example.ui.progression.ProgressionScreen
import com.example.ui.roulette.RouletteScreen
import com.example.ui.roulette.RouletteViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.slots.SlotsScreen
import com.example.ui.slots.SlotsViewModel
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.videopoker.VideoPokerScreen
import com.example.ui.videopoker.VideoPokerViewModel
import com.example.ui.voidwallet.VoidWalletScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var repository: CasinoRepository
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = CasinoRepository(applicationContext)
        soundManager = SoundManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FireKernVoid
                ) {
                    CasinoAppRoot(
                        repository = repository,
                        soundManager = soundManager
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::soundManager.isInitialized) {
            soundManager.onAppForeground()
        }
    }

    override fun onPause() {
        if (::soundManager.isInitialized) {
            soundManager.onAppBackground()
        }
        super.onPause()
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}

private enum class SecondaryRoute {
    NONE,
    VOID_WALLET,
    PROGRESSION,
    LEADERBOARD,
    SETTINGS
}

@Composable
fun CasinoAppRoot(
    repository: CasinoRepository,
    soundManager: SoundManager
) {
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedCasinoApp(
                userId = user.uid,
                defaultDisplayName = user.displayName ?: "Fire Kern Player",
                repository = repository,
                soundManager = soundManager
            )
        }
    }
}

@Composable
private fun AuthenticatedCasinoApp(
    userId: String,
    defaultDisplayName: String,
    repository: CasinoRepository,
    soundManager: SoundManager
) {
    val appContext = LocalContext.current.applicationContext

    val leaderboardViewModel: LeaderboardViewModel = viewModel(
        key = userId,
        factory = viewModelFactory {
            initializer {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                LeaderboardViewModel(
                    repository = LeaderboardRepository(firestore, Firebase.auth),
                    currentUserId = userId
                )
            }
        }
    )

    var currentGame by remember { mutableStateOf<GameType?>(null) }
    var secondaryRoute by remember { mutableStateOf(SecondaryRoute.NONE) }
    var playerDisplayName by remember(userId) {
        mutableStateOf(repository.getPlayerDisplayName(defaultDisplayName))
    }

    val profile by repository.profile.collectAsState()
    val latestLevelUp by repository.latestLevelUp.collectAsState()
    val rankSummary by leaderboardViewModel.playerRankSummary.collectAsStateWithLifecycle()

    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var particleBurstNonce by remember { mutableIntStateOf(0) }
    var activeBurstLevel by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(secondaryRoute, currentGame) {
        isSoundEnabled = soundManager.isSoundEnabled
    }

    // Sync local progression stats to Firestore Top 100 leaderboard whenever XP/rounds/name updates
    LaunchedEffect(profile.xp, profile.stats.totalRoundsPlayed, playerDisplayName) {
        leaderboardViewModel.syncPlayerStats(
            displayName = playerDisplayName,
            totalXp = profile.xp,
            level = profile.level,
            vipTitle = profile.vipTier.title,
            bestWinStreak = profile.stats.bestWinStreak,
            biggestWin = profile.stats.biggestWin,
            totalRoundsPlayed = profile.stats.totalRoundsPlayed
        )
    }

    // Intercept back button when inside a game, wallet, progression, or leaderboard screen
    BackHandler(enabled = secondaryRoute != SecondaryRoute.NONE || currentGame != null) {
        soundManager.playButtonClick()
        if (secondaryRoute != SecondaryRoute.NONE) {
            secondaryRoute = SecondaryRoute.NONE
        } else {
            currentGame = null
        }
    }

    // Trigger level-up fanfare and particle burst whenever a level-up occurs
    LaunchedEffect(latestLevelUp) {
        latestLevelUp?.let { milestone ->
            soundManager.playBigWin()
            activeBurstLevel = milestone.level
            particleBurstNonce++
        }
    }

    // ViewModels for the 4 games wired to SoundManager
    val videoPokerViewModel = remember {
        VideoPokerViewModel(repository, soundManager)
    }
    val slotsViewModel = remember {
        SlotsViewModel(repository, soundManager)
    }
    val blackjackViewModel = remember {
        BlackjackViewModel(repository, soundManager)
    }
    val rouletteViewModel = remember {
        RouletteViewModel(repository, soundManager)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = Pair(secondaryRoute, currentGame),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "casino_game_nav"
        ) { (route, activeGame) ->
            when {
                route == SecondaryRoute.VOID_WALLET -> {
                    VoidWalletScreen(
                        repository = repository,
                        soundManager = soundManager,
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.PROGRESSION -> {
                    ProgressionScreen(
                        repository = repository,
                        soundManager = soundManager,
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.LEADERBOARD -> {
                    LeaderboardScreen(
                        viewModel = leaderboardViewModel,
                        profile = profile,
                        playerDisplayName = playerDisplayName,
                        soundManager = soundManager,
                        onUpdateDisplayName = { updatedName ->
                            repository.setPlayerDisplayName(updatedName)
                            playerDisplayName = repository.getPlayerDisplayName(defaultDisplayName)
                        },
                        onSyncNow = {
                            leaderboardViewModel.syncPlayerStats(
                                displayName = playerDisplayName,
                                totalXp = profile.xp,
                                level = profile.level,
                                vipTitle = profile.vipTier.title,
                                bestWinStreak = profile.stats.bestWinStreak,
                                biggestWin = profile.stats.biggestWin,
                                totalRoundsPlayed = profile.stats.totalRoundsPlayed
                            )
                        },
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        soundManager = soundManager,
                        leaderboardViewModel = leaderboardViewModel,
                        profile = profile,
                        playerDisplayName = playerDisplayName,
                        onUpdateDisplayName = { updatedName ->
                            repository.setPlayerDisplayName(updatedName)
                            playerDisplayName = repository.getPlayerDisplayName(defaultDisplayName)
                        },
                        onSyncNow = {
                            leaderboardViewModel.syncPlayerStats(
                                displayName = playerDisplayName,
                                totalXp = profile.xp,
                                level = profile.level,
                                vipTitle = profile.vipTier.title,
                                bestWinStreak = profile.stats.bestWinStreak,
                                biggestWin = profile.stats.biggestWin,
                                totalRoundsPlayed = profile.stats.totalRoundsPlayed
                            )
                        },
                        onBackClick = {
                            isSoundEnabled = soundManager.isSoundEnabled
                            secondaryRoute = SecondaryRoute.NONE
                        }
                    )
                }
                activeGame == null -> {
                    LobbyScreen(
                        repository = repository,
                        soundManager = soundManager,
                        onSelectGame = { gameType ->
                            currentGame = gameType
                        },
                        onOpenProgression = {
                            secondaryRoute = SecondaryRoute.PROGRESSION
                        },
                        onOpenVoidWallet = {
                            secondaryRoute = SecondaryRoute.VOID_WALLET
                        },
                        onOpenLeaderboard = {
                            secondaryRoute = SecondaryRoute.LEADERBOARD
                        },
                        onOpenSettings = {
                            secondaryRoute = SecondaryRoute.SETTINGS
                        },
                        playerRank = rankSummary.rank,
                        onEmergencyRefill = {
                            repository.emergencyChipRefill()
                            soundManager.playWin()
                        }
                    )
                }
                activeGame == GameType.VIDEO_POKER -> {
                    VideoPokerScreen(
                        viewModel = videoPokerViewModel,
                        profile = profile,
                        isSoundEnabled = isSoundEnabled,
                        onToggleSound = {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                            isSoundEnabled = soundManager.isSoundEnabled
                        },
                        onBackClick = {
                            soundManager.playButtonClick()
                            currentGame = null
                        },
                        onEmergencyRefill = {
                            repository.emergencyChipRefill()
                            soundManager.playWin()
                        }
                    )
                }
                activeGame == GameType.SLOTS -> {
                    SlotsScreen(
                        viewModel = slotsViewModel,
                        profile = profile,
                        isSoundEnabled = isSoundEnabled,
                        onToggleSound = {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                            isSoundEnabled = soundManager.isSoundEnabled
                        },
                        onBackClick = {
                            soundManager.playButtonClick()
                            currentGame = null
                        },
                        onEmergencyRefill = {
                            repository.emergencyChipRefill()
                            soundManager.playWin()
                        }
                    )
                }
                activeGame == GameType.BLACKJACK -> {
                    BlackjackScreen(
                        viewModel = blackjackViewModel,
                        profile = profile,
                        isSoundEnabled = isSoundEnabled,
                        onToggleSound = {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                            isSoundEnabled = soundManager.isSoundEnabled
                        },
                        onBackClick = {
                            soundManager.playButtonClick()
                            currentGame = null
                        },
                        onEmergencyRefill = {
                            repository.emergencyChipRefill()
                            soundManager.playWin()
                        }
                    )
                }
                activeGame == GameType.ROULETTE -> {
                    RouletteScreen(
                        viewModel = rouletteViewModel,
                        profile = profile,
                        isSoundEnabled = isSoundEnabled,
                        onToggleSound = {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                            isSoundEnabled = soundManager.isSoundEnabled
                        },
                        onBackClick = {
                            soundManager.playButtonClick()
                            currentGame = null
                        },
                        onEmergencyRefill = {
                            repository.emergencyChipRefill()
                            soundManager.playWin()
                        }
                    )
                }
            }
        }

        // Level Up Celebration Overlay Modal
        latestLevelUp?.let { milestone ->
            LevelUpCelebrationDialog(
                milestone = milestone,
                onDismiss = {
                    soundManager.playButtonClick()
                    repository.dismissLevelUp()
                }
            )
        }

        // Particle-Based Visual Effect Overlay on Level Up (non-blocking)
        if (particleBurstNonce > 0 && activeBurstLevel != null) {
            LevelUpParticleOverlay(
                burstKey = particleBurstNonce,
                level = activeBurstLevel ?: 1,
                onBurstFinished = { activeBurstLevel = null }
            )
        }
    }
}
