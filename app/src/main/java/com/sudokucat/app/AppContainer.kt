package com.sudokucat.app

import android.content.Context
import com.sudokucat.app.ai.CoachService
import com.sudokucat.app.ai.GeminiClient
import com.sudokucat.app.data.local.AppDatabase
import com.sudokucat.app.data.prefs.ProfileRepository
import com.sudokucat.app.data.prefs.SecureKeyStore
import com.sudokucat.app.data.prefs.SettingsRepository
import com.sudokucat.app.data.prefs.sudokuDataStore
import com.sudokucat.app.data.prefs.sudokuSecretDataStore
import com.sudokucat.app.data.repo.GameRepository
import com.sudokucat.app.data.repo.LessonRepository
import com.sudokucat.app.data.repo.StatsRepository
import com.sudokucat.app.util.CatController
import com.sudokucat.app.util.SoundManager
import com.sudokucat.app.util.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: AppDatabase = AppDatabase.getInstance(context)
    val settingsRepository = SettingsRepository(
        dataStore = context.sudokuDataStore,
        secretStore = context.sudokuSecretDataStore,
        secureKeyStore = SecureKeyStore(),
        bundledApiKey = BuildConfig.GEMINI_API_KEY
    )
    val profileRepository = ProfileRepository(context.sudokuDataStore)
    val gameRepository = GameRepository(database.gameDao())
    val statsRepository = StatsRepository(database.gameDao())
    val lessonRepository = LessonRepository(database.lessonDao())
    val geminiClient = GeminiClient()
    val coachService = CoachService(
        geminiClient,
        settingsRepository,
        profileRepository,
        database.aiCacheDao()
    )
    val soundManager = SoundManager(context)
    val ttsManager = TtsManager(context)
    val catController = CatController(appScope, ttsManager, soundManager)

    init {
        appScope.launch(Dispatchers.IO) {
            runCatching {
                settingsRepository.ensureDefaultApiKey(BuildConfig.GEMINI_API_KEY)
            }
        }
        appScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                soundManager.updateSettings(settings)
                ttsManager.updateSettings(settings)
            }
        }
        appScope.launch {
            profileRepository.touchActivity()
            runCatching {
                database.aiCacheDao().clearOlderThan(
                    System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                )
            }
        }
    }

    suspend fun resetProgress() {
        gameRepository.clearAll()
        lessonRepository.clearAll()
        database.aiCacheDao().clear()
        profileRepository.reset()
    }

    fun shutdown() {
        ttsManager.shutdown()
        soundManager.release()
    }
}
