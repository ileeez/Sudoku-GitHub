package com.sudokucat.app.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.sudokucat.app.data.prefs.UserSettings
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private data class PendingSpeech(val text: String, val onComplete: (() -> Unit)?)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: PendingSpeech? = null
    private var currentSettings = UserSettings()
    private var appliedRate: Float? = null
    private var appliedLanguage: String? = null
    private var utteranceCounter = 0L
    private var speaking = false
    private val completions = ConcurrentHashMap<String, () -> Unit>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            notify(pending?.onComplete)
            pending = null
            return
        }
        ready = true
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                speaking = true
            }

            override fun onDone(utteranceId: String?) {
                speaking = false
                finish(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speaking = false
                finish(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                speaking = false
                finish(utteranceId)
            }
        })
        applyLanguage(force = true)
        applyRate(force = true)
        pending?.let { speech ->
            pending = null
            if (currentSettings.voiceEnabled) speakNow(speech.text, speech.onComplete)
            else notify(speech.onComplete)
        }
    }

    fun updateSettings(settings: UserSettings) {
        val previous = currentSettings
        currentSettings = settings
        if (!ready) return
        if (settings.voiceEnabled != previous.voiceEnabled && !settings.voiceEnabled) {
            tts.stop()
            completions.clear()
            pending = null
        }
        if (settings.speechRate != previous.speechRate) applyRate()
        if (settings.language != previous.language) applyLanguage()
    }

    /**
     * @return true se la frase è stata messa in coda o verrà letta appena il motore
     *         TTS è pronto; false se la voce è disattivata o non disponibile.
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null): Boolean {
        if (text.isBlank() || !currentSettings.voiceEnabled) return false
        if (!ready) {
            pending = PendingSpeech(text, onComplete)
            return true
        }
        speakNow(text, onComplete)
        return true
    }

    fun stop() {
        pending = null
        completions.clear()
        speaking = false
        if (ready) tts.stop()
    }

    fun isSpeaking(): Boolean = speaking

    private fun speakNow(text: String, onComplete: (() -> Unit)?) {
        applyRate()
        // Una nuova frase interrompe la precedente: i suoi callback non devono
        // nascondere il messaggio nuovo.
        completions.clear()
        val utteranceId = "nero_${++utteranceCounter}"
        if (onComplete != null) completions[utteranceId] = onComplete
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            completions.remove(utteranceId)
            notify(onComplete)
        }
    }

    private fun applyRate(force: Boolean = false) {
        if (!ready) return
        val rate = currentSettings.speechRate.coerceIn(0.6f, 1.4f)
        if (!force && appliedRate == rate) return
        tts.setSpeechRate(rate)
        appliedRate = rate
    }

    /**
     * Imposta la lingua e una voce italiana locale una sola volta (o quando
     * cambia davvero). Assegnare la lingua a ogni emissione delle impostazioni
     * faceva cambiare voce a Nero perfino a metà frase.
     */
    private fun applyLanguage(force: Boolean = false) {
        if (!ready) return
        val languageKey = currentSettings.language
        if (!force && appliedLanguage == languageKey) return
        val locale = if (languageKey == "it") Locale.ITALIAN else Locale.getDefault()
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault())
        }
        if (languageKey == "it") {
            val voices = runCatching { tts.voices }.getOrNull().orEmpty()
            val stableVoice = voices.firstOrNull {
                it.locale.language == "it" && !it.isNetworkConnectionRequired
            } ?: voices.firstOrNull { it.locale.language == "it" }
            if (stableVoice != null) {
                runCatching { tts.voice = stableVoice }
            }
        }
        appliedLanguage = languageKey
    }

    private fun finish(utteranceId: String?) {
        val callback = utteranceId?.let { completions.remove(it) } ?: return
        notify(callback)
    }

    private fun notify(callback: (() -> Unit)?) {
        if (callback == null) return
        mainHandler.post(callback)
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }
}
