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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.SoundManager
import com.example.auth.authStateFlow
import com.example.data.CasinoRepository
import com.example.data.CryptoRateRepository
import com.example.data.DailyLoginRepository
import com.example.data.LeaderboardRepository
import com.example.data.ResponsiblePlayManager
import com.example.data.SpendingLimitAlertLevel
import com.example.model.GameType
import com.example.ui.auth.AuthScreen
import com.example.ui.baccarat.BaccaratScreen
import com.example.ui.baccarat.BaccaratViewModel
import com.example.ui.blackjack.BlackjackScreen
import com.example.ui.blackjack.BlackjackViewModel
import com.example.ui.components.BreakReminderDialog
import com.example.ui.components.LevelUpCelebrationDialog
import com.example.ui.components.LevelUpParticleOverlay
import com.example.ui.components.SpendingLimitConfirmationDialog
import com.example.ui.converter.CryptoConverterScreen
import com.example.ui.converter.CryptoConverterViewModel
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.leaderboard.LeaderboardViewModel
import com.example.ui.lobby.DailyLoginViewModel
import com.example.ui.lobby.LobbyScreen
import com.example.ui.missions.MissionsScreen
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var repository: CasinoRepository
    private lateinit var soundManager: SoundManager
    private lateinit var responsiblePlayManager: ResponsiblePlayManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = CasinoRepository(applicationContext)
        soundManager = SoundManager(applicationContext)
        responsiblePlayManager = ResponsiblePlayManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FireKernVoid
                ) {
                    CasinoAppRoot(
                        repository = repository,
                        soundManager = soundManager,
                        responsiblePlayManager = responsiblePlayManager
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
        if (::responsiblePlayManager.isInitialized) {
            responsiblePlayManager.onAppForeground()
        }
    }

    override fun onPause() {
        if (::soundManager.isInitialized) {
            soundManager.onAppBackground()
        }
        if (::responsiblePlayManager.isInitialized) {
            responsiblePlayManager.onAppBackground()
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
    SETTINGS,
    CRYPTO_CONVERTER,
    MISSIONS
}

@Composable
fun CasinoAppRoot(
    repository: CasinoRepository,
    soundManager: SoundManager,
    responsiblePlayManager: ResponsiblePlayManager
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
                soundManager = soundManager,
                responsiblePlayManager = responsiblePlayManager
            )
        }
    }
}

@Composable
private fun AuthenticatedCasinoApp(
    userId: String,
    defaultDisplayName: String,
    repository: CasinoRepository,
    soundManager: SoundManager,
    responsiblePlayManager: ResponsiblePlayManager
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

    val dailyLoginViewModel: DailyLoginViewModel = viewModel(
        key = "daily_login_$userId",
        factory = viewModelFactory {
            initializer {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                DailyLoginViewModel(
                    repository = DailyLoginRepository(firestore, Firebase.auth)
                )
            }
        }
    )

    val cryptoConverterViewModel: CryptoConverterViewModel = viewModel(
        key = "crypto_converter_$userId",
        factory = viewModelFactory {
            initializer {
                CryptoConverterViewModel(
                    rateRepository = CryptoRateRepository(appContext)
                )
            }
        }
    )

    var currentGame by remember { mutableStateOf<GameType?>(null) }
    var secondaryRoute by remember { mutableStateOf(SecondaryRoute.NONE) }
    var progressionInitialTab by remember { mutableIntStateOf(0) }
    var playerDisplayName by remember(userId) {
        mutableStateOf(repository.getPlayerDisplayName(defaultDisplayName))
    }

    val profile by repository.profile.collectAsState()
    val latestLevelUp by repository.latestLevelUp.collectAsState()
    val rankSummary by leaderboardViewModel.playerRankSummary.collectAsStateWithLifecycle()
    val dailyLoginRecord by dailyLoginViewModel.dailyLoginRecord.collectAsStateWithLifecycle()
    val isDailyLoginSyncing by dailyLoginViewModel.isSyncing.collectAsStateWithLifecycle()
    val cryptoRatesState by cryptoConverterViewModel.ratesState.collectAsStateWithLifecycle()
    val selectedCryptoAsset by cryptoConverterViewModel.selectedAsset.collectAsStateWithLifecycle()
    val responsiblePlayState by responsiblePlayManager.state.collectAsStateWithLifecycle()
    val spendingLimitState by responsiblePlayManager.spendingLimitState.collectAsStateWithLifecycle()

    var isSoundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var particleBurstNonce by remember { mutableIntStateOf(0) }
    var activeBurstLevel by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(repository, responsiblePlayManager) {
        repository.onWagerRecorded = { wagerAmount ->
            responsiblePlayManager.recordMonthlySpend(wagerAmount)
        }
        onDispose {
            repository.onWagerRecorded = null
        }
    }

    LaunchedEffect(userId) {
        dailyLoginViewModel.syncLoginSession()
    }

    LaunchedEffect(dailyLoginRecord) {
        dailyLoginRecord?.let { record ->
            repository.syncCheckInStreakFromFirestore(record)
            repository.syncWheelSpinFromFirestore(record)
        }
    }

    LaunchedEffect(secondaryRoute, currentGame) {
        isSoundEnabled = soundManager.isSoundEnabled
        responsiblePlayManager.recordUserActivity()
    }

    LaunchedEffect(profile.stats.totalRoundsPlayed) {
        responsiblePlayManager.recordUserActivity()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            responsiblePlayManager.tick()
        }
    }

    // Sync local progression & loyalty stats to Firestore Top 100 leaderboard whenever XP/rounds/RP/name updates
    LaunchedEffect(
        profile.xp,
        profile.stats.totalRoundsPlayed,
        profile.royalePoints,
        profile.lifetimeRoyalePoints,
        playerDisplayName
    ) {
        leaderboardViewModel.syncPlayerStats(
            displayName = playerDisplayName,
            totalXp = profile.xp,
            level = profile.level,
            vipTitle = profile.vipTier.title,
            bestWinStreak = profile.stats.bestWinStreak,
            biggestWin = profile.stats.biggestWin,
            totalRoundsPlayed = profile.stats.totalRoundsPlayed,
            royalePoints = profile.royalePoints,
            lifetimeRoyalePoints = profile.lifetimeRoyalePoints,
            loyaltyTier = profile.loyaltyTier.title
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

    // ViewModels for the 5 floor games wired to SoundManager
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
    val baccaratViewModel = remember {
        BaccaratViewModel(repository, soundManager)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.isNotEmpty()) {
                            responsiblePlayManager.recordUserActivity()
                        }
                    }
                }
            }
    ) {
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
                        onOpenCryptoConverter = { secondaryRoute = SecondaryRoute.CRYPTO_CONVERTER },
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.CRYPTO_CONVERTER -> {
                    CryptoConverterScreen(
                        viewModel = cryptoConverterViewModel,
                        liveBankroll = profile.bankroll,
                        soundManager = soundManager,
                        onBackClick = {
                            soundManager.playButtonClick()
                            secondaryRoute = SecondaryRoute.NONE
                        }
                    )
                }
                route == SecondaryRoute.MISSIONS -> {
                    MissionsScreen(
                        repository = repository,
                        soundManager = soundManager,
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.PROGRESSION -> {
                    ProgressionScreen(
                        repository = repository,
                        soundManager = soundManager,
                        initialTab = progressionInitialTab,
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
                                totalRoundsPlayed = profile.stats.totalRoundsPlayed,
                                royalePoints = profile.royalePoints,
                                lifetimeRoyalePoints = profile.lifetimeRoyalePoints,
                                loyaltyTier = profile.loyaltyTier.title
                            )
                        },
                        onBackClick = { secondaryRoute = SecondaryRoute.NONE }
                    )
                }
                route == SecondaryRoute.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        soundManager = soundManager,
                        responsiblePlayManager = responsiblePlayManager,
                        leaderboardViewModel = leaderboardViewModel,
                        profile = profile,
                        playerDisplayName = playerDisplayName,
                        dailyLoginRecord = dailyLoginRecord,
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
                                totalRoundsPlayed = profile.stats.totalRoundsPlayed,
                                royalePoints = profile.royalePoints,
                                lifetimeRoyalePoints = profile.lifetimeRoyalePoints,
                                loyaltyTier = profile.loyaltyTier.title
                            )
                            dailyLoginViewModel.syncLoginSession()
                        },
                        onOpenLoyaltyRewards = {
                            progressionInitialTab = 1
                            secondaryRoute = SecondaryRoute.PROGRESSION
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
                            progressionInitialTab = 0
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
                        onOpenCryptoConverter = {
                            secondaryRoute = SecondaryRoute.CRYPTO_CONVERTER
                        },
                        onOpenMissions = {
                            secondaryRoute = SecondaryRoute.MISSIONS
                        },
                        onOpenLoyaltyRewards = {
                            progressionInitialTab = 1
                            secondaryRoute = SecondaryRoute.PROGRESSION
                        },
                        cryptoRatesState = cryptoRatesState,
                        selectedCryptoAsset = selectedCryptoAsset,
                        onSelectCryptoAsset = { asset ->
                            cryptoConverterViewModel.selectAsset(asset)
                        },
                        onRefreshCryptoRates = {
                            cryptoConverterViewModel.refreshRates(force = true)
                        },
                        playerRank = rankSummary.rank,
                        dailyLoginRecord = dailyLoginRecord,
                        isDailyLoginSyncing = isDailyLoginSyncing,
                        onDailyCheckInClaimed = { claimResult ->
                            val latestProfile = repository.profile.value
                            dailyLoginViewModel.recordDailyClaim(
                                currentStreak = claimResult.streak,
                                bestStreak = latestProfile.stats.bestCheckInStreak,
                                totalCheckIns = latestProfile.stats.totalCheckIns,
                                claimedChips = claimResult.chipsAwarded
                            )
                        },
                        onDailyWheelSpun = {
                            dailyLoginViewModel.recordWheelSpin()
                        },
                        onResetDailyCheckInCloud = {
                            dailyLoginViewModel.resetCloudStreak()
                        },
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
                activeGame == GameType.BACCARAT -> {
                    BaccaratScreen(
                        viewModel = baccaratViewModel,
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

        // 60-Minute Continuous Play Break Prompt & Gambling Addiction Support Resources
        if (responsiblePlayState.isBreakPromptVisible) {
            BreakReminderDialog(
                continuousPlayMs = responsiblePlayState.continuousPlayMs,
                isThresholdReached = responsiblePlayState.continuousPlayMs >= responsiblePlayState.breakThresholdMs,
                onTakeBreak = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.takeBreakAndReset()
                    currentGame = null
                    secondaryRoute = SecondaryRoute.NONE
                },
                onContinuePlaying = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.dismissAndContinue()
                }
            )
        }

        // User-Defined Monthly Spending Limit Confirmation Dialog
        if (spendingLimitState.pendingAlert != SpendingLimitAlertLevel.NONE) {
            SpendingLimitConfirmationDialog(
                limitState = spendingLimitState,
                onConfirmContinue = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.acknowledgeSpendingLimitAlert()
                },
                onAdjustLimit = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.acknowledgeSpendingLimitAlert()
                    currentGame = null
                    secondaryRoute = SecondaryRoute.SETTINGS
                },
                onTakeBreak = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.acknowledgeSpendingLimitAlert()
                    currentGame = null
                    secondaryRoute = SecondaryRoute.NONE
                },
                onOpenSupport = {
                    soundManager.playButtonClick()
                    responsiblePlayManager.acknowledgeSpendingLimitAlert()
                    responsiblePlayManager.openSupportResourcesDialog()
                }
            )
        }
    }
}
