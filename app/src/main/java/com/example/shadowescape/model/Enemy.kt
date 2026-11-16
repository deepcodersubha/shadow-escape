package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

enum class EnemyType {
    HOLLOW_SOLDIER,
    ELITE_GUARDIAN,
    SKELETON,
    GOBLIN,
    MUSHROOM,
    FLYING_EYE,
    SLIME,
    GRACE_MIMIC
}

private data class EnemyStats(
    val width: Float,
    val height: Float,
    val maxHp: Float,
    val maxPoise: Float,
    val essenceReward: Int,
    val attackDamage: Float,
    val moveSpeed: Float,
    val patrolSpeed: Float,
    val attackReach: Float,
    val stopReach: Float,
    val windupDuration: Float,
    val attackDuration: Float,
    val cooldownDuration: Float,
    val isFlying: Boolean
)

class Enemy(
    val id: Int,
    private val initialX: Float,
    private val initialY: Float,
    val type: EnemyType = EnemyType.SKELETON,
    val patrolMinX: Float = initialX - 140f,
    val patrolMaxX: Float = initialX + 140f
) {
    private val stats: EnemyStats = when (type) {
        EnemyType.ELITE_GUARDIAN -> EnemyStats(48f, 82f, 160f, 70f, 140, 35f, 95f, 55f, 68f, 56f, 0.45f, 0.34f, 1.6f, false)
        EnemyType.SKELETON, EnemyType.HOLLOW_SOLDIER -> EnemyStats(38f, 68f, 75f, 36f, 55, 20f, 125f, 55f, 54f, 46f, 0.34f, 0.28f, 1.25f, false)
        EnemyType.GOBLIN -> EnemyStats(34f, 56f, 55f, 24f, 45, 16f, 175f, 80f, 48f, 40f, 0.26f, 0.24f, 0.95f, false)
        EnemyType.MUSHROOM -> EnemyStats(36f, 60f, 90f, 44f, 65, 24f, 110f, 48f, 52f, 42f, 0.38f, 0.28f, 1.35f, false)
        EnemyType.FLYING_EYE -> EnemyStats(44f, 42f, 50f, 20f, 50, 17f, 150f, 75f, 52f, 40f, 0.30f, 0.26f, 1.15f, true)
        EnemyType.SLIME -> EnemyStats(42f, 34f, 60f, 26f, 45, 15f, 105f, 52f, 44f, 36f, 0.30f, 0.25f, 1.15f, false)
        EnemyType.GRACE_MIMIC -> EnemyStats(50f, 78f, 240f, 85f, 380, 28f, 130f, 60f, 64f, 50f, 0.38f, 0.30f, 1.35f, false)
    }

    val width: Float get() = bounds.width
    val height: Float get() = bounds.height
    val bounds: AABB = AABB(initialX, initialY, stats.width, stats.height)

    val isFlying: Boolean = stats.isFlying
    var maxHp: Float = stats.maxHp
    var currentHp: Float = maxHp

    var maxPoise: Float = stats.maxPoise
    var currentPoise: Float = maxPoise
    var isStaggered: Boolean = false
    var staggerTimer: Float = 0f
    var onPoiseBroken: (() -> Unit)? = null

    // Hidden Boss (Slime -> Phase 2 Demon Slime)
    var canTransformToDemonBoss: Boolean = false
    var isDemonBossPhase: Boolean = false
    var phase2TransformTimer: Float = 0f
    var onDemonBossTransformed: (() -> Unit)? = null

    // Dynamic Shadow Vessel (Random normal mob erupts mid-fight)
    var isShadowVessel: Boolean = false
    var isShadowVesselAwakened: Boolean = false
    var onShadowVesselAwakened: (() -> Unit)? = null

    // Dedicated Zone Boss & Shadow Echo
    var isZoneBoss: Boolean = false
    var bossTitle: String = ""
    var bossPhase2: Boolean = false
    var onZoneBossPhase2: (() -> Unit)? = null
    var isShadowEcho: Boolean = false
    var shadowEchoEssence: Int = 0

    // Grace Mimic (False Site of Grace encounter)
    var isGraceMimic: Boolean = false
    var graceMimicPlatformId: Int? = null
    var isGraceMimicDefeated: Boolean = false

    // Status Effects (Bleed, Frostbite, Burn, Poison)
    var bleedBuildup: Float = 0f
    var frostBuildup: Float = 0f
    var burnBuildup: Float = 0f
    var poisonBuildup: Float = 0f
    var frostTimer: Float = 0f
    var burnTimer: Float = 0f
    var poisonTimer: Float = 0f

    var vx: Float = 0f
    var vy: Float = 0f
    var facingRight: Boolean = true
    var isAlive: Boolean = true
    var isGrounded: Boolean = false

    // AI & Combat timing
    var isAggroed: Boolean = false
    var windupDuration: Float = stats.windupDuration
    var attackDuration: Float = stats.attackDuration
    var attackWindupTimer: Float = 0f
    var isAttacking: Boolean = false
    var attackTimer: Float = 0f
    var attackCooldown: Float = 0f
    var attackVariant: Int = 0
    var animTime: Float = id * 0.73f

    // Damage flash & death effect
    var hurtTimer: Float = 0f
    var deathFadeTimer: Float = 0f

    var essenceReward: Int = stats.essenceReward
    var attackDamage: Float = stats.attackDamage
    var moveSpeed: Float = stats.moveSpeed
    val patrolSpeed: Float = stats.patrolSpeed

    fun update(
        dt: Float,
        player: Player,
        platforms: List<Platform>,
        onEnemyAttackPlayer: ((Float, Enemy) -> Unit)? = null,
        onEnemyStaggered: (() -> Unit)? = null
    ) {
        if (!isAlive) {
            deathFadeTimer += dt
            return
        }

        animTime += dt
        if (hurtTimer > 0f) hurtTimer -= dt
        if (attackCooldown > 0f) attackCooldown -= dt
        if (frostTimer > 0f) frostTimer -= dt
        if (burnTimer > 0f) {
            burnTimer -= dt
            takeDamage(12f * dt, 0f)
            if (!isAlive) return
        }
        if (poisonTimer > 0f) {
            poisonTimer -= dt
            takeDamage(9f * dt, 0f)
            if (!isAlive) return
        }
        if (phase2TransformTimer > 0f) {
            phase2TransformTimer -= dt
            vx = 0f
            applyMovementAndCollisions(dt, platforms)
            return
        }

        // 1. Stagger / Poise Break state
        if (isStaggered) {
            staggerTimer -= dt
            vx = 0f
            if (isFlying) vy = 0f
            if (staggerTimer <= 0f) {
                isStaggered = false
                currentPoise = maxPoise
            }
            applyMovementAndCollisions(dt, platforms)
            return
        }

        // 2. Detection & Aggro AI
        val cx = bounds.centerX
        val cy = bounds.centerY
        val px = player.bounds.centerX
        val py = player.bounds.centerY
        val distToPlayer = hypot(cx - px, cy - py)
        val verticalLimit = if (isFlying) 240f else 140f
        val isVerticalMatch = abs(cy - py) < verticalLimit

        // Sanctuary Check: Consecrated Sites of Grace are sacred safe zones; enemies cannot aggro or enter
        val inGraceSanctuary = !isGraceMimic && platforms.any {
            it.type == PlatformType.CHECKPOINT && (!it.isGraceMimic || it.isMimicDefeated) &&
            abs(it.bounds.centerX - px) < 190f && abs(it.bounds.bottom - py) < 150f
        }

        if (!inGraceSanctuary && isVerticalMatch && distToPlayer < 360f && player.isAlive) {
            isAggroed = true
        } else if (inGraceSanctuary || distToPlayer > 520f || !player.isAlive) {
            isAggroed = false
        }

        // 3. Movement & Attack Decision
        if (isAttacking) {
            attackTimer -= dt
            vx = 0f
            if (isFlying) vy = 0f
            if (attackTimer <= 0f) {
                isAttacking = false
                attackCooldown = stats.cooldownDuration
            }
        } else if (attackWindupTimer > 0f) {
            attackWindupTimer -= dt
            vx = 0f
            if (isFlying) vy = 0f
            if (attackWindupTimer <= 0f) {
                // Unleash the strike!
                isAttacking = true
                attackTimer = attackDuration
                val attackReach = if (isDemonBossPhase) 96f else stats.attackReach
                val hx = if (facingRight) bounds.right else bounds.left - attackReach
                val attackBox = AABB(hx, bounds.top - 10f, attackReach, bounds.height + 20f)

                if (attackBox.intersects(player.bounds) && !player.isInvulnerable && player.isAlive) {
                    onEnemyAttackPlayer?.invoke(attackDamage, this)
                }
            }
        } else if (isAggroed) {
            facingRight = px > cx
            val reach = if (isDemonBossPhase) 78f else stats.stopReach
            val closeVertically = abs(cy - py) < (if (isFlying) 55f else 140f)

            if (distToPlayer <= reach && closeVertically) {
                vx = 0f
                if (isFlying) vy = 0f
                if (attackCooldown <= 0f) {
                    attackVariant = (attackVariant + 1) % 2
                    attackWindupTimer = windupDuration
                }
            } else {
                val speedFactor = if (frostTimer > 0f) 0.60f else 1.0f
                vx = (if (facingRight) moveSpeed else -moveSpeed) * speedFactor
                if (isFlying) {
                    val targetY = py - 8f
                    vy = ((targetY - cy) * 2.8f).coerceIn(-130f, 130f)
                }
            }
        } else {
            // Calm patrol between bounds
            if (facingRight) {
                vx = patrolSpeed
                if (bounds.right >= patrolMaxX) facingRight = false
            } else {
                vx = -patrolSpeed
                if (bounds.left <= patrolMinX) facingRight = true
            }
            if (isFlying) {
                val targetHoverY = initialY + sin((animTime * 3.2f).toDouble()).toFloat() * 18f
                vy = ((targetHoverY - bounds.y) * 4f).coerceIn(-80f, 80f)
            }
        }

        // Sanctuary boundary leash: enemies cannot step within 180f of any consecrated Grace
        if (!isGraceMimic) {
            val nearestGrace = platforms.firstOrNull {
                it.type == PlatformType.CHECKPOINT && (!it.isGraceMimic || it.isMimicDefeated) &&
                abs(it.bounds.centerX - cx) < 180f
            }
            if (nearestGrace != null) {
                if (nearestGrace.bounds.centerX > cx && vx > 0f) {
                    vx = -patrolSpeed
                    facingRight = false
                    isAggroed = false
                } else if (nearestGrace.bounds.centerX < cx && vx < 0f) {
                    vx = patrolSpeed
                    facingRight = true
                    isAggroed = false
                }
            }
        }

        applyMovementAndCollisions(dt, platforms)
    }

    private fun applyMovementAndCollisions(dt: Float, platforms: List<Platform>) {
        if (!isFlying) {
            vy += 1200f * dt
            if (vy > 1000f) vy = 1000f
        }

        bounds.x += vx * dt
        // Check platform edge / horizontal limits
        for (platform in platforms) {
            if (platform.type == PlatformType.NORMAL && bounds.intersects(platform.bounds)) {
                if (vx > 0f) {
                    bounds.x = platform.bounds.left - bounds.width
                    facingRight = false
                } else if (vx < 0f) {
                    bounds.x = platform.bounds.right
                    facingRight = true
                }
            }
        }

        bounds.y += vy * dt
        isGrounded = false
        for (platform in platforms) {
            if (platform.type == PlatformType.NORMAL && bounds.intersects(platform.bounds)) {
                if (vy > 0f && bounds.bottom - vy * 0.05f <= platform.bounds.top + 20f) {
                    bounds.y = platform.bounds.top - bounds.height
                    vy = 0f
                    isGrounded = !isFlying
                } else if (isFlying && vy < 0f) {
                    bounds.y = platform.bounds.bottom
                    vy = 0f
                }
            }
        }
    }

    fun forceStagger(duration: Float = 2.2f) {
        if (!isAlive) return
        isAttacking = false
        attackWindupTimer = 0f
        attackCooldown = 1.2f
        currentPoise = 0f
        isStaggered = true
        staggerTimer = duration
        onPoiseBroken?.invoke()
    }

    fun cancelAttackWindup(cooldown: Float = 1.4f) {
        attackWindupTimer = 0f
        isAttacking = false
        attackCooldown = cooldown
    }

    fun applyStatus(effect: StatusEffect, amount: Float): String? {
        if (!isAlive || effect == StatusEffect.NONE || amount <= 0f) return null
        when (effect) {
            StatusEffect.BLEED -> {
                bleedBuildup += amount
                if (bleedBuildup >= 100f) {
                    bleedBuildup = 0f
                    val burst = (maxHp * 0.15f).coerceAtLeast(18f)
                    takeDamage(burst, 15f)
                    return "🩸 BLEED! -${burst.toInt()} HP!"
                }
            }
            StatusEffect.FROSTBITE -> {
                frostBuildup += amount
                if (frostBuildup >= 100f) {
                    frostBuildup = 0f
                    frostTimer = 4.5f
                    val burst = (maxHp * 0.10f).coerceAtLeast(14f)
                    takeDamage(burst, 20f)
                    return "❄ FROSTBITE! Slowed!"
                }
            }
            StatusEffect.BURN -> {
                burnBuildup += amount
                if (burnBuildup >= 100f) {
                    burnBuildup = 0f
                    burnTimer = 4.0f
                    return "🔥 IGNITED!"
                }
            }
            StatusEffect.POISON -> {
                poisonBuildup += amount
                if (poisonBuildup >= 100f) {
                    poisonBuildup = 0f
                    poisonTimer = 5.0f
                    return "☠ POISONED!"
                }
            }
            StatusEffect.NONE -> {}
        }
        return null
    }

    fun takeDamage(amount: Float, poiseDamage: Float): Boolean {
        if (!isAlive || phase2TransformTimer > 0f) return false

        currentHp = (currentHp - amount).coerceAtLeast(0f)
        hurtTimer = 0.20f
        currentPoise -= poiseDamage

        if (type == EnemyType.SLIME && canTransformToDemonBoss && !isDemonBossPhase && currentHp <= maxHp * 0.65f) {
            transformToDemonBoss()
            return false
        }

        if (isShadowVessel && !isShadowVesselAwakened && currentHp <= maxHp * 0.50f) {
            awakenShadowVessel()
            return false
        }

        if (isZoneBoss && !bossPhase2 && currentHp <= maxHp * 0.50f) {
            bossPhase2 = true
            attackDamage *= 1.25f
            moveSpeed *= 1.2f
            windupDuration *= 0.85f
            onZoneBossPhase2?.invoke()
        }

        // Poise Break (Stagger)
        if (currentPoise <= 0f && !isStaggered) {
            isStaggered = true
            staggerTimer = 2.6f
            currentPoise = maxPoise
            onPoiseBroken?.invoke()
        }

        if (currentHp <= 0f) {
            isAlive = false
            return true // Enemy defeated
        }
        return false
    }

    private fun awakenShadowVessel() {
        isShadowVesselAwakened = true
        phase2TransformTimer = 0.55f
        maxHp *= 1.85f
        currentHp = maxHp
        maxPoise *= 1.4f
        currentPoise = maxPoise
        attackDamage *= 1.38f
        moveSpeed *= 1.22f
        windupDuration *= 0.85f
        essenceReward = (essenceReward * 2.2f).toInt()
        isStaggered = false
        staggerTimer = 0f
        isAggroed = true
        attackWindupTimer = 0f
        isAttacking = false
        onShadowVesselAwakened?.invoke()
    }

    private fun transformToDemonBoss() {
        isDemonBossPhase = true
        phase2TransformTimer = 0.85f
        val oldBottom = bounds.bottom
        bounds.width = 74f
        bounds.height = 104f
        bounds.y = oldBottom - 104f
        maxHp = 280f
        currentHp = 280f
        maxPoise = 95f
        currentPoise = 95f
        isStaggered = false
        staggerTimer = 0f
        attackDamage = 38f
        essenceReward = 350
        moveSpeed = 115f
        windupDuration = 0.48f
        attackDuration = 0.44f
        isAggroed = true
        hurtTimer = 0f
        attackWindupTimer = 0f
        isAttacking = false
        attackCooldown = 0.4f
        onDemonBossTransformed?.invoke()
    }

    fun reset(startX: Float, startY: Float) {
        isDemonBossPhase = false
        isShadowVesselAwakened = false
        bossPhase2 = false
        phase2TransformTimer = 0f
        bleedBuildup = 0f
        frostBuildup = 0f
        burnBuildup = 0f
        poisonBuildup = 0f
        frostTimer = 0f
        burnTimer = 0f
        poisonTimer = 0f
        if (!isZoneBoss) {
            bounds.width = stats.width
            bounds.height = stats.height
            maxHp = stats.maxHp
            maxPoise = stats.maxPoise
            attackDamage = stats.attackDamage
            essenceReward = stats.essenceReward
            moveSpeed = stats.moveSpeed
            windupDuration = stats.windupDuration
            attackDuration = stats.attackDuration
        } else {
            maxHp = 520f
            maxPoise = 120f
            attackDamage = 34f
            essenceReward = 800
            moveSpeed = 120f
            windupDuration = 0.45f
            attackDuration = 0.40f
        }
        bounds.setPosition(startX, startY)
        currentHp = maxHp
        currentPoise = maxPoise
        isStaggered = false
        staggerTimer = 0f
        vx = 0f
        vy = 0f
        isAlive = true
        isAggroed = false
        attackWindupTimer = 0f
        isAttacking = false
        attackTimer = 0f
        attackCooldown = 0f
        hurtTimer = 0f
        deathFadeTimer = 0f
        attackVariant = 0
        animTime = id * 0.73f
    }
}
