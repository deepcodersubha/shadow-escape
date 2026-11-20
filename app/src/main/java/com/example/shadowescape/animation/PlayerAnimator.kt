package com.example.shadowescape.animation

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.WeaponType
import com.example.shadowescape.renderer.ArmorRenderer
import com.example.shadowescape.renderer.WeaponRenderer
import java.io.IOException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class PlayerAnimState {
    IDLE, RUN, JUMP, FALL, LAND, DEATH
}

class PlayerAnimator(assetManager: AssetManager) {
    val weaponRenderer: WeaponRenderer = WeaponRenderer(assetManager)
    val armorRenderer: ArmorRenderer = ArmorRenderer(assetManager)
    private val animations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private val shinobiAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var shinobiAttackAnim: SpriteAnimation? = null
    private var shinobiAttack2Anim: SpriteAnimation? = null
    private var shinobiAttack3Anim: SpriteAnimation? = null
    private var shinobiDashAnim: SpriteAnimation? = null
    private var shinobiHitAnim: SpriteAnimation? = null

    private val roninAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var roninAttackAnim: SpriteAnimation? = null
    private var roninDashAnim: SpriteAnimation? = null

    // Custom Iron Vanguard Hand-Crafted Sprite Animations (FreeKnight Pack)
    private val vanguardAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var vanguardAttack1Anim: SpriteAnimation? = null
    private var vanguardAttack2Anim: SpriteAnimation? = null
    private var vanguardAttackAnim: SpriteAnimation? = null
    private var vanguardRollAnim: SpriteAnimation? = null
    private var vanguardDashAnim: SpriteAnimation? = null
    private var vanguardSlideAnim: SpriteAnimation? = null
    private var vanguardHitAnim: SpriteAnimation? = null

    // Custom Grave Executioner Hand-Crafted Sprite Animations (Elementals Fire Knight Pack)
    private val executionerAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var executionerAttack1Anim: SpriteAnimation? = null
    private var executionerAttack2Anim: SpriteAnimation? = null
    private var executionerAttack3Anim: SpriteAnimation? = null
    private var executionerSpAtkAnim: SpriteAnimation? = null
    private var executionerRollAnim: SpriteAnimation? = null
    private var executionerHitAnim: SpriteAnimation? = null

    // Custom Astral Sorcerer Hand-Crafted Sprite Animations (Wizard Pack)
    private val sorcererAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var sorcererAttack1Anim: SpriteAnimation? = null
    private var sorcererAttack2Anim: SpriteAnimation? = null
    private var sorcererHitAnim: SpriteAnimation? = null

    // In-House Modular Mannequin Animations (Watermark-free, 128x128 with sockets)
    private val modularAnimations = mutableMapOf<PlayerAnimState, SpriteAnimation>()
    private var modularAttackAnim: SpriteAnimation? = null

    var currentState: PlayerAnimState = PlayerAnimState.IDLE
        private set

    private var wasGrounded: Boolean = true

    // =========================================================================
    // Zero-allocation reusable fields for Dead Cells style Skeletal Avatars
    // =========================================================================
    private val limbFillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val limbOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        color = Color.parseColor("#090D16")
    }
    private val limbHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val armorFillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val armorDetailPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val armorOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = Color.parseColor("#090D16")
    }

    private val flameOuterPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val flameCorePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val flameGlowPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    private val clothFillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val clothShadowPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val clothOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = Color.parseColor("#090D16")
    }

    private val eyeCorePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val eyeGlowPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    // Paths
    private val limbPath = Path()
    private val flamePath = Path()
    private val flameCorePath = Path()
    private val scarfPath = Path()
    private val scarfRibbon2Path = Path()
    private val kiltPath = Path()
    private val capePath = Path()
    private val shieldPath = Path()
    private val destRect = RectF()
    private val weaponAnchor = RectF()

    init {
        loadAssets(assetManager)
    }

    private fun loadAssets(assets: AssetManager) {
        loadAnim(assets, PlayerAnimState.IDLE, "characters/player_idle.png", 128, 128, 6, 8f, true)
        loadAnim(assets, PlayerAnimState.RUN, "characters/player_run.png", 128, 128, 8, 12f, true)
        loadAnim(assets, PlayerAnimState.JUMP, "characters/player_jump.png", 128, 128, 4, 10f, false)
        loadAnim(assets, PlayerAnimState.FALL, "characters/player_fall.png", 128, 128, 4, 10f, true)
        loadAnim(assets, PlayerAnimState.LAND, "characters/player_land.png", 128, 128, 3, 15f, false)
        loadAnim(assets, PlayerAnimState.DEATH, "characters/player_death.png", 128, 128, 6, 8f, false)

        // Custom Shadow Shinobi Hand-Crafted Sprite Animations
        loadShinobiAnim(assets, PlayerAnimState.IDLE, "characters/shinobi/shinobi_idle.png", 128, 104, 8, 10f, true)
        loadShinobiAnim(assets, PlayerAnimState.RUN, "characters/shinobi/shinobi_run.png", 128, 104, 8, 14f, true)
        loadShinobiAnim(assets, PlayerAnimState.JUMP, "characters/shinobi/shinobi_jump.png", 128, 104, 3, 12f, false)
        loadShinobiAnim(assets, PlayerAnimState.FALL, "characters/shinobi/shinobi_fall.png", 128, 104, 2, 10f, true)
        loadShinobiAnim(assets, PlayerAnimState.LAND, "characters/shinobi/shinobi_land.png", 128, 104, 1, 12f, false)
        loadShinobiAnim(assets, PlayerAnimState.DEATH, "characters/shinobi/shinobi_death.png", 128, 104, 5, 10f, false)
        shinobiAttackAnim = loadStrip(assets, "characters/shinobi/shinobi_attack.png", 180, 104, 6, 18f, false)
        shinobiAttack2Anim = loadStrip(assets, "characters/shinobi/shinobi_attack2.png", 180, 104, 6, 18f, false)
        shinobiAttack3Anim = null
        shinobiDashAnim = loadStrip(assets, "characters/shinobi/shinobi_dash.png", 200, 104, 6, 18f, false)
        shinobiHitAnim = loadStrip(assets, "characters/shinobi/shinobi_hit.png", 128, 104, 4, 14f, false)

        // Custom Ronin Sprite Animations (Samurai PixelArt Assets Pack)
        loadRoninAnim(assets, PlayerAnimState.IDLE, "characters/ronin/ronin_idle.png", 128, 128, 9, 10f, true)
        loadRoninAnim(assets, PlayerAnimState.RUN, "characters/ronin/ronin_run.png", 128, 128, 8, 13f, true)
        loadRoninAnim(assets, PlayerAnimState.JUMP, "characters/ronin/ronin_jump.png", 128, 128, 9, 12f, false)
        loadRoninAnim(assets, PlayerAnimState.FALL, "characters/ronin/ronin_jump.png", 128, 128, 9, 10f, true)
        loadRoninAnim(assets, PlayerAnimState.DEATH, "characters/ronin/ronin_death.png", 128, 128, 5, 10f, false)
        roninAttackAnim = loadStrip(assets, "characters/ronin/ronin_attack.png", 128, 128, 14, 22f, false)
        roninDashAnim = loadStrip(assets, "characters/ronin/ronin_dash.png", 128, 128, 7, 18f, false)

        // Custom Iron Vanguard Hand-Crafted Sprite Animations (FreeKnight Pack)
        loadVanguardAnim(assets, PlayerAnimState.IDLE, "characters/vanguard/vanguard_idle.png", 120, 80, 10, 10f, true)
        loadVanguardAnim(assets, PlayerAnimState.RUN, "characters/vanguard/vanguard_run.png", 120, 80, 10, 12f, true)
        loadVanguardAnim(assets, PlayerAnimState.JUMP, "characters/vanguard/vanguard_jump.png", 120, 80, 3, 10f, false)
        loadVanguardAnim(assets, PlayerAnimState.FALL, "characters/vanguard/vanguard_fall.png", 120, 80, 3, 10f, true)
        loadVanguardAnim(assets, PlayerAnimState.DEATH, "characters/vanguard/vanguard_death.png", 120, 80, 10, 10f, false)

        try {
            assets.open("characters/vanguard/vanguard_attack1.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardAttack1Anim = SpriteAnimation(bmp, 120, 80, 4, 14f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_attack2.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardAttack2Anim = SpriteAnimation(bmp, 120, 80, 6, 14f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_attack.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardAttackAnim = SpriteAnimation(bmp, 120, 80, 10, 16f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_roll.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardRollAnim = SpriteAnimation(bmp, 120, 80, 12, 18f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_dash.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardDashAnim = SpriteAnimation(bmp, 120, 80, 2, 12f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_slide.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardSlideAnim = SpriteAnimation(bmp, 120, 80, 4, 12f, false)
            }
        } catch (e: Exception) {}
        try {
            assets.open("characters/vanguard/vanguard_hit.png").use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) vanguardHitAnim = SpriteAnimation(bmp, 120, 80, 1, 10f, false)
            }
        } catch (e: Exception) {}

        // Custom Grave Executioner Hand-Crafted Sprite Animations (Elementals Fire Knight Pack)
        loadStrip(assets, "characters/executioner/executioner_idle.png", 288, 128, 8, 10f, true)?.let {
            executionerAnimations[PlayerAnimState.IDLE] = it
        }
        loadStrip(assets, "characters/executioner/executioner_run.png", 288, 128, 8, 12f, true)?.let {
            executionerAnimations[PlayerAnimState.RUN] = it
        }
        loadStrip(assets, "characters/executioner/executioner_jump.png", 288, 128, 3, 10f, false)?.let {
            executionerAnimations[PlayerAnimState.JUMP] = it
        }
        loadStrip(assets, "characters/executioner/executioner_fall.png", 288, 128, 3, 10f, true)?.let {
            executionerAnimations[PlayerAnimState.FALL] = it
        }
        loadStrip(assets, "characters/executioner/executioner_death.png", 288, 128, 13, 12f, false)?.let {
            executionerAnimations[PlayerAnimState.DEATH] = it
        }
        executionerAttack1Anim = loadStrip(assets, "characters/executioner/executioner_attack1.png", 288, 128, 11, 18f, false)
        executionerAttack2Anim = loadStrip(assets, "characters/executioner/executioner_attack2.png", 288, 128, 19, 24f, false)
        executionerAttack3Anim = loadStrip(assets, "characters/executioner/executioner_attack3.png", 288, 128, 28, 28f, false)
        executionerSpAtkAnim = loadStrip(assets, "characters/executioner/executioner_sp_atk.png", 288, 128, 18, 22f, false)
        executionerRollAnim = loadStrip(assets, "characters/executioner/executioner_roll.png", 288, 128, 8, 16f, false)
        executionerHitAnim = loadStrip(assets, "characters/executioner/executioner_hit.png", 288, 128, 6, 14f, false)

        // Custom Astral Sorcerer Hand-Crafted Sprite Animations (Wizard Pack, 231x190)
        loadStrip(assets, "characters/sorcerer/sorcerer_idle.png", 231, 190, 6, 9f, true)?.let {
            sorcererAnimations[PlayerAnimState.IDLE] = it
        }
        loadStrip(assets, "characters/sorcerer/sorcerer_run.png", 231, 190, 8, 12f, true)?.let {
            sorcererAnimations[PlayerAnimState.RUN] = it
        }
        loadStrip(assets, "characters/sorcerer/sorcerer_jump.png", 231, 190, 2, 10f, false)?.let {
            sorcererAnimations[PlayerAnimState.JUMP] = it
        }
        loadStrip(assets, "characters/sorcerer/sorcerer_fall.png", 231, 190, 2, 10f, true)?.let {
            sorcererAnimations[PlayerAnimState.FALL] = it
        }
        loadStrip(assets, "characters/sorcerer/sorcerer_death.png", 231, 190, 7, 10f, false)?.let {
            sorcererAnimations[PlayerAnimState.DEATH] = it
        }
        sorcererAttack1Anim = loadStrip(assets, "characters/sorcerer/sorcerer_attack1.png", 231, 190, 8, 18f, false)
        sorcererAttack2Anim = loadStrip(assets, "characters/sorcerer/sorcerer_attack2.png", 231, 190, 8, 18f, false)
        sorcererHitAnim = loadStrip(assets, "characters/sorcerer/sorcerer_hit.png", 231, 190, 4, 14f, false)

        // In-House Modular Mannequin Sprite Animations (128x128 per frame, Idle: 6 frames, Run: 8 frames)
        loadStrip(assets, "characters/modular/modular_idle.png", 128, 128, 6, 8f, true)?.let {
            modularAnimations[PlayerAnimState.IDLE] = it
        }
        loadStrip(assets, "characters/modular/modular_run.png", 128, 128, 8, 14f, true)?.let {
            modularAnimations[PlayerAnimState.RUN] = it
        }
        loadStrip(assets, "characters/modular/modular_jump.png", 128, 128, 6, 10f, false)?.let {
            modularAnimations[PlayerAnimState.JUMP] = it
            modularAnimations[PlayerAnimState.FALL] = it
        }
        modularAttackAnim = loadStrip(assets, "characters/modular/modular_attack.png", 128, 128, 6, 16f, false)
    }

    private fun loadStrip(
        assets: AssetManager,
        path: String,
        fw: Int,
        fh: Int,
        count: Int,
        fps: Float,
        looping: Boolean
    ): SpriteAnimation? {
        return try {
            assets.open(path).use { isStream ->
                BitmapFactory.decodeStream(isStream)?.let { bmp ->
                    SpriteAnimation(bmp, fw, fh, count, fps, looping)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadVanguardAnim(
        assets: AssetManager,
        state: PlayerAnimState,
        path: String,
        fw: Int,
        fh: Int,
        count: Int,
        fps: Float,
        looping: Boolean
    ) {
        try {
            assets.open(path).use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) {
                    vanguardAnimations[state] = SpriteAnimation(bmp, fw, fh, count, fps, looping)
                }
            }
        } catch (e: Exception) {}
    }

    private fun loadShinobiAnim(
        assets: AssetManager,
        state: PlayerAnimState,
        path: String,
        fw: Int,
        fh: Int,
        count: Int,
        fps: Float,
        looping: Boolean
    ) {
        try {
            assets.open(path).use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) {
                    shinobiAnimations[state] = SpriteAnimation(bmp, fw, fh, count, fps, looping)
                }
            }
        } catch (e: Exception) {}
    }

    private fun loadRoninAnim(
        assets: AssetManager,
        state: PlayerAnimState,
        path: String,
        fw: Int,
        fh: Int,
        count: Int,
        fps: Float,
        looping: Boolean
    ) {
        try {
            assets.open(path).use { isStream ->
                val bmp = BitmapFactory.decodeStream(isStream)
                if (bmp != null) {
                    roninAnimations[state] = SpriteAnimation(bmp, fw, fh, count, fps, looping)
                }
            }
        } catch (e: Exception) {}
    }

    private fun loadAnim(
        assets: AssetManager,
        state: PlayerAnimState,
        path: String,
        fw: Int,
        fh: Int,
        count: Int,
        fps: Float,
        looping: Boolean
    ) {
        try {
            val inputStream = assets.open(path)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                animations[state] = SpriteAnimation(bitmap, fw, fh, count, fps, looping)
            }
        } catch (e: IOException) {
            // Handled
        }
    }

    fun update(dt: Float, player: Player) {
        val landAnim = if (player.currentOrigin.id == "shinobi" && shinobiAnimations.isNotEmpty()) {
            shinobiAnimations[PlayerAnimState.LAND]
        } else {
            animations[PlayerAnimState.LAND]
        }
        val isLandFinished = landAnim == null || landAnim.isFinished

        val newState = when {
            !player.isAlive -> PlayerAnimState.DEATH
            player.isGrounded && !wasGrounded && player.vx == 0f -> PlayerAnimState.LAND
            currentState == PlayerAnimState.LAND && !isLandFinished && player.isGrounded && player.vx == 0f -> PlayerAnimState.LAND
            player.isGrounded && player.vx != 0f -> PlayerAnimState.RUN
            player.isGrounded -> PlayerAnimState.IDLE
            player.vy < 0f -> PlayerAnimState.JUMP
            else -> PlayerAnimState.FALL
        }

        wasGrounded = player.isGrounded

        if (newState != currentState) {
            currentState = newState
            animations[currentState]?.reset()
            shinobiAnimations[currentState]?.reset()
            vanguardAnimations[currentState]?.reset()
            executionerAnimations[currentState]?.reset()
            sorcererAnimations[currentState]?.reset()
            roninAnimations[currentState]?.reset()
            modularAnimations[currentState]?.reset()
        }

        animations[currentState]?.update(dt)
        if (currentState == PlayerAnimState.RUN) {
            // Dynamically scale running animation FPS with actual horizontal velocity to eliminate sliding
            val speedFactor = (Math.abs(player.vx) / 250f).coerceIn(0.7f, 1.6f)
            modularAnimations[currentState]?.update(dt * speedFactor)
            when (player.currentOrigin.id) {
                "shinobi" -> shinobiAnimations[currentState]?.update(dt * speedFactor)
                "vanguard" -> vanguardAnimations[currentState]?.update(dt * speedFactor)
                "executioner" -> executionerAnimations[currentState]?.update(dt * speedFactor)
                "sorcerer" -> sorcererAnimations[currentState]?.update(dt * speedFactor)
                "forsaken" -> roninAnimations[currentState]?.update(dt * speedFactor)
            }
        } else {
            modularAnimations[currentState]?.update(dt)
            shinobiAnimations[currentState]?.update(dt)
            vanguardAnimations[currentState]?.update(dt)
            executionerAnimations[currentState]?.update(dt)
            sorcererAnimations[currentState]?.update(dt)
            roninAnimations[currentState]?.update(dt)
        }
    }

    fun draw(canvas: Canvas, player: Player, paint: Paint?): Boolean {
        val pb = player.bounds

        // In-House Modular Mannequin: High-precision modular sprite with swappable armors & weapons!
        if (player.useModularSprite && modularAnimations.isNotEmpty()) {
            if (renderModularMannequin(canvas, player, paint)) {
                return true
            }
        }

        // Shadow Shinobi: Render using Shadow Shinobi sprite sheet animations!
        if (player.currentOrigin.id == "shinobi" && shinobiAnimations.isNotEmpty()) {
            val scale = 0.88f
            val spriteW = 128f * scale
            val spriteH = 104f * scale
            val bottomOffset = 4f * scale

            destRect.set(
                pb.centerX - spriteW / 2f,
                pb.bottom - spriteH + bottomOffset,
                pb.centerX + spriteW / 2f,
                pb.bottom + bottomOffset
            )

            canvas.save()
            val flipH = !player.facingRight

            if (player.isRolling && shinobiDashAnim != null) {
                val dashW = 200f * scale
                val dashShiftX = (if (flipH) 1f else -1f) * (24f * scale)
                val dashCx = pb.centerX + dashShiftX
                destRect.set(
                    dashCx - dashW / 2f,
                    pb.bottom - spriteH + bottomOffset,
                    dashCx + dashW / 2f,
                    pb.bottom + bottomOffset
                )
                val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
                val frame = (progress * 5.99f).toInt().coerceIn(0, 5)
                shinobiDashAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isAttacking) {
                if (player.equippedWeapon.type == WeaponType.KATANA && player.equippedWeapon.id != "obsidian_dagger" && shinobiAttackAnim != null) {
                    val atkW = 180f * scale
                    val atkShiftX = (if (flipH) -1f else 1f) * (20f * scale)
                    val atkCx = pb.centerX + atkShiftX
                    destRect.set(
                        atkCx - atkW / 2f,
                        pb.bottom - spriteH + bottomOffset,
                        atkCx + atkW / 2f,
                        pb.bottom + bottomOffset
                    )
                    val frame = (player.attackProgress * 5.99f).toInt().coerceIn(0, 5)
                    if (player.attackComboIndex == 2 && shinobiAttack2Anim != null) {
                        shinobiAttack2Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                    } else {
                        shinobiAttackAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                    }
                } else {
                    // Weapon Cycling / Archetype Swap: dynamic socketed weapon with archetype strike angles
                    val anim = shinobiAnimations[PlayerAnimState.IDLE]
                    anim?.draw(canvas, destRect, flipH, paint)
                    val weaponAnchor = RectF(pb.centerX - 22f, pb.bottom - 74f, pb.centerX + 22f, pb.bottom)
                    weaponRenderer.draw(canvas, player, weaponAnchor)
                }
            } else {
                val anim = shinobiAnimations[currentState] ?: shinobiAnimations[PlayerAnimState.IDLE]
                anim?.draw(canvas, destRect, flipH, paint)
            }

            if (player.isAlive && !player.isRolling) {
                armorRenderer.drawOnPlayer(canvas, player, pb.centerX + (if (flipH) -2f else 2f), pb.bottom - 46f, flipH, 0.95f)
            }
            canvas.restore()
            return true
        }

        // Iron Vanguard: Render using FreeKnight authentic sprite sheet animations!
        if (player.currentOrigin.id == "vanguard" && vanguardAnimations.isNotEmpty()) {
            val spriteH = 144f
            val spriteW = 216f
            val bottomOffset = 2f

            destRect.set(
                pb.centerX - spriteW / 2f,
                pb.bottom - spriteH + bottomOffset,
                pb.centerX + spriteW / 2f,
                pb.bottom + bottomOffset
            )

            canvas.save()
            val flipH = !player.facingRight

            if (player.isRolling && vanguardRollAnim != null) {
                val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
                val frame = (progress * 11.99f).toInt().coerceIn(0, 11)
                vanguardRollAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isAttacking) {
                if (player.equippedWeapon.type == WeaponType.SWORD) {
                    // Signature Straight Sword / Broadsword: render authentic 2-Strike Heavy Cleave
                    if (player.attackComboIndex == 1 && vanguardAttack1Anim != null) {
                        val frame = (player.attackProgress * 3.99f).toInt().coerceIn(0, 3)
                        vanguardAttack1Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                    } else if (player.attackComboIndex == 2 && vanguardAttack2Anim != null) {
                        val frame = (player.attackProgress * 5.99f).toInt().coerceIn(0, 5)
                        vanguardAttack2Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                    } else if (vanguardAttackAnim != null) {
                        val frame = (player.attackProgress * 9.99f).toInt().coerceIn(0, 9)
                        vanguardAttackAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                    }
                } else {
                    // Weapon Cycling / Archetype Swap: dynamic socketed weapon with archetype strike angles
                    val anim = vanguardAnimations[PlayerAnimState.IDLE]
                    anim?.draw(canvas, destRect, flipH, paint)
                    val weaponAnchor = RectF(pb.centerX - 22f, pb.bottom - 74f, pb.centerX + 22f, pb.bottom)
                    weaponRenderer.draw(canvas, player, weaponAnchor)
                }
            } else {
                val anim = vanguardAnimations[currentState] ?: vanguardAnimations[PlayerAnimState.IDLE]
                anim?.draw(canvas, destRect, flipH, paint)
            }

            if (player.isAlive && !player.isRolling) {
                armorRenderer.drawOnPlayer(canvas, player, pb.centerX + (if (flipH) -2f else 2f), pb.bottom - 46f, flipH, 0.95f)
            }
            canvas.restore()
            return true
        }

        // Grave Executioner: Render using Elementals Fire Knight authentic sprite sheet animations!
        if (player.currentOrigin.id == "executioner" && executionerAnimations.isNotEmpty()) {
            val scale = 1.62f
            val spriteW = 288f * scale
            val spriteH = 128f * scale
            val bottomOffset = 2f
            val flipH = !player.facingRight
            // Character center inside 288px frame is ~130px (14px left of center), so shift toward facing side
            val bodyShiftX = (if (flipH) -1f else 1f) * (14f * scale)
            val cx = pb.centerX + bodyShiftX

            destRect.set(
                cx - spriteW / 2f,
                pb.bottom - spriteH + bottomOffset,
                cx + spriteW / 2f,
                pb.bottom + bottomOffset
            )

            canvas.save()

            if (player.isRolling && executionerRollAnim != null) {
                val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
                val frame = (progress * 7.99f).toInt().coerceIn(0, 7)
                executionerRollAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isPerformingSkill && executionerSpAtkAnim != null) {
                val progress = (1f - (player.skillTimer / player.skillDuration)).coerceIn(0f, 1f)
                val frame = (progress * 17.99f).toInt().coerceIn(0, 17)
                executionerSpAtkAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isAttacking) {
                if (player.equippedWeapon.type == WeaponType.GREATSWORD && player.equippedWeapon.id != "battle_axe") {
                    // Signature Greatsword: authentic 3-hit Fire Knight blazing greatsword combo
                    when (player.attackComboIndex) {
                        1 -> if (executionerAttack1Anim != null) {
                            val frame = (player.attackProgress * 10.99f).toInt().coerceIn(0, 10)
                            executionerAttack1Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                        }
                        2 -> if (executionerAttack2Anim != null) {
                            val frame = (player.attackProgress * 18.99f).toInt().coerceIn(0, 18)
                            executionerAttack2Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                        }
                        else -> if (executionerAttack3Anim != null) {
                            val frame = (player.attackProgress * 27.99f).toInt().coerceIn(0, 27)
                            executionerAttack3Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                        }
                    }
                } else {
                    // Weapon Cycling / Archetype Swap: socketed Frakassets weapon with archetype strike angles
                    val anim = executionerAnimations[PlayerAnimState.IDLE]
                    anim?.draw(canvas, destRect, flipH, paint)
                    val weaponAnchor = RectF(pb.centerX - 24f, pb.bottom - 76f, pb.centerX + 24f, pb.bottom)
                    weaponRenderer.draw(canvas, player, weaponAnchor)
                }
            } else {
                val anim = executionerAnimations[currentState] ?: executionerAnimations[PlayerAnimState.IDLE]
                anim?.draw(canvas, destRect, flipH, paint)
            }

            if (player.isAlive && !player.isRolling) {
                armorRenderer.drawOnPlayer(canvas, player, pb.centerX + (if (flipH) -2f else 2f), pb.bottom - 46f, flipH, 0.95f)
            }
            canvas.restore()
            return true
        }

        // Astral Sorcerer: Render using Wizard Pack authentic sprite sheet animations!
        if (player.currentOrigin.id == "sorcerer" && sorcererAnimations.isNotEmpty()) {
            val scale = 0.96f
            val spriteW = 231f * scale
            val spriteH = 190f * scale
            val bottomOffset = 50f * scale // Feet sit at y=140 inside 190px frame
            val flipH = !player.facingRight
            val bodyShiftX = (if (flipH) -1f else 1f) * (5f * scale)
            val cx = pb.centerX + bodyShiftX

            destRect.set(
                cx - spriteW / 2f,
                pb.bottom - spriteH + bottomOffset,
                cx + spriteW / 2f,
                pb.bottom + bottomOffset
            )

            canvas.save()

            if (player.isPerformingSkill && sorcererAttack1Anim != null) {
                val progress = (1f - (player.skillTimer / player.skillDuration)).coerceIn(0f, 1f)
                val frame = (progress * 7.99f).toInt().coerceIn(0, 7)
                sorcererAttack1Anim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isAttacking) {
                if (player.equippedWeapon.type == WeaponType.STAFF) {
                    val anim = if (player.attackComboIndex == 2 && sorcererAttack2Anim != null) sorcererAttack2Anim else sorcererAttack1Anim
                    if (anim != null) {
                        val frame = (player.attackProgress * 7.99f).toInt().coerceIn(0, 7)
                        anim.drawFrame(canvas, destRect, frame, flipH, paint)
                    }
                } else {
                    val anim = sorcererAnimations[PlayerAnimState.IDLE]
                    anim?.draw(canvas, destRect, flipH, paint)
                    weaponAnchor.set(pb.centerX - 22f, pb.bottom - 74f, pb.centerX + 22f, pb.bottom)
                    weaponRenderer.draw(canvas, player, weaponAnchor)
                }
            } else {
                val anim = sorcererAnimations[currentState] ?: sorcererAnimations[PlayerAnimState.IDLE]
                anim?.draw(canvas, destRect, flipH, paint)
            }

            if (player.isAlive && !player.isRolling) {
                armorRenderer.drawOnPlayer(canvas, player, pb.centerX + (if (flipH) -2f else 2f), pb.bottom - 46f, flipH, 0.95f)
            }
            canvas.restore()
            return true
        }

        // Ronin (formerly Forsaken): Render using Samurai PixelArt Assets sprite sheet animations!
        if (player.currentOrigin.id == "forsaken" && roninAnimations.isNotEmpty()) {
            val scale = 1.15f
            val spriteW = 128f * scale
            val spriteH = 128f * scale
            val bottomOffset = 3f * scale

            destRect.set(
                pb.centerX - spriteW / 2f,
                pb.bottom - spriteH + bottomOffset,
                pb.centerX + spriteW / 2f,
                pb.bottom + bottomOffset
            )

            canvas.save()
            val flipH = !player.facingRight

            if (player.isRolling && roninDashAnim != null) {
                val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
                val frame = (progress * 6.99f).toInt().coerceIn(0, 6)
                roninDashAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
            } else if (player.isAttacking) {
                if (player.equippedWeapon.id == player.currentOrigin.startingWeapon.id && roninAttackAnim != null) {
                    val frame = (player.attackProgress * 13.99f).toInt().coerceIn(0, 13)
                    roninAttackAnim!!.drawFrame(canvas, destRect, frame, flipH, paint)
                } else {
                    val anim = roninAnimations[PlayerAnimState.IDLE]
                    anim?.draw(canvas, destRect, flipH, paint)
                    weaponAnchor.set(pb.centerX - 22f, pb.bottom - 74f, pb.centerX + 22f, pb.bottom)
                    weaponRenderer.draw(canvas, player, weaponAnchor)
                }
            } else {
                val anim = roninAnimations[currentState] ?: roninAnimations[PlayerAnimState.IDLE]
                anim?.draw(canvas, destRect, flipH, paint)
            }

            if (player.isAlive && !player.isRolling) {
                armorRenderer.drawOnPlayer(canvas, player, pb.centerX + (if (flipH) -2f else 2f), pb.bottom - 46f, flipH, 0.95f)
            }
            canvas.restore()
            return true
        }

        val heroWidth = 44f
        val heroHeight = 74f

        destRect.set(
            pb.centerX - heroWidth / 2f,
            pb.bottom - heroHeight,
            pb.centerX + heroWidth / 2f,
            pb.bottom
        )

        canvas.save()

        // Acrobatic 360-degree combat roll
        if (player.isRolling) {
            val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
            val rollAngle = progress * 360f
            canvas.rotate(if (player.facingRight) rollAngle else -rollAngle, destRect.centerX(), destRect.centerY())
        } else if (player.isAttacking) {
            // Dead Cells style combat anticipation, snappy lunge, squash & stretch
            val dir = if (player.facingRight) 1f else -1f
            val p = player.attackProgress
            val pivotX = destRect.centerX()
            val pivotY = destRect.bottom - 10f

            when (player.equippedWeapon.type) {
                com.example.shadowescape.model.WeaponType.KATANA -> {
                    when (player.attackComboIndex) {
                        1 -> {
                            if (p < 0.22f) {
                                val t = p / 0.22f
                                canvas.translate(-dir * 6f * t, 0f)
                                canvas.rotate(-dir * 5f * t, pivotX, pivotY)
                                canvas.scale(1f - 0.08f * t, 1f + 0.05f * t, pivotX, pivotY)
                            } else if (p < 0.65f) {
                                val t = (p - 0.22f) / 0.43f
                                val fwd = sin(t * Math.PI.toFloat())
                                canvas.translate(dir * 20f * fwd, 0f)
                                canvas.rotate(dir * 12f * fwd, pivotX, pivotY)
                                canvas.scale(1f + 0.16f * fwd, 1f - 0.09f * fwd, pivotX, pivotY)
                            } else {
                                val t = (p - 0.65f) / 0.35f
                                val fwd = 1f - t
                                canvas.translate(dir * 6f * fwd, 0f)
                                canvas.rotate(dir * 4f * fwd, pivotX, pivotY)
                            }
                        }
                        2 -> {
                            if (p < 0.22f) {
                                val t = p / 0.22f
                                canvas.translate(0f, 6f * t)
                                canvas.scale(1f + 0.10f * t, 1f - 0.10f * t, pivotX, pivotY)
                            } else if (p < 0.65f) {
                                val t = (p - 0.22f) / 0.43f
                                val up = sin(t * Math.PI.toFloat())
                                canvas.translate(dir * 14f * up, -16f * up)
                                canvas.rotate(-dir * 14f * up, pivotX, pivotY)
                                canvas.scale(1f - 0.08f * up, 1f + 0.20f * up, pivotX, pivotY)
                            } else {
                                val t = (p - 0.65f) / 0.35f
                                val settle = 1f - t
                                canvas.translate(dir * 4f * settle, -4f * settle)
                            }
                        }
                        else -> {
                            if (p < 0.25f) {
                                val t = p / 0.25f
                                canvas.translate(0f, -14f * t)
                                canvas.rotate(-dir * 8f * t, pivotX, pivotY)
                                canvas.scale(1f - 0.08f * t, 1f + 0.14f * t, pivotX, pivotY)
                            } else if (p < 0.70f) {
                                val t = (p - 0.25f) / 0.45f
                                val slam = sin(t * Math.PI.toFloat())
                                canvas.translate(dir * 28f * slam, 8f * slam)
                                canvas.rotate(dir * 18f * slam, pivotX, pivotY)
                                canvas.scale(1f + 0.24f * slam, 1f - 0.16f * slam, pivotX, pivotY)
                            } else {
                                val t = (p - 0.70f) / 0.30f
                                val rec = 1f - t
                                canvas.translate(dir * 8f * rec, 2f * rec)
                                canvas.rotate(dir * 5f * rec, pivotX, pivotY)
                            }
                        }
                    }
                }
                com.example.shadowescape.model.WeaponType.SWORD -> {
                    when (player.attackComboIndex) {
                        1 -> {
                            val fwd = sin(p * Math.PI.toFloat())
                            canvas.translate(dir * 18f * fwd, 0f)
                            canvas.rotate(dir * 8f * fwd, pivotX, pivotY)
                            canvas.scale(1f + 0.12f * fwd, 1f - 0.06f * fwd, pivotX, pivotY)
                        }
                        2 -> {
                            val up = sin(p * Math.PI.toFloat())
                            canvas.translate(dir * 12f * up, -10f * up)
                            canvas.rotate(-dir * 10f * up, pivotX, pivotY)
                            canvas.scale(1f - 0.06f * up, 1f + 0.14f * up, pivotX, pivotY)
                        }
                        else -> {
                            val thrust = sin(p * Math.PI.toFloat())
                            canvas.translate(dir * 24f * thrust, 4f * thrust)
                            canvas.rotate(dir * 14f * thrust, pivotX, pivotY)
                            canvas.scale(1f + 0.20f * thrust, 1f - 0.10f * thrust, pivotX, pivotY)
                        }
                    }
                }
                com.example.shadowescape.model.WeaponType.CLUB -> {
                    if (p < 0.28f) {
                        val t = p / 0.28f
                        canvas.translate(-dir * 8f * t, -10f * t)
                        canvas.rotate(-dir * 12f * t, pivotX, pivotY)
                    } else if (p < 0.72f) {
                        val t = (p - 0.28f) / 0.44f
                        val slam = sin(t * Math.PI.toFloat())
                        canvas.translate(dir * 24f * slam, 10f * slam)
                        canvas.rotate(dir * 20f * slam, pivotX, pivotY)
                        canvas.scale(1f + 0.20f * slam, 1f - 0.15f * slam, pivotX, pivotY)
                    } else {
                        val t = (p - 0.72f) / 0.28f
                        val rec = 1f - t
                        canvas.translate(dir * 6f * rec, 2f * rec)
                    }
                }
                com.example.shadowescape.model.WeaponType.GREATSWORD -> {
                    if (player.attackComboIndex == 1) {
                        if (p < 0.30f) {
                            val t = p / 0.30f
                            canvas.translate(-dir * 12f * t, -12f * t)
                            canvas.rotate(-dir * 16f * t, pivotX, pivotY)
                            canvas.scale(1f - 0.12f * t, 1f + 0.12f * t, pivotX, pivotY)
                        } else if (p < 0.75f) {
                            val t = (p - 0.30f) / 0.45f
                            val slam = sin(t * Math.PI.toFloat())
                            canvas.translate(dir * 32f * slam, 14f * slam)
                            canvas.rotate(dir * 24f * slam, pivotX, pivotY)
                            canvas.scale(1f + 0.28f * slam, 1f - 0.18f * slam, pivotX, pivotY)
                        } else {
                            val t = (p - 0.75f) / 0.25f
                            val rec = 1f - t
                            canvas.translate(dir * 10f * rec, 4f * rec)
                            canvas.rotate(dir * 6f * rec, pivotX, pivotY)
                        }
                    } else {
                        if (p < 0.26f) {
                            val t = p / 0.26f
                            canvas.translate(dir * 6f * t, 8f * t)
                            canvas.rotate(dir * 10f * t, pivotX, pivotY)
                            canvas.scale(1f + 0.10f * t, 1f - 0.10f * t, pivotX, pivotY)
                        } else if (p < 0.72f) {
                            val t = (p - 0.26f) / 0.46f
                            val scoop = sin(t * Math.PI.toFloat())
                            canvas.translate(dir * 24f * scoop, -18f * scoop)
                            canvas.rotate(-dir * 22f * scoop, pivotX, pivotY)
                            canvas.scale(1f - 0.10f * scoop, 1f + 0.26f * scoop, pivotX, pivotY)
                        } else {
                            val t = (p - 0.72f) / 0.28f
                            val rec = 1f - t
                            canvas.translate(dir * 8f * rec, -6f * rec)
                            canvas.rotate(-dir * 6f * rec, pivotX, pivotY)
                        }
                    }
                }
                com.example.shadowescape.model.WeaponType.STAFF -> {
                    val pulse = sin(p * Math.PI.toFloat())
                    canvas.translate(dir * 12f * pulse, -6f * pulse)
                    canvas.scale(1f + 0.08f * pulse, 1f + 0.08f * pulse, pivotX, pivotY)
                }
                com.example.shadowescape.model.WeaponType.BOW -> {
                    val drawPull = sin(p * Math.PI.toFloat())
                    canvas.translate(-dir * 4f * drawPull, 0f)
                }
                com.example.shadowescape.model.WeaponType.SCYTHE -> {
                    val sweep = sin(p * Math.PI.toFloat())
                    canvas.translate(dir * 10f * sweep, -4f * sweep)
                    canvas.rotate(dir * 12f * sweep, pivotX, pivotY)
                }
                com.example.shadowescape.model.WeaponType.GAUNTLET -> {
                    val punch = sin(p * Math.PI.toFloat())
                    canvas.translate(dir * 14f * punch, 0f)
                }
            }
        }

        // Render fluid Dead Cells style skeletal avatar
        renderDeadCellsAvatar(canvas, player, destRect)

        // Visibly render the weapon in the character's hand only while swinging
        if (player.isAttacking) {
            weaponRenderer.draw(canvas, player, destRect)
        }

        canvas.restore()
        return true
    }

    /**
     * Renders the modular base mannequin sprite sheet with layered armor and dynamic weapon sockets.
     */
    private fun renderModularMannequin(canvas: Canvas, player: Player, paint: Paint?): Boolean {
        val pb = player.bounds
        val scale = 0.95f
        val spriteW = 128f * scale
        val spriteH = 128f * scale
        val bottomOffset = 14f * scale

        destRect.set(
            pb.centerX - spriteW / 2f,
            pb.bottom - spriteH + bottomOffset,
            pb.centerX + spriteW / 2f,
            pb.bottom + bottomOffset
        )

        canvas.save()
        val flipH = !player.facingRight

        if (player.isRolling) {
            val progress = (1f - (player.rollTimer / player.rollDuration)).coerceIn(0f, 1f)
            val rollAngle = progress * 360f
            canvas.rotate(if (player.facingRight) rollAngle else -rollAngle, pb.centerX, pb.bottom - 32f)
            val anim = modularAnimations[PlayerAnimState.RUN] ?: modularAnimations[PlayerAnimState.IDLE]
            val rollFrames = anim?.frameCount ?: 6
            anim?.drawFrame(canvas, destRect, ((progress * (rollFrames - 0.01f)).toInt() % rollFrames), flipH, paint)
            armorRenderer.drawOnPlayer(canvas, player, pb.centerX, pb.bottom - 32f, flipH, 0.95f)
        } else if (player.isAttacking && modularAttackAnim != null) {
            val progress = player.attackProgress.coerceIn(0f, 0.99f)
            val frameIndex = (progress * 5.99f).toInt().coerceIn(0, 5)
            modularAttackAnim!!.drawFrame(canvas, destRect, frameIndex, flipH, paint)

            val socket = ModularFrameSockets.ATTACK_SOCKETS.getOrElse(frameIndex) { ModularFrameSockets.ATTACK_SOCKETS[0] }
            val torsoX = pb.centerX + socket.torsoXOffset * (if (flipH) -1f else 1f)
            val torsoY = pb.bottom - socket.torsoYOffset
            armorRenderer.drawOnPlayer(canvas, player, torsoX, torsoY, flipH, socket.torsoScale)

            // Render weapon pinned to dynamic hand socket with swing angle
            val is2H = player.equippedWeapon.isTwoHanded
            val handX = pb.centerX + (socket.handXOffset + (if (is2H) 3f else 0f)) * (if (flipH) -1f else 1f)
            val handY = pb.bottom - socket.handYOffset + (if (is2H) -2f else 0f)
            val weaponScale = if (is2H) 1.25f else 1.0f
            weaponAnchor.set(handX - 22f * weaponScale, handY - 40f * weaponScale, handX + 22f * weaponScale, handY + 20f * weaponScale)
            weaponRenderer.draw(canvas, player, weaponAnchor)
        } else {
            val animState = when (currentState) {
                PlayerAnimState.RUN -> PlayerAnimState.RUN
                PlayerAnimState.JUMP, PlayerAnimState.FALL -> PlayerAnimState.JUMP
                else -> PlayerAnimState.IDLE
            }
            val anim = modularAnimations[animState] ?: modularAnimations[PlayerAnimState.IDLE]
            val maxFrames = anim?.frameCount ?: 6
            val frameIndex = (anim?.currentFrame ?: 0).coerceIn(0, maxFrames - 1)
            anim?.draw(canvas, destRect, flipH, paint)

            val socket = when (animState) {
                PlayerAnimState.RUN -> ModularFrameSockets.RUN_SOCKETS.getOrElse(frameIndex) { ModularFrameSockets.RUN_SOCKETS[0] }
                PlayerAnimState.JUMP, PlayerAnimState.FALL -> ModularFrameSockets.JUMP_SOCKETS.getOrElse(frameIndex) { ModularFrameSockets.JUMP_SOCKETS[0] }
                else -> ModularFrameSockets.IDLE_SOCKETS.getOrElse(frameIndex) { ModularFrameSockets.IDLE_SOCKETS[0] }
            }

            val torsoX = pb.centerX + socket.torsoXOffset * (if (flipH) -1f else 1f)
            val torsoY = pb.bottom - socket.torsoYOffset
            armorRenderer.drawOnPlayer(canvas, player, torsoX, torsoY, flipH, socket.torsoScale)
            // Weapon is hidden during Idle, Run, Jump, Fall — only visible while swinging!
        }

        canvas.restore()
        return true
    }

    private fun renderDeadCellsAvatar(canvas: Canvas, player: Player, destRect: RectF) {
        val cx = destRect.centerX()
        val feetY = destRect.bottom
        val dir = if (player.facingRight) 1f else -1f
        val animTime = player.animTime
        val isIdle = currentState == PlayerAnimState.IDLE
        val isRunning = currentState == PlayerAnimState.RUN && player.isGrounded
        val isJumping = currentState == PlayerAnimState.JUMP
        val isFalling = currentState == PlayerAnimState.FALL
        val isAirborne = isJumping || isFalling

        // 1. Organic Breathing Bob & Torso Kinematics
        val breath = if (isIdle) sin(animTime * 3.8f) * 1.8f else 0f
        val runCycle = if (isRunning) animTime * 15f else 0f

        // Dead Cells forward sprint lean (dramatic athletic posture)
        val runLean = if (isRunning) dir * 10f else if (isAirborne) dir * 4f else 0f
        val pelvicBob = if (isRunning) abs(sin(runCycle)) * 3.2f else 0f

        val pelvisX = cx + runLean * 0.2f
        val pelvisY = feetY - 26f - pelvicBob + (if (isJumping) 4f else 0f)

        val chestX = cx + runLean * 0.65f
        val chestY = pelvisY - 18f + breath
        val chestW = 16f
        val chestH = 17f

        val headX = chestX + runLean * 0.35f
        val headY = chestY - 14f

        when (player.currentOrigin.id) {
            "shinobi" -> renderDeadCellsShinobi(canvas, player, cx, pelvisX, pelvisY, chestX, chestY, chestW, chestH, headX, headY, feetY, dir, runCycle, isRunning, isAirborne, animTime)
            "vanguard" -> renderDeadCellsVanguard(canvas, player, cx, pelvisX, pelvisY, chestX, chestY, chestW, chestH, headX, headY, feetY, dir, runCycle, isRunning, isAirborne, animTime)
            "executioner" -> renderDeadCellsExecutioner(canvas, player, cx, pelvisX, pelvisY, chestX, chestY, chestW, chestH, headX, headY, feetY, dir, runCycle, isRunning, isAirborne, animTime)
            "sorcerer" -> renderDeadCellsSorcerer(canvas, player, cx, pelvisX, pelvisY, chestX, chestY, chestW, chestH, headX, headY, feetY, dir, runCycle, isRunning, isAirborne, animTime)
            else -> renderDeadCellsForsaken(canvas, player, cx, pelvisX, pelvisY, chestX, chestY, chestW, chestH, headX, headY, feetY, dir, runCycle, isRunning, isAirborne, animTime)
        }

        if (player.isAlive) {
            armorRenderer.drawOnPlayer(canvas, player, chestX, chestY + 2f, !player.facingRight, 1.05f)
        }
    }

    // =========================================================================
    // 1. SHADOW SHINOBI (Dead Cells Style)
    // =========================================================================
    private fun renderDeadCellsShinobi(
        canvas: Canvas,
        player: Player,
        cx: Float,
        pelvisX: Float,
        pelvisY: Float,
        chestX: Float,
        chestY: Float,
        chestW: Float,
        chestH: Float,
        headX: Float,
        headY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        animTime: Float
    ) {
        val baseSuit = Color.parseColor("#0F172A")
        val wrapColor = Color.parseColor("#1E293B")
        val cyanFlame = Color.parseColor("#00E5FF")
        val cyanCore = Color.parseColor("#A5F3FC")

        // 0. Katana Scabbard Strapped Across Back
        val scabbardAngle = -dir * 32f
        val scabbardX = chestX - dir * 6f
        val scabbardY = chestY + 3f
        canvas.save()
        canvas.rotate(scabbardAngle, scabbardX, scabbardY)
        armorFillPaint.color = Color.parseColor("#090D16")
        canvas.drawRoundRect(scabbardX - 2.5f, scabbardY - 14f, scabbardX + 2.5f, scabbardY + 18f, 2f, 2f, armorFillPaint)
        canvas.drawRoundRect(scabbardX - 2.5f, scabbardY - 14f, scabbardX + 2.5f, scabbardY + 18f, 2f, 2f, limbOutlinePaint)
        // Cyan and gold fittings
        armorDetailPaint.style = Paint.Style.FILL
        armorDetailPaint.color = cyanFlame
        canvas.drawRect(scabbardX - 3f, scabbardY - 12f, scabbardX + 3f, scabbardY - 9f, armorDetailPaint)
        canvas.drawRect(scabbardX - 3f, scabbardY + 10f, scabbardX + 3f, scabbardY + 12f, armorDetailPaint)
        canvas.restore()

        // 1. Multi-Node Sinuous Shadow Scarf (Flowing whip physics)
        drawFluidScarf(canvas, chestX - dir * 2f, chestY - 2f, dir, if (isRunning) 48f else 28f, cyanFlame, Color.parseColor("#0891B2"), animTime, isRunning, isAirborne)

        // 2. Off-hand Arm (Back arm pumping in counter-phase)
        val offShoulderX = chestX - dir * 7f
        val offShoulderY = chestY + 2f
        val armPhase = if (isRunning) -sin(runCycle) * 12f else 0f
        val offElbowX = offShoulderX - dir * 8f + armPhase * dir * 0.6f
        val offElbowY = offShoulderY + 9f - abs(armPhase) * 0.3f
        val offHandX = offElbowX - dir * 4f + armPhase * dir
        val offHandY = offElbowY + 8f

        limbFillPaint.color = baseSuit
        drawCapsuleLimb(canvas, offShoulderX, offShoulderY, offElbowX, offElbowY, 6f, 5f)
        limbFillPaint.color = wrapColor
        drawCapsuleLimb(canvas, offElbowX, offElbowY, offHandX, offHandY, 5f, 4.2f)

        // 3. Articulated Legs (Lag leg first, then Lead leg)
        renderArticulatedLegs(
            canvas, pelvisX, pelvisY, feetY, dir, runCycle, isRunning, isAirborne,
            thighColor = baseSuit, calfColor = wrapColor, bootColor = Color.parseColor("#020617"),
            isTabi = true
        )

        // Shadow sprint dust & wisps at feet
        if (isRunning) {
            val wispPhase = animTime * 18f
            val wispLag = -dir * (10f + (sin(wispPhase) * 6f))
            flameGlowPaint.color = Color.argb(120, 15, 23, 42)
            canvas.drawCircle(pelvisX + wispLag, feetY - 2f, 3.5f + abs(sin(wispPhase)) * 2.5f, flameGlowPaint)
            flameGlowPaint.color = Color.argb(90, 0, 229, 255)
            canvas.drawCircle(pelvisX + wispLag * 0.7f, feetY - 3.5f, 2f + abs(cos(wispPhase)) * 1.8f, flameGlowPaint)
        }

        // 4. Pelvis & Sleek Waist Sash
        armorFillPaint.color = baseSuit
        canvas.drawRoundRect(pelvisX - 7f, pelvisY - 3f, pelvisX + 7f, pelvisY + 4f, 3f, 3f, armorFillPaint)
        canvas.drawRoundRect(pelvisX - 7f, pelvisY - 3f, pelvisX + 7f, pelvisY + 4f, 3f, 3f, limbOutlinePaint)
        // Cyan waist sash
        clothFillPaint.color = Color.parseColor("#0284C7")
        canvas.drawRect(pelvisX - 7.5f, pelvisY - 1f, pelvisX + 7.5f, pelvisY + 2f, clothFillPaint)

        // 5. Fitted Shinobi Vest & Crossed Harness
        armorFillPaint.color = baseSuit
        canvas.drawRoundRect(chestX - chestW * 0.5f, chestY, chestX + chestW * 0.5f, chestY + chestH, 4f, 4f, armorFillPaint)
        canvas.drawRoundRect(chestX - chestW * 0.5f, chestY, chestX + chestW * 0.5f, chestY + chestH, 4f, 4f, limbOutlinePaint)
        // Crossed harness straps
        armorDetailPaint.color = wrapColor
        armorDetailPaint.strokeWidth = 2.0f
        armorDetailPaint.style = Paint.Style.STROKE
        canvas.drawLine(chestX - 6f, chestY + 2f, chestX + 6f, chestY + chestH - 2f, armorDetailPaint)
        canvas.drawLine(chestX + 6f, chestY + 2f, chestX - 6f, chestY + chestH - 2f, armorDetailPaint)
        // Silver ring buckle
        armorDetailPaint.style = Paint.Style.FILL
        armorDetailPaint.color = Color.parseColor("#94A3B8")
        canvas.drawCircle(chestX, chestY + chestH * 0.5f, 2f, armorDetailPaint)

        // 6. Lead Arm (connected toward weapon grip)
        val leadShoulderX = chestX + dir * 7f
        val leadShoulderY = chestY + 2f
        val gripX = destRect.centerX() + dir * 6f
        val gripY = destRect.top + destRect.height() * 0.55f
        val leadElbowX = (leadShoulderX + gripX) * 0.5f + dir * 3.5f
        val leadElbowY = (leadShoulderY + gripY) * 0.5f + 2f

        limbFillPaint.color = baseSuit
        drawCapsuleLimb(canvas, leadShoulderX, leadShoulderY, leadElbowX, leadElbowY, 6.2f, 5.2f)
        limbFillPaint.color = wrapColor
        drawCapsuleLimb(canvas, leadElbowX, leadElbowY, gripX, gripY, 5.2f, 4.5f)

        // 7. Iconic Dead Cells Ethereal Cyan Flame Head & Cowl
        drawDeadCellsHead(canvas, headX, headY, dir, baseSuit, wrapColor, cyanFlame, cyanCore, animTime, player.vx, isShinobi = true)
    }

    // =========================================================================
    // 2. IRON VANGUARD (Dead Cells Style)
    // =========================================================================
    private fun renderDeadCellsVanguard(
        canvas: Canvas,
        player: Player,
        cx: Float,
        pelvisX: Float,
        pelvisY: Float,
        chestX: Float,
        chestY: Float,
        chestW: Float,
        chestH: Float,
        headX: Float,
        headY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        animTime: Float
    ) {
        val steelDark = Color.parseColor("#334155")
        val steelMid = Color.parseColor("#475569")
        val steelLight = Color.parseColor("#64748B")
        val goldAccent = Color.parseColor("#F59E0B")
        val flameGold = Color.parseColor("#F59E0B")
        val flameCore = Color.parseColor("#FEF08A")

        // 1. Strapped Iron Kite Shield on Back
        val shieldCx = chestX - dir * 11f
        val shieldTop = chestY + 1f
        shieldPath.reset()
        shieldPath.moveTo(shieldCx - 9f, shieldTop)
        shieldPath.lineTo(shieldCx + 9f, shieldTop)
        shieldPath.lineTo(shieldCx + 8f, shieldTop + 18f)
        shieldPath.lineTo(shieldCx, shieldTop + 29f)
        shieldPath.lineTo(shieldCx - 8f, shieldTop + 18f)
        shieldPath.close()

        armorFillPaint.color = Color.parseColor("#1E293B")
        canvas.drawPath(shieldPath, armorFillPaint)
        armorOutlinePaint.color = Color.parseColor("#94A3B8")
        canvas.drawPath(shieldPath, armorOutlinePaint)
        // Golden Cross Emblem
        armorDetailPaint.style = Paint.Style.STROKE
        armorDetailPaint.strokeWidth = 2.4f
        armorDetailPaint.color = goldAccent
        canvas.drawLine(shieldCx, shieldTop + 6f, shieldCx, shieldTop + 22f, armorDetailPaint)
        canvas.drawLine(shieldCx - 5f, shieldTop + 12f, shieldCx + 5f, shieldTop + 12f, armorDetailPaint)

        // 2. Articulated Steel Plate Legs & Sabatons
        renderArticulatedLegs(
            canvas, pelvisX, pelvisY, feetY, dir, runCycle, isRunning, isAirborne,
            thighColor = steelMid, calfColor = steelLight, bootColor = steelDark,
            isTabi = false
        )

        // 3. Mail Fauld Skirt
        armorDetailPaint.style = Paint.Style.FILL
        armorDetailPaint.color = steelDark
        canvas.drawRect(pelvisX - 9f, pelvisY - 3f, pelvisX + 9f, pelvisY + 5f, armorDetailPaint)
        canvas.drawRect(pelvisX - 9f, pelvisY - 3f, pelvisX + 9f, pelvisY + 5f, limbOutlinePaint)

        // 4. Segmented Steel Plate Cuirass & Rivets
        armorFillPaint.color = steelMid
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, chestY + chestH, 4f, 4f, armorFillPaint)
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, chestY + chestH, 4f, 4f, limbOutlinePaint)
        // Central breastplate keel ridge
        limbHighlightPaint.color = Color.parseColor("#94A3B8")
        canvas.drawLine(chestX, chestY + 2f, chestX, chestY + chestH - 2f, limbHighlightPaint)
        // Brass rivets
        armorDetailPaint.color = goldAccent
        canvas.drawCircle(chestX - 6f, chestY + 4f, 1.4f, armorDetailPaint)
        canvas.drawCircle(chestX + 6f, chestY + 4f, 1.4f, armorDetailPaint)

        // Layered Curved Steel Pauldrons
        armorFillPaint.color = steelLight
        canvas.drawRoundRect(chestX - 13f, chestY - 1f, chestX - 6f, chestY + 8f, 3f, 3f, armorFillPaint)
        canvas.drawRoundRect(chestX - 13f, chestY - 1f, chestX - 6f, chestY + 8f, 3f, 3f, limbOutlinePaint)
        canvas.drawRoundRect(chestX + 6f, chestY - 1f, chestX + 13f, chestY + 8f, 3f, 3f, armorFillPaint)
        canvas.drawRoundRect(chestX + 6f, chestY - 1f, chestX + 13f, chestY + 8f, 3f, 3f, limbOutlinePaint)

        // 5. Lead Arm
        val leadShoulderX = chestX + dir * 8f
        val leadShoulderY = chestY + 3f
        val gripX = destRect.centerX() + dir * 6f
        val gripY = destRect.top + destRect.height() * 0.55f
        val leadElbowX = (leadShoulderX + gripX) * 0.5f + dir * 3f
        val leadElbowY = (leadShoulderY + gripY) * 0.5f + 2f

        limbFillPaint.color = steelMid
        drawCapsuleLimb(canvas, leadShoulderX, leadShoulderY, leadElbowX, leadElbowY, 7f, 6f)
        limbFillPaint.color = steelLight
        drawCapsuleLimb(canvas, leadElbowX, leadElbowY, gripX, gripY, 6f, 5.2f)

        // 6. Knight Sallet Helm with Radiant Visor Plume
        drawDeadCellsHead(canvas, headX, headY, dir, steelLight, steelMid, flameGold, flameCore, animTime, player.vx, isKnight = true)
    }

    // =========================================================================
    // 3. GRAVE EXECUTIONER (Dead Cells Style)
    // =========================================================================
    private fun renderDeadCellsExecutioner(
        canvas: Canvas,
        player: Player,
        cx: Float,
        pelvisX: Float,
        pelvisY: Float,
        chestX: Float,
        chestY: Float,
        chestW: Float,
        chestH: Float,
        headX: Float,
        headY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        animTime: Float
    ) {
        val darkLeather = Color.parseColor("#18181B")
        val crimsonKilt = Color.parseColor("#7F1D1D")
        val flameCrimson = Color.parseColor("#EF4444")
        val flameCore = Color.parseColor("#FCA5A5")

        // 1. Tattered Multi-tier Crimson Battle-Kilt (fluttering strips)
        drawTatteredKilt(canvas, pelvisX, pelvisY, feetY - 10f, dir, animTime, isRunning, crimsonKilt)

        // 2. Heavy Articulated Spiked Greaves
        renderArticulatedLegs(
            canvas, pelvisX, pelvisY, feetY, dir, runCycle, isRunning, isAirborne,
            thighColor = darkLeather, calfColor = Color.parseColor("#27272A"), bootColor = Color.parseColor("#09090B"),
            isSpiked = true
        )

        // 3. Rugged Muscular Torso with Studded Leather Harness
        armorFillPaint.color = darkLeather
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, chestY + chestH, 4f, 4f, armorFillPaint)
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, chestY + chestH, 4f, 4f, limbOutlinePaint)
        // Harness
        armorDetailPaint.style = Paint.Style.STROKE
        armorDetailPaint.strokeWidth = 2.4f
        armorDetailPaint.color = Color.parseColor("#451A03")
        canvas.drawLine(chestX - 8f, chestY + 2f, chestX + 8f, chestY + chestH - 2f, armorDetailPaint)
        canvas.drawLine(chestX + 8f, chestY + 2f, chestX - 8f, chestY + chestH - 2f, armorDetailPaint)
        armorDetailPaint.style = Paint.Style.FILL
        armorDetailPaint.color = Color.parseColor("#CA8A04")
        canvas.drawCircle(chestX, chestY + chestH * 0.5f, 2f, armorDetailPaint)

        // 4. Colossal Spiked Iron Pauldron on Dominant Shoulder
        val pauldronX = chestX + dir * 9f
        val pauldronY = chestY + 1f
        armorFillPaint.color = Color.parseColor("#27272A")
        canvas.drawCircle(pauldronX, pauldronY, 7f, armorFillPaint)
        canvas.drawCircle(pauldronX, pauldronY, 7f, limbOutlinePaint)
        // 3 Wicked Spikes
        limbOutlinePaint.strokeWidth = 2.5f
        canvas.drawLine(pauldronX, pauldronY - 3f, pauldronX + dir * 4f, pauldronY - 13f, limbOutlinePaint)
        canvas.drawLine(pauldronX + 3f, pauldronY - 1f, pauldronX + dir * 9f, pauldronY - 9f, limbOutlinePaint)
        canvas.drawLine(pauldronX - 3f, pauldronY - 1f, pauldronX - dir * 5f, pauldronY - 10f, limbOutlinePaint)
        limbOutlinePaint.strokeWidth = 2.2f

        // 5. Lead Arm
        val leadShoulderX = chestX + dir * 8f
        val leadShoulderY = chestY + 3f
        val gripX = destRect.centerX() + dir * 6f
        val gripY = destRect.top + destRect.height() * 0.55f
        val leadElbowX = (leadShoulderX + gripX) * 0.5f + dir * 3.5f
        val leadElbowY = (leadShoulderY + gripY) * 0.5f + 2f

        limbFillPaint.color = darkLeather
        drawCapsuleLimb(canvas, leadShoulderX, leadShoulderY, leadElbowX, leadElbowY, 7.5f, 6.2f)
        limbFillPaint.color = Color.parseColor("#27272A")
        drawCapsuleLimb(canvas, leadElbowX, leadElbowY, gripX, gripY, 6.2f, 5.5f)

        // 6. Executioner Horned Hood with Demonic Blood-Red Flame Plume
        drawDeadCellsHead(canvas, headX, headY, dir, darkLeather, Color.parseColor("#27272A"), flameCrimson, flameCore, animTime, player.vx, isExecutioner = true)
    }

    // =========================================================================
    // 4. ASTRAL SORCERER (Dead Cells Style)
    // =========================================================================
    private fun renderDeadCellsSorcerer(
        canvas: Canvas,
        player: Player,
        cx: Float,
        pelvisX: Float,
        pelvisY: Float,
        chestX: Float,
        chestY: Float,
        chestW: Float,
        chestH: Float,
        headX: Float,
        headY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        animTime: Float
    ) {
        val royalViolet = Color.parseColor("#2E1065")
        val mantlePurple = Color.parseColor("#581C87")
        val flameViolet = Color.parseColor("#C084FC")
        val flameCore = Color.parseColor("#F5D0FE")

        // 1. Billowing Starlight Mantle Cape
        drawFluidCape(canvas, chestX - dir * 4f, chestY + 2f, feetY - 6f, dir, mantlePurple, Color.parseColor("#F59E0B"), animTime, isRunning)

        // 2. Articulated Legs inside Split Robes
        renderArticulatedLegs(
            canvas, pelvisX, pelvisY, feetY, dir, runCycle, isRunning, isAirborne,
            thighColor = royalViolet, calfColor = Color.parseColor("#3B0764"), bootColor = Color.parseColor("#1E1B4B"),
            isTabi = false
        )

        // 3. Flowing Astral Robes & Gold Constellation Embroidery
        clothFillPaint.color = royalViolet
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, feetY - 8f, 5f, 5f, clothFillPaint)
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, feetY - 8f, 5f, 5f, limbOutlinePaint)
        // Gold rune line
        armorDetailPaint.style = Paint.Style.STROKE
        armorDetailPaint.strokeWidth = 1.6f
        armorDetailPaint.color = Color.parseColor("#F59E0B")
        canvas.drawLine(chestX, chestY + 4f, chestX, feetY - 12f, armorDetailPaint)
        armorDetailPaint.style = Paint.Style.FILL
        canvas.drawCircle(chestX, chestY + 12f, 1.6f, armorDetailPaint)
        canvas.drawCircle(chestX, chestY + 22f, 1.6f, armorDetailPaint)

        // 4. Floating Levitation Arcane Conduit Crystal at Throat
        val crystalY = chestY + 2f + sin(animTime * 7f) * 2f
        flameGlowPaint.color = Color.argb(160, 192, 132, 252)
        canvas.drawCircle(chestX, crystalY, 7f, flameGlowPaint)
        flameCorePaint.color = Color.parseColor("#C084FC")
        canvas.drawCircle(chestX, crystalY, 3.5f, flameCorePaint)

        // 5. Flared Wizard Sleeves & Lead Arm
        val leadShoulderX = chestX + dir * 7f
        val leadShoulderY = chestY + 3f
        val gripX = destRect.centerX() + dir * 6f
        val gripY = destRect.top + destRect.height() * 0.55f
        val leadElbowX = (leadShoulderX + gripX) * 0.5f + dir * 3.5f
        val leadElbowY = (leadShoulderY + gripY) * 0.5f + 2f

        limbFillPaint.color = royalViolet
        drawCapsuleLimb(canvas, leadShoulderX, leadShoulderY, leadElbowX, leadElbowY, 6.8f, 5.8f)
        limbFillPaint.color = Color.parseColor("#3B0764")
        drawCapsuleLimb(canvas, leadElbowX, leadElbowY, gripX, gripY, 5.8f, 4.8f)

        // 6. Pointed Celestial Hood & Swirling Astral Corona Flame
        drawDeadCellsHead(canvas, headX, headY, dir, royalViolet, Color.parseColor("#3B0764"), flameViolet, flameCore, animTime, player.vx, isSorcerer = true)
    }

    // =========================================================================
    // 5. THE FORSAKEN (Dead Cells Style)
    // =========================================================================
    private fun renderDeadCellsForsaken(
        canvas: Canvas,
        player: Player,
        cx: Float,
        pelvisX: Float,
        pelvisY: Float,
        chestX: Float,
        chestY: Float,
        chestW: Float,
        chestH: Float,
        headX: Float,
        headY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        animTime: Float
    ) {
        val burlapColor = Color.parseColor("#78350F")
        val wrapColor = Color.parseColor("#78716C")
        val flameAmber = Color.parseColor("#D97706")
        val flameCore = Color.parseColor("#FDE68A")

        // 1. Bedroll Pack Lashed to Back
        val packX = chestX - dir * 9f
        val packY = chestY + 5f
        clothFillPaint.color = Color.parseColor("#44403C")
        canvas.drawRoundRect(packX - 5.5f, packY - 8f, packX + 5.5f, packY + 8f, 3f, 3f, clothFillPaint)
        canvas.drawRoundRect(packX - 5.5f, packY - 8f, packX + 5.5f, packY + 8f, 3f, 3f, limbOutlinePaint)
        // Twine ropes
        armorDetailPaint.style = Paint.Style.STROKE
        armorDetailPaint.strokeWidth = 1.8f
        armorDetailPaint.color = Color.parseColor("#D97706")
        canvas.drawLine(packX - 5.5f, packY - 3f, packX + 5.5f, packY - 3f, armorDetailPaint)
        canvas.drawLine(packX - 5.5f, packY + 3f, packX + 5.5f, packY + 3f, armorDetailPaint)

        // 2. Articulated Legs in Bandage Wraps
        renderArticulatedLegs(
            canvas, pelvisX, pelvisY, feetY, dir, runCycle, isRunning, isAirborne,
            thighColor = burlapColor, calfColor = wrapColor, bootColor = Color.parseColor("#291309"),
            isTabi = false
        )

        // 3. Tattered Burlap Poncho with Fluttering Frayed Hem
        clothFillPaint.color = burlapColor
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, pelvisY + 4f, 4f, 4f, clothFillPaint)
        canvas.drawRoundRect(chestX - 9f, chestY, chestX + 9f, pelvisY + 4f, 4f, 4f, limbOutlinePaint)
        // Rope sash belt
        armorDetailPaint.style = Paint.Style.FILL
        armorDetailPaint.color = Color.parseColor("#B45309")
        canvas.drawRect(chestX - 9.5f, pelvisY - 2f, chestX + 9.5f, pelvisY + 2f, armorDetailPaint)

        // 4. Lead Arm
        val leadShoulderX = chestX + dir * 7f
        val leadShoulderY = chestY + 3f
        val gripX = destRect.centerX() + dir * 6f
        val gripY = destRect.top + destRect.height() * 0.55f
        val leadElbowX = (leadShoulderX + gripX) * 0.5f + dir * 3.5f
        val leadElbowY = (leadShoulderY + gripY) * 0.5f + 2f

        limbFillPaint.color = burlapColor
        drawCapsuleLimb(canvas, leadShoulderX, leadShoulderY, leadElbowX, leadElbowY, 6.5f, 5.5f)
        limbFillPaint.color = wrapColor
        drawCapsuleLimb(canvas, leadElbowX, leadElbowY, gripX, gripY, 5.5f, 4.5f)

        // 5. Frayed Wanderer Cowl with Warm Amber Soul Flame
        drawDeadCellsHead(canvas, headX, headY, dir, Color.parseColor("#54301A"), Color.parseColor("#78350F"), flameAmber, flameCore, animTime, player.vx, isForsaken = true)
    }

    // =========================================================================
    // Dead Cells Articulated Kinematics Helpers
    // =========================================================================

    private fun renderArticulatedLegs(
        canvas: Canvas,
        pelvisX: Float,
        pelvisY: Float,
        feetY: Float,
        dir: Float,
        runCycle: Float,
        isRunning: Boolean,
        isAirborne: Boolean,
        thighColor: Int,
        calfColor: Int,
        bootColor: Int,
        isTabi: Boolean = false,
        isSpiked: Boolean = false
    ) {
        val thighLen = 14f
        val calfLen = 15f

        // Two independent articulated legs with Dead Cells athletic bounding
        for (leg in 0..1) {
            val phase = if (isRunning) (runCycle + leg * PI.toFloat()) else (if (leg == 0) 0f else PI.toFloat() * 0.15f)
            val hipX = pelvisX + (if (leg == 0) -dir * 3f else dir * 3f)

            // Hip swing and knee flexion
            val hipSwing = if (isRunning) sin(phase) * 0.65f else (if (isAirborne) -0.2f else 0.08f)
            val kneeBend = if (isRunning) {
                if (sin(phase) > 0f) 0.95f + sin(phase) * 0.65f else 0.25f - sin(phase) * 0.25f
            } else if (isAirborne) {
                1.15f // Tuck knees in air
            } else {
                0.22f // Athletic knee flex in idle
            }

            val kneeX = hipX + sin(hipSwing) * thighLen * dir
            val kneeY = pelvisY + cos(hipSwing) * thighLen

            val shinAngle = hipSwing - kneeBend * dir
            val footX = kneeX + sin(shinAngle) * calfLen * dir
            val footY = minOf(feetY, kneeY + cos(shinAngle) * calfLen)

            // 1. Thigh capsule
            limbFillPaint.color = thighColor
            drawCapsuleLimb(canvas, hipX, pelvisY, kneeX, kneeY, 6.8f, 5.8f)

            // 2. Knee Cop / Plate
            armorFillPaint.color = calfColor
            canvas.drawCircle(kneeX, kneeY, 3.8f, armorFillPaint)
            canvas.drawCircle(kneeX, kneeY, 3.8f, limbOutlinePaint)
            if (isSpiked) {
                // Front spike on kneecap
                canvas.drawLine(kneeX, kneeY, kneeX + dir * 5.5f, kneeY - 2f, limbOutlinePaint)
            }

            // 3. Calf capsule
            limbFillPaint.color = calfColor
            drawCapsuleLimb(canvas, kneeX, kneeY, footX, footY, 5.8f, 4.8f)

            // 4. Foot / Boot
            armorFillPaint.color = bootColor
            val toeX = footX + dir * 6f
            canvas.drawRoundRect(footX - 3.5f, footY - 3f, toeX + 2f, footY + 1.5f, 2f, 2f, armorFillPaint)
            canvas.drawRoundRect(footX - 3.5f, footY - 3f, toeX + 2f, footY + 1.5f, 2f, 2f, limbOutlinePaint)
            if (isTabi) {
                // Split toe notch
                canvas.drawLine(toeX - 1f, footY - 2f, toeX + 1.5f, footY - 2f, limbOutlinePaint)
            }
        }
    }

    private fun drawCapsuleLimb(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        w1: Float,
        w2: Float
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len < 0.2f) return

        val nx = -dy / len
        val ny = dx / len

        limbPath.reset()
        limbPath.moveTo(x1 + nx * (w1 * 0.5f), y1 + ny * (w1 * 0.5f))
        limbPath.lineTo(x2 + nx * (w2 * 0.5f), y2 + ny * (w2 * 0.5f))
        limbPath.lineTo(x2 - nx * (w2 * 0.5f), y2 - ny * (w2 * 0.5f))
        limbPath.lineTo(x1 - nx * (w1 * 0.5f), y1 - ny * (w1 * 0.5f))
        limbPath.close()

        canvas.drawPath(limbPath, limbFillPaint)
        canvas.drawPath(limbPath, limbOutlinePaint)

        // Joint caps
        canvas.drawCircle(x1, y1, w1 * 0.48f, limbFillPaint)
        canvas.drawCircle(x2, y2, w2 * 0.48f, limbFillPaint)
    }

    private fun drawDeadCellsHead(
        canvas: Canvas,
        headX: Float,
        headY: Float,
        dir: Float,
        cowlColor: Int,
        wrapColor: Int,
        flameOuterColor: Int,
        flameCoreColor: Int,
        animTime: Float,
        vx: Float,
        isShinobi: Boolean = false,
        isKnight: Boolean = false,
        isExecutioner: Boolean = false,
        isSorcerer: Boolean = false,
        isForsaken: Boolean = false
    ) {
        val windLag = -dir * (if (abs(vx) > 10f) 8f else 2.5f)

        // 1. Ethereal Homunculus Flame Plume (Dead Cells Signature Burning Head)
        val flameFlicker1 = sin(animTime * 14f) * 3f
        val flameFlicker2 = cos(animTime * 11f) * 4f
        val flameHeight = 18f

        flamePath.reset()
        flamePath.moveTo(headX - 6f, headY)
        flamePath.quadTo(headX - 4f + windLag * 0.5f, headY - 8f, headX - 2f + windLag, headY - flameHeight + flameFlicker1)
        flamePath.quadTo(headX + windLag * 0.8f, headY - 12f, headX + 3f + windLag * 1.2f, headY - flameHeight - 4f + flameFlicker2)
        flamePath.quadTo(headX + 4f + windLag * 0.6f, headY - 6f, headX + 6f, headY)
        flamePath.close()

        flameOuterPaint.color = flameOuterColor
        canvas.drawPath(flamePath, flameOuterPaint)

        // Inner white-hot flame core
        flameCorePath.reset()
        flameCorePath.moveTo(headX - 3.5f, headY)
        flameCorePath.quadTo(headX - 1f + windLag * 0.6f, headY - 6f, headX + windLag * 0.9f, headY - flameHeight * 0.65f + flameFlicker1 * 0.5f)
        flameCorePath.quadTo(headX + 2f + windLag * 0.5f, headY - 4f, headX + 3.5f, headY)
        flameCorePath.close()

        flameCorePaint.color = flameCoreColor
        canvas.drawPath(flameCorePath, flameCorePaint)

        // Glowing Embers floating off plume
        flameGlowPaint.color = flameOuterColor
        val emberY1 = headY - flameHeight - 6f + (animTime * 18f % 16f)
        val emberY2 = headY - flameHeight - 12f + ((animTime * 22f + 8f) % 20f)
        canvas.drawCircle(headX + windLag * 1.5f + sin(animTime * 8f) * 4f, emberY1, 1.8f, flameGlowPaint)
        canvas.drawCircle(headX + windLag * 1.8f + cos(animTime * 9f) * 5f, emberY2, 1.4f, flameGlowPaint)

        // 2. Head Silhouette & Hood/Helmet Base
        armorFillPaint.color = cowlColor
        canvas.drawCircle(headX, headY, 8.8f, armorFillPaint)
        canvas.drawCircle(headX, headY, 8.8f, limbOutlinePaint)

        // 3. Class Specific Headgear Elements
        when {
            isShinobi -> {
                // Lower ninja wrap mask
                armorDetailPaint.style = Paint.Style.FILL
                armorDetailPaint.color = wrapColor
                canvas.drawRoundRect(headX - 8.5f, headY, headX + 8.5f, headY + 8.5f, 3f, 3f, armorDetailPaint)
                canvas.drawRoundRect(headX - 8.5f, headY, headX + 8.5f, headY + 8.5f, 3f, 3f, limbOutlinePaint)
                // Cyan piercing slit eyes
                val eyeX = headX + dir * 3.5f
                val eyeY = headY - 1.5f
                eyeGlowPaint.color = Color.argb(190, 0, 229, 255)
                canvas.drawCircle(eyeX, eyeY, 4.5f, eyeGlowPaint)
                eyeCorePaint.color = Color.parseColor("#00E5FF")
                canvas.drawRoundRect(eyeX - 3.2f, eyeY - 1.2f, eyeX + 3.2f, eyeY + 1.2f, 1f, 1f, eyeCorePaint)
            }
            isKnight -> {
                // Sallet neck flare & brass brow ridge
                canvas.drawRoundRect(headX - dir * 4f, headY + 3f, headX - dir * 11f, headY + 10f, 2f, 2f, armorFillPaint)
                canvas.drawRoundRect(headX - dir * 4f, headY + 3f, headX - dir * 11f, headY + 10f, 2f, 2f, limbOutlinePaint)
                armorDetailPaint.style = Paint.Style.FILL
                armorDetailPaint.color = Color.parseColor("#D97706")
                canvas.drawRect(headX - 8f, headY - 3f, headX + 8f, headY - 1f, armorDetailPaint)
                // Golden Visor Eye Slit
                val visorX = headX + dir * 3.5f
                val visorY = headY + 1f
                eyeGlowPaint.color = Color.argb(190, 245, 158, 11)
                canvas.drawCircle(visorX, visorY, 5f, eyeGlowPaint)
                eyeCorePaint.color = Color.parseColor("#FEF08A")
                canvas.drawRect(visorX - 4f, visorY - 1.4f, visorX + 4f, visorY + 1.4f, eyeCorePaint)
            }
            isExecutioner -> {
                // Twin sweeping curved bone horns
                limbOutlinePaint.strokeWidth = 3.2f
                limbOutlinePaint.color = Color.parseColor("#71717A")
                canvas.drawLine(headX - 4f, headY - 6f, headX - 9f, headY - 17f, limbOutlinePaint)
                canvas.drawLine(headX + 4f, headY - 6f, headX + 9f, headY - 17f, limbOutlinePaint)
                limbOutlinePaint.strokeWidth = 2.2f
                limbOutlinePaint.color = Color.parseColor("#090D16")
                // Blood-red burning eye
                val eyeX = headX + dir * 3f
                val eyeY = headY
                eyeGlowPaint.color = Color.argb(200, 239, 68, 68)
                canvas.drawCircle(eyeX, eyeY, 5f, eyeGlowPaint)
                eyeCorePaint.color = Color.parseColor("#EF4444")
                canvas.drawCircle(eyeX, eyeY, 2.5f, eyeCorePaint)
            }
            isSorcerer -> {
                // Pointed celestial hood tip
                capePath.reset()
                capePath.moveTo(headX - 8f, headY + 4f)
                capePath.lineTo(headX + 8f, headY + 4f)
                capePath.lineTo(headX - dir * 7f, headY - 17f)
                capePath.close()
                canvas.drawPath(capePath, armorFillPaint)
                canvas.drawPath(capePath, limbOutlinePaint)
                // Gold brim trim
                armorDetailPaint.style = Paint.Style.STROKE
                armorDetailPaint.strokeWidth = 2.0f
                armorDetailPaint.color = Color.parseColor("#F59E0B")
                canvas.drawLine(headX - 8f, headY + 3f, headX + 8f, headY + 3f, armorDetailPaint)
                // Pale celestial starlight eyes
                val eyeX = headX + dir * 3.5f
                val eyeY = headY + 1f
                eyeGlowPaint.color = Color.argb(190, 224, 242, 254)
                canvas.drawCircle(eyeX, eyeY, 5f, eyeGlowPaint)
                eyeCorePaint.color = Color.parseColor("#E0F2FE")
                canvas.drawCircle(eyeX, eyeY, 2.4f, eyeCorePaint)
            }
            isForsaken -> {
                // Frayed burlap ragged hood rim
                canvas.drawRect(headX - 8f, headY + 4f, headX + 8f, headY + 10f, armorFillPaint)
                canvas.drawRect(headX - 8f, headY + 4f, headX + 8f, headY + 10f, limbOutlinePaint)
                // Warm amber soul eye
                val eyeX = headX + dir * 3.5f
                val eyeY = headY + 1f
                eyeGlowPaint.color = Color.argb(190, 245, 158, 11)
                canvas.drawCircle(eyeX, eyeY, 5f, eyeGlowPaint)
                eyeCorePaint.color = Color.parseColor("#F59E0B")
                canvas.drawCircle(eyeX, eyeY, 2.5f, eyeCorePaint)
            }
        }
    }

    private fun drawFluidScarf(
        canvas: Canvas,
        anchorX: Float,
        anchorY: Float,
        dir: Float,
        length: Float,
        color1: Int,
        color2: Int,
        animTime: Float,
        isRunning: Boolean,
        isAirborne: Boolean
    ) {
        val trailDir = -dir
        val wave1 = sin(animTime * 13f) * (if (isRunning) 6f else 3.5f)
        val wave2 = cos(animTime * 11f) * (if (isRunning) 7f else 4f)

        val n1X = anchorX + trailDir * (length * 0.35f)
        val n1Y = anchorY + 4f + wave1
        val n2X = anchorX + trailDir * (length * 0.70f)
        val n2Y = anchorY + 10f + wave2 - (if (isAirborne) 6f else 0f)
        val n3X = anchorX + trailDir * length
        val n3Y = anchorY + 15f + sin(animTime * 15f) * 6f

        // Ribbon 1
        scarfPath.reset()
        scarfPath.moveTo(anchorX, anchorY)
        scarfPath.cubicTo(n1X, n1Y, n2X, n2Y, n3X, n3Y)
        scarfPath.lineTo(n3X + trailDir * 2f, n3Y + 4f)
        scarfPath.cubicTo(n2X, n2Y + 4f, n1X, n1Y + 3f, anchorX, anchorY + 5f)
        scarfPath.close()

        clothFillPaint.color = color1
        canvas.drawPath(scarfPath, clothFillPaint)
        canvas.drawPath(scarfPath, clothOutlinePaint)

        // Ribbon 2 (Lower dynamic tail)
        val r2Len = length * 0.75f
        scarfRibbon2Path.reset()
        scarfRibbon2Path.moveTo(anchorX, anchorY + 3f)
        scarfRibbon2Path.cubicTo(
            anchorX + trailDir * (r2Len * 0.35f), anchorY + 9f - wave1,
            anchorX + trailDir * (r2Len * 0.70f), anchorY + 16f - wave2,
            anchorX + trailDir * r2Len, anchorY + 22f + wave1
        )
        scarfRibbon2Path.lineTo(anchorX + trailDir * (r2Len + 2f), anchorY + 25f + wave1)
        scarfRibbon2Path.cubicTo(
            anchorX + trailDir * (r2Len * 0.70f), anchorY + 19f - wave2,
            anchorX + trailDir * (r2Len * 0.35f), anchorY + 12f - wave1,
            anchorX, anchorY + 6f
        )
        scarfRibbon2Path.close()

        clothFillPaint.color = color2
        canvas.drawPath(scarfRibbon2Path, clothFillPaint)
        canvas.drawPath(scarfRibbon2Path, clothOutlinePaint)
    }

    private fun drawFluidCape(
        canvas: Canvas,
        anchorX: Float,
        anchorY: Float,
        bottomY: Float,
        dir: Float,
        capeColor: Int,
        trimColor: Int,
        animTime: Float,
        isRunning: Boolean
    ) {
        val trailDir = -dir
        val capeWave = sin(animTime * 7f) * 4f
        val capeTrail = trailDir * (if (isRunning) 18f else 7f)

        capePath.reset()
        capePath.moveTo(anchorX, anchorY)
        capePath.quadTo(anchorX + capeTrail * 0.6f, anchorY + 16f + capeWave, anchorX + capeTrail, bottomY + capeWave)
        capePath.lineTo(anchorX + capeTrail * 0.5f, bottomY + capeWave + 3f)
        capePath.quadTo(anchorX + capeTrail * 0.2f, anchorY + 20f, anchorX + dir * 3f, anchorY + 8f)
        capePath.close()

        clothFillPaint.color = capeColor
        canvas.drawPath(capePath, clothFillPaint)
        canvas.drawPath(capePath, clothOutlinePaint)

        // Gold trim
        clothOutlinePaint.color = trimColor
        clothOutlinePaint.strokeWidth = 1.8f
        canvas.drawLine(anchorX + capeTrail * 0.5f, bottomY + capeWave + 3f, anchorX + capeTrail, bottomY + capeWave, clothOutlinePaint)
        clothOutlinePaint.color = Color.parseColor("#090D16")
        clothOutlinePaint.strokeWidth = 2.0f
    }

    private fun drawTatteredKilt(
        canvas: Canvas,
        pelvisX: Float,
        pelvisY: Float,
        kiltBottomY: Float,
        dir: Float,
        animTime: Float,
        isRunning: Boolean,
        kiltColor: Int
    ) {
        val wave = sin(animTime * 11f) * (if (isRunning) 5f else 2.5f)
        val trail = -dir * (if (isRunning) 8f else 2f)

        kiltPath.reset()
        kiltPath.moveTo(pelvisX - 9f, pelvisY - 3f)
        kiltPath.lineTo(pelvisX + 9f, pelvisY - 3f)
        kiltPath.lineTo(pelvisX + 11f + trail, kiltBottomY + wave)
        kiltPath.lineTo(pelvisX + 6f + trail * 0.7f, kiltBottomY - 4f)
        kiltPath.lineTo(pelvisX + trail * 0.5f, kiltBottomY + wave * 0.8f)
        kiltPath.lineTo(pelvisX - 5f + trail * 0.3f, kiltBottomY - 4f)
        kiltPath.lineTo(pelvisX - 11f + trail, kiltBottomY + wave * 0.5f)
        kiltPath.close()

        clothFillPaint.color = kiltColor
        canvas.drawPath(kiltPath, clothFillPaint)
        canvas.drawPath(kiltPath, clothOutlinePaint)
    }
}
