package com.example.model

enum class Suit(val symbol: String, val isRed: Boolean, val displayName: String) {
    HEARTS("♥", true, "Hearts"),
    DIAMONDS("♦", true, "Diamonds"),
    CLUBS("♣", false, "Clubs"),
    SPADES("♠", false, "Spades")
}

enum class Rank(val symbol: String, val blackjackValue: Int, val pokerValue: Int) {
    TWO("2", 2, 2),
    THREE("3", 3, 3),
    FOUR("4", 4, 4),
    FIVE("5", 5, 5),
    SIX("6", 6, 6),
    SEVEN("7", 7, 7),
    EIGHT("8", 8, 8),
    NINE("9", 9, 9),
    TEN("10", 10, 10),
    JACK("J", 10, 11),
    QUEEN("Q", 10, 12),
    KING("K", 10, 13),
    ACE("A", 11, 14);

    val isFaceCard: Boolean
        get() = this == JACK || this == QUEEN || this == KING
}

data class Card(
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = true,
    val id: String = "${rank.symbol}_${suit.symbol}"
) {
    val isRed: Boolean get() = suit.isRed
}
