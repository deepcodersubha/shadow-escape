package com.example.shadowescape.engine

import android.graphics.Canvas
import android.os.Build
import android.view.SurfaceHolder
import com.example.shadowescape.ui.GameView
import java.util.concurrent.atomic.AtomicBoolean

class GameLoop(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
    private val running = AtomicBoolean(false)
    var isRunning: Boolean
        get() = running.get()
        set(value) = running.set(value)

    private val targetFPS: Double = 60.0
    private val targetTime: Double = 1000.0 / targetFPS

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        var lastTime = System.nanoTime()

        while (running.get()) {
            startTime = System.nanoTime()
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
            lastTime = now

            val canvas: Canvas? = if (surfaceHolder.surface.isValid) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        surfaceHolder.lockHardwareCanvas()
                    } catch (e: Exception) {
                        try {
                            surfaceHolder.lockCanvas()
                        } catch (e2: Exception) {
                            null
                        }
                    }
                } else {
                    try {
                        surfaceHolder.lockCanvas()
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                null
            }

            if (canvas != null) {
                try {
                    synchronized(surfaceHolder) {
                        gameView.update(dt)
                        gameView.render(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1_000_000
            waitTime = (targetTime - timeMillis).toLong()

            if (waitTime > 0) {
                try {
                    sleep(waitTime)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}
