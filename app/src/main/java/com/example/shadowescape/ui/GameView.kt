package com.example.shadowescape.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.shadowescape.audio.HapticManager
import com.example.shadowescape.audio.SoundManager
import com.example.shadowescape.engine.Camera
import com.example.shadowescape.engine.GameLoop
import com.example.shadowescape.model.Armors
import com.example.shadowescape.model.CharacterOrigins
import com.example.shadowescape.model.EnemyType
import com.example.shadowescape.model.ItemRarity
import com.example.shadowescape.model.LootReward
import com.example.shadowescape.model.MagicProjectile
import com.example.shadowescape.model.Particle
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.WeaponType
import com.example.shadowescape.model.Weapons
import com.example.shadowescape.renderer.GameRenderer
import com.example.shadowescape.world.Level
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var gameLoop: GameLoop? = null
    private val level: Level = Level()
    private val player: Player = Player(level.spawnX, level.spawnY)
    private val camera: Camera = Camera(width.toFloat(), height.toFloat())
    private val renderer: GameRenderer = GameRenderer(context.assets)

    // Audio & Haptics
    private val soundManager: SoundManager = SoundManager(context)
    private val hapticManager: HapticManager = HapticManager(context)

    private var wasGrounded: Boolean = true
    private var wasAlive: Boolean = true

    // Combat Projectiles
    private val projectiles: MutableList<MagicProjectile> = mutableListOf()

    private val particles: MutableList<Particle> = mutableListOf()
    private var particleTimer: Float = 0f

    private var gameStatusText: String = "Shadow Escape"

    // DPI scale helper
    private val density: Float = context.resources.displayMetrics.density

    // Touch button hitboxes
    private val leftBtnRect = RectF()
    private val rightBtnRect = RectF()
    private val attackBtnRect = RectF()
    private val rollBtnRect = RectF()
    private val parryBtnRect = RectF()
    private val jumpBtnRect = RectF()
    private val useBtnRect = RectF()
    private val crimsonFlaskBtnRect = RectF()
    private val ceruleanFlaskBtnRect = RectF()
    private val skillBtnRect = RectF()
    private val diveBtnRect = RectF()
    private val weaponCardRect = RectF()

    // Dead Cells Hitstop (Impact Freeze)
    var hitstopTimer: Float = 0f

    // Character Origin & Attribute Allocation State
    var isSelectingOrigin: Boolean = true
    val baseAttributeStat: Int = 8
    val totalAttributePoints: Int = 20
    var allocatedVigor: Int = 13
    var allocatedEndurance: Int = 12
    var allocatedStrength: Int = 14
    var allocatedAgility: Int = 8
    var allocatedIntelligence: Int = 13
    val statMinusBtnRects = Array(5) { RectF() }
    val statPlusBtnRects = Array(5) { RectF() }
    val statResetBtnRect = RectF()
    private val originAwakenBtnRect = RectF()

    // Pause Menu State & Touch Bounds
    var isPaused: Boolean = false
    private val pauseBtnRect = RectF()
    private val pauseContinueBtnRect = RectF()
    private val pauseEquipmentBtnRect = RectF()
    private val pauseRestartBtnRect = RectF()
    private val pauseRestartGameBtnRect = RectF()
    private val pauseSaveBtnRect = RectF()
    private val pauseLoadBtnRect = RectF()
    private val pauseCloseBtnRect = RectF()

    // Armory & Equipment State & Touch Bounds
    var isViewingArmory: Boolean = false
    var selectedArmorIndex: Int = 0
    val armorSlotRects = List(25) { RectF() }
    val armoryEquipBtnRect = RectF()
    val armoryCloseBtnRect = RectF()

    // Site of Grace Menu State & Touch Bounds
    var isRestingAtGrace: Boolean = false
    private val graceRestBtnRect = RectF()
    private val graceLeaveBtnRect = RectF()
    private val graceCloseBtnRect = RectF()
    private val graceAttrBtnRects = Array(5) { RectF() }
    private val graceBloodPactRect = RectF()
    private val graceAsceticPactRect = RectF()

    init {
        holder.addCallback(this)
        isFocusable = true

        level.onCheckpointReached = {
            soundManager.playCheckpoint()
            hapticManager.vibrateCheckpoint()
        }
        level.onGraceOpened = {
            isRestingAtGrace = true
            player.moveLeft = false
            player.moveRight = false
            player.vx = 0f
            soundManager.playCheckpoint()
            hapticManager.vibrateCheckpoint()
        }
        level.onGraceMimicAwakened = {
            soundManager.playMimicGrowl()
            soundManager.playVisceralStrike()
            hapticManager.vibrateVisceralStrike()
            renderer.triggerScreenShake(0.38f, 15f)
            gameStatusText = "⚠ DECEITFUL GRACE AWAKENED! ⚠"
        }
        level.onSwitchToggled = {
            soundManager.playSwitch()
            hapticManager.vibrateSwitch()
        }
        level.onDoorStateChanged = {
            soundManager.playGate()
        }
        level.onLootDiscovered = { loot ->
            renderer.showLootDiscovery(loot)
            soundManager.playChestOpen()
            hapticManager.vibrateHeavyAttack()
            renderer.triggerScreenShake(0.18f, 6f)
            if (loot.weapon != null) {
                player.unlockWeapon(loot.weapon)
                gameStatusText = "Acquired: ${loot.title}"
            }
            if (loot.isPrimordialSeed) {
                player.stats.maxCrimsonFlasks++
                player.stats.currentCrimsonFlasks = player.stats.maxCrimsonFlasks
                gameStatusText = "Primordial Seed • Max Flasks Increased!"
            }
            if (loot.essenceBonus > 0) {
                player.stats.addEssence(loot.essenceBonus)
            }
        }
        level.onBloodstainReclaimed = { reclaimed ->
            soundManager.playBloodstainReclaim()
            hapticManager.vibrateBloodstain()
            renderer.triggerScreenShake(0.14f, 5f)
            gameStatusText = "✦ LOST ESSENCE RECLAIMED • +$reclaimed Essence ✦"
        }
        level.onMimicTrapSprung = {
            soundManager.playMimicGrowl()
            hapticManager.vibrateVisceralStrike()
            renderer.triggerScreenShake(0.35f, 12f)
            gameStatusText = "⚠ MIMIC TRAP! It clamped onto you! ⚠"
        }
        level.onHiddenBossAwakened = {
            soundManager.playMimicGrowl()
            hapticManager.vibrateVisceralStrike()
            renderer.triggerScreenShake(0.45f, 14f)
            gameStatusText = "⚠ HIDDEN BOSS AWAKENED: DEMON SLIME (PHASE 2) ⚠"
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera.updateViewport(width.toFloat(), height.toFloat())
        gameLoop = GameLoop(holder, this).apply {
            isRunning = true
            start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.updateViewport(width.toFloat(), height.toFloat())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameLoop?.let { loop ->
            loop.isRunning = false
            loop.interrupt()
            while (retry) {
                try {
                    loop.join()
                    retry = false
                } catch (e: InterruptedException) {
                    // retry
                }
            }
        }
        gameLoop = null
        soundManager.release()
    }

    private fun onEnemyKilled(enemy: com.example.shadowescape.model.Enemy) {
        if (enemy.isShadowEcho) {
            player.stats.addEssence(enemy.shadowEchoEssence, applyPactBonus = false)
            level.clearBloodstain()
            soundManager.playVisceralStrike()
            gameStatusText = "✦ SHADOW ECHO VANQUISHED • +${enemy.shadowEchoEssence} Essence (120%)! ✦"
            return
        }

        player.stats.addEssence(enemy.essenceReward)
        if (enemy.isZoneBoss) {
            soundManager.playVisceralStrike()
            gameStatusText = "✦ MALAKOR VANQUISHED • +${enemy.essenceReward} Essence! ✦"
            level.onLootDiscovered?.invoke(
                LootReward(
                    title = "Necro-Bone Bow",
                    subtitle = "Abyssal Bow • Epic",
                    description = "Claimed from Malakor, Crimson Abyss Lord. Fires silent bone bolts that inflict Bleed.",
                    rarity = ItemRarity.EPIC,
                    weapon = Weapons.SHADOW_BONE_BOW
                )
            )
            return
        }

        gameStatusText = "Enemy Felled • +${enemy.essenceReward} Essence"
        if (enemy.type == EnemyType.ELITE_GUARDIAN) {
            level.onLootDiscovered?.invoke(
                LootReward(
                    title = "Crimson Vein Katana",
                    subtitle = "Cursed Katana • Legendary",
                    description = "Felled from the Elite Guardian. Bathed in scarlet essence with unrivaled flurry slicing.",
                    rarity = ItemRarity.LEGENDARY,
                    weapon = Weapons.CRIMSON_VEIN_KATANA
                )
            )
        }
    }

    fun update(dt: Float) {
        if (isSelectingOrigin) {
            renderer.update(dt, player)
            return
        }

        if (isPaused || isRestingAtGrace) {
            // Game is paused or resting at Grace: physics, enemies, projectiles, timers are suspended
            return
        }

        // Dead Cells Hitstop (Impact Freeze): pause physics for 3-5 frames on blade connection
        if (hitstopTimer > 0f) {
            hitstopTimer -= dt
            renderer.update(dt * 0.15f, player)
            return
        }

        // Atmospheric floating motes
        particleTimer += dt
        if (particleTimer > 0.15f) {
            particleTimer = 0f
            val px = Random.nextFloat() * level.width
            val py = Random.nextFloat() * level.height
            val p = Particle(
                px, py,
                (Random.nextFloat() - 0.5f) * 20f,
                -10f - Random.nextFloat() * 15f,
                3f + Random.nextFloat() * 3f,
                120,
                4f + Random.nextFloat() * 4f
            )
            particles.add(p)
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.update(dt)
            if (p.isDead()) {
                iterator.remove()
            }
        }

        // Update active magic projectiles & hit detection vs enemies & mimics
        val pIter = projectiles.iterator()
        while (pIter.hasNext()) {
            val proj = pIter.next()
            proj.update(dt, level.platforms)
            if (!proj.isDead) {
                // Check enemy hits
                for (enemy in level.enemies) {
                    if (enemy.isAlive && proj.bounds.intersects(enemy.bounds)) {
                        proj.isDead = true
                        val killed = enemy.takeDamage(proj.damage, 18f)
                        val statusMsg = enemy.applyStatus(player.equippedWeapon.statusEffect, player.equippedWeapon.statusBuildup)
                        if (statusMsg != null) gameStatusText = statusMsg
                        soundManager.playEnemyHit()
                        hapticManager.vibrateAttack()
                        hitstopTimer = 0.05f
                        if (killed) {
                            onEnemyKilled(enemy)
                        }
                        break
                    }
                }
                // Check mimic chest hits
                if (!proj.isDead) {
                    for (chest in level.chests) {
                        if (chest.isMimic && !chest.isDefeated && proj.bounds.intersects(chest.bounds)) {
                            proj.isDead = true
                            val wasAwakened = chest.isAwakened
                            val killed = chest.takeDamage(proj.damage)
                            soundManager.playEnemyHit()
                            hapticManager.vibrateAttack()
                            hitstopTimer = 0.05f
                            if (!wasAwakened) {
                                soundManager.playMimicGrowl()
                                gameStatusText = "✦ MIMIC AWAKENED! Prepare to fight! ✦"
                            }
                            if (killed) {
                                soundManager.playVisceralStrike()
                                level.onLootDiscovered?.invoke(chest.loot)
                                gameStatusText = "✦ MIMIC SLAIN • Spoils Claimed! ✦"
                            }
                            break
                        }
                    }
                }
            }
            if (proj.isDead) {
                pIter.remove()
            }
        }

        // Update Level entities & enemies with callbacks
        level.update(
            dt, player,
            onEnemyAttackPlayer = { damage, attacker ->
                if (player.isParrying) {
                    attacker?.forceStagger(2.2f)
                    soundManager.playVisceralStrike()
                    hapticManager.vibrateVisceralStrike()
                    renderer.triggerScreenShake(0.22f, 10f)
                    hitstopTimer = 0.08f
                    gameStatusText = "⚡ PARRY! Enemy Staggered — Strike for Visceral Riposte! ⚡"
                } else {
                    val finalDmg = if (player.stats.bloodPactActive) damage * 1.35f else damage
                    player.takeDamage(finalDmg)
                    soundManager.playEnemyHit()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.18f, 7f)
                }
            },
            onEnemyStaggered = {
                soundManager.playHeavySwing(1.3f)
                hapticManager.vibrateHeavyAttack()
                renderer.triggerScreenShake(0.20f, 8f)
                gameStatusText = "✦ ENEMY POISE BROKEN! Execute Visceral Strike! ✦"
            }
        )

        // Dead Cells Ground Pound (Downward Stomp) impact handling
        if (player.hasJustGroundPounded) {
            player.hasJustGroundPounded = false
            renderer.triggerGroundPoundVfx(player.groundPoundImpactX, player.groundPoundImpactY)
            soundManager.playStompImpact()
            hapticManager.vibrateHeavyAttack()
            renderer.triggerScreenShake(0.30f, 14f)
            hitstopTimer = 0.09f

            val impactX = player.groundPoundImpactX
            val impactY = player.groundPoundImpactY
            for (enemy in level.enemies) {
                if (enemy.isAlive) {
                    val dist = kotlin.math.hypot(enemy.bounds.centerX - impactX, enemy.bounds.bottom - impactY)
                    if (dist < 180f) {
                        val stompDmg = 45f + (player.stats.strength - 10).coerceAtLeast(0) * 2.2f
                        val killed = enemy.takeDamage(stompDmg, 80f)
                        soundManager.playEnemyHit()
                        if (killed) {
                            player.stats.addEssence(enemy.essenceReward)
                            gameStatusText = "✦ Concussive Stomp Felled Enemy! +${enemy.essenceReward} Essence ✦"
                            if (enemy.type == EnemyType.ELITE_GUARDIAN) {
                                level.onLootDiscovered?.invoke(
                                    LootReward(
                                        title = "Crimson Vein Katana",
                                        subtitle = "Cursed Katana • Legendary",
                                        description = "Felled from the Elite Guardian. Bathed in scarlet essence with unrivaled flurry slicing.",
                                        rarity = ItemRarity.LEGENDARY,
                                        weapon = Weapons.CRIMSON_VEIN_KATANA
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Weapon Skill combat collisions
        if (player.isPerformingSkill) {
            val skillBox = player.getAttackHitbox()
            for (enemy in level.enemies) {
                if (enemy.isAlive && enemy.id !in player.hitEnemiesThisAttack && skillBox.intersects(enemy.bounds)) {
                    player.hitEnemiesThisAttack.add(enemy.id)
                    val baseDmg = player.equippedWeapon.calculateDamage(player.stats)
                    val isClash = enemy.attackWindupTimer > 0f
                    var skillDmg = baseDmg * 1.9f
                    var skillPoise = 65f
                    if (isClash) {
                        enemy.cancelAttackWindup(1.6f)
                        val refund = player.equippedWeapon.skillManaCost * 0.5f
                        player.stats.currentMana = (player.stats.currentMana + refund).coerceAtMost(player.stats.maxMana)
                        skillDmg *= 1.25f
                        skillPoise *= 1.5f
                        soundManager.playVisceralStrike()
                        hitstopTimer = 0.11f
                        gameStatusText = "⚔ CLASH! Attack Broken • +${refund.toInt()} FP Refunded! ⚔"
                    }
                    val killed = enemy.takeDamage(skillDmg, skillPoise)
                    val statusMsg = enemy.applyStatus(player.equippedWeapon.statusEffect, player.equippedWeapon.statusBuildup * 1.6f)
                    if (statusMsg != null) gameStatusText = statusMsg
                    soundManager.playEnemyHit()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.24f, 10f)
                    hitstopTimer = 0.085f

                    if (killed) {
                        onEnemyKilled(enemy)
                    }
                }
            }

            for (chest in level.chests) {
                if (chest.isMimic && !chest.isDefeated && chest.id !in player.hitEnemiesThisAttack && skillBox.intersects(chest.bounds)) {
                    player.hitEnemiesThisAttack.add(chest.id)
                    val skillDmg = player.equippedWeapon.calculateDamage(player.stats) * 1.9f
                    val wasAwakened = chest.isAwakened
                    val killed = chest.takeDamage(skillDmg)
                    soundManager.playEnemyHit()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.24f, 10f)
                    hitstopTimer = 0.085f
                    if (!wasAwakened) {
                        soundManager.playMimicGrowl()
                        gameStatusText = "✦ MIMIC AWAKENED! Prepare to fight! ✦"
                    }
                    if (killed) {
                        soundManager.playVisceralStrike()
                        level.onLootDiscovered?.invoke(chest.loot)
                        gameStatusText = "✦ MIMIC SLAIN • Spoils Claimed! ✦"
                    }
                }
            }
        }

        // Player Melee Attack vs Enemies & Mimic chests
        if (player.isAttacking && player.attackProgress in 0.16f..0.75f) {
            val attackBox = player.getAttackHitbox()
            for (enemy in level.enemies) {
                if (enemy.isAlive && enemy.id !in player.hitEnemiesThisAttack && attackBox.intersects(enemy.bounds)) {
                    player.hitEnemiesThisAttack.add(enemy.id)
                    var dmg = player.equippedWeapon.calculateDamage(player.stats)
                    var poiseDmg = when (player.equippedWeapon.type) {
                        WeaponType.GREATSWORD -> 36f
                        WeaponType.CLUB -> 30f
                        WeaponType.SCYTHE -> 26f
                        WeaponType.SWORD -> 20f
                        WeaponType.KATANA -> 16f
                        WeaponType.GAUNTLET -> 14f
                        WeaponType.STAFF -> 12f
                        WeaponType.BOW -> 10f
                    }
                    if (player.isShadowFlowStrike) {
                        dmg *= 1.45f
                        poiseDmg *= 1.5f
                    }
                    val wasStaggered = enemy.isStaggered
                    if (wasStaggered) {
                        val critMult = if (player.equippedWeapon.type == WeaponType.KATANA) 2.5f else 2.2f
                        dmg *= critMult
                        enemy.isStaggered = false
                        enemy.currentPoise = enemy.maxPoise
                        soundManager.playVisceralStrike()
                        hapticManager.vibrateVisceralStrike()
                        renderer.triggerScreenShake(0.28f, 12f)
                        hitstopTimer = 0.11f
                        gameStatusText = "☠ VISCERAL RIPOSTE! ${dmg.toInt()} DMG! ☠"
                    }
                    val killed = enemy.takeDamage(dmg, poiseDmg)
                    val statusMsg = enemy.applyStatus(player.equippedWeapon.statusEffect, player.equippedWeapon.statusBuildup)
                    if (statusMsg != null) gameStatusText = statusMsg
                    soundManager.playEnemyHit()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.12f, 5f)
                    hitstopTimer = if (player.equippedWeapon.type == WeaponType.GREATSWORD) 0.085f else 0.055f

                    if (killed) {
                        onEnemyKilled(enemy)
                    }
                }
            }

            // Also check Mimic chests for melee strikes
            for (chest in level.chests) {
                if (chest.isMimic && !chest.isDefeated && chest.id !in player.hitEnemiesThisAttack && attackBox.intersects(chest.bounds)) {
                    player.hitEnemiesThisAttack.add(chest.id)
                    val dmg = player.equippedWeapon.calculateDamage(player.stats)
                    val wasAwakened = chest.isAwakened
                    val killed = chest.takeDamage(dmg)
                    soundManager.playEnemyHit()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.18f, 7f)
                    hitstopTimer = 0.075f

                    if (!wasAwakened) {
                        soundManager.playMimicGrowl()
                        gameStatusText = "✦ MIMIC AWAKENED! Prepare to fight! ✦"
                    }

                    if (killed) {
                        soundManager.playVisceralStrike()
                        level.onLootDiscovered?.invoke(chest.loot)
                        gameStatusText = "✦ MIMIC SLAIN • Spoils Claimed! ✦"
                    }
                }
            }
        }

        // Update Player physics & collisions
        player.update(dt, level.platforms, level.boxes)

        // Audio & Haptics: Land impact
        if (player.isGrounded && !wasGrounded && player.isAlive) {
            soundManager.playLand()
            hapticManager.vibrateLand()
        }
        wasGrounded = player.isGrounded

        // Audio & Haptics: Death impact & Bloodstain Drop
        if (wasAlive && !player.isAlive) {
            soundManager.playDeath()
            hapticManager.vibrateDeath()
            val dropped = player.stats.essence
            level.dropBloodstain(player.lastSafeGroundedX, player.lastSafeGroundedY, dropped)
            player.stats.essence = 0
            if (dropped > 0) {
                gameStatusText = "You died. Dropped $dropped Essence at your bloodstain."
            }
        }
        wasAlive = player.isAlive

        // Audio: Footsteps
        if (player.isGrounded && (player.moveLeft || player.moveRight) && player.vx != 0f && !player.isRolling) {
            soundManager.playFootstep()
        }

        // Audio: Box pushing friction
        if (player.isPushing) {
            soundManager.playBoxPush()
        }

        // Update Renderer animations
        renderer.update(dt, player)

        // Camera smoothly follows player
        camera.follow(
            player.bounds.x + player.width / 2f,
            player.bounds.y + player.height / 2f,
            level.width,
            level.height
        )

        if (!player.isAlive) {
            gameStatusText = "You dissolved in shadow. Tap to respawn at Checkpoint."
        } else if (player.hasWon) {
            gameStatusText = "Reached the Abyss Obelisk! The journey continues..."
        }
    }

    fun render(canvas: Canvas) {
        renderer.render(
            canvas,
            level,
            player,
            particles,
            projectiles,
            camera.x,
            camera.y,
            width.toFloat(),
            height.toFloat(),
            gameStatusText
        )
        if (isSelectingOrigin) {
            renderer.drawOriginSelectionScreen(
                canvas,
                width.toFloat(),
                height.toFloat(),
                allocatedVigor,
                allocatedEndurance,
                allocatedStrength,
                allocatedAgility,
                allocatedIntelligence,
                totalAttributePoints,
                statMinusBtnRects,
                statPlusBtnRects,
                statResetBtnRect,
                originAwakenBtnRect
            )
        } else {
            drawTouchControls(canvas)
            renderer.drawPauseButton(canvas, width.toFloat(), pauseBtnRect)
            if (isViewingArmory) {
                renderer.drawArmoryScreen(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    player,
                    selectedArmorIndex,
                    armorSlotRects,
                    armoryEquipBtnRect,
                    armoryCloseBtnRect
                )
            } else if (isPaused) {
                renderer.drawPauseMenu(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    pauseContinueBtnRect,
                    pauseEquipmentBtnRect,
                    pauseRestartBtnRect,
                    pauseRestartGameBtnRect,
                    pauseSaveBtnRect,
                    pauseLoadBtnRect,
                    pauseCloseBtnRect,
                    player
                )
            } else if (isRestingAtGrace) {
                renderer.drawGraceMenu(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    graceRestBtnRect,
                    graceLeaveBtnRect,
                    graceCloseBtnRect,
                    graceAttrBtnRects,
                    graceBloodPactRect,
                    graceAsceticPactRect,
                    player
                )
            }
        }
    }

    private fun drawTouchControls(canvas: Canvas) {
        val btnBgPaint = Paint().apply {
            color = Color.parseColor("#44101420")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val btnBorderPaint = Paint().apply {
            color = Color.parseColor("#7738BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        val attackBtnBgPaint = Paint().apply {
            color = Color.parseColor("#66DC2626")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val attackBtnBorder = Paint().apply {
            color = Color.parseColor("#AAEF4444")
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            isAntiAlias = true
        }
        val iconPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 24f * density
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val attackIconPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 28f * density
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val btnSize = 58f * density
        val margin = 26f * density
        val bottomY = h - margin - btnSize

        // ==========================================
        // LEFT THUMB ZONE: Movement & Flasks
        // ==========================================

        // Left button ◀
        val leftX = margin
        leftBtnRect.set(leftX, bottomY, leftX + btnSize, bottomY + btnSize)
        canvas.drawRoundRect(leftBtnRect, 16f * density, 16f * density, btnBgPaint)
        canvas.drawRoundRect(leftBtnRect, 16f * density, 16f * density, btnBorderPaint)
        canvas.drawText("◀", leftBtnRect.centerX(), leftBtnRect.centerY() + 8f * density, iconPaint)

        // Right button ▶
        val rightX = leftX + btnSize + 14f * density
        rightBtnRect.set(rightX, bottomY, rightX + btnSize, bottomY + btnSize)
        canvas.drawRoundRect(rightBtnRect, 16f * density, 16f * density, btnBgPaint)
        canvas.drawRoundRect(rightBtnRect, 16f * density, 16f * density, btnBorderPaint)
        canvas.drawText("▶", rightBtnRect.centerX(), rightBtnRect.centerY() + 8f * density, iconPaint)

        // Flask Crimson 🧪 above ◀ (Left Thumb heals HP safely while navigating)
        val flaskSize = 50f * density
        val flaskY = bottomY - flaskSize - 14f * density
        val crimsonX = leftX + (btnSize - flaskSize) / 2f
        crimsonFlaskBtnRect.set(crimsonX, flaskY, crimsonX + flaskSize, flaskY + flaskSize)
        val crimsonBgPaint = Paint().apply {
            color = Color.parseColor("#667F1D1D")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val crimsonBorderPaint = Paint().apply {
            color = Color.parseColor("#DDEF4444")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        val flaskCountPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 11.5f * density
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(crimsonFlaskBtnRect, 14f * density, 14f * density, crimsonBgPaint)
        canvas.drawRoundRect(crimsonFlaskBtnRect, 14f * density, 14f * density, crimsonBorderPaint)
        canvas.drawText("🧪", crimsonFlaskBtnRect.centerX(), crimsonFlaskBtnRect.centerY() + 3f * density, iconPaint)
        canvas.drawText("${player.stats.currentCrimsonFlasks}", crimsonFlaskBtnRect.centerX(), crimsonFlaskBtnRect.bottom - 5f * density, flaskCountPaint)

        // Flask Cerulean 💧 above ▶ (Left Thumb restores FP safely)
        val ceruleanX = rightX + (btnSize - flaskSize) / 2f
        ceruleanFlaskBtnRect.set(ceruleanX, flaskY, ceruleanX + flaskSize, flaskY + flaskSize)
        val ceruleanBgPaint = Paint().apply {
            color = Color.parseColor("#660369A1")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val ceruleanBorderPaint = Paint().apply {
            color = Color.parseColor("#DD38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        canvas.drawRoundRect(ceruleanFlaskBtnRect, 14f * density, 14f * density, ceruleanBgPaint)
        canvas.drawRoundRect(ceruleanFlaskBtnRect, 14f * density, 14f * density, ceruleanBorderPaint)
        canvas.drawText("💧", ceruleanFlaskBtnRect.centerX(), ceruleanFlaskBtnRect.centerY() + 3f * density, iconPaint)
        canvas.drawText("${player.stats.currentCeruleanFlasks}", ceruleanFlaskBtnRect.centerX(), ceruleanFlaskBtnRect.bottom - 5f * density, flaskCountPaint)

        // ==========================================
        // RIGHT THUMB ZONE: Natural Radial Combat Arc
        // ==========================================

        // Centerpiece: Primary ATTACK ⚔ (25% enlarged for effortless mashing)
        val attackSize = 74f * density
        val attackCx = w - margin - btnSize * 1.55f
        val attackCy = bottomY - btnSize * 0.35f
        attackBtnRect.set(attackCx - attackSize / 2f, attackCy - attackSize / 2f, attackCx + attackSize / 2f, attackCy + attackSize / 2f)
        canvas.drawRoundRect(attackBtnRect, 20f * density, 20f * density, attackBtnBgPaint)
        canvas.drawRoundRect(attackBtnRect, 20f * density, 20f * density, attackBtnBorder)
        canvas.drawText("⚔", attackBtnRect.centerX(), attackBtnRect.centerY() + 10f * density, attackIconPaint)

        // JUMP ▲ (Natural thumb extension to the right / lower-right)
        val jumpCx = attackCx + btnSize * 1.08f
        val jumpCy = bottomY + btnSize * 0.05f
        jumpBtnRect.set(jumpCx - btnSize / 2f, jumpCy - btnSize / 2f, jumpCx + btnSize / 2f, jumpCy + btnSize / 2f)
        canvas.drawRoundRect(jumpBtnRect, 16f * density, 16f * density, btnBgPaint)
        canvas.drawRoundRect(jumpBtnRect, 16f * density, 16f * density, btnBorderPaint)
        canvas.drawText("▲", jumpBtnRect.centerX(), jumpBtnRect.centerY() + 8f * density, iconPaint)

        // ROLL 💨 (Lower-left swipe from Attack)
        val rollCx = attackCx - btnSize * 1.08f
        val rollCy = bottomY + btnSize * 0.05f
        rollBtnRect.set(rollCx - btnSize / 2f, rollCy - btnSize / 2f, rollCx + btnSize / 2f, rollCy + btnSize / 2f)
        canvas.drawRoundRect(rollBtnRect, 16f * density, 16f * density, btnBgPaint)
        canvas.drawRoundRect(rollBtnRect, 16f * density, 16f * density, btnBorderPaint)
        canvas.drawText("💨", rollBtnRect.centerX(), rollBtnRect.centerY() + 8f * density, iconPaint)

        // PARRY / DEFLECT 🛡 (Left of Roll/Skill arc)
        val parryCx = rollCx - btnSize * 1.12f
        val parryCy = rollCy - btnSize * 0.50f
        parryBtnRect.set(parryCx - btnSize / 2f, parryCy - btnSize / 2f, parryCx + btnSize / 2f, parryCy + btnSize / 2f)
        val parryBgPaint = Paint().apply {
            color = if (player.isParrying) Color.parseColor("#99CA8A04") else Color.parseColor("#55334155")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val parryBorderPaint = Paint().apply {
            color = if (player.isParrying) Color.parseColor("#FACC15") else Color.parseColor("#FBBF24")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
            isAntiAlias = true
        }
        canvas.drawRoundRect(parryBtnRect, 16f * density, 16f * density, parryBgPaint)
        canvas.drawRoundRect(parryBtnRect, 16f * density, 16f * density, parryBorderPaint)
        canvas.drawText("🛡", parryBtnRect.centerX(), parryBtnRect.centerY() + 8f * density, iconPaint)

        // WEAPON SKILL ⚡ (Upper-left nudge from Attack)
        val skillCx = attackCx - btnSize * 0.72f
        val skillCy = attackCy - btnSize * 1.08f
        skillBtnRect.set(skillCx - btnSize / 2f, skillCy - btnSize / 2f, skillCx + btnSize / 2f, skillCy + btnSize / 2f)
        val skillBgPaint = Paint().apply {
            color = Color.parseColor("#664C1D95")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val skillBorder = Paint().apply {
            color = Color.parseColor("#C084FC")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        val skillSubPaint = Paint().apply {
            color = Color.parseColor("#E9D5FF")
            textSize = 10f * density
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(skillBtnRect, 16f * density, 16f * density, skillBgPaint)
        canvas.drawRoundRect(skillBtnRect, 16f * density, 16f * density, skillBorder)
        canvas.drawText("⚡", skillBtnRect.centerX(), skillBtnRect.centerY() + 2f * density, attackIconPaint)
        canvas.drawText("${player.equippedWeapon.skillManaCost.toInt()}FP", skillBtnRect.centerX(), skillBtnRect.bottom - 5f * density, skillSubPaint)

        // DIVE / DROP ⬇ / ⇣ (Upper-right nudge from Attack, above Jump)
        val diveCx = attackCx + btnSize * 0.75f
        val diveCy = attackCy - btnSize * 1.08f
        diveBtnRect.set(diveCx - btnSize / 2f, diveCy - btnSize / 2f, diveCx + btnSize / 2f, diveCy + btnSize / 2f)
        val isAirborne = !player.isGrounded
        val diveBgPaint = Paint().apply {
            color = if (isAirborne) Color.parseColor("#88B45309") else Color.parseColor("#331E293B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val diveBorder = Paint().apply {
            color = if (isAirborne) Color.parseColor("#F59E0B") else Color.parseColor("#5564748B")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        val diveSubPaint = Paint().apply {
            color = if (isAirborne) Color.parseColor("#FEF08A") else Color.parseColor("#94A3B8")
            textSize = 9.5f * density
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(diveBtnRect, 16f * density, 16f * density, diveBgPaint)
        canvas.drawRoundRect(diveBtnRect, 16f * density, 16f * density, diveBorder)
        val diveIcon = if (isAirborne) "⬇" else "⇣"
        val diveLabel = if (isAirborne) "STOMP" else "DROP"
        canvas.drawText(diveIcon, diveBtnRect.centerX(), diveBtnRect.centerY() + 2f * density, iconPaint)
        canvas.drawText(diveLabel, diveBtnRect.centerX(), diveBtnRect.bottom - 5f * density, diveSubPaint)

        // INTERACT / USE 🖐 (Contextual! Only appears right above Attack when near chests, lanterns, or switches)
        val hasInteractable = level.hasNearInteractable(player)
        if (hasInteractable) {
            val useW = btnSize * 1.5f
            val useH = btnSize * 0.95f
            val useCx = attackCx
            val useCy = attackCy - btnSize * 1.35f
            useBtnRect.set(useCx - useW / 2f, useCy - useH / 2f, useCx + useW / 2f, useCy + useH / 2f)

            val useBgPaint = Paint().apply {
                color = Color.parseColor("#CC78350F")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val useBorder = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.STROKE
                strokeWidth = 2.8f * density
                isAntiAlias = true
            }
            val useTextPaint = Paint().apply {
                color = Color.parseColor("#FEF08A")
                textSize = 13f * density
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawRoundRect(useBtnRect, 16f * density, 16f * density, useBgPaint)
            canvas.drawRoundRect(useBtnRect, 16f * density, 16f * density, useBorder)
            canvas.drawText("🖐 INTERACT", useBtnRect.centerX(), useBtnRect.centerY() + 4.5f * density, useTextPaint)
        } else {
            useBtnRect.setEmpty()
        }

        // Weapon Card (Top Right - tap to swap weapons)
        weaponCardRect.set(w - 236f - 36f - 56f, 30f, w - 36f - 56f, 92f)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()

        if (isSelectingOrigin) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val index = event.actionIndex
                val px = event.getX(index)
                val py = event.getY(index)

                val spent = (allocatedVigor - baseAttributeStat) +
                    (allocatedEndurance - baseAttributeStat) +
                    (allocatedStrength - baseAttributeStat) +
                    (allocatedAgility - baseAttributeStat) +
                    (allocatedIntelligence - baseAttributeStat)
                val remainingPoints = (totalAttributePoints - spent).coerceAtLeast(0)

                // 1. Check Minus Buttons [-]
                for (i in statMinusBtnRects.indices) {
                    if (statMinusBtnRects[i].contains(px, py)) {
                        when (i) {
                            0 -> if (allocatedVigor > baseAttributeStat) allocatedVigor--
                            1 -> if (allocatedEndurance > baseAttributeStat) allocatedEndurance--
                            2 -> if (allocatedStrength > baseAttributeStat) allocatedStrength--
                            3 -> if (allocatedAgility > baseAttributeStat) allocatedAgility--
                            4 -> if (allocatedIntelligence > baseAttributeStat) allocatedIntelligence--
                        }
                        soundManager.playSwitch()
                        hapticManager.vibrateAttack()
                        return true
                    }
                }

                // 2. Check Plus Buttons [+]
                if (remainingPoints > 0) {
                    for (i in statPlusBtnRects.indices) {
                        if (statPlusBtnRects[i].contains(px, py)) {
                            when (i) {
                                0 -> if (allocatedVigor < 20) allocatedVigor++
                                1 -> if (allocatedEndurance < 20) allocatedEndurance++
                                2 -> if (allocatedStrength < 20) allocatedStrength++
                                3 -> if (allocatedAgility < 20) allocatedAgility++
                                4 -> if (allocatedIntelligence < 20) allocatedIntelligence++
                            }
                            soundManager.playSwitch()
                            hapticManager.vibrateAttack()
                            return true
                        }
                    }
                }

                // 3. Check Reset All Points Button
                if (statResetBtnRect.contains(px, py)) {
                    allocatedVigor = baseAttributeStat
                    allocatedEndurance = baseAttributeStat
                    allocatedStrength = baseAttributeStat
                    allocatedAgility = baseAttributeStat
                    allocatedIntelligence = baseAttributeStat
                    soundManager.playSwitch()
                    hapticManager.vibrateHeavyAttack()
                    return true
                }

                // 4. Check Awaken Button
                if (originAwakenBtnRect.contains(px, py)) {
                    val derivedOrigin = CharacterOrigins.deriveClass(
                        allocatedVigor,
                        allocatedEndurance,
                        allocatedStrength,
                        allocatedAgility,
                        allocatedIntelligence
                    )
                    player.applyOrigin(derivedOrigin)
                    isSelectingOrigin = false
                    soundManager.playCheckpoint()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.22f, 7f)
                    return true
                }
            }
            return true
        }

        // ==========================================
        // ARMORY & EQUIPMENT TOUCH HANDLING
        // ==========================================
        if (isViewingArmory) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val index = event.actionIndex
                val px = event.getX(index)
                val py = event.getY(index)

                val armors = player.inventoryArmors.ifEmpty { Armors.ALL_ARMORS }

                if (armoryCloseBtnRect.contains(px, py)) {
                    isViewingArmory = false
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                }

                if (armoryEquipBtnRect.contains(px, py)) {
                    if (selectedArmorIndex in armors.indices) {
                        player.equipArmor(armors[selectedArmorIndex])
                        soundManager.playCheckpoint()
                        hapticManager.vibrateHeavyAttack()
                        gameStatusText = "✦ Equipped ${armors[selectedArmorIndex].name}! ✦"
                    }
                    return true
                }

                for (i in armorSlotRects.indices) {
                    if (i < armors.size && armorSlotRects[i].contains(px, py)) {
                        selectedArmorIndex = i
                        soundManager.playSwitch()
                        hapticManager.vibrateAttack()
                        return true
                    }
                }
            }
            return true
        }

        // ==========================================
        // PAUSE MODAL TOUCH HANDLING
        // ==========================================
        if (isPaused) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val index = event.actionIndex
                val px = event.getX(index)
                val py = event.getY(index)

                if (pauseContinueBtnRect.contains(px, py) || pauseCloseBtnRect.contains(px, py)) {
                    isPaused = false
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                } else if (pauseEquipmentBtnRect.contains(px, py)) {
                    isViewingArmory = true
                    selectedArmorIndex = player.inventoryArmors.indexOf(player.equippedArmor).coerceAtLeast(0)
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                } else if (pauseRestartBtnRect.contains(px, py)) {
                    player.reset(level.currentCheckpointX, level.currentCheckpointY)
                    level.resetBoxes()
                    projectiles.clear()
                    gameStatusText = "✦ RESPAWNED AT CHECKPOINT ✦"
                    wasAlive = true
                    wasGrounded = false
                    isPaused = false
                    soundManager.playCheckpoint()
                    hapticManager.vibrateHeavyAttack()
                    return true
                } else if (pauseRestartGameBtnRect.contains(px, py)) {
                    level.resetFullGame()
                    player.reset(level.spawnX, level.spawnY)
                    level.resetBoxes()
                    projectiles.clear()
                    particles.clear()
                    player.moveLeft = false
                    player.moveRight = false
                    player.isAttacking = false
                    player.isRolling = false
                    player.vx = 0f
                    player.vy = 0f
                    camera.x = 0f
                    camera.y = 0f
                    gameStatusText = "✦ CHOOSE YOUR ORIGIN ✦"
                    wasAlive = true
                    wasGrounded = false
                    isPaused = false
                    isRestingAtGrace = false
                    isSelectingOrigin = true
                    soundManager.playCheckpoint()
                    hapticManager.vibrateHeavyAttack()
                    renderer.triggerScreenShake(0.3f, 8f)
                    return true
                } else if (pauseSaveBtnRect.contains(px, py)) {
                    com.example.shadowescape.engine.SaveManager.saveGame(context, player, level, level.currentCheckpointX, level.currentCheckpointY)
                    soundManager.playCheckpoint()
                    hapticManager.vibrateCheckpoint()
                    gameStatusText = "✦ Soul Memory Saved! ✦"
                    return true
                } else if (pauseLoadBtnRect.contains(px, py)) {
                    val loadedSpawn = com.example.shadowescape.engine.SaveManager.loadGame(context, player, level)
                    if (loadedSpawn != null) {
                        level.currentCheckpointX = loadedSpawn.first
                        level.currentCheckpointY = loadedSpawn.second
                        isPaused = false
                        isSelectingOrigin = false
                        soundManager.playCheckpoint()
                        hapticManager.vibrateCheckpoint()
                        gameStatusText = "✦ Soul Memory Restored (Lv ${player.stats.level})! ✦"
                    } else {
                        soundManager.playSwitch()
                        gameStatusText = "No Soul Memory found yet."
                    }
                    return true
                } else {
                    // Tap outside modal box resumes
                    isPaused = false
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                }
            }
            return true
        }

        // ==========================================
        // SITE OF GRACE MODAL TOUCH HANDLING
        // ==========================================
        if (isRestingAtGrace) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val index = event.actionIndex
                val px = event.getX(index)
                val py = event.getY(index)

                if (graceRestBtnRect.contains(px, py)) {
                    level.restAtGrace(player)
                    com.example.shadowescape.engine.SaveManager.saveGame(context, player, level, level.currentCheckpointX, level.currentCheckpointY)
                    soundManager.playFlaskDrink()
                    hapticManager.vibrateCheckpoint()
                    gameStatusText = "✦ RESTED AT GRACE • PROGRESS SAVED ✦"
                    return true
                }

                if (graceBloodPactRect.contains(px, py)) {
                    player.stats.bloodPactActive = !player.stats.bloodPactActive
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    gameStatusText = if (player.stats.bloodPactActive) {
                        "🩸 BLOOD PACT SEALED: +35% Enemy DMG, +60% Essence!"
                    } else {
                        "🩸 Blood Pact Dispelled."
                    }
                    return true
                }

                if (graceAsceticPactRect.contains(px, py)) {
                    player.stats.asceticPactActive = !player.stats.asceticPactActive
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    gameStatusText = if (player.stats.asceticPactActive) {
                        "🕯 ASCETIC PACT SEALED: Flasks Disabled, -50% Skill FP!"
                    } else {
                        "🕯 Ascetic Pact Dispelled."
                    }
                    return true
                }

                val attrs = arrayOf("VIGOR", "ENDURANCE", "STRENGTH", "AGILITY", "INTELLIGENCE")
                for (i in graceAttrBtnRects.indices) {
                    if (graceAttrBtnRects[i].contains(px, py)) {
                        if (player.stats.levelUp(attrs[i])) {
                            soundManager.playCheckpoint()
                            hapticManager.vibrateHeavyAttack()
                            gameStatusText = "✦ LV ${player.stats.level} • ${attrs[i]} INCREASED ✦"
                        } else {
                            soundManager.playSwitch()
                            gameStatusText = "Insufficient Essence (Need ${player.stats.costForNextLevel()})"
                        }
                        return true
                    }
                }

                if (graceLeaveBtnRect.contains(px, py) || graceCloseBtnRect.contains(px, py)) {
                    isRestingAtGrace = false
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                } else {
                    // Tap outside modal box stands up from Grace
                    isRestingAtGrace = false
                    soundManager.playSwitch()
                    hapticManager.vibrateAttack()
                    return true
                }
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                // Check Pause Button
                if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    val index = event.actionIndex
                    val px = event.getX(index)
                    val py = event.getY(index)
                    if (pauseBtnRect.contains(px, py)) {
                        isPaused = true
                        player.moveLeft = false
                        player.moveRight = false
                        soundManager.playSwitch()
                        hapticManager.vibrateAttack()
                        return true
                    }
                    if (renderer.armorBadgeRect.contains(px, py)) {
                        isViewingArmory = true
                        selectedArmorIndex = player.inventoryArmors.indexOf(player.equippedArmor).coerceAtLeast(0)
                        player.moveLeft = false
                        player.moveRight = false
                        soundManager.playSwitch()
                        hapticManager.vibrateAttack()
                        return true
                    }
                }

                if (!player.isAlive || player.hasWon) {
                    player.reset(level.currentCheckpointX, level.currentCheckpointY)
                    level.resetBoxes()
                    projectiles.clear()
                    gameStatusText = "Shadow Escape"
                    wasAlive = true
                    wasGrounded = false
                    return true
                }

                var pressingLeft = false
                var pressingRight = false

                for (i in 0 until event.pointerCount) {
                    val px = event.getX(i)
                    val py = event.getY(i)

                    if (leftBtnRect.contains(px, py)) {
                        pressingLeft = true
                    } else if (rightBtnRect.contains(px, py)) {
                        pressingRight = true
                    } else if (attackBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            val pcx = player.bounds.centerX
                            val pcy = player.bounds.centerY
                            // 1. Check for Visceral Critical Strike on any staggered enemy in close range!
                            val staggeredEnemy = level.enemies.firstOrNull { enemy ->
                                enemy.isAlive && enemy.isStaggered &&
                                kotlin.math.abs(pcx - enemy.bounds.centerX) < 95f &&
                                kotlin.math.abs(pcy - enemy.bounds.centerY) < 80f
                            }

                            if (staggeredEnemy != null) {
                                val rawDmg = player.equippedWeapon.calculateDamage(player.stats)
                                val critDmg = rawDmg * 3.8f
                                val killed = staggeredEnemy.takeDamage(critDmg, 50f)
                                staggeredEnemy.isStaggered = false

                                player.facingRight = (staggeredEnemy.bounds.centerX > pcx)
                                player.vx = if (player.facingRight) 220f else -220f

                                soundManager.playVisceralStrike()
                                hapticManager.vibrateVisceralStrike()
                                renderer.triggerScreenShake(0.35f, 14f)
                                renderer.triggerVisceralEffect(staggeredEnemy.bounds.centerX, staggeredEnemy.bounds.centerY)

                                if (killed) {
                                    val bonus = (staggeredEnemy.essenceReward * 1.6f).toInt()
                                    player.stats.addEssence(bonus)
                                    gameStatusText = "✦ VISCERAL EXECUTION • +$bonus Essence! ✦"
                                    if (staggeredEnemy.type == EnemyType.ELITE_GUARDIAN) {
                                        level.onLootDiscovered?.invoke(
                                            LootReward(
                                                title = "Crimson Vein Katana",
                                                subtitle = "Cursed Katana • Legendary",
                                                description = "Felled from the Elite Guardian. Bathed in scarlet essence with unrivaled flurry slicing.",
                                                rarity = ItemRarity.LEGENDARY,
                                                weapon = Weapons.CRIMSON_VEIN_KATANA
                                            )
                                        )
                                    }
                                } else {
                                    gameStatusText = "✦ VISCERAL CRITICAL STRIKE • ${critDmg.toInt()} DMG! ✦"
                                }
                            } else {
                                val didAttack = player.attack { newProj ->
                                    projectiles.add(newProj)
                                }
                                if (didAttack) {
                                    when (player.equippedWeapon.type) {
                                        WeaponType.KATANA -> {
                                            when (player.attackComboIndex) {
                                                1 -> {
                                                    soundManager.playSlash(1.0f)
                                                    hapticManager.vibrateAttack()
                                                }
                                                2 -> {
                                                    soundManager.playSlash(1.18f)
                                                    hapticManager.vibrateAttack()
                                                }
                                                else -> {
                                                    soundManager.playHeavySwing()
                                                    hapticManager.vibrateHeavyAttack()
                                                    renderer.triggerScreenShake(0.18f, 7f)
                                                }
                                            }
                                        }
                                        WeaponType.SWORD -> {
                                            when (player.attackComboIndex) {
                                                1 -> soundManager.playSlash(1.05f)
                                                2 -> soundManager.playSlash(1.22f)
                                                else -> {
                                                    soundManager.playHeavySwing()
                                                    renderer.triggerScreenShake(0.16f, 6f)
                                                }
                                            }
                                            hapticManager.vibrateAttack()
                                        }
                                        WeaponType.GREATSWORD -> {
                                            if (player.attackComboIndex == 1) {
                                                soundManager.playHeavySwing(0.95f)
                                                hapticManager.vibrateHeavyAttack()
                                                renderer.triggerScreenShake(0.26f, 10f)
                                            } else {
                                                soundManager.playHeavySwing(0.82f)
                                                hapticManager.vibrateHeavyAttack()
                                                renderer.triggerScreenShake(0.30f, 12f)
                                            }
                                        }
                                        WeaponType.CLUB -> {
                                            if (player.attackComboIndex == 1) {
                                                soundManager.playHeavySwing(1.05f)
                                                renderer.triggerScreenShake(0.18f, 7f)
                                            } else {
                                                soundManager.playHeavySwing(0.90f)
                                                renderer.triggerScreenShake(0.24f, 9f)
                                            }
                                            hapticManager.vibrateHeavyAttack()
                                        }
                                        WeaponType.STAFF -> {
                                            soundManager.playSpellCast()
                                            hapticManager.vibrateAttack()
                                        }
                                        WeaponType.BOW -> {
                                            soundManager.playSlash(1.45f)
                                            hapticManager.vibrateAttack()
                                        }
                                        WeaponType.SCYTHE -> {
                                            soundManager.playHeavySwing(1.05f)
                                            hapticManager.vibrateHeavyAttack()
                                            renderer.triggerScreenShake(0.16f, 6f)
                                        }
                                        WeaponType.GAUNTLET -> {
                                            soundManager.playSlash(1.25f)
                                            hapticManager.vibrateAttack()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (rollBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (player.roll()) {
                                soundManager.playDodgeRoll()
                                hapticManager.vibrateDodgeRoll()
                            }
                        }
                    } else if (parryBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (player.parry()) {
                                soundManager.playSlash(1.35f)
                                hapticManager.vibrateAttack()
                            }
                        }
                    } else if (jumpBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (player.isGrounded && !player.isRolling) {
                                soundManager.playJump()
                                hapticManager.vibrateJump()
                            }
                            player.pressJump()
                        }
                    } else if (level.hasNearInteractable(player) && useBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            level.checkInteractions(player) { biteDamage ->
                                player.takeDamage(biteDamage)
                                soundManager.playMimicGrowl()
                                soundManager.playEnemyHit()
                                hapticManager.vibrateVisceralStrike()
                                renderer.triggerScreenShake(0.35f, 14f)
                            }
                        }
                    } else if (skillBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            val didSkill = player.performWeaponSkill { newProj ->
                                projectiles.add(newProj)
                            }
                            if (didSkill) {
                                soundManager.playWeaponSkill()
                                hapticManager.vibrateHeavyAttack()
                                renderer.triggerScreenShake(0.24f, 8f)
                                gameStatusText = "✦ Weapon Art: ${player.equippedWeapon.skillName}! ✦"
                            }
                        }
                    } else if (diveBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (!player.isGrounded) {
                                if (player.triggerGroundPound()) {
                                    soundManager.playHeavySwing(1.2f)
                                    hapticManager.vibrateAttack()
                                }
                            } else {
                                player.dropThrough()
                                hapticManager.vibrateAttack()
                            }
                        }
                    } else if (crimsonFlaskBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (player.drinkFlask(isCrimson = true)) {
                                soundManager.playFlaskDrink()
                                hapticManager.vibrateCheckpoint()
                            }
                        }
                    } else if (ceruleanFlaskBtnRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            if (player.drinkFlask(isCrimson = false)) {
                                soundManager.playFlaskDrink()
                                hapticManager.vibrateCheckpoint()
                            }
                        }
                    } else if (weaponCardRect.contains(px, py)) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            val didShadowFlow = player.switchWeapon { newProj ->
                                projectiles.add(newProj)
                            }
                            soundManager.playSwitch()
                            if (didShadowFlow) {
                                soundManager.playSlash(1.3f)
                                hapticManager.vibrateHeavyAttack()
                                renderer.triggerScreenShake(0.18f, 7f)
                                gameStatusText = "✦ SHADOW FLOW! Swap Strike with ${player.equippedWeapon.name}! ✦"
                            } else {
                                hapticManager.vibrateAttack()
                            }
                        }
                    }
                }

                player.moveLeft = pressingLeft
                player.moveRight = pressingRight
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val upIdx = event.actionIndex
                val upX = event.getX(upIdx)
                val upY = event.getY(upIdx)
                if (jumpBtnRect.contains(upX, upY)) {
                    player.releaseJump()
                }

                var pressingLeft = false
                var pressingRight = false

                for (i in 0 until event.pointerCount) {
                    if (i == event.actionIndex && (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP)) {
                        continue
                    }
                    val px = event.getX(i)
                    val py = event.getY(i)

                    if (leftBtnRect.contains(px, py)) {
                        pressingLeft = true
                    } else if (rightBtnRect.contains(px, py)) {
                        pressingRight = true
                    }
                }

                player.moveLeft = pressingLeft
                player.moveRight = pressingRight
            }
        }
        return true
    }
}
