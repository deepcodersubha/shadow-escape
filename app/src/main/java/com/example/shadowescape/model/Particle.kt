package com.example.shadowescape.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.hypot

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var alpha: Int,
    var maxLife: Float
) {
    var life: Float = maxLife

    fun update(dt: Float) {
        x += vx * dt
        y += vy * dt
        life -= dt
    }

    fun isDead(): Boolean = life <= 0f

    fun render(canvas: Canvas, paint: Paint, playerCenterX: Float, playerCenterY: Float) {
        val progress = (life / maxLife).coerceIn(0f, 1f)
        val distToPlayer = hypot(x - playerCenterX, y - playerCenterY)

        // Dust motes ignite and glow when caught in the player's lantern aura
        val isIlluminated = distToPlayer < 260f
        val lightIntensity = if (isIlluminated) (1f - distToPlayer / 260f) else 0f

        val finalAlpha = ((alpha * progress) * (0.4f + lightIntensity * 1.2f)).toInt().coerceIn(0, 255)
        if (isIlluminated) {
            paint.color = Color.argb(finalAlpha, 180, 230, 255)
            canvas.drawCircle(x, y, radius * (1f + lightIntensity * 0.4f), paint)
        } else {
            paint.color = Color.argb((finalAlpha * 0.35f).toInt(), 90, 110, 140)
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}
