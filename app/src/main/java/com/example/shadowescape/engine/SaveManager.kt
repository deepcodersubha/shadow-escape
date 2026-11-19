package com.example.shadowescape.engine

import android.content.Context
import com.example.shadowescape.model.CharacterOrigins
import com.example.shadowescape.model.ChestState
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.Weapons
import com.example.shadowescape.world.Level

object SaveManager {
    private const val PREFS_NAME = "shadow_escape_soul_memory"

    fun hasSave(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("has_save", false)
    }

    fun saveGame(context: Context, player: Player, level: Level, spawnX: Float, spawnY: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val invIds = player.inventory.joinToString(",") { it.id }
        val activatedCheckpoints = level.platforms
            .mapIndexedNotNull { idx, p -> if (p.isActivated) idx.toString() else null }
            .joinToString(",")
        val openedChests = level.chests
            .filter { it.state == ChestState.OPENED || it.isDefeated }
            .joinToString(",") { it.id.toString() }

        prefs.edit().apply {
            putBoolean("has_save", true)
            putString("origin_id", player.currentOrigin.id)
            putInt("level", player.stats.level)
            putInt("vigor", player.stats.vigor)
            putInt("endurance", player.stats.endurance)
            putInt("strength", player.stats.strength)
            putInt("agility", player.stats.agility)
            putInt("intelligence", player.stats.intelligence)
            putInt("essence", player.stats.essence)
            putString("equipped_weapon", player.equippedWeapon.id)
            putString("inventory_weapons", invIds)
            putString("equipped_armor", player.equippedArmor.id)
            putBoolean("blood_pact", player.stats.bloodPactActive)
            putBoolean("ascetic_pact", player.stats.asceticPactActive)
            putFloat("spawn_x", spawnX)
            putFloat("spawn_y", spawnY)
            putFloat("player_x", player.bounds.x)
            putFloat("player_y", player.bounds.y)
            putString("activated_checkpoints", activatedCheckpoints)
            putString("opened_chests", openedChests)
            apply()
        }
    }

    fun loadGame(context: Context, player: Player, level: Level): Pair<Float, Float>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_save", false)) return null

        val vig = prefs.getInt("vigor", 10)
        val end = prefs.getInt("endurance", 10)
        val str = prefs.getInt("strength", 10)
        val agi = prefs.getInt("agility", 10)
        val intVal = prefs.getInt("intelligence", 10)
        val originId = prefs.getString("origin_id", "") ?: ""
        val origin = CharacterOrigins.ALL.find { it.id == originId } ?: CharacterOrigins.deriveClass(vig, end, str, agi, intVal)
        player.applyOrigin(origin)

        player.stats.level = prefs.getInt("level", origin.startingLevel)
        player.stats.vigor = vig
        player.stats.endurance = end
        player.stats.strength = str
        player.stats.agility = agi
        player.stats.intelligence = intVal
        player.stats.essence = prefs.getInt("essence", 150)
        player.stats.bloodPactActive = prefs.getBoolean("blood_pact", false)
        player.stats.asceticPactActive = prefs.getBoolean("ascetic_pact", false)
        player.stats.resetToFull()

        val invStr = prefs.getString("inventory_weapons", "") ?: ""
        if (invStr.isNotBlank()) {
            player.inventory.clear()
            for (wid in invStr.split(",")) {
                Weapons.ALL.find { it.id == wid }?.let { w ->
                    if (!player.inventory.contains(w)) player.inventory.add(w)
                }
            }
        }
        val eqId = prefs.getString("equipped_weapon", origin.startingWeapon.id)
        Weapons.ALL.find { it.id == eqId }?.let { w ->
            if (!player.inventory.contains(w)) player.inventory.add(w)
            player.equippedWeapon = w
        }

        val eqArmorId = prefs.getString("equipped_armor", "") ?: ""
        if (eqArmorId.isNotBlank()) {
            com.example.shadowescape.model.Armors.findById(eqArmorId)?.let { a ->
                player.equipArmor(a)
            }
        }

        val cpStr = prefs.getString("activated_checkpoints", "") ?: ""
        if (cpStr.isNotBlank()) {
            for (idxStr in cpStr.split(",")) {
                val idx = idxStr.toIntOrNull() ?: continue
                level.platforms.getOrNull(idx)?.isActivated = true
            }
        }

        val chestStr = prefs.getString("opened_chests", "") ?: ""
        if (chestStr.isNotBlank()) {
            val openedSet = chestStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            for (chest in level.chests) {
                if (chest.id in openedSet) {
                    chest.state = ChestState.OPENED
                    chest.openProgress = 1f
                    if (chest.isMimic) chest.isDefeated = true
                }
            }
        }

        val px = prefs.getFloat("player_x", 120f)
        val py = prefs.getFloat("player_y", 790f)
        player.bounds.setPosition(px, py)
        player.isAlive = true
        player.hasWon = false

        val sx = prefs.getFloat("spawn_x", 120f)
        val sy = prefs.getFloat("spawn_y", 790f)
        return Pair(sx, sy)
    }
}
