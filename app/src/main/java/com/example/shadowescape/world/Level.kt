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

        // Optional side-route platforms to Treasure Chest 1 (not required for progression)
        platforms.add(Platform(4, AABB(460f, 740f, 180f, 24f), PlatformType.ONE_WAY))
        platforms.add(Platform(5, AABB(720f, 610f, 220f, 24f), PlatformType.ONE_WAY))

        // --- ZONE 2: Sunken Sewer Machinery (1380 - 2750) — Continuous Combat Arena ---
        platforms.add(Platform(2, AABB(1380f, 880f, 1370f, 320f), PlatformType.NORMAL))

        // First Site of Grace (Sword-in-Stone, well-spaced in mid-crypt)
        platforms.add(Platform(101, AABB(1620f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false))

        // Optional elevated watchtower for Mimic Miniboss & Chest 3
        platforms.add(Platform(22, AABB(2120f, 710f, 260f, 24f), PlatformType.ONE_WAY))

        // --- ZONE 3: Crimson Abyss Sanctum (2750 - 4200) — Continuous Boss Corridor ---
        platforms.add(Platform(30, AABB(2750f, 880f, 1450f, 320f), PlatformType.NORMAL))

        // Second Site of Grace before the Crimson Sanctum Guardian
        platforms.add(Platform(102, AABB(3280f, 810f, 40f, 70f), PlatformType.CHECKPOINT, false))

        // Optional high alcove for Chest 4 (Legendary Greatsword)
        platforms.add(Platform(33, AABB(3640f, 720f, 220f, 24f), PlatformType.ONE_WAY))

        // Endless Story Portal / Obelisk of the Abyss
        platforms.add(Platform(99, AABB(3960f, 780f, 60f, 100f), PlatformType.EXIT))

        // Ceiling bounds
        platforms.add(Platform(50, AABB(0f, 0f, width, 40f), PlatformType.NORMAL))

        // --- ENEMIES (Combat-focused progression across all 4 monster species) ---
        enemies.clear()
        enemySpawns.clear()
        val e1 = Enemy(1, 780f, 820f, EnemyType.GOBLIN, patrolMinX = 600f, patrolMaxX = 980f)
        val e2 = Enemy(2, 1140f, 810f, EnemyType.SKELETON, patrolMinX = 1000f, patrolMaxX = 1320f)
        val e3 = Enemy(3, 1760f, 810f, EnemyType.SKELETON, patrolMinX = 1580f, patrolMaxX = 1940f)
        val e4 = Enemy(4, 2180f, 720f, EnemyType.FLYING_EYE, patrolMinX = 2000f, patrolMaxX = 2460f)
        val e5 = Enemy(5, 2520f, 820f, EnemyType.GOBLIN, patrolMinX = 2360f, patrolMaxX = 2700f)
        val e6 = Enemy(6, 3060f, 820f, EnemyType.MUSHROOM, patrolMinX = 2900f, patrolMaxX = 3240f)
        val e7 = Enemy(7, 3480f, 795f, EnemyType.ELITE_GUARDIAN, patrolMinX = 3300f, patrolMaxX = 3760f)
        val e8 = Enemy(8, 960f, 846f, EnemyType.SLIME, patrolMinX = 820f, patrolMaxX = 1120f)
        val e9 = Enemy(9, 2040f, 846f, EnemyType.SLIME, patrolMinX = 1880f, patrolMaxX = 2240f)
        val e10 = Enemy(10, 2900f, 846f, EnemyType.SLIME, patrolMinX = 2760f, patrolMaxX = 3100f)
        val e11 = Enemy(11, 3920f, 795f, EnemyType.ELITE_GUARDIAN, patrolMinX = 3760f, patrolMaxX = 4120f).apply {
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

        enemies.addAll(listOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11))
        enemySpawns.add(Triple(e1, 780f, 820f))
        enemySpawns.add(Triple(e2, 1140f, 810f))
        enemySpawns.add(Triple(e3, 1760f, 810f))
        enemySpawns.add(Triple(e4, 2180f, 720f))
        enemySpawns.add(Triple(e5, 2520f, 820f))
        enemySpawns.add(Triple(e6, 3060f, 820f))
        enemySpawns.add(Triple(e7, 3480f, 795f))
        enemySpawns.add(Triple(e8, 960f, 846f))
        enemySpawns.add(Triple(e9, 2040f, 846f))
        enemySpawns.add(Triple(e10, 2900f, 846f))
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
