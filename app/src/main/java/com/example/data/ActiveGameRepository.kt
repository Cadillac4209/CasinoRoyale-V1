package com.example.data

import com.example.data.local.dao.ActiveGameStateDao
import com.example.data.local.entity.ActiveGameStateEntity
import com.example.model.Card
import com.example.model.GameType
import com.example.model.Rank
import com.example.model.Suit
import com.example.ui.blackjack.BlackjackHand
import com.example.ui.roulette.ActiveRouletteBet
import com.example.ui.roulette.RouletteBetType
import com.example.ui.slots.SlotSymbol
import com.example.ui.videopoker.DoubleUpGame
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JsonClass(generateAdapter = true)
data class CardSnapshot(
    val suit: String,
    val rank: String,
    val isFaceUp: Boolean,
    val id: String
) {
    fun toCard(): Card = Card(
        suit = Suit.valueOf(suit),
        rank = Rank.valueOf(rank),
        isFaceUp = isFaceUp,
        id = id
    )

    companion object {
        fun fromCard(card: Card): CardSnapshot = CardSnapshot(
            suit = card.suit.name,
            rank = card.rank.name,
            isFaceUp = card.isFaceUp,
            id = card.id
        )
    }
}

@JsonClass(generateAdapter = true)
data class BlackjackHandSnapshot(
    val cards: List<CardSnapshot>,
    val bet: Long,
    val isBusted: Boolean,
    val isBlackjack: Boolean,
    val isDoubled: Boolean,
    val isStanding: Boolean
) {
    fun toBlackjackHand(): BlackjackHand = BlackjackHand(
        cards = cards.map { it.toCard() },
        bet = bet,
        isBusted = isBusted,
        isBlackjack = isBlackjack,
        isDoubled = isDoubled,
        isStanding = isStanding
    )

    companion object {
        fun fromBlackjackHand(hand: BlackjackHand): BlackjackHandSnapshot = BlackjackHandSnapshot(
            cards = hand.cards.map { CardSnapshot.fromCard(it) },
            bet = hand.bet,
            isBusted = hand.isBusted,
            isBlackjack = hand.isBlackjack,
            isDoubled = hand.isDoubled,
            isStanding = hand.isStanding
        )
    }
}

@JsonClass(generateAdapter = true)
data class ActiveBlackjackSnapshot(
    val stage: String,
    val selectedChip: String,
    val currentBet: Long,
    val previousBet: Long,
    val dealerHand: List<CardSnapshot>,
    val playerHands: List<BlackjackHandSnapshot>,
    val activeHandIndex: Int,
    val insuranceBet: Long,
    val statusMessage: String,
    val remainingShoeCards: List<CardSnapshot>
)

@JsonClass(generateAdapter = true)
data class ActiveSlotsSnapshot(
    val grid: List<List<String>>,
    val finalGrid: List<List<String>>,
    val activeLines: Int,
    val betPerLine: Long,
    val freeSpinsRemaining: Int,
    val lastWin: Long,
    val isSpinning: Boolean,
    val isFreeSpinActive: Boolean,
    val costPaid: Long,
    val statusText: String
) {
    fun toGridSymbols(): List<List<SlotSymbol>> =
        grid.map { col -> col.map { name -> SlotSymbol.valueOf(name) } }

    fun toFinalGridSymbols(): List<List<SlotSymbol>> =
        finalGrid.map { col -> col.map { name -> SlotSymbol.valueOf(name) } }

    companion object {
        fun symbolsToNames(matrix: List<List<SlotSymbol>>): List<List<String>> =
            matrix.map { col -> col.map { it.name } }
    }
}

@JsonClass(generateAdapter = true)
data class DoubleUpGameSnapshot(
    val dealerCard: CardSnapshot,
    val options: List<CardSnapshot>,
    val selectedIndex: Int?,
    val resultText: String,
    val isWin: Boolean?
) {
    fun toDoubleUpGame(): DoubleUpGame = DoubleUpGame(
        dealerCard = dealerCard.toCard(),
        options = options.map { it.toCard() },
        selectedIndex = selectedIndex,
        resultText = resultText,
        isWin = isWin
    )

    companion object {
        fun fromDoubleUpGame(game: DoubleUpGame): DoubleUpGameSnapshot = DoubleUpGameSnapshot(
            dealerCard = CardSnapshot.fromCard(game.dealerCard),
            options = game.options.map { CardSnapshot.fromCard(it) },
            selectedIndex = game.selectedIndex,
            resultText = game.resultText,
            isWin = game.isWin
        )
    }
}

@JsonClass(generateAdapter = true)
data class ActiveVideoPokerSnapshot(
    val gameState: String,
    val cards: List<CardSnapshot>,
    val heldIndices: List<Int>,
    val coinsBet: Int,
    val chipDenom: String,
    val winAmount: Long,
    val statusMessage: String,
    val doubleUpGame: DoubleUpGameSnapshot?,
    val remainingDeckCards: List<CardSnapshot>
)

@JsonClass(generateAdapter = true)
data class RouletteBetSnapshot(
    val key: String,
    val betTypeId: String,
    val straightNumber: Int? = null,
    val amount: Long
) {
    fun toActiveRouletteBet(): ActiveRouletteBet {
        val betType = when (betTypeId) {
            "STRAIGHT" -> RouletteBetType.Straight(straightNumber ?: 0)
            "RED" -> RouletteBetType.Red
            "BLACK" -> RouletteBetType.Black
            "EVEN" -> RouletteBetType.Even
            "ODD" -> RouletteBetType.Odd
            "LOW" -> RouletteBetType.Low
            "HIGH" -> RouletteBetType.High
            "DOZEN_1" -> RouletteBetType.Dozen1
            "DOZEN_2" -> RouletteBetType.Dozen2
            "DOZEN_3" -> RouletteBetType.Dozen3
            "COLUMN_1" -> RouletteBetType.Column1
            "COLUMN_2" -> RouletteBetType.Column2
            "COLUMN_3" -> RouletteBetType.Column3
            else -> RouletteBetType.Red
        }
        return ActiveRouletteBet(type = betType, amount = amount)
    }

    companion object {
        fun fromEntry(key: String, bet: ActiveRouletteBet): RouletteBetSnapshot {
            val (typeId, number) = when (val t = bet.type) {
                is RouletteBetType.Straight -> "STRAIGHT" to t.number
                RouletteBetType.Red -> "RED" to null
                RouletteBetType.Black -> "BLACK" to null
                RouletteBetType.Even -> "EVEN" to null
                RouletteBetType.Odd -> "ODD" to null
                RouletteBetType.Low -> "LOW" to null
                RouletteBetType.High -> "HIGH" to null
                RouletteBetType.Dozen1 -> "DOZEN_1" to null
                RouletteBetType.Dozen2 -> "DOZEN_2" to null
                RouletteBetType.Dozen3 -> "DOZEN_3" to null
                RouletteBetType.Column1 -> "COLUMN_1" to null
                RouletteBetType.Column2 -> "COLUMN_2" to null
                RouletteBetType.Column3 -> "COLUMN_3" to null
            }
            return RouletteBetSnapshot(
                key = key,
                betTypeId = typeId,
                straightNumber = number,
                amount = bet.amount
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class ActiveRouletteSnapshot(
    val selectedChip: String,
    val bets: List<RouletteBetSnapshot>,
    val isSpinning: Boolean,
    val targetNumber: Int?,
    val currentTotalBet: Long,
    val history: List<Int>,
    val statusMessage: String
)

@JsonClass(generateAdapter = true)
data class ActiveBaccaratSnapshot(
    val selectedChip: String,
    val playerBet: Long,
    val bankerBet: Long,
    val tieBet: Long,
    val pairBet: Long,
    val previousPlayerBet: Long,
    val previousBankerBet: Long,
    val previousTieBet: Long,
    val previousPairBet: Long,
    val isDealing: Boolean,
    val playerCards: List<CardSnapshot>,
    val bankerCards: List<CardSnapshot>,
    val historyCodes: List<String>,
    val statusMessage: String,
    val remainingShoeCards: List<CardSnapshot>
)

object GameStateJsonCodec {
    private val moshi: Moshi = Moshi.Builder().build()

    private val blackjackAdapter = moshi.adapter(ActiveBlackjackSnapshot::class.java)
    private val slotsAdapter = moshi.adapter(ActiveSlotsSnapshot::class.java)
    private val videoPokerAdapter = moshi.adapter(ActiveVideoPokerSnapshot::class.java)
    private val rouletteAdapter = moshi.adapter(ActiveRouletteSnapshot::class.java)
    private val baccaratAdapter = moshi.adapter(ActiveBaccaratSnapshot::class.java)

    fun encodeBlackjack(snapshot: ActiveBlackjackSnapshot): String = blackjackAdapter.toJson(snapshot)
    fun decodeBlackjack(json: String): ActiveBlackjackSnapshot? = runCatching {
        blackjackAdapter.fromJson(json)
    }.getOrNull()

    fun encodeSlots(snapshot: ActiveSlotsSnapshot): String = slotsAdapter.toJson(snapshot)
    fun decodeSlots(json: String): ActiveSlotsSnapshot? = runCatching {
        slotsAdapter.fromJson(json)
    }.getOrNull()

    fun encodeVideoPoker(snapshot: ActiveVideoPokerSnapshot): String = videoPokerAdapter.toJson(snapshot)
    fun decodeVideoPoker(json: String): ActiveVideoPokerSnapshot? = runCatching {
        videoPokerAdapter.fromJson(json)
    }.getOrNull()

    fun encodeRoulette(snapshot: ActiveRouletteSnapshot): String = rouletteAdapter.toJson(snapshot)
    fun decodeRoulette(json: String): ActiveRouletteSnapshot? = runCatching {
        rouletteAdapter.fromJson(json)
    }.getOrNull()

    fun encodeBaccarat(snapshot: ActiveBaccaratSnapshot): String = baccaratAdapter.toJson(snapshot)
    fun decodeBaccarat(json: String): ActiveBaccaratSnapshot? = runCatching {
        baccaratAdapter.fromJson(json)
    }.getOrNull()
}

class ActiveGameRepository(
    private val activeGameStateDao: ActiveGameStateDao
) {
    val activeGameStates: Flow<List<ActiveGameStateEntity>> =
        activeGameStateDao.observeActiveGameStates()

    val resumableGameTypes: Flow<Set<GameType>> = activeGameStates.map { entities ->
        entities.mapNotNull { entity ->
            runCatching { GameType.valueOf(entity.gameType) }.getOrNull()
        }.toSet()
    }

    fun observeGameState(gameType: GameType): Flow<ActiveGameStateEntity?> =
        activeGameStateDao.observeGameState(gameType.name)

    suspend fun saveBlackjackState(
        snapshot: ActiveBlackjackSnapshot,
        isInProgress: Boolean,
        currentBet: Long
    ) {
        activeGameStateDao.upsertActiveGameState(
            ActiveGameStateEntity(
                gameType = GameType.BLACKJACK.name,
                isInProgress = isInProgress,
                stageLabel = snapshot.stage,
                currentBet = currentBet,
                statePayloadJson = GameStateJsonCodec.encodeBlackjack(snapshot),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadBlackjackState(): ActiveBlackjackSnapshot? {
        val entity = activeGameStateDao.getActiveGameState(GameType.BLACKJACK.name) ?: return null
        return GameStateJsonCodec.decodeBlackjack(entity.statePayloadJson)
    }

    suspend fun saveSlotsState(
        snapshot: ActiveSlotsSnapshot,
        isInProgress: Boolean,
        currentBet: Long
    ) {
        activeGameStateDao.upsertActiveGameState(
            ActiveGameStateEntity(
                gameType = GameType.SLOTS.name,
                isInProgress = isInProgress,
                stageLabel = if (snapshot.isSpinning) "SPINNING" else if (snapshot.freeSpinsRemaining > 0) "FREE_SPINS" else "IDLE",
                currentBet = currentBet,
                statePayloadJson = GameStateJsonCodec.encodeSlots(snapshot),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadSlotsState(): ActiveSlotsSnapshot? {
        val entity = activeGameStateDao.getActiveGameState(GameType.SLOTS.name) ?: return null
        return GameStateJsonCodec.decodeSlots(entity.statePayloadJson)
    }

    suspend fun saveVideoPokerState(
        snapshot: ActiveVideoPokerSnapshot,
        isInProgress: Boolean,
        currentBet: Long
    ) {
        activeGameStateDao.upsertActiveGameState(
            ActiveGameStateEntity(
                gameType = GameType.VIDEO_POKER.name,
                isInProgress = isInProgress,
                stageLabel = snapshot.gameState,
                currentBet = currentBet,
                statePayloadJson = GameStateJsonCodec.encodeVideoPoker(snapshot),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadVideoPokerState(): ActiveVideoPokerSnapshot? {
        val entity = activeGameStateDao.getActiveGameState(GameType.VIDEO_POKER.name) ?: return null
        return GameStateJsonCodec.decodeVideoPoker(entity.statePayloadJson)
    }

    suspend fun saveRouletteState(
        snapshot: ActiveRouletteSnapshot,
        isInProgress: Boolean,
        currentBet: Long
    ) {
        activeGameStateDao.upsertActiveGameState(
            ActiveGameStateEntity(
                gameType = GameType.ROULETTE.name,
                isInProgress = isInProgress,
                stageLabel = if (snapshot.isSpinning) "SPINNING" else "BETTING",
                currentBet = currentBet,
                statePayloadJson = GameStateJsonCodec.encodeRoulette(snapshot),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadRouletteState(): ActiveRouletteSnapshot? {
        val entity = activeGameStateDao.getActiveGameState(GameType.ROULETTE.name) ?: return null
        return GameStateJsonCodec.decodeRoulette(entity.statePayloadJson)
    }

    suspend fun saveBaccaratState(
        snapshot: ActiveBaccaratSnapshot,
        isInProgress: Boolean,
        currentBet: Long
    ) {
        activeGameStateDao.upsertActiveGameState(
            ActiveGameStateEntity(
                gameType = GameType.BACCARAT.name,
                isInProgress = isInProgress,
                stageLabel = if (snapshot.isDealing) "DEALING" else "BETTING",
                currentBet = currentBet,
                statePayloadJson = GameStateJsonCodec.encodeBaccarat(snapshot),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadBaccaratState(): ActiveBaccaratSnapshot? {
        val entity = activeGameStateDao.getActiveGameState(GameType.BACCARAT.name) ?: return null
        return GameStateJsonCodec.decodeBaccarat(entity.statePayloadJson)
    }

    suspend fun clearGameState(gameType: GameType) {
        activeGameStateDao.deleteActiveGameState(gameType.name)
    }

    suspend fun clearAllGameStates() {
        activeGameStateDao.clearAllActiveGameStates()
    }
}
