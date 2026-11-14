package com.example.ui.videopoker

import com.example.model.Card
import com.example.model.PokerVariant
import com.example.model.Rank

enum class PokerHandRank(val displayName: String, val multiplier: Int) {
    NATURAL_ROYAL_FLUSH("NATURAL ROYAL FLUSH", 800),
    FOUR_DEUCES("FOUR DEUCES (2s)", 200),
    WILD_ROYAL_FLUSH("WILD ROYAL FLUSH", 25),
    FIVE_OF_A_KIND("FIVE OF A KIND", 15),
    ROYAL_FLUSH("ROYAL FLUSH", 800),
    STRAIGHT_FLUSH("STRAIGHT FLUSH", 50),
    FOUR_OF_A_KIND("4 OF A KIND", 25),
    FULL_HOUSE("FULL HOUSE", 9),
    FLUSH("FLUSH", 6),
    STRAIGHT("STRAIGHT", 4),
    THREE_OF_A_KIND("3 OF A KIND", 3),
    TWO_PAIR("TWO PAIR", 2),
    JACKS_OR_BETTER("JACKS OR BETTER", 1),
    NOTHING("NO WIN", 0)
}

object PokerHandEvaluator {

    fun evaluate(cards: List<Card>, variant: PokerVariant = PokerVariant.JACKS_OR_BETTER): PokerHandRank {
        if (cards.size != 5) return PokerHandRank.NOTHING

        return if (variant == PokerVariant.DEUCES_WILD) {
            evaluateDeucesWild(cards)
        } else {
            evaluateJacksOrBetter(cards)
        }
    }

    private fun evaluateJacksOrBetter(cards: List<Card>): PokerHandRank {
        val ranks = cards.map { it.rank }.sortedByDescending { it.pokerValue }
        val suits = cards.map { it.suit }

        val isFlush = suits.distinct().size == 1

        val rankValues = ranks.map { it.pokerValue }
        var isStraight = rankValues.zipWithNext().all { (a, b) -> a - b == 1 }
        val isAceLowStraight = rankValues == listOf(14, 5, 4, 3, 2)
        if (isAceLowStraight) isStraight = true

        val rankCounts = ranks.groupingBy { it }.eachCount()
        val counts = rankCounts.values.sortedDescending()

        return when {
            isFlush && isStraight && rankValues.first() == 14 && rankValues[1] == 13 -> PokerHandRank.ROYAL_FLUSH
            isFlush && isStraight -> PokerHandRank.STRAIGHT_FLUSH
            counts == listOf(4, 1) -> PokerHandRank.FOUR_OF_A_KIND
            counts == listOf(3, 2) -> PokerHandRank.FULL_HOUSE
            isFlush -> PokerHandRank.FLUSH
            isStraight -> PokerHandRank.STRAIGHT
            counts == listOf(3, 1, 1) -> PokerHandRank.THREE_OF_A_KIND
            counts == listOf(2, 2, 1) -> PokerHandRank.TWO_PAIR
            counts.firstOrNull() == 2 -> {
                val pairRank = rankCounts.entries.first { it.value == 2 }.key
                if (pairRank.pokerValue >= Rank.JACK.pokerValue) {
                    PokerHandRank.JACKS_OR_BETTER
                } else {
                    PokerHandRank.NOTHING
                }
            }
            else -> PokerHandRank.NOTHING
        }
    }

    private fun evaluateDeucesWild(cards: List<Card>): PokerHandRank {
        val deucesCount = cards.count { it.rank == Rank.TWO }
        val nonDeuces = cards.filter { it.rank != Rank.TWO }

        if (deucesCount == 4) return PokerHandRank.FOUR_DEUCES

        val isNatural = deucesCount == 0
        val suits = nonDeuces.map { it.suit }
        val isSameSuit = suits.distinct().size <= 1

        val nonDeuceRanks = nonDeuces.map { it.rank.pokerValue }.distinct().sorted()

        // Check for 5 of a kind
        val maxSameRank = if (nonDeuces.isEmpty()) 0 else nonDeuces.groupingBy { it.rank }.eachCount().values.maxOrNull() ?: 0
        if (maxSameRank + deucesCount >= 5) {
            return PokerHandRank.FIVE_OF_A_KIND
        }

        // Check for Royal Flush (10, J, Q, K, A)
        val royalRanks = setOf(10, 11, 12, 13, 14)
        val nonDeuceRoyalCount = nonDeuces.count { royalRanks.contains(it.rank.pokerValue) }
        val allNonDeucesAreRoyal = nonDeuceRoyalCount == nonDeuces.size

        if (isSameSuit && allNonDeucesAreRoyal && nonDeuces.size + deucesCount == 5) {
            return if (isNatural) PokerHandRank.NATURAL_ROYAL_FLUSH else PokerHandRank.WILD_ROYAL_FLUSH
        }

        // Check 4 of a kind
        if (maxSameRank + deucesCount >= 4) {
            return PokerHandRank.FOUR_OF_A_KIND
        }

        // Check Straight Flush
        if (isSameSuit) {
            val span = if (nonDeuceRanks.isNotEmpty()) nonDeuceRanks.last() - nonDeuceRanks.first() else 0
            if (span <= 4) {
                return PokerHandRank.STRAIGHT_FLUSH
            }
        }

        // Check Full House
        val nonDeuceCounts = nonDeuces.groupingBy { it.rank }.eachCount().values.sortedDescending()
        if (deucesCount == 1) {
            if (nonDeuceCounts == listOf(2, 2)) return PokerHandRank.FULL_HOUSE
        } else if (deucesCount == 0) {
            if (nonDeuceCounts == listOf(3, 2)) return PokerHandRank.FULL_HOUSE
        }

        // Check Flush
        if (isSameSuit) {
            return PokerHandRank.FLUSH
        }

        // Check Straight
        val span = if (nonDeuceRanks.isNotEmpty()) nonDeuceRanks.last() - nonDeuceRanks.first() else 0
        if (nonDeuceRanks.size + deucesCount == 5 && span <= 4) {
            return PokerHandRank.STRAIGHT
        }

        // Check 3 of a Kind
        if (maxSameRank + deucesCount >= 3) {
            return PokerHandRank.THREE_OF_A_KIND
        }

        return PokerHandRank.NOTHING
    }
}
