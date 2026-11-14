package com.example

import com.example.model.Card
import com.example.model.FeltTheme
import com.example.model.CardBackTheme
import com.example.model.PlayerProfile
import com.example.model.PokerVariant
import com.example.model.ProgressionManager
import com.example.model.Rank
import com.example.model.SlotsVariant
import com.example.model.Suit
import com.example.model.VipTier
import com.example.ui.blackjack.BlackjackHand
import com.example.ui.roulette.RouletteBetType
import com.example.ui.roulette.RouletteColor
import com.example.ui.roulette.RouletteData
import com.example.ui.slots.SlotSymbol
import com.example.ui.videopoker.PokerHandEvaluator
import com.example.ui.videopoker.PokerHandRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verify_royal_flush_evaluation() {
    val cards = listOf(
      Card(Suit.SPADES, Rank.ACE),
      Card(Suit.SPADES, Rank.KING),
      Card(Suit.SPADES, Rank.QUEEN),
      Card(Suit.SPADES, Rank.JACK),
      Card(Suit.SPADES, Rank.TEN)
    )
    val rank = PokerHandEvaluator.evaluate(cards)
    assertEquals(PokerHandRank.ROYAL_FLUSH, rank)
  }

  @Test
  fun verify_deuces_wild_evaluation() {
    // 4 Deuces
    val fourDeuces = listOf(
      Card(Suit.HEARTS, Rank.TWO),
      Card(Suit.SPADES, Rank.TWO),
      Card(Suit.CLUBS, Rank.TWO),
      Card(Suit.DIAMONDS, Rank.TWO),
      Card(Suit.HEARTS, Rank.ACE)
    )
    assertEquals(PokerHandRank.FOUR_DEUCES, PokerHandEvaluator.evaluate(fourDeuces, PokerVariant.DEUCES_WILD))

    // 5 of a kind (Three Aces + two 2s)
    val fiveOfAKind = listOf(
      Card(Suit.HEARTS, Rank.ACE),
      Card(Suit.SPADES, Rank.ACE),
      Card(Suit.CLUBS, Rank.ACE),
      Card(Suit.DIAMONDS, Rank.TWO),
      Card(Suit.HEARTS, Rank.TWO)
    )
    assertEquals(PokerHandRank.FIVE_OF_A_KIND, PokerHandEvaluator.evaluate(fiveOfAKind, PokerVariant.DEUCES_WILD))
  }

  @Test
  fun verify_jacks_or_better_pair() {
    val winCards = listOf(
      Card(Suit.HEARTS, Rank.JACK),
      Card(Suit.SPADES, Rank.JACK),
      Card(Suit.CLUBS, Rank.FOUR),
      Card(Suit.DIAMONDS, Rank.SEVEN),
      Card(Suit.HEARTS, Rank.TWO)
    )
    assertEquals(PokerHandRank.JACKS_OR_BETTER, PokerHandEvaluator.evaluate(winCards))

    val loseCards = listOf(
      Card(Suit.HEARTS, Rank.TEN),
      Card(Suit.SPADES, Rank.TEN),
      Card(Suit.CLUBS, Rank.FOUR),
      Card(Suit.DIAMONDS, Rank.SEVEN),
      Card(Suit.HEARTS, Rank.TWO)
    )
    assertEquals(PokerHandRank.NOTHING, PokerHandEvaluator.evaluate(loseCards))
  }

  @Test
  fun verify_blackjack_scores() {
    val blackjackHand = BlackjackHand(
      cards = listOf(
        Card(Suit.HEARTS, Rank.ACE),
        Card(Suit.SPADES, Rank.KING)
      )
    )
    assertEquals(21, blackjackHand.totalScore)

    val softHand = BlackjackHand(
      cards = listOf(
        Card(Suit.HEARTS, Rank.ACE),
        Card(Suit.SPADES, Rank.SIX)
      )
    )
    assertEquals(17, softHand.totalScore)
    assertTrue(softHand.isSoft)
  }

  @Test
  fun verify_roulette_rules() {
    assertEquals(RouletteColor.GREEN, RouletteData.getColor(0))
    assertEquals(RouletteColor.RED, RouletteData.getColor(32))
    assertEquals(RouletteColor.BLACK, RouletteData.getColor(15))
    assertTrue(RouletteBetType.Red.isWinning(32))
    assertTrue(RouletteBetType.Even.isWinning(14))
    assertTrue(RouletteBetType.Low.isWinning(18))
    assertTrue(RouletteBetType.Straight(7).isWinning(7))
  }

  @Test
  fun verify_progressive_leveling_and_unlocks() {
    val starter = PlayerProfile(xp = 0L)
    assertEquals(1, starter.level)
    assertEquals(VipTier.BRONZE, starter.vipTier)
    assertTrue(starter.isPokerVariantUnlocked(PokerVariant.JACKS_OR_BETTER))
    assertFalse(starter.isPokerVariantUnlocked(PokerVariant.DEUCES_WILD))
    assertFalse(starter.isFeltUnlocked(FeltTheme.ROYAL_NAVY))

    // 500 XP = Level 2 (500 needed for lvl 2)
    val lvl2Player = PlayerProfile(xp = 500L)
    assertEquals(2, lvl2Player.level)
    assertTrue(lvl2Player.isFeltUnlocked(FeltTheme.ROYAL_NAVY))

    // Level 3 requires 500 + 750 = 1250 XP
    val lvl3Player = PlayerProfile(xp = 1250L)
    assertEquals(3, lvl3Player.level)
    assertTrue(lvl3Player.isPokerVariantUnlocked(PokerVariant.DEUCES_WILD))

    // High roller limits
    val highRoller = PlayerProfile(xp = 25000L)
    assertTrue(highRoller.level >= 10)
    assertTrue(highRoller.maxBetAllowed >= 10000L)
    assertTrue(highRoller.isSlotsVariantUnlocked(SlotsVariant.NEON_FRENZY))
  }

  @Test
  fun verify_slot_symbols() {
    assertTrue(SlotSymbol.WILD.isWild)
    assertTrue(SlotSymbol.SCATTER.isScatter)
    assertEquals(1000, SlotSymbol.WILD.payoutForCount(5))
    assertEquals(500, SlotSymbol.SEVEN.payoutForCount(5))
  }

  @Test
  fun verify_void_power_tiers_and_staking() {
    // 0 staked = NONE (1.0x)
    assertEquals(com.example.model.VoidPowerTier.NONE, com.example.model.VoidPowerTier.tierForStake(0L))
    assertEquals(1.0f, com.example.model.VoidPowerTier.NONE.xpMultiplier, 0.01f)

    // 5 staked = QUANTUM (+25% XP, +15% bonus)
    assertEquals(com.example.model.VoidPowerTier.QUANTUM, com.example.model.VoidPowerTier.tierForStake(5L))
    assertEquals(1.25f, com.example.model.VoidPowerTier.QUANTUM.xpMultiplier, 0.01f)
    assertEquals(1.15f, com.example.model.VoidPowerTier.QUANTUM.bonusMultiplier, 0.01f)

    // 15 staked = SUPERNOVA (+50% XP, +35% bonus)
    assertEquals(com.example.model.VoidPowerTier.SUPERNOVA, com.example.model.VoidPowerTier.tierForStake(15L))
    assertEquals(1.50f, com.example.model.VoidPowerTier.SUPERNOVA.xpMultiplier, 0.01f)

    // 30 staked = SINGULARITY (2.0x XP Overdrive, +60% bonus)
    assertEquals(com.example.model.VoidPowerTier.SINGULARITY, com.example.model.VoidPowerTier.tierForStake(30L))
    assertEquals(2.00f, com.example.model.VoidPowerTier.SINGULARITY.xpMultiplier, 0.01f)
  }

  @Test
  fun verify_void_wallet_state_and_solana_address() {
    val wallet = com.example.model.VoidWallet(
      balance = 10L,
      stakedAmount = 15L
    )
    assertEquals(25L, wallet.totalVoid)
    assertEquals(com.example.model.VoidPowerTier.SUPERNOVA, wallet.activePowerTier)
    assertTrue(wallet.solanaAddress.startsWith("Vo1d"))
    assertTrue(wallet.transactions.isNotEmpty())
  }

  @Test
  fun verify_player_currency_entity_and_defaults() {
    val entity = com.example.data.local.entity.PlayerCurrencyEntity(
      bankroll = 15000L,
      xp = 3500L,
      voidBalance = 12L,
      voidStaked = 5L,
      currentWinStreak = 4,
      bestWinStreak = 9
    )
    assertEquals(1, entity.id)
    assertEquals(15000L, entity.bankroll)
    assertEquals(3500L, entity.xp)
    assertEquals(12L, entity.voidBalance)
    assertEquals(5L, entity.voidStaked)
    assertEquals(4, entity.currentWinStreak)
    assertEquals(9, entity.bestWinStreak)
  }

  @Test
  fun verify_currency_transaction_entity() {
    val tx = com.example.data.local.entity.CurrencyTransactionEntity(
      transactionId = 42L,
      gameType = "BLACKJACK",
      amountDelta = 250L,
      balanceAfter = 10250L,
      note = "Natural Blackjack payout",
      timestamp = 1700000000000L
    )
    assertEquals(42L, tx.transactionId)
    assertEquals("BLACKJACK", tx.gameType)
    assertEquals(250L, tx.amountDelta)
    assertEquals(10250L, tx.balanceAfter)
    assertEquals("Natural Blackjack payout", tx.note)
  }

  @Test
  fun verify_sound_profile_presets() {
    val vegas = com.example.audio.SoundProfilePreset.VEGAS_CASINO
    assertEquals("Vegas Floor", vegas.displayName)

    val arcade = com.example.audio.SoundProfilePreset.CRISP_ARCADE
    assertEquals("Crisp Arcade", arcade.displayName)

    val lounge = com.example.audio.SoundProfilePreset.SUBTLE_LOUNGE
    assertEquals("Subtle Lounge", lounge.displayName)

    val muted = com.example.audio.SoundProfilePreset.MUTED
    assertEquals("Muted", muted.displayName)
  }

  @Test
  fun verify_player_stats_win_streak_tracking() {
    val stats = com.example.model.PlayerStats(
      totalRoundsPlayed = 10,
      totalWagered = 1000L,
      totalWon = 2500L,
      currentWinStreak = 3,
      bestWinStreak = 5
    )
    assertEquals(3, stats.currentWinStreak)
    assertEquals(5, stats.bestWinStreak)
    assertEquals(1500L, stats.netProfit)
  }

  @Test
  fun verify_daily_checkin_schedule_rewards() {
    val schedule = com.example.model.DailyCheckInSchedule
    assertEquals(7, schedule.days.size)

    val day1 = schedule.getRewardForDay(1)
    assertEquals(1500L, day1.chipsBonus)
    assertEquals(200L, day1.xpBonus)
    assertEquals(0L, day1.voidTokens)
    assertFalse(day1.isJackpotDay)

    val day3 = schedule.getRewardForDay(3)
    assertEquals(5000L, day3.chipsBonus)
    assertEquals(650L, day3.xpBonus)
    assertEquals(1L, day3.voidTokens)

    val day7 = schedule.getRewardForDay(7)
    assertEquals(50000L, day7.chipsBonus)
    assertEquals(6000L, day7.xpBonus)
    assertEquals(5L, day7.voidTokens)
    assertTrue(day7.isJackpotDay)
  }

  @Test
  fun verify_daily_checkin_streak_evaluation() {
    val schedule = com.example.model.DailyCheckInSchedule

    // Case 1: First-time check-in (timestamp 0)
    val firstTime = schedule.evaluateStreak(
      lastCheckInTimestamp = 0L,
      savedStreak = 0,
      bestStreak = 0,
      totalCheckIns = 0,
      currentTime = 1700000000000L
    )
    assertTrue(firstTime.isClaimAvailableToday)
    assertEquals(1, firstTime.targetDayInCycle)
    assertEquals(0, firstTime.currentStreak)

    // Case 2: Same calendar day (already claimed today)
    val todayCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 5, 10, 0, 0)
    }
    val sameDayCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 5, 14, 0, 0)
    }
    val sameDay = schedule.evaluateStreak(
      lastCheckInTimestamp = todayCal.timeInMillis,
      savedStreak = 2,
      bestStreak = 4,
      totalCheckIns = 2,
      currentTime = sameDayCal.timeInMillis
    )
    assertFalse(sameDay.isClaimAvailableToday)
    assertEquals(2, sameDay.currentStreak)
    assertEquals(2, sameDay.targetDayInCycle)
    assertTrue(sameDay.millisUntilNextClaim > 0L)

    // Case 3: Next consecutive day (e.g. 24 hours later)
    val nextDayCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 5, 10, 0, 0)
    }
    val followingDayCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 6, 11, 0, 0)
    }
    val consecutive = schedule.evaluateStreak(
      lastCheckInTimestamp = nextDayCal.timeInMillis,
      savedStreak = 3,
      bestStreak = 3,
      totalCheckIns = 3,
      currentTime = followingDayCal.timeInMillis
    )
    assertTrue(consecutive.isClaimAvailableToday)
    assertEquals(4, consecutive.targetDayInCycle) // Ready for Day 4!
    assertFalse(consecutive.isStreakBroken)

    // Case 4: Broken streak (missed 3 days)
    val lateDayCal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 9, 11, 0, 0)
    }
    val broken = schedule.evaluateStreak(
      lastCheckInTimestamp = nextDayCal.timeInMillis,
      savedStreak = 3,
      bestStreak = 5,
      totalCheckIns = 3,
      currentTime = lateDayCal.timeInMillis
    )
    assertTrue(broken.isClaimAvailableToday)
    assertEquals(1, broken.targetDayInCycle) // Reset back to Day 1
    assertTrue(broken.isStreakBroken)
  }

  @Test
  fun verify_fire_kern_default_felt_and_leaderboard_entry() {
    val defaultProfile = PlayerProfile()
    assertEquals(FeltTheme.FIRE_KERN_FORGE, defaultProfile.selectedFelt)
    assertTrue(defaultProfile.isFeltUnlocked(FeltTheme.FIRE_KERN_FORGE))
    assertEquals(FeltTheme.FIRE_KERN_FORGE, FeltTheme.fromId("unknown_felt"))

    val entry = com.example.model.LeaderboardEntry(
      userId = "user-1",
      displayName = "Ember Ace",
      totalXp = 12500L,
      level = 6,
      vipTitle = "Gold VIP",
      bestWinStreak = 4,
      biggestWin = 50000L,
      totalRoundsPlayed = 75
    )
    assertEquals("user-1", entry.userId)
    assertEquals("Ember Ace", entry.displayName)
    assertEquals(12500L, entry.totalXp)
    assertEquals(6, entry.level)
    assertEquals("Gold VIP", entry.vipTitle)
  }

  @Test
  fun verify_deck_export_and_restore_preserves_remaining_cards() {
    val deck = com.example.model.Deck(1)
    val drawn1 = deck.draw()
    val drawn2 = deck.draw()
    val remaining = deck.exportRemainingCards()
    assertEquals(50, remaining.size)

    val restoredDeck = com.example.model.Deck(1)
    restoredDeck.restoreCards(remaining)
    assertEquals(50, restoredDeck.remainingCards())
    assertEquals(remaining.first(), restoredDeck.draw())
    assertFalse(remaining.contains(drawn1) && remaining.contains(drawn2) && remaining.size == 52)
  }

  @Test
  fun verify_active_game_snapshots_json_round_trip() {
    // Blackjack
    val bjSnapshot = com.example.data.ActiveBlackjackSnapshot(
      stage = "PLAYER_TURN",
      selectedChip = "FIFTY",
      currentBet = 150L,
      previousBet = 100L,
      dealerHand = listOf(
        com.example.data.CardSnapshot.fromCard(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)),
        com.example.data.CardSnapshot.fromCard(Card(Suit.HEARTS, Rank.TEN, isFaceUp = false))
      ),
      playerHands = listOf(
        com.example.data.BlackjackHandSnapshot.fromBlackjackHand(
          BlackjackHand(
            cards = listOf(Card(Suit.DIAMONDS, Rank.NINE), Card(Suit.CLUBS, Rank.NINE)),
            bet = 150L
          )
        )
      ),
      activeHandIndex = 0,
      insuranceBet = 75L,
      statusMessage = "HIT OR STAND?",
      remainingShoeCards = listOf(com.example.data.CardSnapshot.fromCard(Card(Suit.HEARTS, Rank.FIVE)))
    )
    val bjJson = com.example.data.GameStateJsonCodec.encodeBlackjack(bjSnapshot)
    val bjDecoded = com.example.data.GameStateJsonCodec.decodeBlackjack(bjJson)
    assertEquals(bjSnapshot, bjDecoded)

    // Slots
    val gridNames = com.example.data.ActiveSlotsSnapshot.symbolsToNames(
      List(5) { listOf(SlotSymbol.WILD, SlotSymbol.SEVEN, SlotSymbol.CHERRY) }
    )
    val slotsSnapshot = com.example.data.ActiveSlotsSnapshot(
      grid = gridNames,
      finalGrid = gridNames,
      activeLines = 9,
      betPerLine = 25L,
      freeSpinsRemaining = 8,
      lastWin = 500L,
      isSpinning = true,
      isFreeSpinActive = true,
      costPaid = 0L,
      statusText = "FREE SPIN!"
    )
    val slotsJson = com.example.data.GameStateJsonCodec.encodeSlots(slotsSnapshot)
    val slotsDecoded = com.example.data.GameStateJsonCodec.decodeSlots(slotsJson)
    assertEquals(slotsSnapshot, slotsDecoded)

    // Video Poker
    val vpSnapshot = com.example.data.ActiveVideoPokerSnapshot(
      gameState = "DRAW_READY",
      cards = listOf(
        com.example.data.CardSnapshot.fromCard(Card(Suit.HEARTS, Rank.JACK)),
        com.example.data.CardSnapshot.fromCard(Card(Suit.SPADES, Rank.JACK)),
        com.example.data.CardSnapshot.fromCard(Card(Suit.CLUBS, Rank.FOUR)),
        com.example.data.CardSnapshot.fromCard(Card(Suit.DIAMONDS, Rank.SEVEN)),
        com.example.data.CardSnapshot.fromCard(Card(Suit.HEARTS, Rank.TWO))
      ),
      heldIndices = listOf(0, 1),
      coinsBet = 5,
      chipDenom = "TWENTY_FIVE",
      winAmount = 0L,
      statusMessage = "HOLD CARDS & DRAW",
      doubleUpGame = null,
      remainingDeckCards = listOf(com.example.data.CardSnapshot.fromCard(Card(Suit.CLUBS, Rank.ACE)))
    )
    val vpJson = com.example.data.GameStateJsonCodec.encodeVideoPoker(vpSnapshot)
    val vpDecoded = com.example.data.GameStateJsonCodec.decodeVideoPoker(vpJson)
    assertEquals(vpSnapshot, vpDecoded)

    // Roulette
    val rlSnapshot = com.example.data.ActiveRouletteSnapshot(
      selectedChip = "HUNDRED",
      bets = listOf(
        com.example.data.RouletteBetSnapshot.fromEntry(
          "17",
          com.example.ui.roulette.ActiveRouletteBet(RouletteBetType.Straight(17), 100L)
        ),
        com.example.data.RouletteBetSnapshot.fromEntry(
          "RED",
          com.example.ui.roulette.ActiveRouletteBet(RouletteBetType.Red, 200L)
        )
      ),
      isSpinning = true,
      targetNumber = 17,
      currentTotalBet = 300L,
      history = listOf(17, 0, 32),
      statusMessage = "WHEEL SPINNING..."
    )
    val rlJson = com.example.data.GameStateJsonCodec.encodeRoulette(rlSnapshot)
    val rlDecoded = com.example.data.GameStateJsonCodec.decodeRoulette(rlJson)
    assertEquals(rlSnapshot, rlDecoded)
    assertEquals(RouletteBetType.Straight(17), rlDecoded?.bets?.first()?.toActiveRouletteBet()?.type)
  }

  @Test
  fun verify_level_up_particle_engine_burst_and_physics() {
    val particles = com.example.ui.components.LevelUpParticleEngine.createRadialBurst(
      originX = 500f,
      originY = 400f,
      count = 60,
      random = kotlin.random.Random(42)
    ).toMutableList()
    assertEquals(60, particles.size)

    val fountain = com.example.ui.components.LevelUpParticleEngine.createCornerFountain(
      originX = 100f,
      originY = 900f,
      aimRight = true,
      count = 20,
      random = kotlin.random.Random(42)
    )
    assertEquals(20, fountain.size)
    assertTrue(fountain.all { it.vx > 0f && it.vy < 0f })

    val shockwaves = mutableListOf(
      com.example.ui.components.LevelUpShockwave(
        originX = 500f,
        originY = 400f,
        maxRadius = 300f,
        color = com.example.ui.theme.GoldPrimary,
        strokeWidth = 8f,
        durationSec = 0.5f
      )
    )

    val initialX = particles.first().x
    com.example.ui.components.LevelUpParticleEngine.stepParticles(particles, shockwaves, 0.04f)
    assertTrue(particles.isNotEmpty())
    assertTrue(shockwaves.isNotEmpty())
    assertTrue(particles.first().x != initialX || particles.first().y != 400f)

    // Advance enough time so all particles and shockwaves expire cleanly
    repeat(100) {
      com.example.ui.components.LevelUpParticleEngine.stepParticles(particles, shockwaves, 0.04f)
    }
    assertTrue(particles.isEmpty())
    assertTrue(shockwaves.isEmpty())
  }

  @Test
  fun verify_game_speed_scaling_and_lookup() {
    assertEquals(com.example.model.GameSpeed.NORMAL, com.example.model.GameSpeed.fromId(null))
    assertEquals(com.example.model.GameSpeed.NORMAL, com.example.model.GameSpeed.fromId("invalid_speed"))
    assertEquals(com.example.model.GameSpeed.RELAXED, com.example.model.GameSpeed.fromId("relaxed"))
    assertEquals(com.example.model.GameSpeed.FAST, com.example.model.GameSpeed.fromId("fast"))
    assertEquals(com.example.model.GameSpeed.TURBO, com.example.model.GameSpeed.fromId("turbo"))

    assertEquals(1300L, com.example.model.GameSpeed.RELAXED.scaleDelay(1000L))
    assertEquals(1000L, com.example.model.GameSpeed.NORMAL.scaleDelay(1000L))
    assertEquals(550L, com.example.model.GameSpeed.FAST.scaleDelay(1000L))
    assertEquals(250L, com.example.model.GameSpeed.TURBO.scaleDelay(1000L))

    // Minimum floor of 15ms for positive delays, and 0 for non-positive delays
    assertEquals(15L, com.example.model.GameSpeed.TURBO.scaleDelay(10L))
    assertEquals(0L, com.example.model.GameSpeed.TURBO.scaleDelay(0L))
  }
}
