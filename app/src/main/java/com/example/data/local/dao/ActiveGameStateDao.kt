package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ActiveGameStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveGameStateDao {
    @Query("SELECT * FROM active_game_states WHERE gameType = :gameType LIMIT 1")
    fun observeGameState(gameType: String): Flow<ActiveGameStateEntity?>

    @Query("SELECT * FROM active_game_states WHERE isInProgress = 1 ORDER BY updatedTimestamp DESC")
    fun observeActiveGameStates(): Flow<List<ActiveGameStateEntity>>

    @Query("SELECT * FROM active_game_states WHERE gameType = :gameType LIMIT 1")
    suspend fun getActiveGameState(gameType: String): ActiveGameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveGameState(state: ActiveGameStateEntity)

    @Query("DELETE FROM active_game_states WHERE gameType = :gameType")
    suspend fun deleteActiveGameState(gameType: String)

    @Query("DELETE FROM active_game_states")
    suspend fun clearAllActiveGameStates()
}
