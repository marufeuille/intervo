package dev.marufeuille.intervo.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrate(pattern: VibratePattern) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (pattern) {
            VibratePattern.WORKOUT_START ->
                VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
            VibratePattern.EXERCISE_DONE ->
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
            VibratePattern.REST_DONE ->
                VibrationEffect.createWaveform(longArrayOf(0, 600), -1)
            VibratePattern.COUNTDOWN_TICK ->
                VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            VibratePattern.WORKOUT_COMPLETE ->
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 500), -1)
            VibratePattern.ERROR ->
                VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80, 80, 80), -1)
        }
        v.vibrate(effect)
    }
}

enum class VibratePattern {
    WORKOUT_START,
    EXERCISE_DONE,
    REST_DONE,
    COUNTDOWN_TICK,
    WORKOUT_COMPLETE,
    ERROR
}
