package com.sudokucat.app.data.repo

import com.sudokucat.app.data.local.GameDao
import com.sudokucat.app.data.local.GameEntity
import com.sudokucat.app.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class StatsData(
    val wins: Int = 0,
    val losses: Int = 0,
    val bestTime: Long? = null,
    val averageTime: Long? = null,
    val averageMistakes: Double = 0.0,
    val totalTime: Long = 0,
    val guidedWins: Int = 0,
    val photoWins: Int = 0,
    val winsByDifficulty: Map<Difficulty, Int> = emptyMap(),
    val history: List<GameEntity> = emptyList()
) {
    val completed: Int get() = wins + losses
    val winRate: Float get() = if (completed == 0) 0f else wins.toFloat() / completed
}

class StatsRepository(private val dao: GameDao) {
    val stats: Flow<StatsData> = combine(
        dao.observeWins(),
        dao.observeLosses(),
        dao.observeBestTime(),
        dao.observeAverageTime(),
        dao.observeAverageMistakes(),
        dao.observeTotalTime(),
        dao.observeGuidedWins(),
        dao.observePhotoWins(),
        dao.observeHistory(20)
    ) { values ->
        StatsData(
            wins = values[0] as Int,
            losses = values[1] as Int,
            bestTime = values[2] as Long?,
            averageTime = (values[3] as Double?)?.toLong(),
            averageMistakes = values[4] as Double? ?: 0.0,
            totalTime = values[5] as Long? ?: 0L,
            guidedWins = values[6] as Int,
            photoWins = values[7] as Int,
            history = values[8] as List<GameEntity>
        )
    }.combineDifficultyStats()

    private fun Flow<StatsData>.combineDifficultyStats(): Flow<StatsData> =
        combine(
            this,
            dao.observeWinsForDifficulty(Difficulty.EASY.name),
            dao.observeWinsForDifficulty(Difficulty.MEDIUM.name),
            dao.observeWinsForDifficulty(Difficulty.HARD.name),
            dao.observeWinsForDifficulty(Difficulty.EXPERT.name)
        ) { data, easy, medium, hard, expert ->
            data.copy(
                winsByDifficulty = mapOf(
                    Difficulty.EASY to easy,
                    Difficulty.MEDIUM to medium,
                    Difficulty.HARD to hard,
                    Difficulty.EXPERT to expert
                )
            )
        }
}
