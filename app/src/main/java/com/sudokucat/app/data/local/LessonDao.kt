package com.sudokucat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id")
    suspend fun getById(id: String): LessonProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun clearAll()
}

@Dao
interface AiCacheDao {
    @Query("SELECT * FROM ai_cache WHERE cacheKey = :key")
    suspend fun get(key: String): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(cache: AiCacheEntity)

    @Query("DELETE FROM ai_cache")
    suspend fun clear()

    @Query("DELETE FROM ai_cache WHERE createdAt < :threshold")
    suspend fun clearOlderThan(threshold: Long)
}
