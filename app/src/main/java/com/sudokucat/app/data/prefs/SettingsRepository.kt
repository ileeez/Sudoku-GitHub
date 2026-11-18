package com.sudokucat.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sudokucat.app.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.sudokuDataStore: DataStore<Preferences> by preferencesDataStore(name = "sudoku_cat_settings")

/** Archivio separato per la chiave API cifrata: escluso dai backup di sistema. */
val Context.sudokuSecretDataStore: DataStore<Preferences> by preferencesDataStore(name = "sudoku_cat_secret")

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Chiaro"),
    DARK("Scuro");

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val soundVolume: Float = 0.55f,
    val voiceEnabled: Boolean = true,
    val speechRate: Float = 0.95f,
    val language: String = "it",
    val defaultDifficulty: Difficulty = Difficulty.MEDIUM,
    val highlightErrors: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val showTimer: Boolean = true,
    val catEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val aiEnabled: Boolean = true,
    val geminiOcrEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.8-flash"
) {
    val hasApiKey: Boolean get() = geminiApiKey.isNotBlank()
}

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val secretStore: DataStore<Preferences>,
    private val secureKeyStore: SecureKeyStore,
    /** Chiave inclusa nella build: usata subito come fallback, prima che la semina cifrata finisca. */
    private val bundledApiKey: String = ""
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val soundVolume = floatPreferencesKey("sound_volume")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val speechRate = floatPreferencesKey("speech_rate")
        val language = stringPreferencesKey("language")
        val difficulty = stringPreferencesKey("default_difficulty")
        val highlightErrors = booleanPreferencesKey("highlight_errors")
        val autoRemoveNotes = booleanPreferencesKey("auto_remove_notes")
        val showTimer = booleanPreferencesKey("show_timer")
        val catEnabled = booleanPreferencesKey("cat_enabled")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val aiEnabled = booleanPreferencesKey("ai_enabled")
        val geminiOcrEnabled = booleanPreferencesKey("gemini_ocr_enabled")
        val geminiApiKey = stringPreferencesKey("gemini_api_key_encrypted")
        val geminiModel = stringPreferencesKey("gemini_model")
    }

    val settings: Flow<UserSettings> = combine(
        dataStore.data.catch { emit(emptyPreferences()) },
        secretStore.data.catch { emit(emptyPreferences()) }
    ) { prefs, secret ->
        UserSettings(
            theme = ThemeMode.from(prefs[Keys.theme]),
            soundVolume = prefs[Keys.soundVolume] ?: 0.55f,
            voiceEnabled = prefs[Keys.voiceEnabled] ?: true,
            speechRate = prefs[Keys.speechRate] ?: 0.95f,
            language = prefs[Keys.language] ?: "it",
            defaultDifficulty = Difficulty.from(prefs[Keys.difficulty]),
            highlightErrors = prefs[Keys.highlightErrors] ?: true,
            autoRemoveNotes = prefs[Keys.autoRemoveNotes] ?: true,
            showTimer = prefs[Keys.showTimer] ?: true,
            catEnabled = prefs[Keys.catEnabled] ?: true,
            hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
            aiEnabled = prefs[Keys.aiEnabled] ?: true,
            geminiOcrEnabled = prefs[Keys.geminiOcrEnabled] ?: true,
            // La chiave della build è disponibile da subito: la copia cifrata nei
            // DataStore è solo la persistenza, non il gate per usarla.
            geminiApiKey = secureKeyStore.decrypt(secret[Keys.geminiApiKey] ?: "")
                .ifBlank { bundledApiKey },
            geminiModel = prefs[Keys.geminiModel] ?: "gemini-3.8-flash"
        )
    }

    suspend fun snapshot(): UserSettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = update { it[Keys.theme] = mode.name }
    suspend fun setSoundVolume(value: Float) = update { it[Keys.soundVolume] = value.coerceIn(0f, 1f) }
    suspend fun setVoiceEnabled(enabled: Boolean) = update { it[Keys.voiceEnabled] = enabled }
    suspend fun setSpeechRate(value: Float) = update { it[Keys.speechRate] = value.coerceIn(0.6f, 1.4f) }
    suspend fun setLanguage(value: String) = update { it[Keys.language] = value }
    suspend fun setDefaultDifficulty(value: Difficulty) = update { it[Keys.difficulty] = value.name }
    suspend fun setHighlightErrors(value: Boolean) = update { it[Keys.highlightErrors] = value }
    suspend fun setAutoRemoveNotes(value: Boolean) = update { it[Keys.autoRemoveNotes] = value }
    suspend fun setShowTimer(value: Boolean) = update { it[Keys.showTimer] = value }
    suspend fun setCatEnabled(value: Boolean) = update { it[Keys.catEnabled] = value }
    suspend fun setHapticsEnabled(value: Boolean) = update { it[Keys.hapticsEnabled] = value }
    suspend fun setAiEnabled(value: Boolean) = update { it[Keys.aiEnabled] = value }
    suspend fun setGeminiOcrEnabled(value: Boolean) = update { it[Keys.geminiOcrEnabled] = value }
    suspend fun setGeminiModel(value: String) = update {
        it[Keys.geminiModel] = value.trim().ifBlank { "gemini-3.8-flash" }
    }

    suspend fun setGeminiApiKey(value: String) = secretStore.edit {
        if (value.isBlank()) it.remove(Keys.geminiApiKey)
        else it[Keys.geminiApiKey] = secureKeyStore.encrypt(value.trim())
    }

    /**
     * Se la build contiene una chiave personale preimpostata, la salva cifrata
     * al primo avvio. Non sovrascrive mai una chiave già presente o inserita
     * manualmente dall'utente.
     */
    suspend fun ensureDefaultApiKey(defaultKey: String) {
        if (defaultKey.isBlank()) return
        val stored = secretStore.data.catch { emit(emptyPreferences()) }.first()[Keys.geminiApiKey]
        val current = runCatching { secureKeyStore.decrypt(stored ?: "") }.getOrDefault("")
        if (current.isNotBlank()) return
        val encrypted = runCatching { secureKeyStore.encrypt(defaultKey.trim()) }.getOrDefault("")
        // Se Keystore non è disponibile, la chiave resta utilizzabile in memoria
        // tramite bundledApiKey e verrà riscritta al prossimo avvio.
        if (encrypted.isNotBlank()) {
            runCatching { secretStore.edit { it[Keys.geminiApiKey] = encrypted } }
        }
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}

data class UserProfile(
    val xp: Int = 0,
    val streak: Int = 0,
    val lastActiveDay: Long = 0,
    val aiCallsToday: Int = 0,
    val aiCallsDay: Long = 0
) {
    val level: Int get() = 1 + xp / 250
    val xpInLevel: Int get() = xp % 250
    val xpForNextLevel: Int get() = 250
    val levelProgress: Float get() = xpInLevel / 250f
}

class ProfileRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val xp = intPreferencesKey("profile_xp")
        val streak = intPreferencesKey("profile_streak")
        val lastActiveDay = longPreferencesKey("profile_last_active_day")
        val aiCallsToday = intPreferencesKey("ai_calls_today")
        val aiCallsDay = longPreferencesKey("ai_calls_day")
    }

    val profile: Flow<UserProfile> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserProfile(
                xp = prefs[Keys.xp] ?: 0,
                streak = prefs[Keys.streak] ?: 0,
                lastActiveDay = prefs[Keys.lastActiveDay] ?: 0,
                aiCallsToday = prefs[Keys.aiCallsToday] ?: 0,
                aiCallsDay = prefs[Keys.aiCallsDay] ?: 0
            )
        }

    suspend fun addXp(amount: Int) {
        if (amount <= 0) return
        dataStore.edit { prefs ->
            prefs[Keys.xp] = (prefs[Keys.xp] ?: 0) + amount
        }
        touchActivity()
    }

    suspend fun touchActivity() {
        val today = LocalDate.now().toEpochDay()
        dataStore.edit { prefs ->
            val last = prefs[Keys.lastActiveDay] ?: 0L
            if (last != today) {
                val current = prefs[Keys.streak] ?: 0
                prefs[Keys.streak] = if (last == today - 1) current + 1 else 1
                prefs[Keys.lastActiveDay] = today
            }
        }
    }

    /** Limite locale anti-abuso: massimo 40 richieste AI al giorno. */
    suspend fun canCallAi(): Boolean {
        val today = LocalDate.now().toEpochDay()
        val prefs = dataStore.data.first()
        val day = prefs[Keys.aiCallsDay] ?: 0L
        val calls = if (day == today) prefs[Keys.aiCallsToday] ?: 0 else 0
        return calls < 40
    }

    suspend fun registerAiCall() {
        val today = LocalDate.now().toEpochDay()
        dataStore.edit { prefs ->
            val day = prefs[Keys.aiCallsDay] ?: 0L
            if (day != today) {
                prefs[Keys.aiCallsDay] = today
                prefs[Keys.aiCallsToday] = 1
            } else {
                prefs[Keys.aiCallsToday] = (prefs[Keys.aiCallsToday] ?: 0) + 1
            }
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.xp)
            prefs.remove(Keys.streak)
            prefs.remove(Keys.lastActiveDay)
            prefs.remove(Keys.aiCallsToday)
            prefs.remove(Keys.aiCallsDay)
        }
    }
}
