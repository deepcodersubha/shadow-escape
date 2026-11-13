package com.example.shadowescape.animation

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.RectF
import com.example.shadowescape.model.Enemy
import com.example.shadowescape.model.EnemyType
import kotlin.math.abs
import kotlin.math.sin

private class MonsterSpriteSet(
    val idle: SpriteAnimation,
    val move: SpriteAnimation,
    val attack1: SpriteAnimation,
    val attack2: SpriteAnimation,
    val hit: SpriteAnimation,
    val death: SpriteAnimation,
    val special: SpriteAnimation? = null
)

class EnemyAnimator(assetManager: AssetManager) {

    private val spriteSets = mutableMapOf<String, MonsterSpriteSet>()
    private val destRect = RectF()

    private val normalPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    private val hurtPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(255, 200, 200), Color.rgb(130, 35, 35))
    }

    private val elitePaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(255, 215, 185), Color.rgb(42, 10, 12))
    }

    // Phase 1 Molten Slime Paints (pre-allocated)
    private val slimeBodyPaint = Paint().apply {
        color = Color.parseColor("#B44418")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val slimeCorePaint = Paint().apply {
        color = Color.parseColor("#EA580C")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val slimeRimPaint = Paint().apply {
        color = Color.parseColor("#431407")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val slimeEyePaint = Paint().apply {
        color = Color.parseColor("#FACC15")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val slimeRect = RectF()

    init {
        loadSet(
            assetManager,
            key = "skeleton",
            dir = "monsters/skeleton",
            idleName = "idle.png", idleFrames = 4,
            moveName = "walk.png", moveFrames = 4,
            atk1Frames = 8,
            atk2Frames = 8,
            hitFrames = 4,
            deathFrames = 4,
            specialName = "shield.png", specialFrames = 4
        )
        loadSet(
            assetManager,
            key = "goblin",
            dir = "monsters/goblin",
            idleName = "idle.png", idleFrames = 4,
            moveName = "run.png", moveFrames = 8,
            atk1Frames = 8,
            atk2Frames = 8,
            hitFrames = 4,
            deathFrames = 4
        )
        loadSet(
            assetManager,
            key = "mushroom",
            dir = "monsters/mushroom",
            idleName = "idle.png", idleFrames = 4,
            moveName = "run.png", moveFrames = 8,
            atk1Frames = 8,
            atk2Frames = 8,
            hitFrames = 4,
            deathFrames = 4
        )
        loadSet(
            assetManager,
            key = "flying_eye",
            dir = "monsters/flying_eye",
            idleName = "flight.png", idleFrames = 8,
            moveName = "flight.png", moveFrames = 8,
            atk1Frames = 8,
            atk2Frames = 8,
            hitFrames = 4,
            deathFrames = 4
        )
        loadCustomSet(
            assetManager,
            key = "demon_slime",
            dir = "monsters/demon_slime",
            fw = 288, fh = 160,
            idleFrames = 6,
            moveFrames = 12,
            atkFrames = 15,
            hitFrames = 5,
            deathFrames = 22
        )
    }

    private fun loadCustomSet(
        assets: AssetManager,
        key: String,
        dir: String,
        fw: Int,
        fh: Int,
        idleFrames: Int,
        moveFrames: Int,
        atkFrames: Int,
        hitFrames: Int,
        deathFrames: Int,
        moveName: String = "walk.png"
    ) {
        fun loadCustom(name: String, count: Int, fps: Float, looping: Boolean): SpriteAnimation? {
            return try {
                assets.open("$dir/$name").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) SpriteAnimation(bmp, fw, fh, count, fps, looping) else null
                }
            } catch (_: Exception) {
                null
            }
        }
        val idle = loadCustom("idle.png", idleFrames, 8f, true) ?: return
        val move = loadCustom(moveName, moveFrames, 12f, true) ?: idle
        val atk = loadCustom("attack.png", atkFrames, 16f, false) ?: idle
        val hit = loadCustom("hit.png", hitFrames, 14f, false) ?: idle
        val death = loadCustom("death.png", deathFrames, 13f, false) ?: idle
        spriteSets[key] = MonsterSpriteSet(idle, move, atk, atk, hit, death, null)
    }

    private fun loadStrip(
        assets: AssetManager,
        path: String,
        frameCount: Int,
        fps: Float,
        looping: Boolean
    ): SpriteAnimation? {
        return try {
            assets.open(path).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) SpriteAnimation(bmp, 150, 150, frameCount, fps, looping) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadSet(
        assets: AssetManager,
        key: String,
        dir: String,
        idleName: String,
        idleFrames: Int,
        moveName: String,
        moveFrames: Int,
        atk1Frames: Int,
        atk2Frames: Int,
        hitFrames: Int,
        deathFrames: Int,
        specialName: String? = null,
        specialFrames: Int = 4
    ) {
        val idle = loadStrip(assets, "$dir/$idleName", idleFrames, 8f, true) ?: return
        val move = loadStrip(assets, "$dir/$moveName", moveFrames, 11f, true) ?: idle
        val atk1 = loadStrip(assets, "$dir/attack.png", atk1Frames, 16f, false) ?: idle
        val atk2 = loadStrip(assets, "$dir/attack2.png", atk2Frames, 16f, false) ?: atk1
        val hit = loadStrip(assets, "$dir/hit.png", hitFrames, 14f, false) ?: idle
        val death = loadStrip(assets, "$dir/death.png", deathFrames, 9f, false) ?: idle
        val special = specialName?.let { loadStrip(assets, "$dir/$it", specialFrames, 9f, true) }

        spriteSets[key] = MonsterSpriteSet(idle, move, atk1, atk2, hit, death, special)
    }

    fun draw(canvas: Canvas, enemy: Enemy, alpha: Int = 255): Boolean {
        if (enemy.type == EnemyType.SLIME && !enemy.isDemonBossPhase) {
            drawPhase1Slime(canvas, enemy, alpha)
            return true
        }

        val key = when (enemy.type) {
            EnemyType.SKELETON, EnemyType.HOLLOW_SOLDIER, EnemyType.ELITE_GUARDIAN -> "skeleton"
            EnemyType.GOBLIN -> "goblin"
            EnemyType.MUSHROOM -> "mushroom"
            EnemyType.FLYING_EYE -> "flying_eye"
            EnemyType.SLIME -> "demon_slime"
        }
        val set = spriteSets[key] ?: return false

        val isDemonBoss = enemy.type == EnemyType.SLIME && enemy.isDemonBossPhase
        val scale = when {
            isDemonBoss -> 1.50f
            enemy.type == EnemyType.ELITE_GUARDIAN -> 2.10f
            enemy.type == EnemyType.SKELETON || enemy.type == EnemyType.HOLLOW_SOLDIER -> 1.68f
            enemy.type == EnemyType.GOBLIN -> 1.88f
            enemy.type == EnemyType.MUSHROOM -> 1.90f
            else -> 1.85f
        }

        val eb = enemy.bounds
        val halfFrame = if (isDemonBoss) 144f * scale else 75f * scale

        if (isDemonBoss) {
            val footOffset = 158f * scale
            destRect.set(
                eb.centerX - halfFrame,
                eb.bottom - footOffset,
                eb.centerX + halfFrame,
                eb.bottom + (2f * scale)
            )
        } else if (enemy.isFlying) {
            // Flying Eye body is vertically centered around y = 76 inside the 150x150 frame
            val centerYOffset = 76f * scale
            val dropOffset = if (!enemy.isAlive) (enemy.deathFadeTimer * 48f).coerceAtMost(28f) else 0f
            destRect.set(
                eb.centerX - halfFrame,
                eb.centerY - centerYOffset + dropOffset,
                eb.centerX + halfFrame,
                eb.centerY - centerYOffset + 150f * scale + dropOffset
            )
        } else {
            // Grounded creatures have feet anchored at y = 100 in each 150x150 frame
            val footOffset = 100f * scale
            destRect.set(
                eb.centerX - halfFrame,
                eb.bottom - footOffset,
                eb.centerX + halfFrame,
                eb.bottom + (50f * scale)
            )
        }

        val flipH = if (isDemonBoss) enemy.facingRight else !enemy.facingRight

        val activePaint = when {
            enemy.hurtTimer > 0f -> hurtPaint
            enemy.type == EnemyType.ELITE_GUARDIAN -> elitePaint
            else -> normalPaint
        }
        activePaint.alpha = alpha.coerceIn(0, 255)

        val anim: SpriteAnimation
        val frame: Int

        when {
            !enemy.isAlive -> {
                anim = set.death
                val progress = (enemy.deathFadeTimer / 0.55f).coerceIn(0f, 1f)
                frame = (progress * (anim.frameCount - 0.01f)).toInt().coerceIn(0, anim.frameCount - 1)
            }
            enemy.hurtTimer > 0f && !enemy.isAttacking -> {
                anim = set.hit
                val progress = (1f - (enemy.hurtTimer / 0.20f)).coerceIn(0f, 1f)
                frame = (progress * (anim.frameCount - 0.01f)).toInt().coerceIn(0, anim.frameCount - 1)
            }
            enemy.isStaggered -> {
                anim = set.special ?: set.hit
                frame = if (set.special != null) {
                    ((enemy.animTime * 6f).toInt()) % anim.frameCount
                } else {
                    (anim.frameCount - 1).coerceAtLeast(0)
                }
            }
            enemy.attackWindupTimer > 0f -> {
                anim = selectAttackAnim(enemy, set)
                val progress = (1f - (enemy.attackWindupTimer / enemy.windupDuration)).coerceIn(0f, 1f)
                val windupEnd = (anim.frameCount / 2).coerceAtLeast(1)
                frame = (progress * windupEnd).toInt().coerceIn(0, windupEnd - 1)
            }
            enemy.isAttacking -> {
                anim = selectAttackAnim(enemy, set)
                val progress = (1f - (enemy.attackTimer / enemy.attackDuration)).coerceIn(0f, 1f)
                val swingStart = (anim.frameCount / 2).coerceAtLeast(1)
                val remaining = (anim.frameCount - swingStart).coerceAtLeast(1)
                frame = (swingStart + (progress * remaining).toInt()).coerceIn(swingStart, anim.frameCount - 1)
            }
            abs(enemy.vx) > 8f || enemy.isFlying -> {
                anim = set.move
                val speedFps = if (enemy.isAggroed && !enemy.isFlying) anim.fps * 1.25f else anim.fps
                frame = ((enemy.animTime * speedFps).toInt()) % anim.frameCount
            }
            else -> {
                anim = set.idle
                frame = ((enemy.animTime * anim.fps).toInt()) % anim.frameCount
            }
        }

        anim.drawFrame(canvas, destRect, frame, flipH, activePaint)
        return true
    }

    private fun selectAttackAnim(enemy: Enemy, set: MonsterSpriteSet): SpriteAnimation {
        return if (enemy.type == EnemyType.ELITE_GUARDIAN || enemy.attackVariant == 1) {
            set.attack2
        } else {
            set.attack1
        }
    }

    private fun drawPhase1Slime(canvas: Canvas, enemy: Enemy, alpha: Int) {
        val eb = enemy.bounds
        val wobble = sin((enemy.animTime * (if (abs(enemy.vx) > 8f) 11f else 5f)).toDouble()).toFloat()
        val squashX = 1f + wobble * 0.10f
        val squashY = 1f - wobble * 0.10f
        val deathScale = if (!enemy.isAlive) (1f - enemy.deathFadeTimer / 0.8f).coerceIn(0f, 1f) else 1f
        val w = eb.width * squashX * (1f + (1f - deathScale) * 0.35f)
        val h = eb.height * squashY * deathScale

        slimeRect.set(eb.centerX - w / 2f, eb.bottom - h, eb.centerX + w / 2f, eb.bottom)

        val a = alpha.coerceIn(0, 255)
        slimeBodyPaint.color = if (enemy.hurtTimer > 0f) Color.parseColor("#FCA5A5") else Color.parseColor("#B44418")
        slimeBodyPaint.alpha = a
        slimeCorePaint.alpha = a
        slimeRimPaint.alpha = a
        slimeEyePaint.alpha = a

        canvas.drawRoundRect(slimeRect, w * 0.42f, h * 0.55f, slimeBodyPaint)
        canvas.drawRoundRect(
            slimeRect.left + 5f,
            slimeRect.top + 5f,
            slimeRect.right - 5f,
            slimeRect.bottom - 4f,
            w * 0.35f,
            h * 0.45f,
            slimeCorePaint
        )
        canvas.drawRoundRect(slimeRect, w * 0.42f, h * 0.55f, slimeRimPaint)

        if (enemy.isAlive) {
            val dir = if (enemy.facingRight) 1f else -1f
            val eyeY = slimeRect.top + h * 0.44f
            val eyeCenter = eb.centerX + dir * 6f
            canvas.drawCircle(eyeCenter - 5f, eyeY, 2.8f, slimeEyePaint)
            canvas.drawCircle(eyeCenter + 5f, eyeY, 2.8f, slimeEyePaint)
        }
    }
}
