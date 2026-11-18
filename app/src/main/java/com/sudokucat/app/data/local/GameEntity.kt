package com.sudokucat.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.domain.model.GameStatus
import com.sudokucat.app.domain.engine.SudokuEngine

@Entity(
    tableName = "games",
    indices = [Index("status"), Index("mode"), Index("updatedAt")]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val difficulty: String,
    val givens: String,
    val solution: String,
    val values: String,
    val notes: String,
    val mistakes: Int,
    val hints: Int,
    val elapsedSeconds: Long,
    val status: String,
    val xpAwarded: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
) {
    val difficultyEnum: Difficulty get() = Difficulty.from(difficulty)
    val modeEnum: GameMode get() = GameMode.from(mode)
    val statusEnum: GameStatus get() = GameStatus.from(status)
    val givenBoard: IntArray get() = SudokuEngine.parseOrEmpty(givens)
    val solutionBoard: IntArray get() = SudokuEngine.parseOrEmpty(solution)
    val valueBoard: IntArray get() = SudokuEngine.parseOrEmpty(values)
    val noteMasks: IntArray
        get() = notes.split(',').mapNotNull { it.toIntOrNull() }.let { list ->
            IntArray(81) { i -> list.getOrElse(i) { 0 } }
        }

    companion object {
        fun notesToString(notes: IntArray): String = notes.joinToString(",")
    }
}

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val completed: Boolean,
    val stars: Int,
    val bestScore: Int,
    val updatedAt: Long
)

@Entity(tableName = "ai_cache")
data class AiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val response: String,
    val createdAt: Long
)
