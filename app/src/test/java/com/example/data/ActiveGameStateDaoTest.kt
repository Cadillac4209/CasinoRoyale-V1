package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CasinoDatabase
import com.example.data.local.dao.ActiveGameStateDao
import com.example.data.local.entity.ActiveGameStateEntity
import com.example.model.Card
import com.example.model.GameType
import com.example.model.Rank
import com.example.model.Suit
import com.example.ui.blackjack.BlackjackHand
import com.example.ui.slots.SlotSymbol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveGameStateDaoTest {

    private lateinit var database: CasinoDatabase
    private lateinit var dao: ActiveGameStateDao
    private lateinit var repository: ActiveGameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CasinoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.activeGameStateDao()
        repository = ActiveGameRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndQueryActiveGameState_returnsExactSavedEntity() = runBlocking {
        val entity = ActiveGameStateEntity(
            gameType = GameType.BLACKJACK.name,
            isInProgress = true,
            stageLabel = "PLAYER_TURN",
            currentBet = 250L,
            statePayloadJson = "{\"stage\":\"PLAYER_TURN\"}"
        )

        dao.upsertActiveGameState(entity)
        val loaded = dao.getActiveGameState(GameType.BLACKJACK.name)

        assertNotNull(loaded)
        assertEquals(GameType.BLACKJACK.name, loaded?.gameType)
        assertTrue(loaded?.isInProgress == true)
        assertEquals("PLAYER_TURN", loaded?.stageLabel)
        assertEquals(250L, loaded?.currentBet)
    }

    @Test
    fun observeActiveGameStates_filtersOnlyInProgressGamesAndUpdatesOnDelete() = runBlocking {
        val bjSnapshot = ActiveBlackjackSnapshot(
            stage = "PLAYER_TURN",
            selectedChip = "TWENTY_FIVE",
            currentBet = 100L,
            previousBet = 100L,
            dealerHand = listOf(CardSnapshot.fromCard(Card(Suit.SPADES, Rank.TEN))),
            playerHands = listOf(
                BlackjackHandSnapshot.fromBlackjackHand(
                    BlackjackHand(
                        cards = listOf(Card(Suit.HEARTS, Rank.EIGHT), Card(Suit.DIAMONDS, Rank.SEVEN)),
                        bet = 100L
                    )
                )
            ),
            activeHandIndex = 0,
            insuranceBet = 0L,
            statusMessage = "HIT OR STAND?",
            remainingShoeCards = emptyList()
        )
        repository.saveBlackjackState(bjSnapshot, isInProgress = true, currentBet = 100L)

        // Inactive roulette table chips should not appear in resumableGameTypes
        val rlSnapshot = ActiveRouletteSnapshot(
            selectedChip = "TEN",
            bets = emptyList(),
            isSpinning = false,
            targetNumber = null,
            currentTotalBet = 0L,
            history = listOf(17),
            statusMessage = "PLACE BETS"
        )
        repository.saveRouletteState(rlSnapshot, isInProgress = false, currentBet = 0L)

        val resumable = repository.resumableGameTypes.first()
        assertTrue(resumable.contains(GameType.BLACKJACK))
        assertFalse(resumable.contains(GameType.ROULETTE))

        val loadedBj = repository.loadBlackjackState()
        assertNotNull(loadedBj)
        assertEquals(100L, loadedBj?.currentBet)
        assertEquals(2, loadedBj?.playerHands?.first()?.cards?.size)

        repository.clearGameState(GameType.BLACKJACK)
        assertNull(repository.loadBlackjackState())
        assertTrue(repository.resumableGameTypes.first().isEmpty())
    }

    @Test
    fun saveAndRestoreSlotsFreeSpins_persistsGridAndRemainingSpins() = runBlocking {
        val grid = ActiveSlotsSnapshot.symbolsToNames(
            List(5) { listOf(SlotSymbol.SCATTER, SlotSymbol.SEVEN, SlotSymbol.WILD) }
        )
        val snapshot = ActiveSlotsSnapshot(
            grid = grid,
            finalGrid = grid,
            activeLines = 9,
            betPerLine = 50L,
            freeSpinsRemaining = 7,
            lastWin = 1200L,
            isSpinning = false,
            isFreeSpinActive = false,
            costPaid = 0L,
            statusText = "7 FREE SPINS LEFT!"
        )

        repository.saveSlotsState(snapshot, isInProgress = true, currentBet = 450L)
        val restored = repository.loadSlotsState()

        assertNotNull(restored)
        assertEquals(7, restored?.freeSpinsRemaining)
        assertEquals(50L, restored?.betPerLine)
        assertEquals(SlotSymbol.SCATTER, restored?.toGridSymbols()?.first()?.first())

        repository.clearAllGameStates()
        assertTrue(repository.activeGameStates.first().isEmpty())
    }
}
