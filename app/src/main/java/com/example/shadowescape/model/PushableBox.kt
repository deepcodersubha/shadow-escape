package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB

class PushableBox(
    val id: Int,
    startX: Float,
    startY: Float,
    val size: Float = 60f
) {
    val bounds: AABB = AABB(startX, startY, size, size)
    var vx: Float = 0f
    var vy: Float = 0f
    var isGrounded: Boolean = false

    private val gravity: Float = 1300f
    private val friction: Float = 10f

    fun update(dt: Float, platforms: List<Platform>) {
        // Apply gravity
        vy += gravity * dt
        if (vy > 1200f) vy = 1200f

        // Apply horizontal friction
        vx -= vx * friction * dt
        if (kotlin.math.abs(vx) < 5f) vx = 0f

        // Move horizontally and resolve platform collisions
        bounds.x += vx * dt
        resolveHorizontalCollisions(platforms)

        // Move vertically and resolve platform collisions
        bounds.y += vy * dt
        isGrounded = false
        resolveVerticalCollisions(platforms)
    }

    private fun resolveHorizontalCollisions(platforms: List<Platform>) {
        for (platform in platforms) {
            if (platform.type == PlatformType.DOOR && platform.isActivated) continue
            if (platform.type != PlatformType.NORMAL && platform.type != PlatformType.DOOR) continue

            if (bounds.intersects(platform.bounds)) {
                if (vx > 0f) {
                    bounds.x = platform.bounds.left - bounds.width
                    vx = 0f
                } else if (vx < 0f) {
                    bounds.x = platform.bounds.right
                    vx = 0f
                }
            }
        }
    }

    private fun resolveVerticalCollisions(platforms: List<Platform>) {
        for (platform in platforms) {
            if (platform.type == PlatformType.DOOR && platform.isActivated) continue
            if (platform.type != PlatformType.NORMAL && platform.type != PlatformType.DOOR) continue

            if (bounds.intersects(platform.bounds)) {
                if (vy > 0f) {
                    bounds.y = platform.bounds.top - bounds.height
                    vy = 0f
                    isGrounded = true
                } else if (vy < 0f) {
                    bounds.y = platform.bounds.bottom
                    vy = 0f
                }
            }
        }
    }

    fun push(forceX: Float, platforms: List<Platform>) {
        vx = forceX
        // Pre-check if pushing directly into a solid platform
        val testX = bounds.x + vx * 0.02f
        val tempBounds = AABB(testX, bounds.y, bounds.width, bounds.height)
        for (platform in platforms) {
            if (platform.type == PlatformType.DOOR && platform.isActivated) continue
            if (platform.type == PlatformType.NORMAL || platform.type == PlatformType.DOOR) {
                if (tempBounds.intersects(platform.bounds)) {
                    vx = 0f
                    return
                }
            }
        }
    }

    fun reset(startX: Float, startY: Float) {
        bounds.setPosition(startX, startY)
        vx = 0f
        vy = 0f
        isGrounded = false
    }
}
