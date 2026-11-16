package com.example.shadowescape.world

import com.example.shadowescape.model.ChestState
import com.example.shadowescape.model.ChestType
import com.example.shadowescape.model.Enemy
import com.example.shadowescape.model.EnemyType
import com.example.shadowescape.model.ItemRarity
import com.example.shadowescape.model.LootReward
import com.example.shadowescape.model.Platform
import com.example.shadowescape.model.PlatformType
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.PushableBox
import com.example.shadowescape.model.TreasureChest
import com.example.shadowescape.model.Weapons
import com.example.shadowescape.physics.AABB
import kotlin.math.abs

class Level {
    val width: Float = 4200f
    val height: Float = 1200f

    val spawnX: Float = 360f
    val spawnY: Float = 750f

    var currentCheckpointX: Float = spawnX
    var currentCheckpointY: Float = spawnY

    var checkpointNotification: String? = null
    var checkpointNotificationTimer: Float = 0f

    // Souls Bloodstain / Lost Essence Recovery
    var hasBloodstain: Boolean = false
    var bloodstainX: Float = 0f
    var bloodstainY: Float = 0f
    var bloodstainEssence: Int = 0
    var bloodstainAnimTimer: Float = 0f

    var onCheckpointReached: (() -> Unit)? = null
    var onGraceOpened: (() -> Unit)? = null
    var onGraceMimicAwakened: (() -> Unit)? = null
    var onSwitchToggled: (() -> Unit)? = null
    var onDoorStateChanged: (() -> Unit)? = null
    var onLootDiscovered: ((LootReward) -> Unit)? = null
    var onBloodstainReclaimed: ((Int) -> Unit)? = null
    var onMimicTrapSprung: (() -> Unit)? = null
    var onHiddenBossAwakened: ((Enemy) -> Unit)? = null

    val platforms: MutableList<Platform> = mutableListOf()
    val boxes: MutableList<PushableBox> = mutableListOf()
    val enemies: MutableList<Enemy> = mutableListOf()
    val chests: MutableList<TreasureChest> = mutableListOf()
    private val enemySpawns: MutableList<Triple<Enemy, Float, Float>> = mutableListOf()

    init {
        buildWorld()
    }

    fun buildWorld() {
        platforms.clear()
        boxes.clear()

        // --- ZONE 1: Dark Slate Dungeon (0 - 1380) — Main Combat Floor ---
        platforms.add(Platform(1, AABB(0f, 880f, 1380f, 320f), PlatformType.NORMAL))

        // Site of Grace 1: Ashen Sepulcher Threshold Sanctuary (Safe starting sanctuary)
        platforms.add(Platform(100, AABB(420f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false, isGraceMimic = false))

        // Optional side-route platforms to Treasure Chest 1 (not required for progression)
        platforms.add(Platform(4, AABB(460f, 740f, 180f, 24f), PlatformType.ONE_WAY))
        platforms.add(Platform(5, AABB(720f, 610f, 220f, 24f), PlatformType.ONE_WAY))

        // --- ZONE 2: Sunken Sewer Machinery (1380 - 2750) — Continuous Combat Arena ---
        platforms.add(Platform(2, AABB(1380f, 880f, 1370f, 320f), PlatformType.NORMAL))

        // Site of Grace 2: Glintstone Crypt Cavern Entrance (Safe sanctuary)
        platforms.add(Platform(101, AABB(1480f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false, isGraceMimic = false))

        // Optional elevated watchtower for Mimic Miniboss & Chest 3
        platforms.add(Platform(22, AABB(2120f, 710f, 260f, 24f), PlatformType.ONE_WAY))

        // Site of Grace 3: The Deceitful Grace (GRACE MIMIC! Transforms when touched)
        platforms.add(Platform(103, AABB(2240f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false, isGraceMimic = true))

        // --- ZONE 3: Crimson Abyss Sanctum (2750 - 4200) — Continuous Boss Corridor ---
        platforms.add(Platform(30, AABB(2750f, 880f, 1450f, 320f), PlatformType.NORMAL))

        // Site of Grace 4: Crucible Bastion Sanctum (Safe sanctuary before Lord Malakor)
        platforms.add(Platform(102, AABB(3300f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false, isGraceMimic = false))

        // Optional high alcove for Chest 4 (Legendary Greatsword)
        platforms.add(Platform(33, AABB(3640f, 720f, 220f, 24f), PlatformType.ONE_WAY))

        // Endless Story Portal / Obelisk of the Abyss
        platforms.add(Platform(99, AABB(3960f, 780f, 60f, 100f), PlatformType.EXIT))

        // Ceiling bounds
        platforms.add(Platform(50, AABB(0f, 0f, width, 40f), PlatformType.NORMAL))

        // --- ENEMIES (Sanctuary-buffered spawns: strictly no enemies near Sites of Grace) ---
        enemies.clear()
        enemySpawns.clear()
        val e1 = Enemy(1, 800f, 820f, EnemyType.GOBLIN, patrolMinX = 720f, patrolMaxX = 980f)
        val e8 = Enemy(8, 980f, 846f, EnemyType.SLIME, patrolMinX = 860f, patrolMaxX = 1120f)
        val e2 = Enemy(2, 1240f, 810f, EnemyType.SKELETON, patrolMinX = 1140f, patrolMaxX = 1350f)
        val e3 = Enemy(3, 1820f, 810f, EnemyType.SKELETON, patrolMinX = 1720f, patrolMaxX = 1960f)
        val e9 = Enemy(9, 2040f, 846f, EnemyType.SLIME, patrolMinX = 1940f, patrolMaxX = 2140f)
        val e4 = Enemy(4, 2420f, 720f, EnemyType.FLYING_EYE, patrolMinX = 2340f, patrolMaxX = 2560f)
        val e5 = Enemy(5, 2640f, 820f, EnemyType.GOBLIN, patrolMinX = 2560f, patrolMaxX = 2740f)
        val e10 = Enemy(10, 2920f, 846f, EnemyType.SLIME, patrolMinX = 2820f, patrolMaxX = 3060f)
        val e6 = Enemy(6, 3080f, 820f, EnemyType.MUSHROOM, patrolMinX = 2960f, patrolMaxX = 3140f)
        val e7 = Enemy(7, 3560f, 795f, EnemyType.ELITE_GUARDIAN, patrolMinX = 3480f, patrolMaxX = 3740f)
        val e11 = Enemy(11, 3920f, 795f, EnemyType.ELITE_GUARDIAN, patrolMinX = 3780f, patrolMaxX = 4120f).apply {
            isZoneBoss = true
            bossTitle = "MALAKOR, CRIMSON ABYSS LORD"
            maxHp = 520f
            currentHp = 520f
            maxPoise = 120f
            currentPoise = 120f
            attackDamage = 34f
            essenceReward = 800
            moveSpeed = 120f
            onZoneBossPhase2 = {
                checkpointNotification = "⚠ MALAKOR ENTERS PHASE II: ABYSSAL FURY! ⚠"
                checkpointNotificationTimer = 3.5f
            }
        }

        enemies.addAll(listOf(e1, e8, e2, e3, e9, e4, e5, e10, e6, e7, e11))
        enemySpawns.add(Triple(e1, 800f, 820f))
        enemySpawns.add(Triple(e8, 980f, 846f))
        enemySpawns.add(Triple(e2, 1240f, 810f))
        enemySpawns.add(Triple(e3, 1820f, 810f))
        enemySpawns.add(Triple(e9, 2040f, 846f))
        enemySpawns.add(Triple(e4, 2420f, 720f))
        enemySpawns.add(Triple(e5, 2640f, 820f))
        enemySpawns.add(Triple(e10, 2920f, 846f))
        enemySpawns.add(Triple(e6, 3080f, 820f))
        enemySpawns.add(Triple(e7, 3560f, 795f))
        enemySpawns.add(Triple(e11, 3920f, 795f))
        assignRandomHiddenSlimeBoss()

        // --- SOULS TREASURE CHESTS ---
        chests.clear()
        // Chest 1: Optional side-platform Wooden Chest in Zone 1
        chests.add(
            TreasureChest(
                id = 1,
                x = 810f,
                y = 572f,
                type = ChestType.WOODEN,
                loot = LootReward(
                    title = "Nightprowler Talon",
                    subtitle = "Curved Greatsword • Rare",
                    description = "Obsidian talon carved into a deadly curved greatsword with high Agility scaling.",
                    rarity = ItemRarity.RARE,
                    weapon = Weapons.NIGHTPROWLER_TALON
                )
            )
        )
        // Chest 2: Iron Chest along Zone 2 main path
        chests.add(
            TreasureChest(
                id = 2,
                x = 1960f,
                y = 832f,
                type = ChestType.IRON,
                loot = LootReward(
                    title = "Primordial Seed",
                    subtitle = "Ancient Relic • Epic",
                    description = "A luminous seed from the primordial world-tree. Permanently increases flask capacity by +1.",
                    rarity = ItemRarity.EPIC,
                    isPrimordialSeed = true
                )
            )
        )
        // Chest 3: Optional Mimic Miniboss Chest on Zone 2 Watchtower
        chests.add(
            TreasureChest(
                id = 3,
                x = 2230f,
                y = 672f,
                type = ChestType.IRON,
                loot = LootReward(
                    title = "Wyrm Wing Halberd",
                    subtitle = "Ancient Relic • Epic",
                    description = "Carved from an ancient wyrm horn. High poise cleaves and sweeping dragon thrusts.",
                    rarity = ItemRarity.EPIC,
                    weapon = Weapons.WYRM_WING_HALBERD,
                    essenceBonus = 150
                ),
                isMimic = true
            )
        )
        // Chest 4: Optional Gilded Chest on Zone 3 balcony
        chests.add(
            TreasureChest(
                id = 4,
                x = 3730f,
                y = 682f,
                type = ChestType.GILDED,
                loot = LootReward(
                    title = "Pale Eclipse Greatsword",
                    subtitle = "Legendary Greatsword • Legendary",
                    description = "Consecrated moonlight blade radiating celestial frost sorcery. Devastating poise breaker.",
                    rarity = ItemRarity.LEGENDARY,
                    weapon = Weapons.PALE_ECLIPSE_GREATSWORD
                )
            )
        )
    }

    fun dropBloodstain(x: Float, y: Float, essence: Int) {
        bloodstainX = x
        bloodstainY = y
        bloodstainEssence = essence
        hasBloodstain = (essence > 0)
        bloodstainAnimTimer = 0f

        enemies.removeAll { it.isShadowEcho }
        if (essence >= 100) {
            val bonusEssence = (essence * 1.2f).toInt()
            val echo = Enemy(
                id = 999,
                initialX = (x - 18f).coerceAtLeast(60f),
                initialY = (y - 60f).coerceAtLeast(100f),
                type = EnemyType.SKELETON,
                patrolMinX = (x - 140f).coerceAtLeast(40f),
                patrolMaxX = (x + 140f).coerceAtMost(width - 40f)
            ).apply {
                isShadowEcho = true
                bossTitle = "YOUR SHADOW ECHO"
                shadowEchoEssence = bonusEssence
                essenceReward = bonusEssence
                maxHp = 135f
                currentHp = 135f
                attackDamage = 24f
            }
            enemies.add(echo)
        }
    }

    fun clearBloodstain() {
        hasBloodstain = false
        bloodstainEssence = 0
        enemies.removeAll { it.isShadowEcho }
    }

    fun respawnEnemies() {
        enemies.removeAll { it.isShadowEcho }
        // Reset undefeated Grace Mimics back into disguised dormant state
        for (platform in platforms) {
            if (platform.isGraceMimic && !platform.isMimicDefeated) {
                platform.isMimicAwakened = false
                enemies.removeAll { it.isGraceMimic && it.graceMimicPlatformId == platform.id }
            }
        }
        for (spawn in enemySpawns) {
            val (enemy, sx, sy) = spawn
            enemy.reset(sx, sy)
        }
        for (chest in chests) {
            if (chest.isMimic && !chest.isDefeated) {
                chest.reset()
            }
        }
        assignRandomHiddenSlimeBoss()
    }

    private fun assignRandomHiddenSlimeBoss() {
        val slimes = enemies.filter { it.type == EnemyType.SLIME }
        if (slimes.isNotEmpty()) {
            val chosen = slimes.random()
            for (slime in slimes) {
                slime.canTransformToDemonBoss = (slime === chosen)
                slime.onDemonBossTransformed = {
                    checkpointNotification = "⚠ HIDDEN BOSS AWAKENED: DEMON SLIME ⚠"
                    checkpointNotificationTimer = 3.5f
                    onHiddenBossAwakened?.invoke(slime)
                }
            }
        }

        val vesselCandidates = enemies.filter {
            !it.isZoneBoss && !it.isShadowEcho &&
                (it.type == EnemyType.GOBLIN || it.type == EnemyType.SKELETON || it.type == EnemyType.MUSHROOM)
        }
        val chosenVessel = vesselCandidates.randomOrNull()
        for (mob in vesselCandidates) {
            mob.isShadowVessel = (mob === chosenVessel)
            mob.onShadowVesselAwakened = {
                checkpointNotification = "⚠ SHADOW VESSEL AWAKENED! ⚠"
                checkpointNotificationTimer = 3.2f
            }
        }
    }

    fun update(
        dt: Float,
        player: Player,
        onEnemyAttackPlayer: ((Float, Enemy?) -> Unit)? = null,
        onEnemyStaggered: (() -> Unit)? = null
    ) {
        // Update boxes
        for (box in boxes) {
            box.update(dt, platforms)
        }

        // Update chests & mimic encounters
        for (chest in chests) {
            chest.update(dt, player, platforms) { dmg -> onEnemyAttackPlayer?.invoke(dmg, null) }
        }

        // Check Bloodstain recovery (if no living Shadow Echo guards it)
        val echoAlive = enemies.any { it.isShadowEcho && it.isAlive }
        if (hasBloodstain && !echoAlive && player.isAlive) {
            bloodstainAnimTimer += dt
            val distH = abs(player.bounds.centerX - bloodstainX)
            val distV = abs((player.bounds.bottom - 10f) - bloodstainY)
            if (distH < 135f && distV < 140f) {
                val reclaimed = bloodstainEssence
                player.stats.addEssence(reclaimed)
                clearBloodstain()
                onBloodstainReclaimed?.invoke(reclaimed)
            }
        }

        // Update active enemies
        for (enemy in enemies) {
            enemy.update(dt, player, platforms, onEnemyAttackPlayer, onEnemyStaggered)
        }

        // Check if any active Grace Mimic was defeated
        for (platform in platforms) {
            if (platform.isGraceMimic && platform.isMimicAwakened && !platform.isMimicDefeated) {
                val mimic = enemies.firstOrNull { it.isGraceMimic && it.graceMimicPlatformId == platform.id }
                if (mimic != null && !mimic.isAlive) {
                    platform.isMimicDefeated = true
                    platform.isActivated = true
                    currentCheckpointX = platform.bounds.x
                    currentCheckpointY = platform.bounds.top - player.height
                    checkpointNotification = "✦ SITE OF GRACE PURIFIED & CONSECRATED ✦"
                    checkpointNotificationTimer = 4.0f
                    onCheckpointReached?.invoke()
                }
            }
        }

        // Handle Checkpoint notification fade
        if (checkpointNotificationTimer > 0f) {
            checkpointNotificationTimer -= dt
            if (checkpointNotificationTimer <= 0f) {
                checkpointNotification = null
            }
        }

        // Update Pressure Plates & Dynamic Doors
        checkPressurePlates(player)

        // Check if player passed Checkpoints
        checkCheckpoints(player)
    }

    private fun checkPressurePlates(player: Player) {
        for (platform in platforms) {
            if (platform.type == PlatformType.PRESSURE_PLATE) {
                var pressed = false

                // Check if player stands on the plate
                if (player.bounds.intersects(platform.bounds) ||
                    (abs(player.bounds.bottom - platform.bounds.top) < 8f &&
                     player.bounds.right > platform.bounds.left &&
                     player.bounds.left < platform.bounds.right)
                ) {
                    pressed = true
                }

                // Check if any box rests on the plate
                for (box in boxes) {
                    if (box.bounds.intersects(platform.bounds) ||
                        (abs(box.bounds.bottom - platform.bounds.top) < 8f &&
                         box.bounds.right > platform.bounds.left &&
                         box.bounds.left < platform.bounds.right)
                    ) {
                        pressed = true
                        break
                    }
                }

                val wasPressed = platform.isActivated
                platform.isActivated = pressed
                if (wasPressed != pressed) {
                    onDoorStateChanged?.invoke()
                }

                // Pressure Plate 20 controls Door 21
                if (platform.id == 20) {
                    for (door in platforms) {
                        if (door.id == 21) {
                            door.isActivated = pressed
                        }
                    }
                }
            }
        }
    }

    private fun checkCheckpoints(player: Player) {
        for (platform in platforms) {
            if (platform.type == PlatformType.CHECKPOINT) {
                // Deceitful Grace Mimics cannot be registered as checkpoints until purified
                if (platform.isGraceMimic && !platform.isMimicDefeated) {
                    continue
                }
                if (player.bounds.intersects(platform.bounds) ||
                    abs((player.bounds.x + player.width / 2f) - (platform.bounds.x + platform.bounds.width / 2f)) < 40f
                ) {
                    if (!platform.isActivated) {
                        platform.isActivated = true
                        currentCheckpointX = platform.bounds.x
                        currentCheckpointY = platform.bounds.top - player.height
                        checkpointNotification = "✦ SITE OF GRACE DISCOVERED ✦"
                        checkpointNotificationTimer = 3.2f
                        onCheckpointReached?.invoke()
                    }
                }
            }
        }
    }

    fun awakenGraceMimic(platform: Platform, player: Player, onPlayerDamaged: ((Float) -> Unit)? = null) {
        if (platform.isMimicAwakened) return
        platform.isMimicAwakened = true

        val cx = platform.bounds.centerX
        val cy = platform.bounds.bottom - 78f
        val phantom = Enemy(
            id = 200 + platform.id,
            initialX = cx - 25f,
            initialY = cy,
            type = EnemyType.GRACE_MIMIC,
            patrolMinX = (cx - 160f).coerceAtLeast(60f),
            patrolMaxX = (cx + 160f).coerceAtMost(width - 60f)
        ).apply {
            isGraceMimic = true
            graceMimicPlatformId = platform.id
            isZoneBoss = true
            bossTitle = "DECEITFUL GRACE PHANTOM"
            vy = -260f
            isAttacking = true
            attackTimer = 0.28f
            attackCooldown = 0.9f
        }

        enemies.add(phantom)
        onPlayerDamaged?.invoke(16f) // Surprise burst shockwave
        onGraceMimicAwakened?.invoke()
        checkpointNotification = "⚠ DECEITFUL GRACE AWAKENED! ⚠"
        checkpointNotificationTimer = 3.6f
    }

    fun checkInteractions(player: Player, onPlayerDamaged: ((Float) -> Unit)? = null) {
        // Check Chest interactions
        for (chest in chests) {
            if (chest.state == ChestState.CLOSED) {
                val dist = abs((player.bounds.x + player.width / 2f) - (chest.bounds.x + chest.bounds.width / 2f))
                if (dist < 70f && abs(player.bounds.y - chest.bounds.y) < 80f) {
                    if (chest.isMimic) {
                        chest.awaken(surpriseBite = true, player = player, onPlayerDamaged = onPlayerDamaged)
                        onMimicTrapSprung?.invoke()
                        return
                    } else if (chest.open()) {
                        onLootDiscovered?.invoke(chest.loot)
                        return
                    }
                }
            }
        }

        for (platform in platforms) {
            if (platform.type == PlatformType.CHECKPOINT) {
                val dist = abs((player.bounds.x + player.width / 2f) - (platform.bounds.x + platform.bounds.width / 2f))
                if (dist < 65f && abs(player.bounds.y - platform.bounds.y) < 80f) {
                    if (platform.isGraceMimic && !platform.isMimicDefeated) {
                        awakenGraceMimic(platform, player, onPlayerDamaged)
                        return
                    }
                    if (!platform.isActivated) {
                        platform.isActivated = true
                        currentCheckpointX = platform.bounds.x
                        currentCheckpointY = platform.bounds.top - player.height
                    }
                    onGraceOpened?.invoke()
                    return
                }
            }

            if (platform.type == PlatformType.SWITCH) {
                val dist = abs((player.bounds.x + player.width / 2f) - (platform.bounds.x + platform.bounds.width / 2f))
                if (dist < 70f && abs(player.bounds.y - platform.bounds.y) < 90f) {
                    platform.isActivated = !platform.isActivated
                    onSwitchToggled?.invoke()
                    // Switch 10 controls Door 11
                    for (p in platforms) {
                        if (p.id == 11) {
                            p.isActivated = platform.isActivated
                            onDoorStateChanged?.invoke()
                        }
                    }
                }
            }
        }
    }

    fun hasNearInteractable(player: Player): Boolean {
        val px = player.bounds.x + player.width / 2f
        val py = player.bounds.y

        for (chest in chests) {
            if (chest.state == ChestState.CLOSED) {
                val dist = abs(px - (chest.bounds.x + chest.bounds.width / 2f))
                if (dist < 70f && abs(py - chest.bounds.y) < 80f) return true
            }
        }

        for (platform in platforms) {
            if (platform.type == PlatformType.CHECKPOINT) {
                if (platform.isGraceMimic && platform.isMimicAwakened && !platform.isMimicDefeated) {
                    continue
                }
                val dist = abs(px - (platform.bounds.x + platform.bounds.width / 2f))
                if (dist < 65f && abs(py - platform.bounds.y) < 80f) return true
            }
            if (platform.type == PlatformType.SWITCH) {
                val dist = abs(px - (platform.bounds.x + platform.bounds.width / 2f))
                if (dist < 70f && abs(py - platform.bounds.y) < 90f) return true
            }
        }
        return false
    }

    fun restAtGrace(player: Player) {
        player.stats.resetToFull()
        respawnEnemies()
        checkpointNotification = "✦ Rested at Grace • Foes & Flasks Restored ✦"
        checkpointNotificationTimer = 3.0f
        onCheckpointReached?.invoke()
    }

    fun resetBoxes() {
        for (box in boxes) {
            if (box.id == 1) box.reset(1550f, 815f)
            if (box.id == 2) box.reset(2580f, 815f)
        }
    }

    fun resetFullGame() {
        buildWorld()
        currentCheckpointX = spawnX
        currentCheckpointY = spawnY
        checkpointNotification = null
        checkpointNotificationTimer = 0f
        hasBloodstain = false
        bloodstainEssence = 0
    }
}
