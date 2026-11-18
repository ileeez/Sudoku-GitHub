package com.sudokucat.app.data.repo

import com.sudokucat.app.data.local.GameDao
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.domain.model.GameStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameRepository(private val dao: GameDao) {

    fun observeActive(mode: GameMode = GameMode.CLASSIC): Flow<GameEntity?> =
        dao.observeActiveGame(mode.name)

    fun observeAllActive(): Flow<List<GameEntity>> = dao.observeActiveGames()

    fun observeById(id: Long): Flow<GameEntity?> = dao.observeById(id)

    fun observeHistory(limit: Int = 30): Flow<List<GameEntity>> = dao.observeHistory(limit)

    suspend fun get(id: Long): GameEntity? = dao.getById(id)

    suspend fun active(mode: GameMode): GameEntity? = dao.getActiveGame(mode.name)

    suspend fun createGame(
        difficulty: Difficulty,
        mode: GameMode,
        random: Random = Random.Default
    ): Long = withContext(Dispatchers.Default) {
        val generated = SudokuEngine.generate(difficulty, random)
        val now = System.currentTimeMillis()
        dao.deleteActiveForMode(mode.name)
        dao.insert(
            GameEntity(
                mode = mode.name,
                difficulty = difficulty.name,
                givens = SudokuEngine.serialize(generated.givens),
                solution = SudokuEngine.serialize(generated.solution),
                values = SudokuEngine.serialize(generated.givens),
                notes = IntArray(81).joinToString(","),
                mistakes = 0,
                hints = 0,
                elapsedSeconds = 0,
                status = GameStatus.PLAYING.name,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun recordPhotoSession(
        puzzle: IntArray,
        solution: IntArray,
        elapsedSeconds: Long
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.insert(
            GameEntity(
                mode = GameMode.PHOTO.name,
                difficulty = Difficulty.MEDIUM.name,
                givens = SudokuEngine.serialize(puzzle),
                solution = SudokuEngine.serialize(solution),
                values = SudokuEngine.serialize(solution),
                notes = IntArray(81).joinToString(","),
                mistakes = 0,
                hints = 0,
                elapsedSeconds = elapsedSeconds,
                status = GameStatus.WON.name,
                xpAwarded = true,
                createdAt = now,
                updatedAt = now,
                completedAt = now
            )
        )
    }

    suspend fun save(entity: GameEntity) {
        dao.update(entity)
    }

    suspend fun clearHistory() = dao.clearHistory()
    suspend fun clearAll() = dao.clearAll()
    suspend fun abandonActive() = dao.abandonActiveGames(System.currentTimeMillis())
}
