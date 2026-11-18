package com.sudokucat.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sudokucat.app.R
import com.sudokucat.app.data.prefs.UserSettings

class SoundManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val tapId = soundPool.load(context, R.raw.tap, 1)
    private val correctId = soundPool.load(context, R.raw.correct, 1)
    private val errorId = soundPool.load(context, R.raw.error, 1)
    private val winId = soundPool.load(context, R.raw.win, 1)
    private val fishId = soundPool.load(context, R.raw.fish, 1)
    private val meowId = soundPool.load(context, R.raw.meow, 1)

    @Volatile
    private var volume: Float = 0.55f

    fun updateSettings(settings: UserSettings) {
        volume = settings.soundVolume
    }

    fun tap() = play(tapId, 0.7f)
    fun correct() = play(correctId, 1f)
    fun error() = play(errorId, 1f)
    fun win() = play(winId, 1f)
    fun fish() = play(fishId, 0.8f)
    fun meow() = play(meowId, 0.9f)

    private fun play(soundId: Int, localVolume: Float) {
        if (soundId == 0 || volume <= 0.001f) return
        soundPool.play(soundId, volume * localVolume, volume * localVolume, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
