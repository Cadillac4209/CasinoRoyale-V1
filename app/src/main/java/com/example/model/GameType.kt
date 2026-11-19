package com.example.model

enum class GameType(
    val title: String,
    val subtitle: String,
    val tag: String,
    val minBet: Long,
    val maxBet: Long
) {
    VIDEO_POKER("Video Poker", "Jacks or Better & Double Up", "VP", 10L, 500L),
    SLOTS("Royale Slots", "5-Reel 9-Line Mega Jackpot", "SL", 5L, 1000L),
    BLACKJACK("Blackjack 21", "Classic Vegas Rules 3:2", "BJ", 10L, 5000L),
    ROULETTE("Roulette Royale", "European 37-Pocket Wheel", "RL", 5L, 2500L),
    BACCARAT("Royal Baccarat", "Punto Banco • 11:1 Pair Side Bet", "BC", 10L, 5000L)
}
