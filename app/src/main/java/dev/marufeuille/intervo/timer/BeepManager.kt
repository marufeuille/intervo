package dev.marufeuille.intervo.timer

import android.media.AudioManager
import android.media.ToneGenerator

class BeepManager {

    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
    }.getOrNull()

    fun beep(pattern: BeepPattern) {
        val tg = toneGenerator ?: return
        when (pattern) {
            BeepPattern.COUNTDOWN ->
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            BeepPattern.PHASE_DONE ->
                tg.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            BeepPattern.WORKOUT_COMPLETE ->
                tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 600)
        }
    }

    fun release() {
        toneGenerator?.release()
    }
}
