package com.example.shadowescape.renderer

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.Weapon
import com.example.shadowescape.model.WeaponType
import kotlin.math.PI
import kotlin.math.sin

class WeaponRenderer(assetManager: AssetManager? = null) {
    private data class WeaponSpriteSpec(
        val assetPath: String,
        val uprightRotDeg: Float,
        val gripX: Float,
        val gripY: Float,
        val drawSize: Float,
        val tintColor: Int? = null
    )

    private val spriteSpecs = mapOf(
        "katana" to WeaponSpriteSpec("weapons/pack_katana.png", 45f, 12f, 51f, 56f),
        "crimson_vein" to WeaponSpriteSpec("weapons/pack_crimson_vein.png", 45f, 22f, 39f, 54f),
        "obsidian_dagger" to WeaponSpriteSpec("weapons/pack_obsidian_dagger.png", 45f, 20f, 39f, 40f),
        "broadsword" to WeaponSpriteSpec("weapons/pack_broadsword.png", 45f, 12f, 51f, 54f),
        "iron_spear" to WeaponSpriteSpec("weapons/pack_iron_spear.png", 45f, 12f, 51f, 68f),
        "wyrm_wing_halberd" to WeaponSpriteSpec("weapons/pack_wyrm_wing_halberd.png", 45f, 12f, 43f, 66f),
        "battle_axe" to WeaponSpriteSpec("weapons/pack_battle_axe.png", 45f, 12f, 51f, 62f),
        "greatsword" to WeaponSpriteSpec("weapons/pack_greatsword.png", 45f, 16f, 49f, 72f),
        "pale_eclipse_gs" to WeaponSpriteSpec("weapons/pack_pale_eclipse_gs.png", 45f, 12f, 51f, 72f),
        "nightprowler_talon" to WeaponSpriteSpec("weapons/pack_nightprowler_talon.png", 45f, 12f, 51f, 66f),
        "war_hammer" to WeaponSpriteSpec("weapons/pack_war_hammer.png", 45f, 12f, 43f, 58f),
        "crystal_mace" to WeaponSpriteSpec("weapons/pack_crystal_mace.png", 45f, 18f, 41f, 52f),
        "club" to WeaponSpriteSpec("weapons/pack_club.png", 45f, 12f, 51f, 50f),
        "staff" to WeaponSpriteSpec("weapons/pack_staff.png", 45f, 12f, 49f, 64f),
        "sun_bow" to WeaponSpriteSpec("weapons/pack_sun_bow.png", 45f, 26f, 34f, 48f),
        "shadow_bone_bow" to WeaponSpriteSpec("weapons/pack_shadow_bone_bow.png", 45f, 26f, 34f, 48f),
        "soul_scythe" to WeaponSpriteSpec("weapons/pack_soul_scythe.png", 45f, 12f, 51f, 68f),
        "royal_scythe" to WeaponSpriteSpec("weapons/pack_royal_scythe.png", 45f, 12f, 51f, 68f),
        "molten_gauntlets" to WeaponSpriteSpec("weapons/pack_molten_gauntlets.png", 45f, 16f, 46f, 36f),
        "crystal_claws" to WeaponSpriteSpec("weapons/pack_crystal_claws.png", 45f, 16f, 46f, 36f)
    )

    private val loadedBitmaps = mutableMapOf<String, Bitmap>()
    private val tintedPaints = mutableMapOf<Int, Paint>()
    private val spritePaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }
    private val spriteSrcRect = Rect(0, 0, 64, 64)

    init {
        if (assetManager != null) {
            val uniquePaths = spriteSpecs.values.map { it.assetPath }.distinct()
            for (path in uniquePaths) {
                try {
                    assetManager.open(path).use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { bmp ->
                            loadedBitmaps[path] = bmp
                        }
                    }
                } catch (_: Exception) {}
            }
            for (spec in spriteSpecs.values) {
                val tint = spec.tintColor
                if (tint != null && !tintedPaints.containsKey(tint)) {
                    tintedPaints[tint] = Paint().apply {
                        isFilterBitmap = false
                        isAntiAlias = false
                        colorFilter = LightingColorFilter(tint, 0)
                    }
                }
            }
        }
    }

    // Zero-allocation reusable paints for 60 FPS combat rendering
    private val bladePaint = Paint().apply { isAntiAlias = true }
    private val bladeEdgePaint = Paint().apply { isAntiAlias = true }
    private val bladeFullerPaint = Paint().apply { isAntiAlias = true }
    private val bladeGlowPaint = Paint().apply { isAntiAlias = true }
    private val guardPaint = Paint().apply { isAntiAlias = true }
    private val hiltPaint = Paint().apply { isAntiAlias = true }
    private val pommelPaint = Paint().apply { isAntiAlias = true }
    private val wrapPaint = Paint().apply { isAntiAlias = true }
    private val handGlovePaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        isAntiAlias = true
    }
    private val handKnucklePaint = Paint().apply {
        color = Color.parseColor("#334155")
        isAntiAlias = true
    }
    private val runePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Reusable drawing paths & rects
    private val bladePath = Path()
    private val edgePath = Path()
    private val tempRect = RectF()

    fun draw(canvas: Canvas, player: Player, destRect: RectF) {
        val weapon = player.equippedWeapon
        val dir = if (player.facingRight) 1f else -1f
        val isAttacking = player.isAttacking
        val p = player.attackProgress

        // Compute natural grip origin in character's hand
        var handX = destRect.centerX() + (dir * 6f)
        var handY = destRect.top + (destRect.height() * 0.55f)
        var weaponAngle: Float

        if (isAttacking) {
            // Compute dynamic attack swing angle & lunging hand offsets
            val (startAngle, endAngle) = getAttackSwingAngles(weapon.type, player.attackComboIndex)
            val t = ((p - 0.10f) / 0.80f).coerceIn(0f, 1f)
            val sweepFactor = sin(t * (PI.toFloat() / 2f))
            val baseAngle = startAngle + (endAngle - startAngle) * sweepFactor

            weaponAngle = if (dir > 0) baseAngle else (180f - baseAngle)

            // Dynamic hand displacement during swing
            val lunge = sin(p * PI.toFloat())
            when (weapon.type) {
                WeaponType.KATANA -> {
                    handX += dir * (10f * lunge)
                    handY += when (player.attackComboIndex) {
                        2 -> -8f * lunge // rising uppercut
                        3 -> 4f * lunge
                        else -> 0f
                    }
                }
                WeaponType.SWORD -> {
                    if (player.attackComboIndex == 3) {
                        // Thrust: lock angle straight forward, lunge deep
                        weaponAngle = if (dir > 0) 0f else 180f
                        handX += dir * (24f * lunge)
                        handY += 2f * lunge
                    } else {
                        handX += dir * (12f * lunge)
                        handY += if (player.attackComboIndex == 2) -6f * lunge else 0f
                    }
                }
                WeaponType.GREATSWORD -> {
                    // Colossal two-handed commitment
                    handX += dir * (16f * lunge)
                    handY += if (player.attackComboIndex == 1) (10f * lunge) else (-12f * lunge)
                }
                WeaponType.CLUB -> {
                    handX += dir * (14f * lunge)
                    handY += 8f * lunge
                }
                WeaponType.STAFF -> {
                    // Spell thrust straight forward
                    weaponAngle = if (dir > 0) -8f else 188f
                    handX += dir * (16f * lunge)
                    handY += -4f * lunge
                }
                WeaponType.BOW -> {
                    // Draw bow forward at shoulder height
                    weaponAngle = if (dir > 0) 0f else 180f
                    handX += dir * 14f
                    handY += -4f
                }
                WeaponType.SCYTHE -> {
                    handX += dir * (16f * lunge)
                    handY += if (player.attackComboIndex == 1) (6f * lunge) else (-8f * lunge)
                }
                WeaponType.GAUNTLET -> {
                    weaponAngle = if (dir > 0) 0f else 180f
                    handX += dir * (18f * lunge)
                    handY += if (player.attackComboIndex % 2 == 0) -5f * lunge else 3f * lunge
                }
            }
        } else {
            // Passive carry poses (Idle, Running, Jumping, Falling)
            val animTime = player.animTime
            when (weapon.type) {
                WeaponType.GREATSWORD -> {
                    // Iconic Dark Souls Colossal Greatsword shoulder carry!
                    handX = destRect.centerX() + (dir * 3f)
                    handY = destRect.top + (destRect.height() * 0.46f)
                    val shoulderBaseAngle = -56f
                    val sway = if (player.vx != 0f) sin(animTime * 9f) * 4f else sin(animTime * 3f) * 1.5f
                    val angle = shoulderBaseAngle + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.KATANA -> {
                    // Ready Iaido hip posture (blade angled down-backward)
                    handX = destRect.centerX() - (dir * 2f)
                    handY = destRect.top + (destRect.height() * 0.58f)
                    val sway = if (player.vx != 0f) -12f else sin(animTime * 3f) * 1.5f
                    val angle = 36f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.SWORD -> {
                    // One-handed relaxed low ready (blade angled forward-down)
                    handX = destRect.centerX() + (dir * 7f)
                    handY = destRect.top + (destRect.height() * 0.56f)
                    val sway = if (player.vx != 0f) sin(animTime * 10f) * 6f else sin(animTime * 3f) * 2f
                    val angle = 22f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.CLUB -> {
                    // Resting on shoulder
                    handX = destRect.centerX() + (dir * 4f)
                    handY = destRect.top + (destRect.height() * 0.48f)
                    val sway = if (player.vx != 0f) sin(animTime * 8f) * 5f else sin(animTime * 2.5f) * 2f
                    val angle = -48f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.STAFF -> {
                    // Upright walking conduit with levitating focus
                    handX = destRect.centerX() + (dir * 9f)
                    handY = destRect.top + (destRect.height() * 0.58f)
                    val sway = if (player.vx != 0f) sin(animTime * 6f) * 3f else sin(animTime * 2f) * 1.5f
                    val angle = -84f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.BOW -> {
                    handX = destRect.centerX() + (dir * 8f)
                    handY = destRect.top + (destRect.height() * 0.50f)
                    val sway = if (player.vx != 0f) sin(animTime * 8f) * 4f else sin(animTime * 2.5f) * 1.5f
                    val angle = 15f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.SCYTHE -> {
                    handX = destRect.centerX() + (dir * 2f)
                    handY = destRect.top + (destRect.height() * 0.46f)
                    val sway = if (player.vx != 0f) sin(animTime * 8f) * 4f else sin(animTime * 2.5f) * 1.5f
                    val angle = -62f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
                WeaponType.GAUNTLET -> {
                    handX = destRect.centerX() + (dir * 10f)
                    handY = destRect.top + (destRect.height() * 0.56f)
                    val sway = if (player.vx != 0f) sin(animTime * 11f) * 8f else sin(animTime * 3f) * 2f
                    val angle = 8f + sway
                    weaponAngle = if (dir > 0) angle else (180f - angle)
                }
            }
        }

        // Render the weapon model positioned and rotated at hand
        canvas.save()
        canvas.translate(handX, handY)
        canvas.rotate(weaponAngle)

        val spec = spriteSpecs[weapon.id]
        val spriteBmp = spec?.let { loadedBitmaps[it.assetPath] }
        if (spec != null && spriteBmp != null) {
            canvas.save()
            if (dir < 0f) {
                canvas.scale(1f, -1f)
            }
            canvas.rotate(spec.uprightRotDeg)
            val s = spec.drawSize / 64f
            val left = -spec.gripX * s
            val top = -spec.gripY * s
            tempRect.set(left, top, left + spec.drawSize, top + spec.drawSize)
            val drawPaint = spec.tintColor?.let { tintedPaints[it] } ?: spritePaint
            canvas.drawBitmap(spriteBmp, spriteSrcRect, tempRect, drawPaint)
            canvas.restore()
        } else {
            when (weapon.type) {
                WeaponType.KATANA -> renderKatana(canvas, weapon)
                WeaponType.SWORD -> renderSword(canvas, weapon)
                WeaponType.GREATSWORD -> renderGreatsword(canvas, weapon)
                WeaponType.CLUB -> renderClub(canvas, weapon)
                WeaponType.STAFF -> renderStaff(canvas, weapon, player.animTime)
                WeaponType.BOW -> renderSword(canvas, weapon)
                WeaponType.SCYTHE -> renderGreatsword(canvas, weapon)
                WeaponType.GAUNTLET -> renderClub(canvas, weapon)
            }
        }

        // Render character gauntlet/glove holding the grip firmly (both hands if 2-handed!)
        renderGripHand(canvas, weapon.isTwoHanded)

        canvas.restore()
    }

    fun drawIcon(canvas: Canvas, weaponId: String, dst: RectF): Boolean {
        val spec = spriteSpecs[weaponId] ?: return false
        val bmp = loadedBitmaps[spec.assetPath] ?: return false
        val drawPaint = spec.tintColor?.let { tintedPaints[it] } ?: spritePaint
        canvas.drawBitmap(bmp, spriteSrcRect, dst, drawPaint)
        return true
    }

    private fun getAttackSwingAngles(type: WeaponType, comboIndex: Int): Pair<Float, Float> {
        return when (type) {
            WeaponType.KATANA -> when (comboIndex) {
                1 -> Pair(-38f, 24f)  // Fast horizontal cut
                2 -> Pair(48f, -68f)  // Rising low-to-high cut
                else -> Pair(-115f, 45f) // Overhead executioner slash
            }
            WeaponType.SWORD -> when (comboIndex) {
                1 -> Pair(-46f, 32f)  // Diagonal cleave
                2 -> Pair(42f, -54f)  // Rising backhand slice
                else -> Pair(0f, 0f)   // Straight forward lunging thrust
            }
            WeaponType.GREATSWORD -> when (comboIndex) {
                1 -> Pair(-135f, 68f) // Slow colossal downward slam
                2 -> Pair(75f, -118f) // Massive upward earth-cleaving scoop
                else -> Pair(-150f, 82f) // Blazing overhead guillotine finisher
            }
            WeaponType.CLUB -> when (comboIndex) {
                1 -> Pair(-75f, 36f)  // Heavy diagonal bash
                else -> Pair(-125f, 65f) // Two-handed ground concussion
            }
            WeaponType.STAFF -> Pair(-10f, 4f) // Astral beam thrust
            WeaponType.BOW -> Pair(-4f, 4f) // Bow draw
            WeaponType.SCYTHE -> when (comboIndex) {
                1 -> Pair(-105f, 52f)
                else -> Pair(65f, -95f)
            }
            WeaponType.GAUNTLET -> Pair(-8f, 8f) // Rapid jab/hook
        }
    }

    private fun renderKatana(canvas: Canvas, weapon: Weapon) {
        val isScarlet = weapon.id == "crimson_vein"
        val bladeLen = 34f

        // 1. Tsuka (Handle)
        hiltPaint.color = Color.parseColor("#090D16")
        canvas.drawRoundRect(-11f, -1.8f, 0f, 1.8f, 1f, 1f, hiltPaint)

        // Diamond tsuka-ito wrap highlights
        wrapPaint.color = if (isScarlet) Color.parseColor("#DC2626") else Color.parseColor("#F59E0B")
        for (i in 0..1) {
            val wx = -8f + (i * 4f)
            canvas.drawCircle(wx, 0f, 1.0f, wrapPaint)
        }

        // 2. Tsuba (Handguard)
        guardPaint.color = if (isScarlet) Color.parseColor("#991B1B") else Color.parseColor("#D97706")
        canvas.drawOval(-1.2f, -4.2f, 2.0f, 4.2f, guardPaint)

        // 3. Curved Katana Blade
        bladePath.reset()
        bladePath.moveTo(2f, -1.6f)
        bladePath.lineTo(bladeLen * 0.7f, -1.4f)
        bladePath.lineTo(bladeLen, -0.5f) // Kissaki needle point
        bladePath.lineTo(bladeLen * 0.85f, 1.4f)
        bladePath.lineTo(2f, 1.6f)
        bladePath.close()

        bladePaint.color = if (isScarlet) Color.parseColor("#450A0A") else Color.parseColor("#1E293B")
        canvas.drawPath(bladePath, bladePaint)

        // Razor Hamon cutting edge
        edgePath.reset()
        edgePath.moveTo(2f, 1.6f)
        edgePath.lineTo(bladeLen * 0.85f, 1.4f)
        edgePath.lineTo(bladeLen, -0.5f)

        bladeEdgePaint.style = Paint.Style.STROKE
        bladeEdgePaint.strokeWidth = 1.2f
        bladeEdgePaint.color = if (isScarlet) Color.parseColor("#EF4444") else Color.parseColor("#00E5FF")
        canvas.drawPath(edgePath, bladeEdgePaint)

        // Glowing blade aura for Crimson Vein or Shadow Katana
        bladeGlowPaint.style = Paint.Style.STROKE
        bladeGlowPaint.strokeWidth = 2.4f
        bladeGlowPaint.color = if (isScarlet) Color.argb(90, 239, 68, 68) else Color.argb(70, 0, 229, 255)
        canvas.drawPath(edgePath, bladeGlowPaint)
    }

    private fun renderSword(canvas: Canvas, weapon: Weapon) {
        val isHalberd = weapon.id == "wyrm_wing_halberd"
        val bladeLen = if (isHalberd) 40f else 30f

        // 1. Pommel
        pommelPaint.color = if (isHalberd) Color.parseColor("#F59E0B") else Color.parseColor("#94A3B8")
        canvas.drawCircle(-11f, 0f, 2.2f, pommelPaint)

        // 2. Leather Grip
        hiltPaint.color = Color.parseColor("#451A03")
        canvas.drawRoundRect(-10f, -1.6f, 0f, 1.6f, 1f, 1f, hiltPaint)

        // 3. Crossguard
        guardPaint.color = if (isHalberd) Color.parseColor("#D97706") else Color.parseColor("#64748B")
        canvas.drawRoundRect(0f, -6f, 2.8f, 6f, 1f, 1f, guardPaint)

        // 4. Double-edged Blade
        bladePath.reset()
        bladePath.moveTo(2.8f, -2.5f)
        bladePath.lineTo(bladeLen - 6f, -2.2f)
        bladePath.lineTo(bladeLen, 0f) // Sharp point
        bladePath.lineTo(bladeLen - 6f, 2.2f)
        bladePath.lineTo(2.8f, 2.5f)
        bladePath.close()

        bladePaint.color = if (isHalberd) Color.parseColor("#CBD5E1") else Color.parseColor("#E2E8F0")
        canvas.drawPath(bladePath, bladePaint)

        // Polished center fuller channel
        bladeFullerPaint.color = Color.parseColor("#94A3B8")
        canvas.drawLine(4f, 0f, bladeLen - 8f, 0f, bladeFullerPaint)

        // Glowing dragon wing gold highlights if Halberd
        if (isHalberd) {
            bladeGlowPaint.style = Paint.Style.STROKE
            bladeGlowPaint.strokeWidth = 2.0f
            bladeGlowPaint.color = Color.argb(100, 245, 158, 11)
            canvas.drawPath(bladePath, bladeGlowPaint)
        }
    }

    private fun renderGreatsword(canvas: Canvas, weapon: Weapon) {
        val isDarkmoon = weapon.id == "pale_eclipse_gs"
        val isBloodhound = weapon.id == "nightprowler_talon"
        val bladeLen = 44f
        val bladeWidth = 4.8f

        // 1. Heavy Counterweight Pommel
        pommelPaint.color = if (isDarkmoon) Color.parseColor("#0284C7") else Color.parseColor("#475569")
        canvas.drawCircle(-14f, 0f, 2.8f, pommelPaint)

        // 2. Extended Two-Handed Grip
        hiltPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(-13f, -2.0f, 0f, 2.0f, 1.2f, 1.2f, hiltPaint)
        wrapPaint.color = if (isDarkmoon) Color.parseColor("#38BDF8") else Color.parseColor("#78716C")
        for (i in 0..2) {
            val wx = -11f + (i * 3.5f)
            canvas.drawLine(wx, -1.8f, wx + 1.2f, 1.8f, wrapPaint)
        }

        // 3. Forged Crossguard
        guardPaint.color = if (isDarkmoon) Color.parseColor("#0369A1") else Color.parseColor("#334155")
        canvas.drawRoundRect(0f, -8f, 3.8f, 8f, 1.5f, 1.5f, guardPaint)

        // 4. Colossal Blade Slab
        bladePath.reset()
        if (isBloodhound) {
            // Curved notched crescent greatsword
            bladePath.moveTo(3.8f, -bladeWidth)
            bladePath.lineTo(bladeLen * 0.6f, -bladeWidth * 1.2f)
            bladePath.lineTo(bladeLen * 0.8f, -bladeWidth * 0.7f)
            bladePath.lineTo(bladeLen, 0f)
            bladePath.lineTo(bladeLen * 0.75f, bladeWidth * 1.4f)
            bladePath.lineTo(3.8f, bladeWidth)
        } else if (isDarkmoon) {
            // Majestic consecrated moonlight blade
            bladePath.moveTo(3.8f, -bladeWidth)
            bladePath.lineTo(bladeLen - 8f, -bladeWidth)
            bladePath.lineTo(bladeLen, 0f)
            bladePath.lineTo(bladeLen - 8f, bladeWidth)
            bladePath.lineTo(3.8f, bladeWidth)
        } else {
            // Executioner Blade (Massive square-tipped guillotine slab)
            bladePath.moveTo(3.8f, -bladeWidth)
            bladePath.lineTo(bladeLen, -bladeWidth * 0.85f)
            bladePath.lineTo(bladeLen + 2f, bladeWidth * 0.85f)
            bladePath.lineTo(3.8f, bladeWidth)
        }
        bladePath.close()

        if (isDarkmoon) {
            // Frosty translucent moonlight crystal shader
            bladePaint.color = Color.parseColor("#38BDF8")
            canvas.drawPath(bladePath, bladePaint)

            // Inner starlight core
            bladeEdgePaint.style = Paint.Style.STROKE
            bladeEdgePaint.strokeWidth = 1.5f
            bladeEdgePaint.color = Color.parseColor("#F0F9FF")
            canvas.drawPath(bladePath, bladeEdgePaint)

            // Ambient pale cyan glow
            bladeGlowPaint.style = Paint.Style.STROKE
            bladeGlowPaint.strokeWidth = 3.5f
            bladeGlowPaint.color = Color.argb(90, 56, 189, 248)
            canvas.drawPath(bladePath, bladeGlowPaint)
        } else {
            // Dark refined iron slab with ground-cracking bevel
            bladePaint.color = Color.parseColor("#1E293B")
            canvas.drawPath(bladePath, bladePaint)

            // Sharp hardened silver bevel edge
            bladeEdgePaint.style = Paint.Style.STROKE
            bladeEdgePaint.strokeWidth = 1.5f
            bladeEdgePaint.color = Color.parseColor("#CBD5E1")
            canvas.drawPath(bladePath, bladeEdgePaint)

            // Central blood groove
            bladeFullerPaint.color = Color.parseColor("#0F172A")
            canvas.drawLine(6f, 0f, bladeLen - 9f, 0f, bladeFullerPaint)
        }
    }

    private fun renderClub(canvas: Canvas, weapon: Weapon) {
        val clubLen = 28f

        // 1. Twine Grip
        hiltPaint.color = Color.parseColor("#451A03")
        canvas.drawRoundRect(-8f, -1.8f, 0f, 1.8f, 1f, 1f, hiltPaint)
        wrapPaint.color = Color.parseColor("#D97706")
        canvas.drawLine(-6f, -1.5f, -4f, 1.5f, wrapPaint)
        canvas.drawLine(-3f, -1.5f, -1f, 1.5f, wrapPaint)

        // 2. Gnarled Tapering Oak Trunk
        bladePath.reset()
        bladePath.moveTo(0f, -1.8f)
        bladePath.lineTo(clubLen * 0.5f, -3.2f)
        bladePath.lineTo(clubLen, -4.8f)
        bladePath.lineTo(clubLen + 2.5f, 0f)
        bladePath.lineTo(clubLen, 4.8f)
        bladePath.lineTo(clubLen * 0.5f, 3.2f)
        bladePath.lineTo(0f, 1.8f)
        bladePath.close()

        bladePaint.color = Color.parseColor("#78350F")
        canvas.drawPath(bladePath, bladePaint)

        // Wood grain bark lines
        bladeFullerPaint.color = Color.parseColor("#451A03")
        canvas.drawLine(6f, -0.8f, clubLen - 5f, -1.5f, bladeFullerPaint)
        canvas.drawLine(9f, 1f, clubLen - 4f, 1.8f, bladeFullerPaint)

        // Iron spikes/studs driven through the club head
        pommelPaint.color = Color.parseColor("#334155")
        canvas.drawCircle(clubLen - 8f, -2.8f, 1.5f, pommelPaint)
        canvas.drawCircle(clubLen - 4f, 2.5f, 1.6f, pommelPaint)
        canvas.drawCircle(clubLen - 10f, 1.5f, 1.4f, pommelPaint)
    }

    private fun renderStaff(canvas: Canvas, weapon: Weapon, animTime: Float) {
        val staffLen = 36f

        // 1. Dark Cosmic Wood Shaft
        hiltPaint.color = Color.parseColor("#1E1B4B")
        canvas.drawRoundRect(-14f, -1.5f, staffLen - 6f, 1.5f, 1f, 1f, hiltPaint)

        // Metallic golden reinforcing rings
        wrapPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(-6f, -2f, -4f, 2f, wrapPaint)
        canvas.drawRect(5f, -2f, 7f, 2f, wrapPaint)
        canvas.drawRect(staffLen - 10f, -2.2f, staffLen - 8f, 2.2f, wrapPaint)

        // 2. Astral Crescent Head
        val headX = staffLen - 3f
        guardPaint.color = Color.parseColor("#7C3AED")
        guardPaint.style = Paint.Style.STROKE
        guardPaint.strokeWidth = 2.2f
        canvas.drawArc(headX - 6f, -7f, headX + 6f, 7f, 45f, 270f, false, guardPaint)

        // 3. Floating Celestial Crystal Core
        val pulse = sin(animTime * 6f) * 1.0f
        bladePaint.style = Paint.Style.FILL
        bladePaint.color = Color.parseColor("#C084FC")
        canvas.drawCircle(headX, 0f, 3.2f + pulse * 0.3f, bladePaint)

        // Concentric astral rune ring
        runePaint.color = Color.argb(160, 192, 132, 252)
        canvas.drawCircle(headX, 0f, 5.5f + pulse * 0.7f, runePaint)
    }

    private fun renderGripHand(canvas: Canvas, isTwoHanded: Boolean = false) {
        // Primary lead hand wrapping the hilt
        canvas.drawRoundRect(-4f, -4f, 4f, 4f, 2f, 2f, handGlovePaint)
        canvas.drawRoundRect(-2f, -3.5f, 3f, 3.5f, 1.5f, 1.5f, handKnucklePaint)

        // For heavy two-handed weapons, render supporting secondary hand on the shaft/grip!
        if (isTwoHanded) {
            canvas.drawRoundRect(-11f, -3.5f, -4f, 3.5f, 2f, 2f, handGlovePaint)
            canvas.drawRoundRect(-9.5f, -3f, -5f, 3f, 1.5f, 1.5f, handKnucklePaint)
        }
    }
}
