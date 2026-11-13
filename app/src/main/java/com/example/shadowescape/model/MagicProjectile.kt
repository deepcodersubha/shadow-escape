package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB

class MagicProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val radius: Float = 9f
) {
    var life: Float = 1.2f
    var isDead: Boolean = false

    val bounds: AABB get() = AABB(x - radius, y - radius, radius * 2f, radius * 2f)

    fun update(dt: Float, platforms: List<Platform>) {
        if (isDead) return

        x += vx * dt
        y += vy * dt
        life -= dt

        if (life <= 0f) {
            isDead = true
            return
        }

        // Collide with solid platforms
        for (platform in platforms) {
            if (platform.type == PlatformType.NORMAL || (platform.type == PlatformType.DOOR && !platform.isActivated)) {
                if (bounds.intersects(platform.bounds)) {
                    isDead = true
                    return
                }
            }
        }
    }
}
