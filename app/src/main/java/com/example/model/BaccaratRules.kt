package com.example.model

enum class BaccaratBetSpot(
    val title: String,
    val payoutLabel: String
) {
    PLAYER("PLAYER", "1:1"),
    BANKER("BANKER", "0.95:1"),
    TIE("TIE", "8:1"),
    DRAGON_PAIR("PAIR", "11:1")
}

enum class BaccaratOutcome(
    val shortCode: String,
    val displayTitle: String
) {
    PLAYER_WIN("P", "PLAYER WINS!"),
    BANKER_WIN("B", "BANKER WINS!"),
    TIE("T", "NATURAL TIE!")
}

data class BaccaratSimulatedDeal(
    val playerInitial: List<Card>,
    val bankerInitial: List<Card>,
    val playerThirdCard: Card?,
    val bankerThirdCard: Card?,
    val isNatural: Boolean,
    val playerFinalScore: Int,
    val bankerFinalScore: Int,
    val outcome: BaccaratOutcome,
    val hasPlayerPair: Boolean,
    val hasBankerPair: Boolean
) {
    val playerFinalCards: List<Card> get() = if (playerThirdCard != null) playerInitial + playerThirdCard else playerInitial
    val bankerFinalCards: List<Card> get() = if (bankerThirdCard != null) bankerInitial + bankerThirdCard else bankerInitial
    val hasAnyPair: Boolean get() = hasPlayerPair || hasBankerPair
}

data class BaccaratPayoutBreakdown(
    val totalReturned: Long,
    val isPushOnly: Boolean,
    val netGain: Long,
    val winningLabels: List<String>
)

object BaccaratRules {

    fun cardPoint(card: Card): Int {
        return when (card.rank) {
            Rank.ACE -> 1
            Rank.TWO -> 2
            Rank.THREE -> 3
            Rank.FOUR -> 4
            Rank.FIVE -> 5
            Rank.SIX -> 6
            Rank.SEVEN -> 7
            Rank.EIGHT -> 8
            Rank.NINE -> 9
            Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 0
        }
    }

    fun handScore(cards: List<Card>): Int {
        if (cards.isEmpty()) return 0
        return cards.sumOf { cardPoint(it) } % 10
    }

    fun isPair(cards: List<Card>): Boolean {
        if (cards.size < 2) return false
        return cards[0].rank == cards[1].rank
    }

    fun shouldPlayerDrawThirdCard(playerInitialScore: Int): Boolean {
        return playerInitialScore in 0..5
    }

    /**
     * Authentic Macau / Vegas Punto Banco Banker Third-Card Tableau.
     */
    fun shouldBankerDrawThirdCard(
        bankerInitialScore: Int,
        playerThirdCardPoint: Int?
    ): Boolean {
        if (playerThirdCardPoint == null) {
            return bankerInitialScore in 0..5
        }
        return when (bankerInitialScore) {
            0, 1, 2 -> true
            3 -> playerThirdCardPoint != 8
            4 -> playerThirdCardPoint in 2..7
            5 -> playerThirdCardPoint in 4..7
            6 -> playerThirdCardPoint in 6..7
            else -> false
        }
    }

    fun resolveFromCards(
        playerInitial: List<Card>,
        bankerInitial: List<Card>,
        drawNextCard: () -> Card
    ): BaccaratSimulatedDeal {
        require(playerInitial.size == 2 && bankerInitial.size == 2) {
            "Baccarat requires exactly 2 initial cards for Player and Banker."
        }
        val pScore = handScore(playerInitial)
        val bScore = handScore(bankerInitial)
        val isNatural = pScore >= 8 || bScore >= 8

        var pThird: Card? = null
        var bThird: Card? = null

        if (!isNatural) {
            if (shouldPlayerDrawThirdCard(pScore)) {
                pThird = drawNextCard()
            }
            val pThirdPoint = pThird?.let { cardPoint(it) }
            if (shouldBankerDrawThirdCard(bScore, pThirdPoint)) {
                bThird = drawNextCard()
            }
        }

        val finalPlayerCards = if (pThird != null) playerInitial + pThird else playerInitial
        val finalBankerCards = if (bThird != null) bankerInitial + bThird else bankerInitial
        val finalPlayerScore = handScore(finalPlayerCards)
        val finalBankerScore = handScore(finalBankerCards)

        val outcome = when {
            finalPlayerScore > finalBankerScore -> BaccaratOutcome.PLAYER_WIN
            finalBankerScore > finalPlayerScore -> BaccaratOutcome.BANKER_WIN
            else -> BaccaratOutcome.TIE
        }

        return BaccaratSimulatedDeal(
            playerInitial = playerInitial,
            bankerInitial = bankerInitial,
            playerThirdCard = pThird,
            bankerThirdCard = bThird,
            isNatural = isNatural,
            playerFinalScore = finalPlayerScore,
            bankerFinalScore = finalBankerScore,
            outcome = outcome,
            hasPlayerPair = isPair(playerInitial),
            hasBankerPair = isPair(bankerInitial)
        )
    }

    fun calculatePayouts(
        bets: Map<BaccaratBetSpot, Long>,
        outcome: BaccaratOutcome,
        hasAnyPair: Boolean
    ): BaccaratPayoutBreakdown {
        var totalReturn = 0L
        var totalStaked = 0L
        val labels = mutableListOf<String>()

        bets.forEach { (spot, wager) ->
            if (wager <= 0L) return@forEach
            totalStaked += wager
            when (spot) {
                BaccaratBetSpot.PLAYER -> {
                    when (outcome) {
                        BaccaratOutcome.PLAYER_WIN -> {
                            totalReturn += wager * 2L
                            labels.add("Player 1:1")
                        }
                        BaccaratOutcome.TIE -> {
                            totalReturn += wager
                            labels.add("Player Push")
                        }
                        BaccaratOutcome.BANKER_WIN -> Unit
                    }
                }
                BaccaratBetSpot.BANKER -> {
                    when (outcome) {
                        BaccaratOutcome.BANKER_WIN -> {
                            // Standard 5% commission on Banker winnings (19:20 profit + original stake)
                            val profit = ((wager * 19L) / 20L).coerceAtLeast(1L)
                            totalReturn += wager + profit
                            labels.add("Banker 0.95:1")
                        }
                        BaccaratOutcome.TIE -> {
                            totalReturn += wager
                            labels.add("Banker Push")
                        }
                        BaccaratOutcome.PLAYER_WIN -> Unit
                    }
                }
                BaccaratBetSpot.TIE -> {
                    if (outcome == BaccaratOutcome.TIE) {
                        totalReturn += wager * 9L // 8:1 profit + original stake
                        labels.add("Tie 8:1")
                    }
                }
                BaccaratBetSpot.DRAGON_PAIR -> {
                    if (hasAnyPair) {
                        totalReturn += wager * 12L // 11:1 profit + original stake
                        labels.add("Pair 11:1")
                    }
                }
            }
        }

        val onlyPush = outcome == BaccaratOutcome.TIE &&
            (bets[BaccaratBetSpot.TIE] ?: 0L) == 0L &&
            !hasAnyPair &&
            totalReturn > 0L

        return BaccaratPayoutBreakdown(
            totalReturned = totalReturn,
            isPushOnly = onlyPush,
            netGain = totalReturn - totalStaked,
            winningLabels = labels
        )
    }
}
