package com.example.shadowescape.animation

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.Weapon
import com.example.shadowescape.model.WeaponType

private data class SlashConfig(
    val sheetKey: String,
    val scaleX: Float,
    val scaleY: Float,
    val forwardOffset: Float,
    val verticalOffset: Float = -4f,
    val rotationDeg: Float = 0f,
    val invertVertical: Boolean = false
)

class SlashAnimator(assetManager: AssetManager) {

    private val sheets = mutableMapOf<String, SpriteAnimation>()
    private val destRect = RectF()
    private val slashPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    init {
        loadSlash(assetManager, "blue_slash_thin", "slashes/blue_slash_thin.png", 6)
        loadSlash(assetManager, "blue_slash_wide", "slashes/blue_slash_wide.png", 6)
        loadSlash(assetManager, "purple_slash_thin", "slashes/purple_slash_thin.png", 6)
        loadSlash(assetManager, "purple_slash_wide", "slashes/purple_slash_wide.png", 6)
        loadSlash(assetManager, "red_slash_thin", "slashes/red_slash_thin.png", 6)
        loadSlash(assetManager, "red_slash_wide", "slashes/red_slash_wide.png", 6)
        loadSlash(assetManager, "white_slash_thin", "slashes/white_slash_thin.png", 6)
        loadSlash(assetManager, "white_slash_wide", "slashes/white_slash_wide.png", 6)
        loadSlash(assetManager, "pure_white_slash_thin", "slashes/pure_white_slash_thin.png", 6)
        loadSlash(assetManager, "pure_white_slash_wide", "slashes/pure_white_slash_wide.png", 6)
        loadSlash(assetManager, "blue_group_slashes", "slashes/blue_group_slashes.png", 20)
        loadSlash(assetManager, "red_yellow_group_slashes", "slashes/red_yellow_group_slashes.png", 20)
        loadSlash(assetManager, "white_group_slashes", "slashes/white_group_slashes.png", 20)
    }

    private fun loadSlash(assets: AssetManager, key: String, path: String, frameCount: Int) {
        try {
            assets.open(path).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    sheets[key] = SpriteAnimation(bmp, 220, 160, frameCount, 24f, false)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun drawAttackSlash(canvas: Canvas, player: Player): Boolean {
        val weapon = player.equippedWeapon
        val cfg = resolveAttackConfig(weapon, player.attackComboIndex)
        val anim = sheets[cfg.sheetKey] ?: return false

        val p = player.attackProgress.coerceIn(0f, 1f)
        val frame = (p * (anim.frameCount - 0.01f)).toInt().coerceIn(0, anim.frameCount - 1)
        val pb = player.bounds
        val dir = if (player.facingRight) 1f else -1f

        val baseW = 158f * cfg.scaleX
        val baseH = 115f * cfg.scaleY
        val centerX = pb.centerX + dir * cfg.forwardOffset
        val centerY = pb.centerY + cfg.verticalOffset

        destRect.set(
            centerX - baseW / 2f,
            centerY - baseH / 2f,
            centerX + baseW / 2f,
            centerY + baseH / 2f
        )

        slashPaint.alpha = when {
            p < 0.08f -> (p / 0.08f * 255f).toInt().coerceIn(0, 255)
            p > 0.85f -> ((1f - p) / 0.15f * 255f).toInt().coerceIn(0, 255)
            else -> 255
        }

        canvas.save()
        if (cfg.rotationDeg != 0f) {
            canvas.rotate(cfg.rotationDeg * dir, centerX, centerY)
        }
        if (cfg.invertVertical) {
            canvas.scale(1f, -1f, centerX, centerY)
        }
        anim.drawFrame(canvas, destRect, frame, flipHorizontal = !player.facingRight, paint = slashPaint)
        canvas.restore()
        return true
    }

    fun drawSkillSlash(canvas: Canvas, player: Player): Boolean {
        val (key, scale, forwardOffset) = when (player.activeSkillId) {
            "crimson_vein" -> Triple("red_yellow_group_slashes", 1.55f, 48f)
            "greatsword", "battle_axe", "war_hammer" -> Triple("red_yellow_group_slashes", 1.65f, 46f)
            "katana", "pale_eclipse_gs", "staff" -> Triple("blue_group_slashes", 1.50f, 38f)
            "wyrm_wing_halberd", "broadsword", "obsidian_dagger", "iron_spear" -> Triple("white_group_slashes", 1.48f, 38f)
            else -> Triple("white_group_slashes", 1.45f, 32f)
        }
        val anim = sheets[key] ?: return false

        val p = player.skillProgress.coerceIn(0f, 1f)
        val frame = (p * (anim.frameCount - 0.01f)).toInt().coerceIn(0, anim.frameCount - 1)
        val pb = player.bounds
        val dir = if (player.facingRight) 1f else -1f

        val baseW = 180f * scale
        val baseH = 132f * scale
        val centerX = pb.centerX + dir * forwardOffset
        val centerY = pb.centerY - 6f

        destRect.set(
            centerX - baseW / 2f,
            centerY - baseH / 2f,
            centerX + baseW / 2f,
            centerY + baseH / 2f
        )

        slashPaint.alpha = when {
            p < 0.08f -> (p / 0.08f * 255f).toInt().coerceIn(0, 255)
            p > 0.88f -> ((1f - p) / 0.12f * 255f).toInt().coerceIn(0, 255)
            else -> 255
        }

        canvas.save()
        anim.drawFrame(canvas, destRect, frame, flipHorizontal = !player.facingRight, paint = slashPaint)
        canvas.restore()
        return true
    }

    private fun resolveAttackConfig(weapon: Weapon, combo: Int): SlashConfig {
        val upSlash = (combo == 2)
        val heavyFinisher = (combo >= 3)

        return when (weapon.id) {
            "obsidian_dagger" -> SlashConfig(
                sheetKey = if (heavyFinisher) "blue_slash_thin" else "white_slash_thin",
                scaleX = if (heavyFinisher) 0.80f else 0.70f,
                scaleY = if (heavyFinisher) 0.82f else 0.72f,
                forwardOffset = 26f,
                rotationDeg = if (upSlash) -12f else 6f,
                invertVertical = upSlash
            )
            "crimson_vein" -> SlashConfig(
                sheetKey = if (heavyFinisher) "red_slash_wide" else "red_slash_thin",
                scaleX = if (heavyFinisher) 1.24f else 1.12f,
                scaleY = if (heavyFinisher) 1.20f else 1.04f,
                forwardOffset = 38f,
                rotationDeg = if (upSlash) -10f else 8f,
                invertVertical = upSlash
            )
            "katana" -> SlashConfig(
                sheetKey = if (heavyFinisher) "purple_slash_wide" else "purple_slash_thin",
                scaleX = if (heavyFinisher) 1.20f else 1.10f,
                scaleY = if (heavyFinisher) 1.16f else 1.02f,
                forwardOffset = 36f,
                rotationDeg = if (upSlash) -10f else 6f,
                invertVertical = upSlash
            )
            "iron_spear" -> SlashConfig(
                sheetKey = if (heavyFinisher) "pure_white_slash_thin" else "blue_slash_thin",
                scaleX = 1.38f,
                scaleY = 0.56f,
                forwardOffset = 54f,
                rotationDeg = if (upSlash) -6f else 4f,
                invertVertical = upSlash
            )
            "broadsword" -> SlashConfig(
                sheetKey = if (heavyFinisher) "white_slash_wide" else "white_slash_thin",
                scaleX = if (heavyFinisher) 1.22f else 1.08f,
                scaleY = if (heavyFinisher) 1.18f else 1.06f,
                forwardOffset = 36f,
                rotationDeg = if (upSlash) -8f else 8f,
                invertVertical = upSlash
            )
            "wyrm_wing_halberd" -> SlashConfig(
                sheetKey = if (upSlash) "blue_slash_thin" else "white_slash_wide",
                scaleX = 1.34f,
                scaleY = if (upSlash) 1.05f else 1.24f,
                forwardOffset = 46f,
                rotationDeg = if (upSlash) -12f else 6f,
                invertVertical = upSlash
            )
            "nightprowler_talon" -> SlashConfig(
                sheetKey = "purple_slash_wide",
                scaleX = 1.30f,
                scaleY = 1.24f,
                forwardOffset = 40f,
                rotationDeg = if (upSlash) -12f else 8f,
                invertVertical = upSlash
            )
            "pale_eclipse_gs" -> SlashConfig(
                sheetKey = "blue_slash_wide",
                scaleX = 1.46f,
                scaleY = 1.38f,
                forwardOffset = 48f,
                rotationDeg = if (upSlash) -10f else 10f,
                invertVertical = upSlash
            )
            "battle_axe" -> SlashConfig(
                sheetKey = "red_slash_wide",
                scaleX = 1.34f,
                scaleY = 1.30f,
                forwardOffset = 42f,
                rotationDeg = if (upSlash) -8f else 12f,
                invertVertical = upSlash
            )
            "greatsword" -> SlashConfig(
                sheetKey = "pure_white_slash_wide",
                scaleX = 1.44f,
                scaleY = 1.36f,
                forwardOffset = 46f,
                rotationDeg = if (upSlash) -8f else 12f,
                invertVertical = upSlash
            )
            "war_hammer" -> SlashConfig(
                sheetKey = "pure_white_slash_wide",
                scaleX = 1.26f,
                scaleY = 1.22f,
                forwardOffset = 38f,
                rotationDeg = if (upSlash) -6f else 14f,
                invertVertical = upSlash
            )
            "crystal_mace" -> SlashConfig(
                sheetKey = "purple_slash_wide",
                scaleX = 1.08f,
                scaleY = 1.08f,
                forwardOffset = 32f,
                invertVertical = upSlash
            )
            else -> when (weapon.type) {
                WeaponType.KATANA -> SlashConfig("purple_slash_thin", 1.08f, 1.02f, 34f, invertVertical = upSlash)
                WeaponType.SWORD -> SlashConfig("white_slash_thin", 1.10f, 1.08f, 36f, invertVertical = upSlash)
                WeaponType.GREATSWORD -> SlashConfig("white_slash_wide", 1.38f, 1.30f, 44f, invertVertical = upSlash)
                WeaponType.CLUB -> SlashConfig("white_slash_wide", 1.05f, 1.05f, 32f, invertVertical = upSlash)
                WeaponType.STAFF -> SlashConfig("blue_slash_thin", 0.92f, 0.94f, 28f, invertVertical = upSlash)
                WeaponType.BOW -> SlashConfig("white_slash_thin", 0.85f, 0.70f, 32f, invertVertical = upSlash)
                WeaponType.SCYTHE -> SlashConfig("purple_slash_wide", 1.32f, 1.24f, 42f, invertVertical = upSlash)
                WeaponType.GAUNTLET -> SlashConfig("red_slash_thin", 0.88f, 0.88f, 24f, invertVertical = upSlash)
            }
        }
    }
}
