package com.sudokucat.app.data.repo

import com.sudokucat.app.data.local.LessonDao
import com.sudokucat.app.data.local.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

class LessonRepository(private val dao: LessonDao) {
    fun observeProgress(): Flow<List<LessonProgressEntity>> = dao.observeAll()

    suspend fun getProgress(id: String): LessonProgressEntity? = dao.getById(id)

    suspend fun saveResult(
        lessonId: String,
        completed: Boolean,
        stars: Int,
        score: Int
    ) {
        val previous = dao.getById(lessonId)
        dao.upsert(
            LessonProgressEntity(
                lessonId = lessonId,
                completed = previous?.completed == true || completed,
                stars = maxOf(previous?.stars ?: 0, stars),
                bestScore = maxOf(previous?.bestScore ?: 0, score),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAll() = dao.clearAll()
}
