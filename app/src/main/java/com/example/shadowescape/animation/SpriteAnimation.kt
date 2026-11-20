package com.example.shadowescape.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class SpriteAnimation(
    val bitmap: Bitmap,
    val frameWidth: Int,
    val frameHeight: Int,
    val frameCount: Int,
    val fps: Float,
    val looping: Boolean = true
) {
    var currentFrame: Int = 0
        private set
    private var elapsed: Float = 0f
    private val frameDuration: Float = if (fps > 0f) 1f / fps else 0.1f

    val isFinished: Boolean
        get() = !looping && currentFrame >= frameCount - 1

    init {
        require(frameCount > 0) { "frameCount must be > 0" }
        require(frameWidth > 0) { "frameWidth must be > 0" }
        require(frameHeight > 0) { "frameHeight must be > 0" }
        require(fps > 0f) { "fps must be > 0" }
    }

    fun update(dt: Float) {
        if (!looping && currentFrame >= frameCount - 1) return

        elapsed += dt
        while (elapsed >= frameDuration) {
            elapsed -= frameDuration
            if (currentFrame < frameCount - 1) {
                currentFrame++
            } else {
                if (looping) {
                    currentFrame = 0
                } else {
                    currentFrame = frameCount - 1
                    break
                }
            }
        }
    }

    fun reset() {
        currentFrame = 0
        elapsed = 0f
    }

    fun draw(canvas: Canvas, destRect: RectF, flipHorizontal: Boolean, paint: Paint?) {
        drawFrame(canvas, destRect, currentFrame, flipHorizontal, paint)
    }

    fun drawFrame(canvas: Canvas, destRect: RectF, frame: Int, flipHorizontal: Boolean, paint: Paint?) {
        val clampedFrame = frame.coerceIn(0, frameCount - 1)
        val srcX = clampedFrame * frameWidth
        val srcRect = Rect(srcX, 0, srcX + frameWidth, frameHeight)

        canvas.save()
        if (flipHorizontal) {
            canvas.scale(-1f, 1f, destRect.centerX(), destRect.centerY())
        }
        canvas.drawBitmap(bitmap, srcRect, destRect, paint)
        canvas.restore()
    }
}
