package com.example.shadowescape.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.IOException

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var lastFootstepTime: Long = 0L
    private var lastBoxPushTime: Long = 0L

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds(context)
    }

    private fun loadSounds(context: Context) {
        val soundFiles = listOf(
            "jump.wav",
            "land.wav",
            "footstep.wav",
            "switch.wav",
            "gate.wav",
            "checkpoint.wav",
            "death.wav",
            "box_push.wav",
            "slash.wav",
            "heavy_swing.wav",
            "spell_cast.wav",
            "dodge_roll.wav",
            "flask_drink.wav",
            "enemy_hit.wav",
            "chest_open.wav",
            "visceral_strike.wav",
            "bloodstain_reclaim.wav",
            "mimic_growl.wav",
            "stomp_impact.wav",
            "weapon_skill.wav"
        )

        val assets = context.assets
        for (file in soundFiles) {
            try {
                val fd = assets.openFd("audio/$file")
                val soundId = soundPool.load(fd, 1)
                soundMap[file] = soundId
            } catch (e: IOException) {
                // Ignore missing file
            }
        }
    }

    private fun play(fileName: String, volume: Float = 1.0f, rate: Float = 1.0f) {
        val soundId = soundMap[fileName] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, rate)
    }

    fun playJump() {
        play("jump.wav", 0.75f, 1.0f)
    }

    fun playLand() {
        play("land.wav", 0.85f, 1.0f)
    }

    fun playFootstep() {
        val now = System.currentTimeMillis()
        if (now - lastFootstepTime > 260) {
            lastFootstepTime = now
            val randomRate = 0.95f + (Math.random().toFloat() * 0.1f)
            play("footstep.wav", 0.35f, randomRate)
        }
    }

    fun playSwitch() {
        play("switch.wav", 0.85f, 1.0f)
    }

    fun playGate() {
        play("gate.wav", 0.9f, 1.0f)
    }

    fun playCheckpoint() {
        play("checkpoint.wav", 0.95f, 1.0f)
    }

    fun playDeath() {
        play("death.wav", 1.0f, 1.0f)
    }

    fun playBoxPush() {
        val now = System.currentTimeMillis()
        if (now - lastBoxPushTime > 220) {
            lastBoxPushTime = now
            play("box_push.wav", 0.5f, 1.0f)
        }
    }

    fun playSlash(rate: Float = 1.0f) {
        val finalRate = rate * (0.96f + (Math.random().toFloat() * 0.08f))
        play("slash.wav", 0.9f, finalRate)
    }

    fun playHeavySwing(rate: Float = 0.95f) {
        val finalRate = rate * (0.97f + (Math.random().toFloat() * 0.06f))
        play("heavy_swing.wav", 1.0f, finalRate)
    }

    fun playSpellCast() {
        play("spell_cast.wav", 0.9f, 1.05f)
    }

    fun playDodgeRoll() {
        play("dodge_roll.wav", 0.85f, 1.0f)
    }

    fun playFlaskDrink() {
        play("flask_drink.wav", 0.95f, 1.0f)
    }

    fun playEnemyHit() {
        play("enemy_hit.wav", 0.9f, 0.98f + (Math.random().toFloat() * 0.08f))
    }

    fun playChestOpen() {
        play("chest_open.wav", 1.0f, 1.0f)
    }

    fun playVisceralStrike() {
        play("visceral_strike.wav", 1.0f, 1.0f)
    }

    fun playBloodstainReclaim() {
        play("bloodstain_reclaim.wav", 0.95f, 1.0f)
    }

    fun playMimicGrowl() {
        play("mimic_growl.wav", 1.0f, 1.0f)
    }

    fun playStompImpact() {
        play("stomp_impact.wav", 1.0f, 1.0f)
    }

    fun playWeaponSkill() {
        play("weapon_skill.wav", 1.0f, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
