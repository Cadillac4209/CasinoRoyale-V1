package com.example.ui.blackjack

import com.example.model.Card
import com.example.model.Rank

data class BlackjackHand(
    val cards: List<Card> = emptyList(),
    val bet: Long = 0L,
    val isBusted: Boolean = false,
    val isBlackjack: Boolean = false,
    val isDoubled: Boolean = false,
    val isStanding: Boolean = false
) {
    val totalScore: Int
        get() {
            var sum = 0
            var aces = 0
            for (card in cards) {
                if (card.rank == Rank.ACE) {
                    aces++
                    sum += 11
                } else {
                    sum += card.rank.blackjackValue
                }
            }
            while (sum > 21 && aces > 0) {
                sum -= 10
                aces--
            }
            return sum
        }

    val isSoft: Boolean
        get() {
            var sum = 0
            var aces = 0
            for (card in cards) {
                if (card.rank == Rank.ACE) {
                    aces++
                    sum += 11
                } else {
                    sum += card.rank.blackjackValue
                }
            }
            return aces > 0 && sum <= 21
        }

    val displayScore: String
        get() {
            if (cards.isEmpty()) return ""
            if (isBlackjack) return "BLACKJACK!"
            if (totalScore > 21) return "BUST ($totalScore)"
            val soft = isSoft
            return if (soft && totalScore < 21) {
                "${totalScore - 10} / $totalScore"
            } else {
                "$totalScore"
            }
        }

    val canSplit: Boolean
        get() = cards.size == 2 && cards[0].rank == cards[1].rank

    val canDouble: Boolean
        get() = cards.size == 2 && !isDoubled
}
