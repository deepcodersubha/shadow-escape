package com.example.shadowescape.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrateJump() {
        vibrate(15, 70)
    }

    fun vibrateLand() {
        vibrate(28, 130)
    }

    fun vibrateSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 30, 25)
            val amplitudes = intArrayOf(0, 100, 0, 160)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    }

    fun vibrateCheckpoint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 40, 50)
            val amplitudes = intArrayOf(0, 120, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(70)
        }
    }

    fun vibrateDeath() {
        vibrate(110, 220)
    }

    fun vibrateAttack() {
        vibrate(18, 95)
    }

    fun vibrateHeavyAttack() {
        vibrate(45, 180)
    }

    fun vibrateVisceralStrike() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 35, 40, 75)
            val amplitudes = intArrayOf(0, 160, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(90)
        }
    }

    fun vibrateBloodstain() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 25, 30, 45)
            val amplitudes = intArrayOf(0, 100, 0, 180)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(60)
        }
    }

    fun vibrateDodgeRoll() {
        vibrate(22, 80)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val clampedAmp = amplitude.coerceIn(1, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
