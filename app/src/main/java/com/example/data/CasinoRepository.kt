package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BlackjackVariant
import com.example.model.CardBackTheme
import com.example.model.DailyBonusWheelCatalog
import com.example.model.DailyLoginRecord
import com.example.model.DailyWheelSpinResult
import com.example.model.DailyWheelStatus
import com.example.model.FeltTheme
import com.example.model.GameSpeed
import com.example.model.GameType
import com.example.model.WheelPrizeType
import com.example.model.LevelMilestone
import com.example.model.LoyaltyRewardUnlockResult
import com.example.model.PlayerProfile
import com.example.model.PlayerStats
import com.example.model.PokerVariant
import com.example.model.ProgressionManager
import com.example.model.RouletteVariant
import com.example.model.RoyaleLoyaltyCatalog
import com.example.model.SlotsVariant
import com.example.model.VoidTransaction
import com.example.model.VoidTxType
import com.example.model.VoidWallet
import com.example.model.CheckInDayReward
import com.example.model.CheckInStreakStatus
import com.example.model.DailyCheckInClaimResult
import com.example.model.DailyCheckInSchedule
import com.example.model.MissionClaimResult
import com.example.data.local.CasinoDatabase
import com.example.data.local.dao.ActiveGameStateDao
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.entity.CurrencyTransactionEntity
import com.example.data.local.entity.PlayerCurrencyEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class CasinoRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vegas_royale_casino_prefs", Context.MODE_PRIVATE)

    private val database: CasinoDatabase = CasinoDatabase.getInstance(context)
    val currencyDao: CurrencyDao = database.currencyDao()
    val currencyRepository: CurrencyRepository = CurrencyRepository(currencyDao)
    val activeGameStateDao: ActiveGameStateDao = database.activeGameStateDao()
    val activeGameRepository: ActiveGameRepository = ActiveGameRepository(activeGameStateDao)
    val missionsRepository: MissionsRepository = MissionsRepository(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private val _gameSpeed = MutableStateFlow(
        GameSpeed.fromId(prefs.getString("game_speed", GameSpeed.NORMAL.id))
    )
    val gameSpeed: StateFlow<GameSpeed> = _gameSpeed.asStateFlow()

    private val _latestLevelUp = MutableStateFlow<LevelMilestone?>(null)
    val latestLevelUp: StateFlow<LevelMilestone?> = _latestLevelUp.asStateFlow()

    var onWagerRecorded: ((Long) -> Unit)? = null

    init {
        missionsRepository.syncWithProfile(_profile.value)
        // Sync Room database with player currency on startup
        ioScope.launch {
            val existing = currencyDao.getPlayerCurrency()
            if (existing != null) {
                val updated = _profile.value.copy(
                    bankroll = existing.bankroll,
                    xp = existing.xp,
                    lastDailyBonusTime = existing.lastDailyBonusTime,
                    stats = _profile.value.stats.copy(
                        totalWagered = existing.totalWagered,
                        totalWon = existing.totalWon,
                        currentWinStreak = existing.currentWinStreak,
                        bestWinStreak = existing.bestWinStreak
                    ),
                    voidWallet = _profile.value.voidWallet.copy(
                        balance = existing.voidBalance,
                        stakedAmount = existing.voidStaked
                    )
                )
                _profile.value = updated
                missionsRepository.syncWithProfile(updated)
            } else {
                val current = _profile.value
                currencyDao.insertOrUpdateCurrency(
                    PlayerCurrencyEntity(
                        id = 1,
                        bankroll = current.bankroll,
                        xp = current.xp,
                        voidBalance = current.voidWallet.balance,
                        voidStaked = current.voidWallet.stakedAmount,
                        lastDailyBonusTime = current.lastDailyBonusTime,
                        currentWinStreak = current.stats.currentWinStreak,
                        bestWinStreak = current.stats.bestWinStreak,
                        totalWagered = current.stats.totalWagered,
                        totalWon = current.stats.totalWon,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun loadProfile(): PlayerProfile {
        val bankroll = prefs.getLong("bankroll", 10000L)
        val xp = prefs.getLong("xp", 0L)
        val lastDaily = prefs.getLong("last_daily_bonus", 0L)

        val stats = PlayerStats(
            totalWagered = prefs.getLong("stat_total_wagered", 0L),
            totalWon = prefs.getLong("stat_total_won", 0L),
            biggestWin = prefs.getLong("stat_biggest_win", 0L),
            totalRoundsPlayed = prefs.getInt("stat_rounds_played", 0),
            videoPokerHands = prefs.getInt("stat_vp_hands", 0),
            slotsSpins = prefs.getInt("stat_slots_spins", 0),
            blackjackHands = prefs.getInt("stat_bj_hands", 0),
            rouletteSpins = prefs.getInt("stat_rl_spins", 0),
            baccaratHands = prefs.getInt("stat_bc_hands", 0),
            totalJackpots = prefs.getInt("stat_jackpots", 0),
            totalLevelUps = prefs.getInt("stat_level_ups", 0),
            totalVoidEarned = prefs.getLong("stat_void_earned", 10L),
            currentWinStreak = prefs.getInt("stat_win_streak", 0),
            bestWinStreak = prefs.getInt("stat_best_win_streak", 0),
            checkInStreak = prefs.getInt("stat_checkin_streak", 0),
            bestCheckInStreak = prefs.getInt("stat_best_checkin_streak", 0),
            lastCheckInTimestamp = prefs.getLong("stat_last_checkin_time", 0L),
            totalCheckIns = prefs.getInt("stat_total_checkins", 0)
        )

        val felt = FeltTheme.fromId(prefs.getString("selected_felt", FeltTheme.FIRE_KERN_FORGE.id) ?: FeltTheme.FIRE_KERN_FORGE.id)
        val cardBack = CardBackTheme.fromId(prefs.getString("selected_card_back", CardBackTheme.ROYAL_RED.id) ?: CardBackTheme.ROYAL_RED.id)
        val pokerVariant = PokerVariant.values().firstOrNull { it.id == prefs.getString("selected_poker_var", PokerVariant.JACKS_OR_BETTER.id) } ?: PokerVariant.JACKS_OR_BETTER
        val slotsVariant = SlotsVariant.values().firstOrNull { it.id == prefs.getString("selected_slots_var", SlotsVariant.CLASSIC_ROYALE.id) } ?: SlotsVariant.CLASSIC_ROYALE
        val bjVariant = BlackjackVariant.values().firstOrNull { it.id == prefs.getString("selected_bj_var", BlackjackVariant.VEGAS_STRIP.id) } ?: BlackjackVariant.VEGAS_STRIP
        val rlVariant = RouletteVariant.values().firstOrNull { it.id == prefs.getString("selected_rl_var", RouletteVariant.EUROPEAN.id) } ?: RouletteVariant.EUROPEAN

        // Load Void Wallet
        val savedVoidBalance = prefs.getLong("void_wallet_balance", 10L)
        val savedVoidStaked = prefs.getLong("void_wallet_staked", 0L)
        val savedVoidAddress = prefs.getString("void_solana_address", null) ?: generateSolanaAddress()

        val voidWallet = VoidWallet(
            solanaAddress = savedVoidAddress,
            balance = savedVoidBalance,
            stakedAmount = savedVoidStaked
        )

        val totalWageredPref = prefs.getLong("stat_total_wagered", 0L)
        val hasSavedLoyalty = prefs.contains("lifetime_royale_points")
        val seededFromVolume = if (!hasSavedLoyalty && totalWageredPref > 0L) {
            (totalWageredPref / RoyaleLoyaltyCatalog.CHIPS_PER_BASE_POINT).coerceAtLeast(0L)
        } else {
            0L
        }
        val royalePoints = if (hasSavedLoyalty) prefs.getLong("royale_points", 0L).coerceAtLeast(0L) else seededFromVolume
        val lifetimeRoyalePoints = if (hasSavedLoyalty) prefs.getLong("lifetime_royale_points", 0L).coerceAtLeast(0L) else seededFromVolume
        val unlockedLoyaltyRewards = prefs.getStringSet("unlocked_loyalty_rewards", emptySet())?.toSet() ?: emptySet()

        return PlayerProfile(
            bankroll = bankroll,
            xp = xp,
            lastDailyBonusTime = lastDaily,
            royalePoints = royalePoints,
            lifetimeRoyalePoints = lifetimeRoyalePoints,
            unlockedLoyaltyRewardIds = unlockedLoyaltyRewards,
            stats = stats,
            selectedFelt = felt,
            selectedCardBack = cardBack,
            selectedPokerVariant = pokerVariant,
            selectedSlotsVariant = slotsVariant,
            selectedBlackjackVariant = bjVariant,
            selectedRouletteVariant = rlVariant,
            voidWallet = voidWallet
        )
    }

    private fun saveProfile(p: PlayerProfile) {
        prefs.edit().apply {
            putLong("bankroll", p.bankroll)
            putLong("xp", p.xp)
            putLong("last_daily_bonus", p.lastDailyBonusTime)
            putLong("stat_total_wagered", p.stats.totalWagered)
            putLong("stat_total_won", p.stats.totalWon)
            putLong("stat_biggest_win", p.stats.biggestWin)
            putInt("stat_rounds_played", p.stats.totalRoundsPlayed)
            putInt("stat_vp_hands", p.stats.videoPokerHands)
            putInt("stat_slots_spins", p.stats.slotsSpins)
            putInt("stat_bj_hands", p.stats.blackjackHands)
            putInt("stat_rl_spins", p.stats.rouletteSpins)
            putInt("stat_bc_hands", p.stats.baccaratHands)
            putInt("stat_jackpots", p.stats.totalJackpots)
            putInt("stat_level_ups", p.stats.totalLevelUps)
            putLong("stat_void_earned", p.stats.totalVoidEarned)
            putInt("stat_win_streak", p.stats.currentWinStreak)
            putInt("stat_best_win_streak", p.stats.bestWinStreak)
            putInt("stat_checkin_streak", p.stats.checkInStreak)
            putInt("stat_best_checkin_streak", p.stats.bestCheckInStreak)
            putLong("stat_last_checkin_time", p.stats.lastCheckInTimestamp)
            putInt("stat_total_checkins", p.stats.totalCheckIns)
            putString("selected_felt", p.selectedFelt.id)
            putString("selected_card_back", p.selectedCardBack.id)
            putString("selected_poker_var", p.selectedPokerVariant.id)
            putString("selected_slots_var", p.selectedSlotsVariant.id)
            putString("selected_bj_var", p.selectedBlackjackVariant.id)
            putString("selected_rl_var", p.selectedRouletteVariant.id)
            putLong("void_wallet_balance", p.voidWallet.balance)
            putLong("void_wallet_staked", p.voidWallet.stakedAmount)
            putString("void_solana_address", p.voidWallet.solanaAddress)
            putLong("royale_points", p.royalePoints)
            putLong("lifetime_royale_points", p.lifetimeRoyalePoints)
            putStringSet("unlocked_loyalty_rewards", p.unlockedLoyaltyRewardIds.toSet())
            apply()
        }

        ioScope.launch {
            currencyDao.insertOrUpdateCurrency(
                PlayerCurrencyEntity(
                    id = 1,
                    bankroll = p.bankroll,
                    xp = p.xp,
                    voidBalance = p.voidWallet.balance,
                    voidStaked = p.voidWallet.stakedAmount,
                    lastDailyBonusTime = p.lastDailyBonusTime,
                    currentWinStreak = p.stats.currentWinStreak,
                    bestWinStreak = p.stats.bestWinStreak,
                    checkInStreak = p.stats.checkInStreak,
                    bestCheckInStreak = p.stats.bestCheckInStreak,
                    lastCheckInTimestamp = p.stats.lastCheckInTimestamp,
                    totalCheckIns = p.stats.totalCheckIns,
                    totalWagered = p.stats.totalWagered,
                    totalWon = p.stats.totalWon,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun generateSolanaAddress(): String {
        val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val prefix = "Vo1d"
        val randomSuffix = (1..38).map { base58Chars[Random.nextInt(base58Chars.length)] }.joinToString("")
        val addr = prefix + randomSuffix
        prefs.edit().putString("void_solana_address", addr).apply()
        return addr
    }

    private fun generateTxHash(): String {
        val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        return (1..44).map { base58Chars[Random.nextInt(base58Chars.length)] }.joinToString("")
    }

    fun canWager(amount: Long): Boolean = _profile.value.bankroll >= amount

    fun placeWager(amount: Long, gameType: GameType? = null): Boolean {
        if (!canWager(amount)) return false
        val current = _profile.value
        val oldLevel = current.level

        // Apply active Void Power XP multiplier!
        val voidMultiplier = current.voidWallet.activePowerTier.xpMultiplier
        val baseGain = (20L + amount / 4).coerceAtLeast(10L)
        val xpGain = (baseGain * voidMultiplier).toLong()
        val newXp = current.xp + xpGain

        // Earn Royale Loyalty Points proportional to gameplay volume & loyalty tier
        val earnedRoyalePoints = RoyaleLoyaltyCatalog.calculatePointsEarned(amount, current.loyaltyTier)

        val updated = current.copy(
            bankroll = current.bankroll - amount,
            xp = newXp,
            royalePoints = current.royalePoints + earnedRoyalePoints,
            lifetimeRoyalePoints = current.lifetimeRoyalePoints + earnedRoyalePoints,
            stats = current.stats.copy(
                totalWagered = current.stats.totalWagered + amount
            )
        )
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.syncWithProfile(updated)
        onWagerRecorded?.invoke(amount)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = gameType?.name ?: "CASINO_FLOOR",
                    amountDelta = -amount,
                    balanceAfter = updated.bankroll,
                    note = "Wager ${gameType?.title ?: "Bet"}"
                )
            )
        }

        checkLevelUp(oldLevel, updated.level)
        return true
    }

    fun awardPayout(gameType: GameType, payoutAmount: Long, isJackpot: Boolean = false) {
        val current = _profile.value
        val oldLevel = current.level

        // Apply active Void Power XP multiplier!
        val voidMultiplier = current.voidWallet.activePowerTier.xpMultiplier
        val baseWinGain = (payoutAmount / 2).coerceAtLeast(25L) + if (isJackpot) 500L else 0L
        val winXpBonus = (baseWinGain * voidMultiplier).toLong()
        val newXp = current.xp + winXpBonus

        // Void token minting rewards:
        var voidMinted = 0L
        if (isJackpot) {
            voidMinted += 10L
        } else if (payoutAmount >= 1000L) {
            voidMinted += 2L
        } else if (payoutAmount >= 300L) {
            voidMinted += 1L
        }

        val updatedWallet = if (voidMinted > 0) {
            val newTx = VoidTransaction(
                id = UUID.randomUUID().toString(),
                type = VoidTxType.MINT_REWARD,
                amount = voidMinted,
                txHash = generateTxHash(),
                timestamp = System.currentTimeMillis(),
                note = "Won $${payoutAmount} in ${gameType.title} (+${voidMinted} \$VOID)"
            )
            current.voidWallet.copy(
                balance = current.voidWallet.balance + voidMinted,
                transactions = listOf(newTx) + current.voidWallet.transactions.take(20)
            )
        } else {
            current.voidWallet
        }

        val newStreak = current.stats.currentWinStreak + 1
        val newBestStreak = maxOf(current.stats.bestWinStreak, newStreak)

        val newStats = current.stats.copy(
            totalWon = current.stats.totalWon + payoutAmount,
            biggestWin = maxOf(current.stats.biggestWin, payoutAmount),
            totalRoundsPlayed = current.stats.totalRoundsPlayed + 1,
            videoPokerHands = if (gameType == GameType.VIDEO_POKER) current.stats.videoPokerHands + 1 else current.stats.videoPokerHands,
            slotsSpins = if (gameType == GameType.SLOTS) current.stats.slotsSpins + 1 else current.stats.slotsSpins,
            blackjackHands = if (gameType == GameType.BLACKJACK) current.stats.blackjackHands + 1 else current.stats.blackjackHands,
            rouletteSpins = if (gameType == GameType.ROULETTE) current.stats.rouletteSpins + 1 else current.stats.rouletteSpins,
            baccaratHands = if (gameType == GameType.BACCARAT) current.stats.baccaratHands + 1 else current.stats.baccaratHands,
            totalJackpots = if (isJackpot) current.stats.totalJackpots + 1 else current.stats.totalJackpots,
            totalVoidEarned = current.stats.totalVoidEarned + voidMinted,
            currentWinStreak = newStreak,
            bestWinStreak = newBestStreak
        )

        val updated = current.copy(
            bankroll = current.bankroll + payoutAmount,
            xp = newXp,
            stats = newStats,
            voidWallet = updatedWallet
        )
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.recordRoundFinished(gameType = gameType, isWin = true, profile = updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = gameType.name,
                    amountDelta = payoutAmount,
                    balanceAfter = updated.bankroll,
                    note = "${gameType.title} Win (Streak $newStreak)"
                )
            )
        }

        checkLevelUp(oldLevel, updated.level)
    }

    fun recordLoss(gameType: GameType) {
        val current = _profile.value
        val newStats = current.stats.copy(
            totalRoundsPlayed = current.stats.totalRoundsPlayed + 1,
            currentWinStreak = 0,
            videoPokerHands = if (gameType == GameType.VIDEO_POKER) current.stats.videoPokerHands + 1 else current.stats.videoPokerHands,
            slotsSpins = if (gameType == GameType.SLOTS) current.stats.slotsSpins + 1 else current.stats.slotsSpins,
            blackjackHands = if (gameType == GameType.BLACKJACK) current.stats.blackjackHands + 1 else current.stats.blackjackHands,
            rouletteSpins = if (gameType == GameType.ROULETTE) current.stats.rouletteSpins + 1 else current.stats.rouletteSpins,
            baccaratHands = if (gameType == GameType.BACCARAT) current.stats.baccaratHands + 1 else current.stats.baccaratHands
        )
        val updated = current.copy(stats = newStats)
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.recordRoundFinished(gameType = gameType, isWin = false, profile = updated)
    }

    private fun checkLevelUp(oldLevel: Int, newLevel: Int) {
        if (newLevel > oldLevel) {
            val levelsGained = newLevel - oldLevel
            val voidReward = (levelsGained * 5L) // 5 $VOID per level up!

            val milestone = ProgressionManager.getMilestoneForLevel(newLevel)
                ?: LevelMilestone(
                    level = newLevel,
                    title = "VIP Level $newLevel (+${voidReward} \$VOID)",
                    description = "Congratulations on advancing your casino prestige and earning $voidReward \$VOID!",
                    rewardIconEmoji = "⭐"
                )
            _latestLevelUp.value = milestone

            val curr = _profile.value
            val newTx = VoidTransaction(
                id = UUID.randomUUID().toString(),
                type = VoidTxType.MINT_REWARD,
                amount = voidReward,
                txHash = generateTxHash(),
                timestamp = System.currentTimeMillis(),
                note = "Level Up Reward: Advanced to Level $newLevel (+${voidReward} \$VOID)"
            )
            val updatedWallet = curr.voidWallet.copy(
                balance = curr.voidWallet.balance + voidReward,
                transactions = listOf(newTx) + curr.voidWallet.transactions.take(20)
            )

            val withLevelUpCount = curr.copy(
                stats = curr.stats.copy(
                    totalLevelUps = curr.stats.totalLevelUps + levelsGained,
                    totalVoidEarned = curr.stats.totalVoidEarned + voidReward
                ),
                voidWallet = updatedWallet
            )
            _profile.value = withLevelUpCount
            saveProfile(withLevelUpCount)
        }
    }

    fun dismissLevelUp() {
        _latestLevelUp.value = null
    }

    // VOID WALLET OPERATIONS
    fun stakeVoid(amount: Long): Boolean {
        val curr = _profile.value
        if (amount <= 0 || curr.voidWallet.balance < amount) return false

        val newTx = VoidTransaction(
            id = UUID.randomUUID().toString(),
            type = VoidTxType.STAKE_POWER,
            amount = amount,
            txHash = generateTxHash(),
            timestamp = System.currentTimeMillis(),
            note = "Staked $amount \$VOID for Power Multipliers"
        )
        val newWallet = curr.voidWallet.copy(
            balance = curr.voidWallet.balance - amount,
            stakedAmount = curr.voidWallet.stakedAmount + amount,
            transactions = listOf(newTx) + curr.voidWallet.transactions.take(20)
        )
        val updated = curr.copy(voidWallet = newWallet)
        _profile.value = updated
        saveProfile(updated)
        return true
    }

    fun unstakeVoid(amount: Long): Boolean {
        val curr = _profile.value
        if (amount <= 0 || curr.voidWallet.stakedAmount < amount) return false

        val newTx = VoidTransaction(
            id = UUID.randomUUID().toString(),
            type = VoidTxType.UNSTAKE,
            amount = amount,
            txHash = generateTxHash(),
            timestamp = System.currentTimeMillis(),
            note = "Unstaked $amount \$VOID to Wallet Balance"
        )
        val newWallet = curr.voidWallet.copy(
            balance = curr.voidWallet.balance + amount,
            stakedAmount = curr.voidWallet.stakedAmount - amount,
            transactions = listOf(newTx) + curr.voidWallet.transactions.take(20)
        )
        val updated = curr.copy(voidWallet = newWallet)
        _profile.value = updated
        saveProfile(updated)
        return true
    }

    fun swapChipsToVoid(chipsAmount: Long): Boolean {
        val curr = _profile.value
        val chipsPerVoid = 1000L
        val voidToReceive = chipsAmount / chipsPerVoid
        if (chipsAmount <= 0 || chipsAmount % chipsPerVoid != 0L || curr.bankroll < chipsAmount) return false

        val newTx = VoidTransaction(
            id = UUID.randomUUID().toString(),
            type = VoidTxType.SWAP_CHIPS_TO_VOID,
            amount = voidToReceive,
            txHash = generateTxHash(),
            timestamp = System.currentTimeMillis(),
            note = "Swapped $${chipsAmount} Chips for $voidToReceive \$VOID"
        )
        val newWallet = curr.voidWallet.copy(
            balance = curr.voidWallet.balance + voidToReceive,
            transactions = listOf(newTx) + curr.voidWallet.transactions.take(20)
        )
        val updated = curr.copy(
            bankroll = curr.bankroll - chipsAmount,
            voidWallet = newWallet,
            stats = curr.stats.copy(totalVoidEarned = curr.stats.totalVoidEarned + voidToReceive)
        )
        _profile.value = updated
        saveProfile(updated)
        return true
    }

    fun swapVoidToChips(voidAmount: Long): Boolean {
        val curr = _profile.value
        val chipsPerVoid = 1000L
        if (voidAmount <= 0 || curr.voidWallet.balance < voidAmount) return false
        val chipsToReceive = voidAmount * chipsPerVoid

        val newTx = VoidTransaction(
            id = UUID.randomUUID().toString(),
            type = VoidTxType.SWAP_VOID_TO_CHIPS,
            amount = voidAmount,
            txHash = generateTxHash(),
            timestamp = System.currentTimeMillis(),
            note = "Converted $voidAmount \$VOID into +$${chipsToReceive} Chips"
        )
        val newWallet = curr.voidWallet.copy(
            balance = curr.voidWallet.balance - voidAmount,
            transactions = listOf(newTx) + curr.voidWallet.transactions.take(20)
        )
        val updated = curr.copy(
            bankroll = curr.bankroll + chipsToReceive,
            voidWallet = newWallet
        )
        _profile.value = updated
        saveProfile(updated)
        return true
    }

    fun selectFelt(theme: FeltTheme) {
        if (_profile.value.isFeltUnlocked(theme)) {
            val updated = _profile.value.copy(selectedFelt = theme)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun selectCardBack(theme: CardBackTheme) {
        if (_profile.value.isCardBackUnlocked(theme)) {
            val updated = _profile.value.copy(selectedCardBack = theme)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun selectPokerVariant(variant: PokerVariant) {
        if (_profile.value.isPokerVariantUnlocked(variant)) {
            val updated = _profile.value.copy(selectedPokerVariant = variant)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun selectSlotsVariant(variant: SlotsVariant) {
        if (_profile.value.isSlotsVariantUnlocked(variant)) {
            val updated = _profile.value.copy(selectedSlotsVariant = variant)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun selectBlackjackVariant(variant: BlackjackVariant) {
        if (_profile.value.isBlackjackVariantUnlocked(variant)) {
            val updated = _profile.value.copy(selectedBlackjackVariant = variant)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun selectRouletteVariant(variant: RouletteVariant) {
        if (_profile.value.isRouletteVariantUnlocked(variant)) {
            val updated = _profile.value.copy(selectedRouletteVariant = variant)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun getDailyWheelStatus(currentTimeMs: Long = System.currentTimeMillis()): DailyWheelStatus {
        return DailyBonusWheelCatalog.evaluateStatus(
            lastSpinTimestampMs = _profile.value.lastDailyBonusTime,
            currentTimeMs = currentTimeMs
        )
    }

    fun claimDailyWheelSpin(
        sliceIndex: Int,
        nowMs: Long = System.currentTimeMillis()
    ): DailyWheelSpinResult? {
        val status = getDailyWheelStatus(nowMs)
        if (!status.canSpin) return null

        val current = _profile.value
        val safeIndex = sliceIndex.coerceIn(0, DailyBonusWheelCatalog.slices.lastIndex)
        val slice = DailyBonusWheelCatalog.slices[safeIndex]

        val chipsAwarded: Long
        val royalePointsAwarded: Long
        when (slice.prizeType) {
            WheelPrizeType.VIRTUAL_CHIPS -> {
                val voidBonusMultiplier = current.voidWallet.activePowerTier.bonusMultiplier
                chipsAwarded = (slice.amount * voidBonusMultiplier).toLong()
                royalePointsAwarded = 0L
            }
            WheelPrizeType.ROYALE_POINTS -> {
                chipsAwarded = 0L
                royalePointsAwarded = slice.amount
            }
        }

        val updated = current.copy(
            bankroll = current.bankroll + chipsAwarded,
            royalePoints = current.royalePoints + royalePointsAwarded,
            lifetimeRoyalePoints = current.lifetimeRoyalePoints + royalePointsAwarded,
            lastDailyBonusTime = nowMs
        )
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.syncWithProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "DAILY_WHEEL",
                    amountDelta = chipsAwarded,
                    balanceAfter = updated.bankroll,
                    note = if (chipsAwarded > 0L) {
                        "Daily Wheel Spin: +$$chipsAwarded Chips"
                    } else {
                        "Daily Wheel Spin: +$royalePointsAwarded Royale Points"
                    }
                )
            )
        }

        return DailyWheelSpinResult(
            slice = slice,
            chipsAwarded = chipsAwarded,
            royalePointsAwarded = royalePointsAwarded,
            newBankroll = updated.bankroll,
            newRoyalePoints = updated.royalePoints,
            newLifetimeRoyalePoints = updated.lifetimeRoyalePoints,
            claimedAtMs = nowMs
        )
    }

    fun claimDailyBonus(amount: Long) {
        val current = _profile.value
        // Apply active Void bonus multiplier!
        val voidBonusMultiplier = current.voidWallet.activePowerTier.bonusMultiplier
        val boostedAmount = (amount * voidBonusMultiplier).toLong()

        val updated = current.copy(
            bankroll = current.bankroll + boostedAmount,
            lastDailyBonusTime = System.currentTimeMillis()
        )
        _profile.value = updated
        saveProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "DAILY_BONUS",
                    amountDelta = boostedAmount,
                    balanceAfter = updated.bankroll,
                    note = "Daily Wheel Bonus Claimed"
                )
            )
        }
    }

    fun emergencyChipRefill() {
        val current = _profile.value
        val minTarget = 2500L
        if (current.bankroll >= minTarget) return
        val delta = minTarget - current.bankroll
        val updated = current.copy(
            bankroll = minTarget
        )
        _profile.value = updated
        saveProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "EMERGENCY_REFILL",
                    amountDelta = delta,
                    balanceAfter = minTarget,
                    note = "Floor Manager Complimentary Grant"
                )
            )
        }
    }

    fun resetStats() {
        val resetP = PlayerProfile(bankroll = 10000L, xp = 0L, lastDailyBonusTime = 0L)
        _profile.value = resetP
        saveProfile(resetP)
    }

    fun getDailyCheckInStatus(): CheckInStreakStatus {
        val stats = _profile.value.stats
        return DailyCheckInSchedule.evaluateStreak(
            lastCheckInTimestamp = stats.lastCheckInTimestamp,
            savedStreak = stats.checkInStreak,
            bestStreak = stats.bestCheckInStreak,
            totalCheckIns = stats.totalCheckIns
        )
    }

    fun claimDailyCheckIn(): DailyCheckInClaimResult {
        val current = _profile.value
        val oldLevel = current.level
        val stats = current.stats
        val status = getDailyCheckInStatus()

        val targetDay = status.targetDayInCycle
        val reward = DailyCheckInSchedule.getRewardForDay(targetDay)

        // Calculate new streak
        val newStreak = if (status.isStreakBroken || stats.checkInStreak == 0) {
            1
        } else {
            stats.checkInStreak + 1
        }
        val newBestStreak = maxOf(stats.bestCheckInStreak, newStreak)
        val isNewBest = newStreak > stats.bestCheckInStreak

        // Multipliers from active Void Power Tier
        val activePower = current.voidWallet.activePowerTier
        val finalChips = (reward.chipsBonus * activePower.bonusMultiplier).toLong()
        val finalXp = (reward.xpBonus * activePower.xpMultiplier).toLong()
        val finalVoid = reward.voidTokens

        // Update Void wallet if void tokens awarded
        val updatedWallet = if (finalVoid > 0L) {
            val newBalance = current.voidWallet.balance + finalVoid
            val txList = current.voidWallet.transactions.toMutableList()
            txList.add(
                0,
                VoidTransaction(
                    id = UUID.randomUUID().toString(),
                    type = VoidTxType.MINT_REWARD,
                    amount = finalVoid,
                    txHash = "5K" + UUID.randomUUID().toString().replace("-", "").take(30),
                    timestamp = System.currentTimeMillis(),
                    note = "Day $targetDay Login Streak Reward"
                )
            )
            current.voidWallet.copy(
                balance = newBalance,
                transactions = txList.take(50)
            )
        } else {
            current.voidWallet
        }

        val updatedStats = stats.copy(
            checkInStreak = newStreak,
            bestCheckInStreak = newBestStreak,
            lastCheckInTimestamp = System.currentTimeMillis(),
            totalCheckIns = stats.totalCheckIns + 1,
            totalVoidEarned = stats.totalVoidEarned + finalVoid
        )

        val updated = current.copy(
            bankroll = current.bankroll + finalChips,
            xp = current.xp + finalXp,
            stats = updatedStats,
            voidWallet = updatedWallet
        )

        _profile.value = updated
        saveProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "DAILY_CHECKIN",
                    amountDelta = finalChips,
                    balanceAfter = updated.bankroll,
                    note = "Day $targetDay streak login: +$finalChips chips, +$finalXp XP" + if (finalVoid > 0) ", +$finalVoid \$VOID" else ""
                )
            )
        }

        checkLevelUp(oldLevel, updated.level)

        return DailyCheckInClaimResult(
            dayNumber = targetDay,
            streak = newStreak,
            chipsAwarded = finalChips,
            xpAwarded = finalXp,
            voidTokensAwarded = finalVoid,
            isNewBestStreak = isNewBest
        )
    }

    /**
     * Testing tool: Sets lastCheckInTimestamp back 24 hours so next day can be claimed immediately.
     */
    fun simulateNextDayForTesting() {
        val current = _profile.value
        val stats = current.stats
        val shiftedTimestamp = if (stats.lastCheckInTimestamp > 0L) {
            stats.lastCheckInTimestamp - 86400000L
        } else {
            System.currentTimeMillis() - 86400000L
        }
        val updatedStats = stats.copy(lastCheckInTimestamp = shiftedTimestamp)
        val updated = current.copy(stats = updatedStats)
        _profile.value = updated
        saveProfile(updated)
    }

    /**
     * Testing tool: Clears check-in timestamp and resets streak to 0 for initial Day 1 test.
     */
    fun resetCheckInForTesting() {
        val current = _profile.value
        val stats = current.stats.copy(checkInStreak = 0, lastCheckInTimestamp = 0L)
        val updated = current.copy(stats = stats)
        _profile.value = updated
        saveProfile(updated)
    }

    fun syncCheckInStreakFromFirestore(record: DailyLoginRecord) {
        val current = _profile.value
        val stats = current.stats
        val cloudClaimMs = record.lastBonusClaimAt?.toDate()?.time ?: 0L
        val shouldHydrateFromCloud =
            record.totalCheckIns > stats.totalCheckIns ||
                (stats.lastCheckInTimestamp == 0L && record.totalCheckIns > 0 && cloudClaimMs > 0L)
        if (!shouldHydrateFromCloud) return

        val updatedStats = stats.copy(
            checkInStreak = maxOf(stats.checkInStreak, record.currentStreak),
            bestCheckInStreak = maxOf(stats.bestCheckInStreak, record.bestStreak),
            totalCheckIns = maxOf(stats.totalCheckIns, record.totalCheckIns),
            lastCheckInTimestamp = if (cloudClaimMs > 0L) cloudClaimMs else stats.lastCheckInTimestamp
        )
        val updated = current.copy(stats = updatedStats)
        _profile.value = updated
        saveProfile(updated)
    }

    fun syncWheelSpinFromFirestore(record: DailyLoginRecord) {
        val cloudWheelMs = record.lastWheelSpinAt?.toDate()?.time ?: 0L
        if (cloudWheelMs <= 0L) return
        val current = _profile.value
        if (cloudWheelMs > current.lastDailyBonusTime) {
            val updated = current.copy(lastDailyBonusTime = cloudWheelMs)
            _profile.value = updated
            saveProfile(updated)
        }
    }

    fun getPlayerDisplayName(defaultName: String): String {
        val saved = prefs.getString("player_display_name", null)?.trim()
        return if (!saved.isNullOrBlank()) saved else defaultName.trim().ifBlank { "Fire Kern Player" }.take(32)
    }

    fun setPlayerDisplayName(name: String) {
        val clean = name.trim().ifBlank { "Fire Kern Player" }.take(32)
        prefs.edit().putString("player_display_name", clean).apply()
    }

    fun setGameSpeed(speed: GameSpeed) {
        _gameSpeed.value = speed
        prefs.edit().putString("game_speed", speed.id).apply()
    }

    fun scaleDelay(baseMs: Long): Long = _gameSpeed.value.scaleDelay(baseMs)

    fun claimMissionReward(missionId: String): MissionClaimResult? {
        val current = _profile.value
        val mission = missionsRepository.claimMission(missionId, current) ?: return null
        val oldLevel = current.level

        val updatedWallet = if (mission.rewardVoid > 0L) {
            val newTx = VoidTransaction(
                id = UUID.randomUUID().toString(),
                type = VoidTxType.MINT_REWARD,
                amount = mission.rewardVoid,
                txHash = generateTxHash(),
                timestamp = System.currentTimeMillis(),
                note = "Mission Reward: ${mission.title} (+${mission.rewardVoid} \$VOID)"
            )
            current.voidWallet.copy(
                balance = current.voidWallet.balance + mission.rewardVoid,
                transactions = listOf(newTx) + current.voidWallet.transactions.take(20)
            )
        } else {
            current.voidWallet
        }

        val updated = current.copy(
            bankroll = current.bankroll + mission.rewardChips,
            xp = current.xp + mission.rewardXp,
            stats = current.stats.copy(
                totalVoidEarned = current.stats.totalVoidEarned + mission.rewardVoid
            ),
            voidWallet = updatedWallet
        )
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.syncWithProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "MISSION_REWARD",
                    amountDelta = mission.rewardChips,
                    balanceAfter = updated.bankroll,
                    note = "Quest Claimed: ${mission.title}"
                )
            )
        }

        checkLevelUp(oldLevel, updated.level)
        return MissionClaimResult(
            mission = mission,
            chipsAwarded = mission.rewardChips,
            xpAwarded = mission.rewardXp,
            voidAwarded = mission.rewardVoid
        )
    }

    fun unlockLoyaltyReward(rewardId: String): LoyaltyRewardUnlockResult? {
        val current = _profile.value
        val reward = RoyaleLoyaltyCatalog.rewards.firstOrNull { it.id == rewardId } ?: return null
        if (!RoyaleLoyaltyCatalog.isRewardEligibleToUnlock(current, reward)) return null

        val oldLevel = current.level
        val updatedWallet = if (reward.voidReward > 0L) {
            val newTx = VoidTransaction(
                id = UUID.randomUUID().toString(),
                type = VoidTxType.MINT_REWARD,
                amount = reward.voidReward,
                txHash = generateTxHash(),
                timestamp = System.currentTimeMillis(),
                note = "Loyalty Reward: ${reward.title} (+${reward.voidReward} \$VOID)"
            )
            current.voidWallet.copy(
                balance = current.voidWallet.balance + reward.voidReward,
                transactions = listOf(newTx) + current.voidWallet.transactions.take(20)
            )
        } else {
            current.voidWallet
        }

        val remainingPoints = (current.royalePoints - reward.pointsCost).coerceAtLeast(0L)
        val updated = current.copy(
            bankroll = current.bankroll + reward.chipsReward,
            xp = current.xp + reward.xpReward,
            royalePoints = remainingPoints,
            unlockedLoyaltyRewardIds = current.unlockedLoyaltyRewardIds + reward.id,
            stats = current.stats.copy(
                totalVoidEarned = current.stats.totalVoidEarned + reward.voidReward
            ),
            voidWallet = updatedWallet
        )
        _profile.value = updated
        saveProfile(updated)
        missionsRepository.syncWithProfile(updated)

        ioScope.launch {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = "LOYALTY_REWARD",
                    amountDelta = reward.chipsReward,
                    balanceAfter = updated.bankroll,
                    note = "Royale Points Unlocked: ${reward.title} (-${reward.pointsCost} RP)"
                )
            )
        }

        checkLevelUp(oldLevel, updated.level)
        return LoyaltyRewardUnlockResult(
            reward = reward,
            chipsAwarded = reward.chipsReward,
            xpAwarded = reward.xpReward,
            voidAwarded = reward.voidReward,
            remainingRoyalePoints = remainingPoints
        )
    }
}
