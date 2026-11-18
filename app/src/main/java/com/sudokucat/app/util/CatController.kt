package com.sudokucat.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CatExpression {
    HAPPY,
    CURIOUS,
    FOCUSED,
    CONFUSED,
    SAD,
    EXCITED,
    SLEEPY
}

data class CatState(
    val expression: CatExpression = CatExpression.CURIOUS,
    val message: String? = null,
    val bubbleVisible: Boolean = false,
    val reactionToken: Long = 0L
)

class CatController(
    private val scope: CoroutineScope,
    private val ttsManager: TtsManager,
    private val soundManager: SoundManager
) {
    private val _state = MutableStateFlow(CatState())
    val state: StateFlow<CatState> = _state.asStateFlow()
    private var hideJob: Job? = null
    private var safetyHideJob: Job? = null
    private var bubbleShownAt = 0L
    private var bubbleMessage: String? = null

    fun greet() {
        say(
            "Miao! Sono Nero. Ogni numero ha il suo posto: troviamolo insieme, un passo alla volta.",
            CatExpression.HAPPY,
            speak = false,
            autoHide = true
        )
    }

    fun react(expression: CatExpression, sound: Boolean = false, token: Boolean = true) {
        _state.value = _state.value.copy(
            expression = expression,
            reactionToken = if (token) System.currentTimeMillis() else _state.value.reactionToken
        )
        if (sound) {
            when (expression) {
                CatExpression.SAD -> soundManager.error()
                CatExpression.EXCITED, CatExpression.HAPPY -> soundManager.correct()
                else -> Unit
            }
        }
    }

    fun say(
        message: String,
        expression: CatExpression = CatExpression.CURIOUS,
        speak: Boolean = true,
        autoHide: Boolean = true
    ) {
        hideJob?.cancel()
        safetyHideJob?.cancel()
        bubbleShownAt = System.currentTimeMillis()
        bubbleMessage = message
        _state.value = CatState(
            expression = expression,
            message = message,
            bubbleVisible = true,
            reactionToken = System.currentTimeMillis()
        )

        val willSpeak = if (speak) {
            ttsManager.speak(message) {
                if (autoHide) scheduleHideAfterSpeech()
            }
        } else {
            false
        }

        if (autoHide) {
            if (!willSpeak) {
                // Voce disattivata o non disponibile: nascondi dopo un tempo
                // proporzionato alla lunghezza del testo, mai a metà lettura.
                hideJob = scope.launch {
                    delay(readingDuration(message))
                    awaitSpeechEnd()
                    hideBubble(stopSpeech = false)
                }
            } else {
                // Rete di sicurezza per motori TTS che non consegnano onDone.
                safetyHideJob = scope.launch {
                    delay(60_000)
                    awaitSpeechEnd()
                    hideBubble(stopSpeech = false)
                }
            }
        }
    }

    fun openDefault(message: String, expression: CatExpression = CatExpression.CURIOUS) {
        hideJob?.cancel()
        safetyHideJob?.cancel()
        bubbleShownAt = System.currentTimeMillis()
        bubbleMessage = message
        _state.value = CatState(
            expression = expression,
            message = message,
            bubbleVisible = true,
            reactionToken = System.currentTimeMillis()
        )
        soundManager.meow()
    }

    /** Chiusura voluta dall'utente: nasconde la bolla e ferma subito la voce. */
    fun dismissBubble() {
        hideBubble(stopSpeech = true)
    }

    fun speakCurrent() {
        val message = _state.value.message ?: return
        bubbleShownAt = System.currentTimeMillis()
        bubbleMessage = message
        _state.value = _state.value.copy(bubbleVisible = true)
        ttsManager.speak(message) {
            if (_state.value.message == message) scheduleHideAfterSpeech()
        }
    }

    fun updateSettings() {
        // Le impostazioni vengono propagate direttamente dai manager.
    }

    private fun scheduleHideAfterSpeech() {
        hideJob?.cancel()
        val message = bubbleMessage
        val elapsed = System.currentTimeMillis() - bubbleShownAt
        val minimumVisible = if (message != null) readingDuration(message) else 2_800L
        hideJob = scope.launch {
            // La bolla resta visibile almeno quanto serve a leggere il testo,
            // anche se il motore TTS segnala la fine troppo presto o non ha voce.
            delay((minimumVisible - elapsed).coerceAtLeast(2_800L))
            awaitSpeechEnd()
            hideBubble(stopSpeech = false)
        }
    }

    private suspend fun awaitSpeechEnd() {
        var waited = 0L
        while (ttsManager.isSpeaking() && waited < 30_000L) {
            delay(750)
            waited += 750
        }
    }

    private fun hideBubble(stopSpeech: Boolean) {
        hideJob?.cancel()
        safetyHideJob?.cancel()
        _state.value = _state.value.copy(bubbleVisible = false)
        if (stopSpeech) ttsManager.stop()
    }

    private fun readingDuration(message: String): Long =
        (3_500L + message.length * 55L).coerceAtMost(20_000L)
}
