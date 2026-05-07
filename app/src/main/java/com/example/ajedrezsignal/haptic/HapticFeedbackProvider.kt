package com.example.ajedrezsignal.haptic

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackProvider(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrateColumn(index: Int) {
        val position = index + 1
        val pattern = mutableListOf<Long>(0)
        var remaining = position
        if (remaining >= 5) {
            pattern.add(300); pattern.add(200)
            remaining -= 5
        }
        repeat(remaining) {
            pattern.add(50); pattern.add(150)
        }
        performVibration(pattern.toLongArray())
    }

    fun vibrateRow(index: Int) {
        val position = index + 1
        val pattern = mutableListOf<Long>(0)
        var remaining = position
        if (remaining >= 5) {
            pattern.add(300); pattern.add(250)
            remaining -= 5
        }
        repeat(remaining) {
            pattern.add(150); pattern.add(200)
        }
        performVibration(pattern.toLongArray())
    }

    fun vibrateMove(moveStr: String, isCheck: Boolean = false) {
        if (moveStr.length < 4) return
        if (isCheck) performVibration(longArrayOf(0, 50, 50, 50, 50, 50))
        else performVibration(longArrayOf(0, 100))

        Handler(Looper.getMainLooper()).postDelayed({
            val oc = moveStr[0].lowercaseChar() - 'a'
            val or = moveStr[1].digitToInt() - 1
            val dc = moveStr[2].lowercaseChar() - 'a'
            val dr = moveStr[3].digitToInt() - 1

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({ vibrateColumn(oc) }, 0)
            handler.postDelayed({ vibrateRow(or) }, 2000)
            handler.postDelayed({ vibrateColumn(dc) }, 4000)
            handler.postDelayed({ vibrateRow(dr) }, 6000)
        }, 1000)
    }

    fun illegalMoveError() { performVibration(longArrayOf(0, 50, 100, 50)) }
    fun heartbeat() { performVibration(longArrayOf(0, 100, 100, 100)) }
    fun confirmTone() { performVibration(longArrayOf(0, 20, 50, 20)) }

    private fun performVibration(timings: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            vibrator.vibrate(timings, -1)
        }
    }
}
