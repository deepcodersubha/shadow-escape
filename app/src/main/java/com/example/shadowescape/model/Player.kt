package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB

class Player(startX: Float, startY: Float) {
    val width: Float = 36f
    val height: Float = 64f
    val bounds: AABB = AABB(startX, startY, width, height)

    // Elden Ring style stats & attributes
    val stats: PlayerStats = PlayerStats()
    var currentOrigin: CharacterOrigin = CharacterOrigins.SHADOW_SHINOBI

    // Combat & Weaponry
    var useModularSprite: Boolean = true
    var equippedWeapon: Weapon = Weapons.SHADOW_KATANA
    val inventory: MutableList<Weapon> = mutableListOf(
        Weapons.SHADOW_KATANA,
        Weapons.OBSIDIAN_DAGGER,
        Weapons.STEEL_BROADSWORD,
        Weapons.IRON_SPEAR,
        Weapons.IRON_BATTLE_AXE,
        Weapons.EXECUTIONER_GREATSWORD,
        Weapons.GOLDEN_WARHAMMER,
        Weapons.GREAT_HAMMER,
        Weapons.GIGIS_DARK_STAFF,
        Weapons.ASTRAL_STAFF
    )

    // Visual Armor & Attire Customization
    var equippedArmor: ArmorPiece = Armors.SHADOW_CHITIN_VEST
    val inventoryArmors: MutableList<ArmorPiece> = mutableListOf(
        Armors.SHADOW_CHITIN_VEST,
        Armors.GILDED_SUN_TEMPLAR,
        Armors.GLINTSTONE_SPELLPLATE,
        Armors.CRIMSON_EXECUTIONER,
        Armors.ASTRAL_STARLIGHT_MANTLE,
        Armors.DARK_IRON_FULLPLATE,
        Armors.TWILIGHT_PHANTOM_SHROUD,
        Armors.MASTERLESS_RONIN_GAMBESON,
        Armors.WEATHERED_TARNISHED_CUIRASS,
        Armors.DRAGONBONE_COLOSSUS,
        Armors.ROYAL_SUN_AEGIS,
        Armors.CRUCIBLE_CHAMPION_PLATE,
        Armors.VOID_KNIGHT_DREADPLATE,
        Armors.PALE_STAR_AEGIS,
        Armors.GILDED_DRAGON_LORD,
        Armors.FROSTBORNE_REGALIA,
        Armors.SHADOW_STALKER_SUIT
    )

    fun equipArmor(armor: ArmorPiece) {
        equippedArmor = armor
        if (!inventoryArmors.contains(armor)) {
            inventoryArmors.add(armor)
        }
    }
    var isAttacking: Boolean = false
    var attackTimer: Float = 0f
    var attackDuration: Float = 0.22f
    var attackComboIndex: Int = 1
    var comboResetTimer: Float = 0f
    val hitEnemiesThisAttack: MutableSet<Int> = mutableSetOf()

    val attackProgress: Float
        get() = if (attackDuration > 0f) (1f - (attackTimer / attackDuration)).coerceIn(0f, 1f) else 1f

    // Weapon Arts & Skills
    var isPerformingSkill: Boolean = false
    var skillTimer: Float = 0f
    var skillDuration: Float = 0.35f
    var activeSkillId: String? = null
    val skillProgress: Float
        get() = if (skillDuration > 0f) (1f - (skillTimer / skillDuration)).coerceIn(0f, 1f) else 1f

    var isRolling: Boolean = false
    var rollTimer: Float = 0f
    val rollDuration: Float = 0.32f
    val rollSpeed: Float = 460f

    // Timed Parry / Deflect
    var isParrying: Boolean = false
    var parryTimer: Float = 0f
    var parryCooldown: Float = 0f
    val parryWindowDuration: Float = 0.24f

    // Shadow Flow (Combo-Finisher Weapon Swap Strike)
    var shadowFlowTimer: Float = 0f
    var isShadowFlowStrike: Boolean = false

    // Dead Cells kinetic jump & platforming mechanics
    var coyoteTimer: Float = 0f
    val coyoteDuration: Float = 0.12f
    var jumpBufferTimer: Float = 0f
    val jumpBufferDuration: Float = 0.14f
    var wasGroundedLastFrame: Boolean = false
    var isJumpHeld: Boolean = false
    var onJumpTriggered: (() -> Unit)? = null

    // Dead Cells Downward Smash / Stomp
    var isGroundPounding: Boolean = false
    var hasJustGroundPounded: Boolean = false
    var groundPoundImpactX: Float = 0f
    var groundPoundImpactY: Float = 0f

    // One-Way Semi-Solid Platform Drop-through
    var dropThroughTimer: Float = 0f

    val isInvulnerable: Boolean
        get() = isRolling ||
                (isPerformingSkill && (activeSkillId == "katana" || activeSkillId == "nightprowler_talon")) ||
                isGroundPounding

    // Flask Drinking State
    var isDrinkingFlask: Boolean = false
    var drinkFlaskTimer: Float = 0f
    var lastDrunkFlaskType: String? = null

    var vx: Float = 0f
    var vy: Float = 0f

    var isGrounded: Boolean = false
    var lastSafeGroundedX: Float = startX + width / 2f
    var lastSafeGroundedY: Float = startY + height
    var facingRight: Boolean = true
    var isAlive: Boolean = true
    var hasWon: Boolean = false
    var isPushing: Boolean = false

    var moveLeft: Boolean = false
    var moveRight: Boolean = false
    var jumpRequested: Boolean = false
    var animTime: Float = 0f

    private val gravity: Float = 1300f
    private val moveSpeed: Float = 280f
    private val pushSpeed: Float = 160f
    private val jumpStrength: Float = -700f

    fun update(dt: Float, platforms: List<Platform>, boxes: List<PushableBox> = emptyList()) {
        if (!isAlive || hasWon) return

        // Update Stamina & Mana recovery
        stats.update(dt)
        if (stats.currentHp <= 0f) {
            isAlive = false
            return
        }

        // Timers update
        if (dropThroughTimer > 0f) dropThroughTimer -= dt
        if (jumpBufferTimer > 0f) jumpBufferTimer -= dt
        if (parryCooldown > 0f) parryCooldown -= dt
        if (shadowFlowTimer > 0f) shadowFlowTimer -= dt

        if (isParrying) {
            parryTimer -= dt
            if (parryTimer <= 0f) {
                isParrying = false
            }
        }

        // Coyote Time: detect walking off edge
        if (wasGroundedLastFrame && !isGrounded && vy >= 0f && coyoteTimer <= 0f && !isGroundPounding) {
            coyoteTimer = coyoteDuration
        } else if (coyoteTimer > 0f) {
            coyoteTimer -= dt
        }

        // Combo reset timer
        if (comboResetTimer > 0f) {
            comboResetTimer -= dt
            if (comboResetTimer <= 0f && !isAttacking) {
                attackComboIndex = 1
            }
        }

        // Combat timers & movement
        if (isDrinkingFlask) {
            drinkFlaskTimer -= dt
            vx = 0f
            if (drinkFlaskTimer <= 0f) {
                isDrinkingFlask = false
            }
        } else if (isRolling) {
            rollTimer -= dt
            vx = if (facingRight) rollSpeed else -rollSpeed
            animTime += dt * 20f
            if (rollTimer <= 0f) {
                isRolling = false
            }
        } else if (isPerformingSkill) {
            skillTimer -= dt
            val dir = if (facingRight) 1f else -1f
            val p = skillProgress

            when (activeSkillId) {
                "katana" -> { // Phantom Dash: surges forward through enemies
                    vx = dir * 650f * (1f - p * 0.5f)
                }
                "broadsword" -> { // Stalwart Guardbreaker: heavy step forward
                    vx = dir * 260f * (1f - p)
                }
                "greatsword" -> { // Titan Slam: leap forward and smash down
                    if (p < 0.45f) {
                        vx = dir * 180f
                    } else {
                        vx = 0f
                    }
                }
                "nightprowler_talon" -> { // Shadow Somersault: backflip away
                    vx = -dir * 220f * (1f - p)
                }
                "wyrm_wing_halberd" -> { // Vortical Tempest: spinning advance
                    vx = dir * 220f
                }
                "crimson_vein" -> { // Scarlet Cascade: 5-hit lunge
                    vx = dir * 280f * (1f - p * 0.6f)
                }
                else -> {
                    vx = 0f
                }
            }

            if (skillTimer <= 0f) {
                isPerformingSkill = false
            }
        } else if (isGroundPounding) {
            // Dead Cells Downward Smash Stomp plunge
            vx = 0f
            vy = 1050f
            animTime += dt * 10f
        } else if (isAttacking) {
            attackTimer -= dt
            // Plant feet in place during normal attacks (no forward lunge or backward recoil)
            vx = 0f

            if (attackTimer <= 0f) {
                isAttacking = false
                isShadowFlowStrike = false
            }
        } else if (isParrying) {
            vx = 0f
        } else {
            isPushing = false
            vx = 0f
            if (moveLeft) {
                vx = -moveSpeed
                facingRight = false
            }
            if (moveRight) {
                vx = moveSpeed
                facingRight = true
            }

            // Check if pushing against any box
            for (box in boxes) {
                val isVerticallyAligned = bounds.bottom > box.bounds.top + 5f && bounds.top < box.bounds.bottom - 5f
                if (isVerticallyAligned) {
                    if (moveRight && bounds.right >= box.bounds.left - 5f && bounds.left < box.bounds.left) {
                        isPushing = true
                        vx = pushSpeed
                        box.push(pushSpeed, platforms)
                    } else if (moveLeft && bounds.left <= box.bounds.right + 5f && bounds.right > box.bounds.right) {
                        isPushing = true
                        vx = -pushSpeed
                        box.push(-pushSpeed, platforms)
                    }
                }
            }
        }

        // Dead Cells Air-Stall during aerial slashes: dampens fall speed for aerial combos
        if (!isGrounded && isAttacking && attackProgress < 0.65f) {
            if (vy > 35f) vy = 35f
        }

        if (vx != 0f && isGrounded && !isRolling) {
            animTime += dt * 12f
        } else if (isGrounded && !isRolling) {
            animTime += dt * 2f
        }

        // Apex Float: Near jump peak (|vy| < 90), reduce gravity by 40% for fluid Dead Cells feel
        val effectiveGravity = if (!isGrounded && kotlin.math.abs(vy) < 90f) {
            gravity * 0.60f
        } else if (isGroundPounding) {
            gravity * 1.8f
        } else {
            gravity
        }

        vy += effectiveGravity * dt
        if (vy > 1200f) vy = 1200f

        // Jump Execution (handles Jump Buffer & Coyote Time)
        val wantsToJump = jumpRequested || jumpBufferTimer > 0f
        val canJump = (isGrounded || coyoteTimer > 0f) && !isRolling && !isDrinkingFlask && !isGroundPounding
        if (wantsToJump && canJump) {
            if (stats.useStamina(10f)) {
                vy = jumpStrength
                isGrounded = false
                coyoteTimer = 0f
                jumpBufferTimer = 0f
                jumpRequested = false
                onJumpTriggered?.invoke()
            }
        }

        bounds.x += vx * dt
        resolveHorizontalCollisions(platforms, boxes)

        bounds.y += vy * dt
        val wasGrounded = isGrounded
        isGrounded = false
        resolveVerticalCollisions(platforms, boxes, dt)

        // Ground pound landing detection
        if (isGroundPounding && isGrounded) {
            isGroundPounding = false
            hasJustGroundPounded = true
            groundPoundImpactX = bounds.centerX
            groundPoundImpactY = bounds.bottom
        }

        wasGroundedLastFrame = isGrounded
    }

    fun attack(onSpawnProjectile: ((MagicProjectile) -> Unit)? = null): Boolean {
        if (isRolling || isAttacking || !isAlive) return false
        if (equippedWeapon.manaCost > 0f && stats.currentMana < equippedWeapon.manaCost) return false
        if (!stats.useStamina(equippedWeapon.staminaCost)) return false

        if (equippedWeapon.manaCost > 0f) {
            stats.useMana(equippedWeapon.manaCost)
        }

        // Distinct combo choreography by weapon family
        val maxCombos = when (equippedWeapon.type) {
            WeaponType.KATANA -> 3      // Light: rapid 3-hit multi-strike
            WeaponType.SWORD -> 3       // Straight sword: 3-hit martial chain & thrust
            WeaponType.GREATSWORD -> 3  // Colossal: 3-hit heavy ground slam, upward cleave & overhead finisher
            WeaponType.CLUB -> 2        // Blunt: 2-hit diagonal smash & skull-cracker
            WeaponType.STAFF -> 1       // Spellcasting: 1-hit cosmic conduit burst
            WeaponType.BOW -> 1         // Ranged: 1-hit rapid arrow shot
            WeaponType.SCYTHE -> 2      // Reaper: 2-hit wide sweeping arcs
            WeaponType.GAUNTLET -> 4    // Brawler: ultra-fast 4-hit punch flurry
        }

        if (comboResetTimer > 0f) {
            attackComboIndex = (attackComboIndex % maxCombos) + 1
        } else {
            attackComboIndex = 1
        }

        // Distinct attack durations & combo reset windows:
        // Light weapons = fast strikes & tight windows; Heavy weapons = slow windups & generous windows
        val (dur, comboWindow) = when (equippedWeapon.type) {
            WeaponType.KATANA -> when (attackComboIndex) {
                1 -> Pair(0.16f, 0.46f) // Fast razor slice
                2 -> Pair(0.18f, 0.46f) // Rising uppercut
                else -> Pair(0.24f, 0.52f) // Twin cross flurry
            }
            WeaponType.SWORD -> when (attackComboIndex) {
                1 -> Pair(0.22f, 0.50f) // Diagonal cleave
                2 -> Pair(0.22f, 0.50f) // Backhand slice
                else -> Pair(0.28f, 0.55f) // Deep lunging thrust
            }
            WeaponType.GREATSWORD -> when (attackComboIndex) {
                1 -> Pair(0.38f, 0.72f) // Slow, heavy downward ground slam
                2 -> Pair(0.42f, 0.75f) // Upward earth-cleaving scoop
                else -> Pair(0.52f, 0.78f) // Colossal blazing overhead finisher
            }
            WeaponType.CLUB -> when (attackComboIndex) {
                1 -> Pair(0.26f, 0.52f) // Heavy blunt smash
                else -> Pair(0.36f, 0.62f) // Overhead two-handed skull-cracker
            }
            WeaponType.STAFF -> Pair(0.28f, 0.42f) // Arcane conduit pulse
            WeaponType.BOW -> Pair(0.22f, 0.40f) // Fast arrow draw & release
            WeaponType.SCYTHE -> when (attackComboIndex) {
                1 -> Pair(0.28f, 0.56f)
                else -> Pair(0.34f, 0.62f)
            }
            WeaponType.GAUNTLET -> when (attackComboIndex) {
                4 -> Pair(0.20f, 0.44f)
                else -> Pair(0.13f, 0.36f)
            }
        }

        comboResetTimer = comboWindow
        isAttacking = true
        isShadowFlowStrike = false
        if (attackComboIndex == maxCombos) {
            shadowFlowTimer = 0.75f
        }
        hitEnemiesThisAttack.clear()
        attackDuration = dur
        attackTimer = attackDuration

        if (equippedWeapon.type == WeaponType.STAFF || equippedWeapon.type == WeaponType.BOW) {
            val speedMag = if (equippedWeapon.type == WeaponType.BOW) 760f else 580f
            val projSpeed = if (facingRight) speedMag else -speedMag
            val px = if (facingRight) bounds.right + 12f else bounds.left - 12f
            val py = bounds.top + 26f
            onSpawnProjectile?.invoke(
                MagicProjectile(px, py, projSpeed, 0f, equippedWeapon.calculateDamage(stats))
            )
        }
        return true
    }

    fun applyOrigin(origin: CharacterOrigin) {
        currentOrigin = origin
        stats.vigor = origin.vigor
        stats.endurance = origin.endurance
        stats.strength = origin.strength
        stats.agility = origin.agility
        stats.intelligence = origin.intelligence
        stats.level = origin.startingLevel
        stats.resetToFull()
        inventory.clear()
        inventory.add(origin.startingWeapon)
        val cyclingRoster = listOf(
            Weapons.PALE_ECLIPSE_GREATSWORD,
            Weapons.SHADOW_KATANA,
            Weapons.OBSIDIAN_DAGGER,
            Weapons.STEEL_BROADSWORD,
            Weapons.IRON_SPEAR,
            Weapons.NIGHTPROWLER_TALON,
            Weapons.IRON_BATTLE_AXE,
            Weapons.EXECUTIONER_GREATSWORD,
            Weapons.GOLDEN_WARHAMMER,
            Weapons.ASTRAL_STAFF,
            Weapons.SUN_BOW,
            Weapons.SOUL_SCYTHE,
            Weapons.MOLTEN_GAUNTLETS
        )
        for (w in cyclingRoster) {
            if (w.id != origin.startingWeapon.id) {
                inventory.add(w)
            }
        }
        equippedWeapon = origin.startingWeapon
        equippedArmor = Armors.getStartingArmorForOrigin(origin.id)
        if (!inventoryArmors.contains(equippedArmor)) {
            inventoryArmors.add(0, equippedArmor)
        }
        attackComboIndex = 1
        comboResetTimer = 0f
        isAttacking = false
        isRolling = false
    }

    fun drinkFlask(isCrimson: Boolean): Boolean {
        if (isRolling || isAttacking || isDrinkingFlask || !isAlive) return false
        val success = if (isCrimson) stats.drinkCrimsonFlask() else stats.drinkCeruleanFlask()
        if (success) {
            isDrinkingFlask = true
            drinkFlaskTimer = 0.40f
            lastDrunkFlaskType = if (isCrimson) "CRIMSON" else "CERULEAN"
            return true
        }
        return false
    }

    fun takeDamage(amount: Float): Boolean {
        if (!isAlive || isInvulnerable) return false
        val defPercent = (equippedArmor.physicalDef / 100f).coerceIn(0f, 0.70f)
        val reducedAmount = (amount * (1f - defPercent)).coerceAtLeast(1f)
        val died = stats.takeDamage(reducedAmount)
        if (died) {
            isAlive = false
        }
        return true
    }

    fun parry(): Boolean {
        if (isRolling || isDrinkingFlask || isPerformingSkill || isParrying || parryCooldown > 0f || !isAlive) return false
        if (!stats.useStamina(10f)) return false

        isAttacking = false
        isShadowFlowStrike = false
        isParrying = true
        parryTimer = parryWindowDuration
        parryCooldown = 0.45f
        return true
    }

    fun roll(): Boolean {
        if (isRolling || !isGrounded || !isAlive) return false
        if (!stats.useStamina(18f)) return false

        isRolling = true
        rollTimer = rollDuration
        isAttacking = false
        return true
    }

    fun switchWeapon(onSpawnProjectile: ((MagicProjectile) -> Unit)? = null): Boolean {
        if (inventory.isEmpty()) return false
        val currentIndex = inventory.indexOf(equippedWeapon)
        val nextIndex = (currentIndex + 1) % inventory.size
        equippedWeapon = inventory[nextIndex]
        comboResetTimer = 0f

        if (shadowFlowTimer > 0f && !isRolling && !isPerformingSkill && isAlive && stats.useStamina(10f)) {
            shadowFlowTimer = 0f
            isAttacking = true
            isShadowFlowStrike = true
            attackComboIndex = when (equippedWeapon.type) {
                WeaponType.KATANA, WeaponType.SWORD, WeaponType.GREATSWORD -> 3
                WeaponType.CLUB, WeaponType.SCYTHE -> 2
                WeaponType.GAUNTLET -> 4
                WeaponType.STAFF, WeaponType.BOW -> 1
            }
            attackDuration = 0.22f
            attackTimer = attackDuration
            hitEnemiesThisAttack.clear()
            if (equippedWeapon.type == WeaponType.STAFF || equippedWeapon.type == WeaponType.BOW) {
                val speedMag = if (equippedWeapon.type == WeaponType.BOW) 780f else 620f
                val projSpeed = if (facingRight) speedMag else -speedMag
                val px = if (facingRight) bounds.right + 12f else bounds.left - 12f
                val py = bounds.top + 26f
                onSpawnProjectile?.invoke(
                    MagicProjectile(px, py, projSpeed, 0f, equippedWeapon.calculateDamage(stats) * 1.35f)
                )
            }
            return true
        }

        attackComboIndex = 1
        isAttacking = false
        isShadowFlowStrike = false
        return false
    }

    fun unlockWeapon(weapon: Weapon) {
        if (!inventory.contains(weapon)) {
            inventory.add(weapon)
        }
        equippedWeapon = weapon
        attackComboIndex = 1
        comboResetTimer = 0f
        isAttacking = false
    }

    fun getAttackHitbox(): AABB {
        val reach = when (equippedWeapon.type) {
            WeaponType.KATANA -> if (attackComboIndex == 3) 95f else 80f
            WeaponType.SWORD -> if (attackComboIndex == 3) 100f else 85f
            WeaponType.GREATSWORD -> 115f
            WeaponType.CLUB -> if (attackComboIndex == 3) 90f else 75f
            WeaponType.STAFF -> 70f
            WeaponType.BOW -> 65f
            WeaponType.SCYTHE -> 112f
            WeaponType.GAUNTLET -> 68f
        }
        val hx = if (facingRight) bounds.right else bounds.left - reach
        return AABB(hx, bounds.top - 15f, reach, bounds.height + 30f)
    }

    fun performWeaponSkill(onSpawnProjectile: ((MagicProjectile) -> Unit)? = null): Boolean {
        if (isRolling || isAttacking || isPerformingSkill || isDrinkingFlask || !isAlive) return false
        val cost = if (stats.asceticPactActive) equippedWeapon.skillManaCost * 0.5f else equippedWeapon.skillManaCost
        if (stats.currentMana < cost) return false
        if (!stats.useStamina(16f)) return false

        stats.useMana(cost)
        isPerformingSkill = true
        activeSkillId = equippedWeapon.id
        hitEnemiesThisAttack.clear()

        when (equippedWeapon.id) {
            "katana" -> { // Phantom Dash
                skillDuration = 0.28f
                skillTimer = skillDuration
            }
            "broadsword" -> { // Stalwart Guardbreaker
                skillDuration = 0.38f
                skillTimer = skillDuration
                vy = -260f
            }
            "greatsword" -> { // Titan Slam
                skillDuration = 0.52f
                skillTimer = skillDuration
                vy = -380f
            }
            "staff" -> { // Supernova Burst
                skillDuration = 0.35f
                skillTimer = skillDuration
                vx = 0f
            }
            "club" -> { // Earth Tremor
                skillDuration = 0.36f
                skillTimer = skillDuration
            }
            "nightprowler_talon" -> { // Shadow Somersault
                skillDuration = 0.42f
                skillTimer = skillDuration
                vy = -540f
            }
            "wyrm_wing_halberd" -> { // Vortical Tempest
                skillDuration = 0.45f
                skillTimer = skillDuration
            }
            "pale_eclipse_gs" -> { // Frostfall Wave
                skillDuration = 0.48f
                skillTimer = skillDuration
                val projSpeed = if (facingRight) 620f else -620f
                val px = if (facingRight) bounds.right + 10f else bounds.left - 10f
                val py = bounds.bottom - 24f
                onSpawnProjectile?.invoke(
                    MagicProjectile(px, py, projSpeed, 0f, equippedWeapon.calculateDamage(stats) * 1.6f)
                )
            }
            "crimson_vein" -> { // Scarlet Cascade
                skillDuration = 0.40f
                skillTimer = skillDuration
            }
            "sun_bow", "shadow_bone_bow" -> { // Triple Arrow Volley
                skillDuration = 0.30f
                skillTimer = skillDuration
                val dir = if (facingRight) 1f else -1f
                val px = if (facingRight) bounds.right + 10f else bounds.left - 10f
                val py = bounds.top + 26f
                val arrowDmg = equippedWeapon.calculateDamage(stats) * 0.95f
                onSpawnProjectile?.invoke(MagicProjectile(px, py, dir * 780f, -90f, arrowDmg))
                onSpawnProjectile?.invoke(MagicProjectile(px, py, dir * 780f, 0f, arrowDmg))
                onSpawnProjectile?.invoke(MagicProjectile(px, py, dir * 780f, 90f, arrowDmg))
            }
            "soul_scythe", "royal_scythe" -> { // Reaper Crescent
                skillDuration = 0.36f
                skillTimer = skillDuration
            }
            "molten_gauntlets", "crystal_claws" -> { // Rising Uppercut / Barrage
                skillDuration = 0.32f
                skillTimer = skillDuration
                vy = -340f
            }
            else -> {
                skillDuration = 0.35f
                skillTimer = skillDuration
            }
        }
        return true
    }

    fun triggerGroundPound(): Boolean {
        if (!isGrounded && !isGroundPounding && !isRolling && isAlive) {
            isGroundPounding = true
            isAttacking = false
            vy = 980f
            vx = 0f
            return true
        }
        return false
    }

    fun dropThrough(): Boolean {
        if (isGrounded) {
            dropThroughTimer = 0.28f
            return true
        }
        return false
    }

    private fun resolveHorizontalCollisions(platforms: List<Platform>, boxes: List<PushableBox>) {
        for (platform in platforms) {
            if (platform.type == PlatformType.DOOR && platform.isActivated) continue
            if (platform.type == PlatformType.PRESSURE_PLATE || platform.type == PlatformType.CHECKPOINT) continue
            if (platform.type == PlatformType.ONE_WAY) continue // One-way semi-solids don't block horizontally

            if (bounds.intersects(platform.bounds)) {
                if (platform.type == PlatformType.HAZARD) {
                    if (!isInvulnerable) {
                        stats.takeDamage(stats.maxHp)
                        isAlive = false
                        return
                    }
                }
                if (platform.type == PlatformType.EXIT) {
                    hasWon = true
                    return
                }

                if (vx > 0f) {
                    bounds.x = platform.bounds.left - width
                } else if (vx < 0f) {
                    bounds.x = platform.bounds.right
                }
            }
        }

        // Horizontal box collision
        for (box in boxes) {
            if (bounds.intersects(box.bounds)) {
                if (bounds.bottom > box.bounds.top + 8f) {
                    if (vx > 0f) {
                        bounds.x = box.bounds.left - width
                    } else if (vx < 0f) {
                        bounds.x = box.bounds.right
                    }
                }
            }
        }
    }

    private fun resolveVerticalCollisions(platforms: List<Platform>, boxes: List<PushableBox>, dt: Float) {
        for (platform in platforms) {
            if (platform.type == PlatformType.DOOR && platform.isActivated) continue
            if (platform.type == PlatformType.PRESSURE_PLATE || platform.type == PlatformType.CHECKPOINT) continue

            // One-Way Semi-Solid Platforms
            if (platform.type == PlatformType.ONE_WAY) {
                if (vy <= 0f || dropThroughTimer > 0f || isGroundPounding) continue
                // Check if player's feet passed from above the platform's top edge
                val prevBottom = bounds.bottom - vy * dt
                if (prevBottom <= platform.bounds.top + 14f && bounds.intersects(platform.bounds)) {
                    bounds.y = platform.bounds.top - height
                    vy = 0f
                    isGrounded = true
                    lastSafeGroundedX = bounds.centerX
                    lastSafeGroundedY = bounds.bottom
                }
                continue
            }

            if (bounds.intersects(platform.bounds)) {
                if (platform.type == PlatformType.HAZARD) {
                    if (!isInvulnerable) {
                        stats.takeDamage(stats.maxHp)
                        isAlive = false
                        return
                    }
                }
                if (platform.type == PlatformType.EXIT) {
                    hasWon = true
                    return
                }

                if (vy > 0f) {
                    bounds.y = platform.bounds.top - height
                    vy = 0f
                    isGrounded = true
                    if (platform.type == PlatformType.NORMAL) {
                        lastSafeGroundedX = bounds.centerX
                        lastSafeGroundedY = bounds.bottom
                    }
                } else if (vy < 0f) {
                    bounds.y = platform.bounds.bottom
                    vy = 0f
                }
            }
        }

        // Vertical box collision
        for (box in boxes) {
            if (bounds.intersects(box.bounds)) {
                if (vy > 0f && bounds.bottom - vy * 0.05f <= box.bounds.top + 16f) {
                    bounds.y = box.bounds.top - height
                    vy = 0f
                    isGrounded = true
                } else if (vy < 0f && bounds.top >= box.bounds.bottom - 16f) {
                    bounds.y = box.bounds.bottom
                    vy = 0f
                }
            }
        }
    }

    fun pressJump() {
        if (!isRolling && isAlive) {
            isJumpHeld = true
            jumpBufferTimer = jumpBufferDuration
            jumpRequested = true
        }
    }

    fun releaseJump() {
        isJumpHeld = false
        // Jump Cut: tapping gives short hop, holding gives full leap
        if (vy < -220f) {
            vy = -220f
        }
    }

    fun jump() {
        pressJump()
    }

    fun reset(startX: Float, startY: Float) {
        bounds.setPosition(startX, startY)
        lastSafeGroundedX = startX + width / 2f
        lastSafeGroundedY = startY + height
        vx = 0f
        vy = 0f
        isGrounded = false
        wasGroundedLastFrame = false
        isAlive = true
        hasWon = false
        isPushing = false
        isAttacking = false
        isPerformingSkill = false
        skillTimer = 0f
        isGroundPounding = false
        hasJustGroundPounded = false
        dropThroughTimer = 0f
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        isJumpHeld = false
        isRolling = false
        isDrinkingFlask = false
        drinkFlaskTimer = 0f
        attackComboIndex = 1
        comboResetTimer = 0f
        moveLeft = false
        moveRight = false
        jumpRequested = false
        animTime = 0f
        stats.resetToFull()
    }
}
