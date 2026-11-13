package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB
import kotlin.math.abs
import kotlin.math.hypot

enum class ChestState {
    CLOSED,
    OPENING,
    OPENED
}

enum class ChestType {
    WOODEN,
    IRON,
    GILDED
}

data class LootReward(
    val title: String,
    val subtitle: String,
    val description: String,
    val rarity: ItemRarity,
    val weapon: Weapon? = null,
    val isPrimordialSeed: Boolean = false,
    val isSacredElixirTear: Boolean = false,
    val essenceBonus: Int = 0
)

class TreasureChest(
    val id: Int,
    x: Float,
    y: Float,
    val width: Float = 54f,
    val height: Float = 38f,
    val type: ChestType = ChestType.WOODEN,
    val loot: LootReward,
    val isMimic: Boolean = false
) {
    val initialX: Float = x
    val initialY: Float = y
    val bounds: AABB = AABB(initialX, initialY, width, height)
    var state: ChestState = ChestState.CLOSED
    var openProgress: Float = 0f // 0f (closed) to 1f (fully open)
    var animGlowTimer: Float = 0f

    // Mimic Combat & Behavior state
    var isAwakened: Boolean = false
    val maxMimicHp: Float = 140f
    var currentMimicHp: Float = maxMimicHp
    var mimicVx: Float = 0f
    var mimicVy: Float = 0f
    var mimicHurtTimer: Float = 0f
    var mimicAttackWindup: Float = 0f
    var mimicIsAttacking: Boolean = false
    var mimicAttackTimer: Float = 0f
    var mimicAttackCooldown: Float = 0f
    var mimicFacingRight: Boolean = false
    var isDefeated: Boolean = false
    var deathFadeTimer: Float = 0f

    fun update(
        dt: Float,
        player: Player? = null,
        platforms: List<Platform> = emptyList(),
        onMimicAttackPlayer: ((Float) -> Unit)? = null
    ) {
        animGlowTimer += dt

        if (!isMimic || !isAwakened) {
            if (state == ChestState.OPENING) {
                openProgress += dt * 2.5f
                if (openProgress >= 1f) {
                    openProgress = 1f
                    state = ChestState.OPENED
                }
            }
            return
        }

        // Mimic is awakened!
        if (isDefeated) {
            deathFadeTimer += dt
            return
        }

        if (mimicHurtTimer > 0f) mimicHurtTimer -= dt
        if (mimicAttackCooldown > 0f) mimicAttackCooldown -= dt

        if (player == null || !player.isAlive) {
            mimicVx = 0f
            applyGravityAndCollisions(dt, platforms)
            return
        }

        val cx = bounds.centerX
        val cy = bounds.centerY
        val px = player.bounds.centerX
        val py = player.bounds.centerY
        val dist = hypot(cx - px, cy - py)
        val isVerticalMatch = abs(cy - py) < 120f

        mimicFacingRight = px > cx

        if (mimicIsAttacking) {
            mimicAttackTimer -= dt
            mimicVx = if (mimicFacingRight) 160f else -160f // aggressive lunge
            if (mimicAttackTimer <= 0f) {
                mimicIsAttacking = false
                mimicAttackCooldown = 1.4f
                mimicVx = 0f
            }
        } else if (mimicAttackWindup > 0f) {
            mimicAttackWindup -= dt
            mimicVx = 0f
            if (mimicAttackWindup <= 0f) {
                mimicIsAttacking = true
                mimicAttackTimer = 0.24f
                val reach = 58f
                val hx = if (mimicFacingRight) bounds.right - 10f else bounds.left - reach + 10f
                val attackBox = AABB(hx, bounds.top - 12f, reach, bounds.height + 20f)
                if (attackBox.intersects(player.bounds) && !player.isInvulnerable && player.isAlive) {
                    onMimicAttackPlayer?.invoke(32f)
                }
            }
        } else if (dist < 380f && isVerticalMatch) {
            if (dist < 55f) {
                mimicVx = 0f
                if (mimicAttackCooldown <= 0f) {
                    mimicAttackWindup = 0.32f
                }
            } else {
                mimicVx = if (mimicFacingRight) 105f else -105f
            }
        } else {
            mimicVx = 0f
        }

        applyGravityAndCollisions(dt, platforms)
    }

    private fun applyGravityAndCollisions(dt: Float, platforms: List<Platform>) {
        mimicVy += 1200f * dt
        if (mimicVy > 1000f) mimicVy = 1000f

        bounds.x += mimicVx * dt
        for (platform in platforms) {
            if (platform.type == PlatformType.NORMAL && bounds.intersects(platform.bounds)) {
                if (mimicVx > 0f) {
                    bounds.x = platform.bounds.left - bounds.width
                } else if (mimicVx < 0f) {
                    bounds.x = platform.bounds.right
                }
            }
        }

        bounds.y += mimicVy * dt
        for (platform in platforms) {
            if (platform.type == PlatformType.NORMAL && bounds.intersects(platform.bounds)) {
                if (mimicVy > 0f && bounds.bottom - mimicVy * 0.05f <= platform.bounds.top + 20f) {
                    bounds.y = platform.bounds.top - bounds.height
                    mimicVy = 0f
                }
            }
        }
    }

    fun open(): Boolean {
        if (isMimic) {
            // Mimic doesn't open normally; awaken handled separately
            return false
        }
        if (state == ChestState.CLOSED) {
            state = ChestState.OPENING
            return true
        }
        return false
    }

    fun awaken(surpriseBite: Boolean = false, player: Player? = null, onPlayerDamaged: ((Float) -> Unit)? = null) {
        if (!isMimic || isAwakened) return
        isAwakened = true
        state = ChestState.OPENED
        openProgress = 1f
        if (surpriseBite && player != null) {
            onPlayerDamaged?.invoke(42f)
            // Knockback player
            player.vx = if (player.facingRight) -340f else 340f
            player.vy = -220f
        }
    }

    fun takeDamage(amount: Float): Boolean {
        if (!isMimic || isDefeated) return false

        if (!isAwakened) {
            isAwakened = true
            state = ChestState.OPENED
            openProgress = 1f
        }

        currentMimicHp = (currentMimicHp - amount).coerceAtLeast(0f)
        mimicHurtTimer = 0.22f

        if (currentMimicHp <= 0f) {
            isDefeated = true
            state = ChestState.OPENED
            openProgress = 1f
            return true // Defeated!
        }
        return false
    }

    fun reset() {
        bounds.setPosition(initialX, initialY)
        state = ChestState.CLOSED
        openProgress = 0f
        isAwakened = false
        currentMimicHp = maxMimicHp
        mimicVx = 0f
        mimicVy = 0f
        mimicHurtTimer = 0f
        mimicAttackWindup = 0f
        mimicIsAttacking = false
        mimicAttackTimer = 0f
        mimicAttackCooldown = 0f
        isDefeated = false
        deathFadeTimer = 0f
    }
}
