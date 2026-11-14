package com.example.model

class Deck(private val numberOfDecks: Int = 1) {
    private val cards = mutableListOf<Card>()

    init {
        reset()
    }

    fun reset() {
        cards.clear()
        repeat(numberOfDecks) {
            for (suit in Suit.values()) {
                for (rank in Rank.values()) {
                    cards.add(Card(suit = suit, rank = rank))
                }
            }
        }
        shuffle()
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun draw(): Card {
        if (cards.isEmpty()) {
            reset()
        }
        return cards.removeAt(0)
    }

    fun remainingCards(): Int = cards.size

    fun exportRemainingCards(): List<Card> = cards.toList()

    fun restoreCards(savedCards: List<Card>) {
        if (savedCards.isEmpty()) {
            reset()
        } else {
            cards.clear()
            cards.addAll(savedCards)
        }
    }
}
