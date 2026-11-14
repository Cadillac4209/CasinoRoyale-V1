package com.example.ui.roulette

import com.example.ui.theme.CasinoRed

enum class RouletteColor {
    RED, BLACK, GREEN
}

object RouletteData {
    // Official European Roulette Wheel sequence
    val WHEEL_NUMBERS = listOf(
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
        5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
    )

    val RED_NUMBERS = setOf(
        1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36
    )

    fun getColor(number: Int): RouletteColor = when {
        number == 0 -> RouletteColor.GREEN
        RED_NUMBERS.contains(number) -> RouletteColor.RED
        else -> RouletteColor.BLACK
    }
}

sealed class RouletteBetType(val label: String, val payoutRatio: Int) {
    data class Straight(val number: Int) : RouletteBetType("Number $number", 35)
    object Red : RouletteBetType("RED", 1)
    object Black : RouletteBetType("BLACK", 1)
    object Even : RouletteBetType("EVEN", 1)
    object Odd : RouletteBetType("ODD", 1)
    object Low : RouletteBetType("1 - 18", 1)
    object High : RouletteBetType("19 - 36", 1)
    object Dozen1 : RouletteBetType("1st 12", 2)
    object Dozen2 : RouletteBetType("2nd 12", 2)
    object Dozen3 : RouletteBetType("3rd 12", 2)
    object Column1 : RouletteBetType("Col 1", 2)
    object Column2 : RouletteBetType("Col 2", 2)
    object Column3 : RouletteBetType("Col 3", 2)

    fun isWinning(number: Int): Boolean {
        if (number == 0) {
            return this is Straight && this.number == 0
        }
        val isRed = RouletteData.RED_NUMBERS.contains(number)
        return when (this) {
            is Straight -> this.number == number
            is Red -> isRed
            is Black -> !isRed
            is Even -> number % 2 == 0
            is Odd -> number % 2 != 0
            is Low -> number in 1..18
            is High -> number in 19..36
            is Dozen1 -> number in 1..12
            is Dozen2 -> number in 13..24
            is Dozen3 -> number in 25..36
            is Column1 -> number % 3 == 1
            is Column2 -> number % 3 == 2
            is Column3 -> number % 3 == 0
        }
    }
}

data class ActiveRouletteBet(
    val type: RouletteBetType,
    val amount: Long
)
