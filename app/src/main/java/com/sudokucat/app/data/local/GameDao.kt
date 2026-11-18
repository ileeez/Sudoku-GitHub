package com.sudokucat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = 'PLAYING' ORDER BY updatedAt DESC")
    fun observeActiveGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'PLAYING' AND mode = :mode ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveGame(mode: String): GameEntity?

    @Query("SELECT * FROM games WHERE status = 'PLAYING' AND mode = :mode ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveGame(mode: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status != 'PLAYING' ORDER BY updatedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 30): Flow<List<GameEntity>>

    @Query("SELECT COUNT(*) FROM games WHERE status = 'WON'")
    fun observeWins(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE status = 'LOST'")
    fun observeLosses(): Flow<Int>

    @Query("SELECT MIN(elapsedSeconds) FROM games WHERE status = 'WON'")
    fun observeBestTime(): Flow<Long?>

    @Query("SELECT AVG(elapsedSeconds) FROM games WHERE status = 'WON'")
    fun observeAverageTime(): Flow<Double?>

    @Query("SELECT AVG(mistakes) FROM games WHERE status IN ('WON', 'LOST')")
    fun observeAverageMistakes(): Flow<Double?>

    @Query("SELECT SUM(elapsedSeconds) FROM games WHERE status IN ('WON', 'LOST')")
    fun observeTotalTime(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM games WHERE status = 'WON' AND difficulty = :difficulty")
    fun observeWinsForDifficulty(difficulty: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE status = 'WON' AND mode = 'GUIDED'")
    fun observeGuidedWins(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE status = 'WON' AND mode = 'PHOTO'")
    fun observePhotoWins(): Flow<Int>

    @Query("DELETE FROM games WHERE status = 'PLAYING' AND mode = :mode")
    suspend fun deleteActiveForMode(mode: String)

    @Query("DELETE FROM games WHERE status != 'PLAYING'")
    suspend fun clearHistory()

    @Query("DELETE FROM games")
    suspend fun clearAll()

    @Query("UPDATE games SET status = 'LOST', updatedAt = :now WHERE status = 'PLAYING'")
    suspend fun abandonActiveGames(now: Long)
}
