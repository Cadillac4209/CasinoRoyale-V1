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

  @Test
  fun verify_continuous_play_60min_break_prompt_and_inactivity_reset() {
    var nowMs = 1_000_000L
    val engine = com.example.data.ContinuousPlayTimerEngine(
      breakThresholdMs = 60L * 60L * 1000L, // 60 minutes
      inactivityResetThresholdMs = 5L * 60L * 1000L, // 5 minutes
      initialNowMs = nowMs
    )

    // Simulate 59 minutes of continuous play with active touches every 10 seconds
    repeat(59 * 6) {
      nowMs += 10_000L
      engine.recordActivity(nowMs)
      val triggered = engine.tick(nowMs, isReminderEnabled = true)
      assertFalse(triggered)
      assertFalse(engine.isBreakPromptVisible)
    }
    assertEquals(59L * 60L * 1000L, engine.continuousPlayMs)

    // Brief 2-minute background pause preserves accumulated session time
    engine.setForeground(false, nowMs)
    nowMs += 2L * 60L * 1000L
    engine.setForeground(true, nowMs)
    assertEquals(59L * 60L * 1000L, engine.continuousPlayMs)

    // Final 60 seconds reaches 60 minutes -> triggers break prompt
    repeat(6) { index ->
      nowMs += 10_000L
      engine.recordActivity(nowMs)
      val triggered = engine.tick(nowMs, isReminderEnabled = true)
      if (index < 5) {
        assertFalse(triggered)
      } else {
        assertTrue(triggered)
      }
    }
    assertTrue(engine.isBreakPromptVisible)
    assertEquals(60L * 60L * 1000L, engine.continuousPlayMs)

    // Dismissing resets continuous play timer cleanly
    engine.dismissAndReset(nowMs)
    assertFalse(engine.isBreakPromptVisible)
    assertEquals(0L, engine.continuousPlayMs)

    // Accumulate 10 minutes of play, then go idle for 5 minutes -> resets session to 0
    repeat(10 * 6) {
      nowMs += 10_000L
      engine.recordActivity(nowMs)
      engine.tick(nowMs, isReminderEnabled = true)
    }
    assertEquals(10L * 60L * 1000L, engine.continuousPlayMs)

    nowMs += 5L * 60L * 1000L
    engine.tick(nowMs, isReminderEnabled = true)
    assertEquals(0L, engine.continuousPlayMs)
    assertFalse(engine.isBreakPromptVisible)

    // Verify gambling addiction support resources are populated
    val resources = com.example.ui.components.SupportResourcesCatalog.resources
    assertTrue(resources.size >= 3)
    assertTrue(resources.any { it.helplineNumber == "1-800-522-4700" })
    assertTrue(resources.all { it.websiteUrl.startsWith("https://") })
  }

  @Test
  fun verify_crypto_conversion_calculator_and_formatting() {
    val btcQuote = com.example.model.CryptoQuote(
      asset = com.example.model.CryptoAsset.BTC,
      usdPrice = 100_000.0,
      change24hPercent = 2.45,
      fetchedAtMillis = 1_700_000_000_000L,
      sourceName = "CoinGecko Live"
    )
    // 10,000 credits @ 1,000 credits/$1 = $10.00 reference -> 0.000100 BTC
    val summary = com.example.model.CryptoConversionCalculator.convertCreditsToCrypto(
      virtualCredits = 10_000L,
      quote = btcQuote
    )
    assertEquals(10.0, summary.virtualUsdValue, 0.0001)
    assertEquals(0.0001, summary.convertedAmount, 0.0000001)
    assertEquals("0.000100 BTC", summary.formattedCryptoAmount)

    // Guard negative credits and zero USD price
    val zeroSafe = com.example.model.CryptoConversionCalculator.convertCreditsToCrypto(
      virtualCredits = -500L,
      quote = btcQuote.copy(usdPrice = 0.0)
    )
    assertEquals(0L, zeroSafe.virtualCredits)
    assertEquals(0.0, zeroSafe.convertedAmount, 0.0)
    assertEquals("0.000000 BTC", zeroSafe.formattedCryptoAmount)

    // Formatting checks
    assertEquals("$100,000.00", com.example.model.CryptoConversionCalculator.formatUsdPrice(100_000.0))
    assertEquals("+2.45% (24h)", com.example.model.CryptoConversionCalculator.format24hChange(2.45))
    assertEquals("-1.80% (24h)", com.example.model.CryptoConversionCalculator.format24hChange(-1.8))
    assertEquals("24h N/A", com.example.model.CryptoConversionCalculator.format24hChange(null))
  }

  @Test
  fun verify_crypto_rate_parsers_coingecko_and_coinbase() {
    val coinGeckoJson = """
      {
        "bitcoin": {"usd": 92500.5, "usd_24h_change": 3.12, "last_updated_at": 1730000000},
        "ethereum": {"usd": 3120.25, "usd_24h_change": -0.75, "last_updated_at": 1730000000},
        "solana": {"usd": 185.4, "usd_24h_change": 5.0, "last_updated_at": 1730000000},
        "litecoin": {"usd": 84.1, "usd_24h_change": 1.2, "last_updated_at": 1730000000}
      }
    """.trimIndent()
    val parsedMap = com.example.data.remote.CryptoRateParser.parseCoinGeckoResponse(coinGeckoJson)
    assertEquals(4, parsedMap.size)
    assertEquals(92500.5, parsedMap[com.example.model.CryptoAsset.BTC]!!.usdPrice, 0.01)
    assertEquals(3.12, parsedMap[com.example.model.CryptoAsset.BTC]!!.change24hPercent!!, 0.01)
    assertEquals(1730000000000L, parsedMap[com.example.model.CryptoAsset.BTC]!!.fetchedAtMillis)
    assertEquals("CoinGecko Live", parsedMap[com.example.model.CryptoAsset.ETH]!!.sourceName)

    val coinbaseJson = """
      {"data":{"base":"ETH","currency":"USD","amount":"3145.67"}}
    """.trimIndent()
    val ethQuote = com.example.data.remote.CryptoRateParser.parseCoinbaseJson(
      asset = com.example.model.CryptoAsset.ETH,
      json = coinbaseJson,
      fetchedAtMillis = 1730000005000L
    )
    assertEquals(3145.67, ethQuote!!.usdPrice, 0.001)
    assertEquals("Coinbase Live", ethQuote.sourceName)
    assertEquals(com.example.model.CryptoAsset.ETH, ethQuote.asset)
  }

  @Test
  fun verify_baccarat_punto_banco_rules_and_payouts() {
    // Card points (Ace=1, Face/10=0) & modulo 10 scoring
    val naturalNineHand = listOf(Card(Suit.SPADES, Rank.NINE), Card(Suit.HEARTS, Rank.KING))
    val sevenHand = listOf(Card(Suit.DIAMONDS, Rank.THREE), Card(Suit.CLUBS, Rank.FOUR))
    assertEquals(9, com.example.model.BaccaratRules.handScore(naturalNineHand))
    assertEquals(7, com.example.model.BaccaratRules.handScore(sevenHand))

    // Natural 9 stands immediately (no 3rd cards drawn)
    var drawCount = 0
    val naturalDeal = com.example.model.BaccaratRules.resolveFromCards(
      playerInitial = naturalNineHand,
      bankerInitial = sevenHand,
      drawNextCard = {
        drawCount++
        Card(Suit.CLUBS, Rank.TWO)
      }
    )
    assertTrue(naturalDeal.isNatural)
    assertEquals(0, drawCount)
    assertEquals(com.example.model.BaccaratOutcome.PLAYER_WIN, naturalDeal.outcome)

    // Banker 3rd card tableau: Banker has 4, draws when Player 3rd card is 2..7, stands on 8
    assertTrue(com.example.model.BaccaratRules.shouldBankerDrawThirdCard(4, 5))
    assertFalse(com.example.model.BaccaratRules.shouldBankerDrawThirdCard(4, 8))
    assertFalse(com.example.model.BaccaratRules.shouldBankerDrawThirdCard(3, 8))

    // Payout checks: Banker 5% commission + Dragon Pair 11:1
    val bankerAndPairPayout = com.example.model.BaccaratRules.calculatePayouts(
      bets = mapOf(
        com.example.model.BaccaratBetSpot.BANKER to 100L,
        com.example.model.BaccaratBetSpot.DRAGON_PAIR to 50L
      ),
      outcome = com.example.model.BaccaratOutcome.BANKER_WIN,
      hasAnyPair = true
    )
    // Banker: 100 + 95 = 195; Pair: 50 * 12 = 600; Total = 795
    assertEquals(795L, bankerAndPairPayout.totalReturned)
    assertFalse(bankerAndPairPayout.isPushOnly)

    // Tie pushes Player/Banker stakes cleanly
    val tiePush = com.example.model.BaccaratRules.calculatePayouts(
      bets = mapOf(com.example.model.BaccaratBetSpot.PLAYER to 200L),
      outcome = com.example.model.BaccaratOutcome.TIE,
      hasAnyPair = false
    )
    assertEquals(200L, tiePush.totalReturned)
    assertTrue(tiePush.isPushOnly)
  }

  @Test
  fun verify_royale_missions_evaluator_and_claimable_states() {
    val profile = PlayerProfile(
      xp = 1000L,
      stats = com.example.model.PlayerStats(
        totalWagered = 30_000L,
        biggestWin = 3_000L,
        bestWinStreak = 4
      )
    )
    val snapshot = com.example.data.MissionsSnapshotData(
      dateKey = "2026-11-16",
      dailyRoundsAny = 5L,
      dailyCardRounds = 2L,
      dailyWheelReelRounds = 4L,
      dailyWinsAny = 3L,
      careerWinsAny = 3L,
      gamesSampledCount = 5L,
      claimedIds = setOf("daily_warmup")
    )
    val states = com.example.data.MissionsEvaluator.buildStates(snapshot, profile)
    val warmup = states.first { it.mission.id == "daily_warmup" }
    val cards = states.first { it.mission.id == "daily_card_strategist" }
    val grandTour = states.first { it.mission.id == "career_grand_tour" }

    assertTrue(warmup.isCompleted)
    assertTrue(warmup.isClaimed)
    assertFalse(warmup.isClaimable)

    assertFalse(cards.isCompleted)
    assertEquals(2L, cards.clampedProgress)

    assertTrue(grandTour.isCompleted)
    assertTrue(grandTour.isClaimable)
  }

  @Test
  fun verify_monthly_spending_limit_engine_thresholds_and_rollover() {
    val engine = com.example.data.MonthlySpendingLimitEngine(
      initialEnabled = true,
      initialMonthlyLimit = 10_000L,
      initialMonthlySpent = 0L,
      initialMonthKey = "2026-11"
    )

    // Under 80% (< 8,000): no alert
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.NONE,
      engine.recordWager(5_000L, monthKey = "2026-11")
    )
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.NONE,
      engine.recordWager(2_500L, monthKey = "2026-11")
    )
    assertEquals(7_500L, engine.state.monthlySpent)
    assertFalse(engine.state.isApproachingOrOver)

    // Crosses 80% threshold (7,500 + 1,000 = 8,500 >= 8,000): triggers APPROACHING_THRESHOLD
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.APPROACHING_THRESHOLD,
      engine.recordWager(1_000L, monthKey = "2026-11")
    )
    assertTrue(engine.state.isApproachingOrOver)
    assertEquals(1_500L, engine.state.remainingBeforeLimit)

    // Acknowledge 80% warning -> stays silent for wagers between 80% and 99%
    engine.acknowledgeAlert(monthKey = "2026-11")
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.NONE,
      engine.recordWager(1_000L, monthKey = "2026-11")
    )
    assertEquals(9_500L, engine.state.monthlySpent)

    // Crosses 100% threshold (9,500 + 1,000 = 10,500 >= 10,000): triggers LIMIT_REACHED
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.LIMIT_REACHED,
      engine.recordWager(1_000L, monthKey = "2026-11")
    )
    assertTrue(engine.state.isOverLimit)
    engine.acknowledgeAlert(monthKey = "2026-11")
    assertEquals(com.example.data.SpendingLimitAlertLevel.NONE, engine.state.pendingAlert)

    // Disabling limit suppresses alerts while still tracking spend
    engine.configureLimit(limitAmount = 5_000L, enabled = false, monthKey = "2026-11")
    assertEquals(
      com.example.data.SpendingLimitAlertLevel.NONE,
      engine.recordWager(500L, monthKey = "2026-11")
    )
    assertEquals(11_000L, engine.state.monthlySpent)

    // Calendar month rollover resets monthly spend and clears alert acknowledgments
    assertTrue(engine.syncMonth("2026-12"))
    assertEquals(0L, engine.state.monthlySpent)
    assertFalse(engine.state.acknowledgedApproachingMonth)
    assertFalse(engine.state.acknowledgedLimitReachedMonth)

    // Re-enable with 10,000 cap -> lowering limit to 2,000 after spending 1,700 (85%) triggers APPROACHING_THRESHOLD
    engine.configureLimit(limitAmount = 10_000L, enabled = true, monthKey = "2026-12")
    engine.recordWager(1_700L, monthKey = "2026-12")
    val tightened = engine.configureLimit(limitAmount = 2_000L, enabled = true, monthKey = "2026-12")
    assertEquals(com.example.data.SpendingLimitAlertLevel.APPROACHING_THRESHOLD, tightened.pendingAlert)
  }

  @Test
  fun verify_royale_loyalty_points_tiers_and_rewards_eligibility() {
    // Earn scaling from wager volume + minimum 5 RP per wager
    assertEquals(5L, com.example.model.RoyaleLoyaltyCatalog.calculatePointsEarned(50L, com.example.model.RoyaleLoyaltyTier.EMBER))
    assertEquals(40L, com.example.model.RoyaleLoyaltyCatalog.calculatePointsEarned(1_000L, com.example.model.RoyaleLoyaltyTier.EMBER))
    assertEquals(50L, com.example.model.RoyaleLoyaltyCatalog.calculatePointsEarned(1_000L, com.example.model.RoyaleLoyaltyTier.MOLTEN_GOLD))
    assertEquals(80L, com.example.model.RoyaleLoyaltyCatalog.calculatePointsEarned(1_000L, com.example.model.RoyaleLoyaltyTier.IMPERIAL_CROWN))
    assertEquals(0L, com.example.model.RoyaleLoyaltyCatalog.calculatePointsEarned(0L, com.example.model.RoyaleLoyaltyTier.EMBER))

    // Tier classification & progression math
    assertEquals(com.example.model.RoyaleLoyaltyTier.EMBER, com.example.model.RoyaleLoyaltyTier.tierForPoints(0L))
    assertEquals(com.example.model.RoyaleLoyaltyTier.CRIMSON, com.example.model.RoyaleLoyaltyTier.tierForPoints(250L))
    assertEquals(com.example.model.RoyaleLoyaltyTier.MOLTEN_GOLD, com.example.model.RoyaleLoyaltyTier.tierForPoints(1_000L))
    assertEquals(com.example.model.RoyaleLoyaltyTier.OBSIDIAN_PLATINUM, com.example.model.RoyaleLoyaltyTier.tierForPoints(3_500L))
    assertEquals(com.example.model.RoyaleLoyaltyTier.IMPERIAL_CROWN, com.example.model.RoyaleLoyaltyTier.tierForPoints(10_000L))

    assertEquals(0.5f, com.example.model.RoyaleLoyaltyCatalog.progressToNextTier(125L), 0.01f)
    assertEquals(125L, com.example.model.RoyaleLoyaltyCatalog.pointsRemainingToNextTier(125L))

    // Reward unlock eligibility checks tier gate, point balance, and non-duplication
    val emberReward = com.example.model.RoyaleLoyaltyCatalog.rewards.first { it.id == "ember_chips_starter" }
    val crimsonVault = com.example.model.RoyaleLoyaltyCatalog.rewards.first { it.id == "crimson_chip_vault" }

    val bronzeBalanceProfile = PlayerProfile(
      royalePoints = 300L,
      lifetimeRoyalePoints = 200L // Under 250 Crimson threshold even though 300 RP spendable
    )
    assertTrue(com.example.model.RoyaleLoyaltyCatalog.isRewardEligibleToUnlock(bronzeBalanceProfile, emberReward))
    assertFalse(com.example.model.RoyaleLoyaltyCatalog.isRewardEligibleToUnlock(bronzeBalanceProfile, crimsonVault))

    val crimsonProfile = bronzeBalanceProfile.copy(
      royalePoints = 300L,
      lifetimeRoyalePoints = 300L,
      unlockedLoyaltyRewardIds = setOf(emberReward.id)
    )
    assertFalse(com.example.model.RoyaleLoyaltyCatalog.isRewardEligibleToUnlock(crimsonProfile, emberReward))
    assertTrue(com.example.model.RoyaleLoyaltyCatalog.isRewardEligibleToUnlock(crimsonProfile, crimsonVault))
    assertEquals(2, com.example.model.RoyaleLoyaltyCatalog.countUnlockableRewards(crimsonProfile))
  }

  @Test
  fun verify_daily_bonus_wheel_catalog_cooldown_and_rotation_math() {
    val catalog = com.example.model.DailyBonusWheelCatalog
    val slices = catalog.slices
    assertEquals(8, slices.size)
    assertTrue(slices.any { it.prizeType == com.example.model.WheelPrizeType.VIRTUAL_CHIPS && it.amount >= 25_000L })
    assertTrue(slices.any { it.prizeType == com.example.model.WheelPrizeType.ROYALE_POINTS && it.amount >= 500L })

    // Never spun -> ready immediately
    val initialStatus = catalog.evaluateStatus(lastSpinTimestampMs = 0L, currentTimeMs = 1_700_000_000_000L)
    assertTrue(initialStatus.canSpin)
    assertEquals(0L, initialStatus.millisUntilNextSpin)
    assertEquals("Ready to Spin!", initialStatus.formattedCountdown)

    // 1 second after spin -> locked for 23h 59m 59s
    val spinTime = 1_700_000_000_000L
    val lockedOneSec = catalog.evaluateStatus(lastSpinTimestampMs = spinTime, currentTimeMs = spinTime + 1_000L)
    assertFalse(lockedOneSec.canSpin)
    assertEquals("23h 59m 59s", lockedOneSec.formattedCountdown)

    // 23h 59m 59s after spin -> still locked (1s left)
    val almost24h = catalog.evaluateStatus(
      lastSpinTimestampMs = spinTime,
      currentTimeMs = spinTime + catalog.COOLDOWN_MS - 1_000L
    )
    assertFalse(almost24h.canSpin)
    assertEquals(1_000L, almost24h.millisUntilNextSpin)
    assertEquals("00h 00m 01s", almost24h.formattedCountdown)

    // Exactly 24h elapsed -> unlocked
    val unlockedAt24h = catalog.evaluateStatus(
      lastSpinTimestampMs = spinTime,
      currentTimeMs = spinTime + catalog.COOLDOWN_MS
    )
    assertTrue(unlockedAt24h.canSpin)
    assertEquals(0L, unlockedAt24h.millisUntilNextSpin)

    // Verify pointer angle alignment places every slice dead-center at top (-90 deg)
    val sweep = 360f / slices.size
    var rot = 0f
    for (idx in slices.indices) {
      rot = catalog.computeTargetRotationDegrees(
        currentRotationDeg = rot,
        targetSliceIndex = idx,
        sliceCount = slices.size,
        extraFullSpins = 5
      )
      val sliceCenterAtStop = ((-90f + idx * sweep + sweep / 2f + rot) % 360f + 360f) % 360f
      assertEquals(270f, sliceCenterAtStop, 0.01f)
    }

    // Verify weighted picker always returns valid index
    val rng = kotlin.random.Random(1234)
    repeat(50) {
      val picked = catalog.pickWeightedSliceIndex(rng)
      assertTrue(picked in slices.indices)
    }
  }
}
