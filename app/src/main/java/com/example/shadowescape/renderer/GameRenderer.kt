package com.example.shadowescape.renderer

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.shadowescape.animation.EnemyAnimator
import com.example.shadowescape.animation.PlayerAnimator
import com.example.shadowescape.animation.SlashAnimator
import com.example.shadowescape.model.ChestState
import com.example.shadowescape.model.ChestType
import com.example.shadowescape.model.ItemRarity
import com.example.shadowescape.model.LootReward
import com.example.shadowescape.model.MagicProjectile
import com.example.shadowescape.model.Particle
import com.example.shadowescape.model.PlatformType
import com.example.shadowescape.model.Player
import com.example.shadowescape.model.TreasureChest
import com.example.shadowescape.model.WeaponType
import com.example.shadowescape.world.Level
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(assetManager: AssetManager) {
    val playerAnimator: PlayerAnimator = PlayerAnimator(assetManager)
    val enemyAnimator: EnemyAnimator = EnemyAnimator(assetManager)
    val slashAnimator: SlashAnimator = SlashAnimator(assetManager)
    private val tilesetRenderer: TilesetRenderer = TilesetRenderer(assetManager)

    // Reusable Dynamic Light Mask Bitmaps
    private var lightBitmap: Bitmap? = null
    private var lightCanvas: Canvas? = null
    private val screenRect = RectF()
    private val lightDestRect = RectF()
    private val weaponBadgeIconRect = RectF()
    private val lightScale: Float = 0.5f // Half resolution for 60fps GPU performance
    private var globalTime: Float = 0f

    // Screen shake variables
    private var shakeTimer: Float = 0f
    private var shakeIntensity: Float = 0f

    fun triggerScreenShake(duration: Float, intensity: Float) {
        shakeTimer = duration
        shakeIntensity = intensity
    }

    // Pre-rendered Light Cookie (0 allocations per frame)
    private val lightCookieBitmap: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
        val c = Canvas(this)
        val p = Paint().apply {
            isAntiAlias = true
            isDither = true
            shader = RadialGradient(
                128f, 128f, 128f,
                intArrayOf(Color.WHITE, Color.argb(190, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(128f, 128f, 128f, p)
    }

    // Pre-rendered Bloom Cookies for zero-allocation colored light
    private fun createBloomCookie(color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply {
            isAntiAlias = true
            isDither = true
            shader = RadialGradient(
                64f, 64f, 64f,
                intArrayOf(color, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(64f, 64f, 64f, p)
        return bmp
    }

    private val playerLanternCookie: Bitmap = createBloomCookie(Color.argb(35, 56, 189, 248))
    private val cyanBloomCookie: Bitmap = createBloomCookie(Color.argb(70, 0, 229, 255))
    private val amberBloomCookie: Bitmap = createBloomCookie(Color.argb(60, 245, 158, 11))
    private val purpleBloomCookie: Bitmap = createBloomCookie(Color.argb(85, 124, 58, 237))
    private val redBloomCookie: Bitmap = createBloomCookie(Color.argb(45, 239, 68, 68))

    // Light Cutout Paint (Punches soft light through the ambient darkness)
    private val lightCutoutPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Subtle visibility cutout around enemies so pixel sprites stay legible in shadow
    private val enemyLightCutoutPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        alpha = 165
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Light Blending Paint
    private val lightBlendPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    // Soft Atmospheric Bloom & Glow Halo Paint
    private val bloomPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Dead Cells Combat visual effects paints (Crescents, Ribbons, Cores, Glints)
    private val crescentAuraPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val crescentCorePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }
    private val crescentEdgePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val bladeGlintPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    // Abyssal Void & Crystal Needle VFX (matching reference video combo animations)
    private val voidAuraPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.parseColor("#180424") // Deep obsidian void shadow
    }
    private val needlePath = Path()
    private val needlePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val needleOutlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        color = Color.parseColor("#180424")
    }
    private val shadowSmokePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Souls-like Pause Menu & Pause Button Paints
    private val pauseOverlayPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 7, 10, 18)
        isAntiAlias = true
    }
    private val pauseModalBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0C111E")
        isAntiAlias = true
    }
    private val pauseModalBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }
    private val pauseModalGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.argb(45, 56, 189, 248)
        isAntiAlias = true
    }
    private val pauseBtnBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#66101726")
        isAntiAlias = true
    }
    private val pauseBtnBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }
    private val pauseBtnIconPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }
    private val pauseTitlePaint = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        textSize = 23f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val pauseSubTitlePaint = Paint().apply {
        color = Color.parseColor("#7DD3FC")
        textSize = 11f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val pauseItemTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 15.5f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val pauseItemSubPaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 10.5f
        isAntiAlias = true
    }
    private val pauseBadgePaint = Paint().apply {
        color = Color.parseColor("#F59E0B")
        textSize = 11f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }
    private val pauseDividerPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#334155")
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    // Astral Spell sigil paints
    private val spellCorePaint = Paint().apply { color = Color.parseColor("#FFFFFF"); isAntiAlias = true }
    private val spellAuraPaint = Paint().apply {
        color = Color.parseColor("#C084FC")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val spellNodePaint = Paint().apply { color = Color.parseColor("#00E5FF"); isAntiAlias = true }
    private val spellWavePaint = Paint().apply {
        color = Color.parseColor("#6638BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Zero-allocation Combat Spark Particle System
    private class CombatSpark {
        var active: Boolean = false
        var x: Float = 0f
        var y: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f
        var life: Float = 0f
        var maxLife: Float = 0.22f
        var strokeWidth: Float = 3f
        var r: Int = 255
        var g: Int = 255
        var b: Int = 255
    }
    private val sparkPool = Array(60) { CombatSpark() }
    private var sparkIndex = 0
    private val sparkPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private fun emitSpark(x: Float, y: Float, vx: Float, vy: Float, r: Int, g: Int, b: Int, strokeWidth: Float = 3f, maxLife: Float = 0.22f) {
        val s = sparkPool[sparkIndex]
        sparkIndex = (sparkIndex + 1) % sparkPool.size
        s.active = true
        s.x = x
        s.y = y
        s.vx = vx
        s.vy = vy
        s.r = r
        s.g = g
        s.b = b
        s.strokeWidth = strokeWidth
        s.maxLife = maxLife
        s.life = maxLife
    }

    // Roll ghost trail paint
    private val rollTrailPaint = Paint().apply {
        color = Color.parseColor("#3338BDF8")
        isAntiAlias = true
    }

    // Cinematic rich dark twilight sky gradient
    private val skyPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, 1200f,
            intArrayOf(Color.parseColor("#080D18"), Color.parseColor("#121A2C"), Color.parseColor("#0A0F1D")),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val bgSilhouettePaint = Paint().apply { color = Color.parseColor("#0E1624"); isAntiAlias = true }
    private val midSilhouettePaint = Paint().apply { color = Color.parseColor("#152033"); isAntiAlias = true }

    // Platform paints with crisp edges & subtle rim light
    private val platformCorePaint = Paint().apply { color = Color.parseColor("#141926"); isAntiAlias = true }
    private val platformTopPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val platformSubTopPaint = Paint().apply {
        color = Color.parseColor("#334155")
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val platformShadowPaint = Paint().apply { color = Color.parseColor("#080A10"); isAntiAlias = true }

    // One-Way Semi-Solid Scaffold Platform paints
    private val oneWayPlankPaint = Paint().apply { color = Color.parseColor("#1B2230"); isAntiAlias = true }
    private val oneWayTopRimPaint = Paint().apply {
        color = Color.parseColor("#F59E0B") // Amber/brass top rim
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val oneWayBracketPaint = Paint().apply {
        color = Color.parseColor("#475569")
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val oneWayRunePaint = Paint().apply {
        color = Color.parseColor("#44F59E0B")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    // Ground Pound Shockwave System (Zero Allocation)
    private class GroundPoundVfx {
        var active: Boolean = false
        var x: Float = 0f
        var y: Float = 0f
        var timer: Float = 0f
        val maxDuration: Float = 0.38f
    }
    private val groundPoundVfxPool = Array(4) { GroundPoundVfx() }
    private var groundPoundIndex = 0

    fun triggerGroundPoundVfx(x: Float, y: Float) {
        val v = groundPoundVfxPool[groundPoundIndex]
        groundPoundIndex = (groundPoundIndex + 1) % groundPoundVfxPool.size
        v.active = true
        v.x = x
        v.y = y
        v.timer = v.maxDuration
    }

    private val groundPoundShockRingPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        isAntiAlias = true
    }
    private val groundPoundCorePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Weapon Skill Special VFX Paints
    private val skillAuraPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val skillLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Hazard Spikes paints
    private val spikeFillPaint = Paint().apply { color = Color.parseColor("#2B0F16"); isAntiAlias = true }
    private val spikeEdgePaint = Paint().apply {
        color = Color.parseColor("#DC2626")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val spikeGlowPaint = Paint().apply { color = Color.parseColor("#44EF4444"); isAntiAlias = true }

    // Interactive Switch paints
    private val switchBasePaint = Paint().apply { color = Color.parseColor("#1E2433"); isAntiAlias = true }
    private val switchBorderPaint = Paint().apply {
        color = Color.parseColor("#475569")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val switchActiveCore = Paint().apply { color = Color.parseColor("#00E5FF"); isAntiAlias = true }
    private val switchInactiveCore = Paint().apply { color = Color.parseColor("#F59E0B"); isAntiAlias = true }

    // Iron gate / Door paints
    private val doorFramePaint = Paint().apply { color = Color.parseColor("#171B26"); isAntiAlias = true }
    private val doorBarPaint = Paint().apply {
        color = Color.parseColor("#2D3446")
        strokeWidth = 4f
        isAntiAlias = true
    }

    // Pushable Box paints
    private val boxPaint = Paint().apply { color = Color.parseColor("#222838"); isAntiAlias = true }
    private val boxBorderPaint = Paint().apply {
        color = Color.parseColor("#4A5568")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val boxCrossPaint = Paint().apply {
        color = Color.parseColor("#364052")
        strokeWidth = 2.5f
        isAntiAlias = true
    }

    // Pressure plate paints
    private val plateBasePaint = Paint().apply { color = Color.parseColor("#1E2433"); isAntiAlias = true }
    private val plateTopActive = Paint().apply { color = Color.parseColor("#00E5FF"); strokeWidth = 5f; isAntiAlias = true }
    private val plateTopInactive = Paint().apply { color = Color.parseColor("#64748B"); strokeWidth = 4f; isAntiAlias = true }

    // Checkpoint paints
    private val checkpointPolePaint = Paint().apply { color = Color.parseColor("#334155"); strokeWidth = 5f; isAntiAlias = true }
    private val checkpointActivePaint = Paint().apply { color = Color.parseColor("#00E5FF"); isAntiAlias = true }
    private val checkpointInactivePaint = Paint().apply { color = Color.parseColor("#64748B"); isAntiAlias = true }

    // Exit portal paints
    private val exitPaint = Paint().apply { color = Color.parseColor("#7C3AED"); isAntiAlias = true }
    private val exitGlowPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // Glowing eyes piercing the shadows
    private val playerEyeCorePaint = Paint().apply { color = Color.parseColor("#00E5FF"); isAntiAlias = true }
    private val playerEyeGlowPaint = Paint().apply { color = Color.parseColor("#6638BDF8"); isAntiAlias = true }

    // Procedural player fallback
    private val playerBodyPaint = Paint().apply { color = Color.parseColor("#080B12"); isAntiAlias = true }
    private val playerRimPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val particlePaint = Paint().apply { isAntiAlias = true }

    // Souls HUD paints (HP, Mana, Stamina, Essence)
    private val barBgPaint = Paint().apply { color = Color.parseColor("#990B0E14"); isAntiAlias = true }
    private val barFramePaint = Paint().apply {
        color = Color.parseColor("#BB334155")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val hpFillPaint = Paint().apply { color = Color.parseColor("#DC2626"); isAntiAlias = true }
    private val hpSheenPaint = Paint().apply { color = Color.parseColor("#EF4444"); strokeWidth = 2f; isAntiAlias = true }

    private val manaFillPaint = Paint().apply { color = Color.parseColor("#2563EB"); isAntiAlias = true }
    private val manaSheenPaint = Paint().apply { color = Color.parseColor("#60A5FA"); strokeWidth = 2f; isAntiAlias = true }

    private val staminaFillPaint = Paint().apply { color = Color.parseColor("#16A34A"); isAntiAlias = true }
    private val staminaSheenPaint = Paint().apply { color = Color.parseColor("#4ADE80"); strokeWidth = 2f; isAntiAlias = true }

    private val hudStatTextPaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 18f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val essenceIconPaint = Paint().apply { color = Color.parseColor("#38BDF8"); isAntiAlias = true }
    private val essenceTextPaint = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        textSize = 20f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Weapon badge card
    private val weaponCardPaint = Paint().apply { color = Color.parseColor("#CC0F172A"); isAntiAlias = true }
    private val weaponCardBorder = Paint().apply {
        color = Color.parseColor("#8838BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val weaponNamePaint = Paint().apply {
        color = Color.parseColor("#F1F5F9")
        textSize = 19f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val weaponDmgPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 16f
        isAntiAlias = true
    }

    private val notifPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Enemy & Combat Paints
    private val enemyBodyPaint = Paint().apply { color = Color.parseColor("#0F172A"); isAntiAlias = true }
    private val enemyArmorPaint = Paint().apply { color = Color.parseColor("#334155"); strokeWidth = 3f; isAntiAlias = true }
    private val enemyEyeCorePaint = Paint().apply { color = Color.parseColor("#EF4444"); isAntiAlias = true }
    private val enemyEyeGlowPaint = Paint().apply { color = Color.parseColor("#66EF4444"); isAntiAlias = true }
    private val enemyHurtPaint = Paint().apply { color = Color.parseColor("#F8FAFC"); isAntiAlias = true }
    private val enemyStaggerPaint = Paint().apply { color = Color.parseColor("#F59E0B"); isAntiAlias = true }
    private val enemyBladeSlashPaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val enemyHpBarBg = Paint().apply { color = Color.parseColor("#990B0E14"); isAntiAlias = true }
    private val enemyHpBarFill = Paint().apply { color = Color.parseColor("#EF4444"); isAntiAlias = true }
    private val enemyPoiseBarFill = Paint().apply { color = Color.parseColor("#F59E0B"); isAntiAlias = true }

    // Healing & Flask Paints
    private val healingAuraPaint = Paint().apply { isAntiAlias = true }
    private val crimsonFlaskTextPaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        textSize = 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val ceruleanFlaskTextPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Treasure Chest Paints
    private val chestBasePaint = Paint().apply { isAntiAlias = true }
    private val chestBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val chestLockPaint = Paint().apply { isAntiAlias = true }
    private val chestBeamPaint = Paint().apply { isAntiAlias = true }
    private val chestPromptPaint = Paint().apply {
        color = Color.parseColor("#F59E0B")
        textSize = 12f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val chestPromptBgPaint = Paint().apply {
        color = Color.parseColor("#CC0B0E14")
        isAntiAlias = true
    }

    // Souls Bloodstain Paints
    private val bloodstainBeamPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val bloodstainRunePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2.5f }
    private val bloodstainOrbPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val bloodstainTextPaint = Paint().apply {
        color = Color.parseColor("#F59E0B")
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val bloodstainBadgeBgPaint = Paint().apply {
        color = Color.parseColor("#EE0B0E14")
        isAntiAlias = true
    }

    // Visceral Critical Strike State & Paints
    var visceralEffectTimer: Float = 0f
    var visceralEffectX: Float = 0f
    var visceralEffectY: Float = 0f

    fun triggerVisceralEffect(x: Float, y: Float) {
        visceralEffectTimer = 0.35f
        visceralEffectX = x
        visceralEffectY = y
    }

    private val visceralReticlePaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val visceralCorePaint = Paint().apply {
        color = Color.parseColor("#F87171")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val visceralSlashPaint = Paint().apply {
        color = Color.parseColor("#DC2626")
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val visceralSlashCorePaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val visceralPromptPaint = Paint().apply {
        color = Color.parseColor("#FCA5A5")
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val visceralPromptBg = Paint().apply {
        color = Color.parseColor("#EE450A0A")
        isAntiAlias = true
    }

    // Mimic Monster Paints
    private val mimicPath = Path()
    private val mimicTeethPaint = Paint().apply {
        color = Color.parseColor("#F1F5F9")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val mimicMawPaint = Paint().apply {
        color = Color.parseColor("#450A0A")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val mimicEyePaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val mimicArmPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val mimicHpBg = Paint().apply {
        color = Color.parseColor("#88000000")
        isAntiAlias = true
    }
    private val mimicHpFill = Paint().apply {
        color = Color.parseColor("#DC2626")
        isAntiAlias = true
    }

    // Loot Discovery Modal State & Paints
    var activeLoot: LootReward? = null
    var lootModalTimer: Float = 0f

    fun showLootDiscovery(loot: LootReward) {
        activeLoot = loot
        lootModalTimer = 4.0f
    }

    private val lootModalDim = Paint().apply {
        color = Color.parseColor("#88000000")
        isAntiAlias = true
    }
    private val lootModalCardBg = Paint().apply {
        color = Color.parseColor("#F20F172A")
        isAntiAlias = true
    }
    private val lootModalBorder = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val lootHeaderPaint = Paint().apply {
        color = Color.parseColor("#F59E0B")
        textSize = 14f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val lootTitlePaint = Paint().apply {
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val lootSubtitlePaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 13f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val lootDescPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 13f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val lootScalingBadgeBg = Paint().apply {
        color = Color.parseColor("#331E293B")
        isAntiAlias = true
    }
    private val lootScalingBadgeBorder = Paint().apply {
        color = Color.parseColor("#5564748B")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    private val lootScalingText = Paint().apply {
        textSize = 12f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val lootFooterPaint = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 12f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Origin Selection UI Paints
    private val originOverlayPaint = Paint().apply {
        color = Color.parseColor("#F6060911")
        isAntiAlias = true
    }
    private val originHeaderTitle = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val originHeaderSub = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 16f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val originCardBg = Paint().apply {
        color = Color.parseColor("#EE0F172A")
        isAntiAlias = true
    }
    private val originCardBorder = Paint().apply {
        color = Color.parseColor("#6638BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val originTabInactiveBg = Paint().apply {
        color = Color.parseColor("#AA1E293B")
        isAntiAlias = true
    }
    private val originTabActiveBg = Paint().apply {
        color = Color.parseColor("#DD0284C7")
        isAntiAlias = true
    }
    private val originTabBorder = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val originTabText = Paint().apply {
        color = Color.WHITE
        textSize = 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val originAwakenBtnBg = Paint().apply {
        color = Color.parseColor("#DD0369A1")
        isAntiAlias = true
    }
    private val originAwakenBtnBorder = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val originAwakenBtnText = Paint().apply {
        color = Color.WHITE
        textSize = 21f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val originStatLabel = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val originStatValue = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        textSize = 17f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val originStatBarBg = Paint().apply {
        color = Color.parseColor("#334155")
        isAntiAlias = true
    }
    private val originDescText = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 16f
        isAntiAlias = true
    }
    private val originBadgePaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 15f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Interactive Stat Allocation Terminal Paints
    private val originStatBtnBg = Paint().apply {
        color = Color.parseColor("#EE1E293B")
        isAntiAlias = true
    }
    private val originStatBtnBorder = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val originStatBtnText = Paint().apply {
        color = Color.WHITE
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val originPointsPillBg = Paint().apply {
        color = Color.parseColor("#EE0F172A")
        isAntiAlias = true
    }
    private val originPointsPillBorder = Paint().apply {
        color = Color.parseColor("#F59E0B")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val originPointsPillText = Paint().apply {
        color = Color.parseColor("#FEF08A")
        textSize = 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val originPresetActiveBg = Paint().apply {
        color = Color.parseColor("#DD0284C7")
        isAntiAlias = true
    }
    private val originPresetInactiveBg = Paint().apply {
        color = Color.parseColor("#AA1E293B")
        isAntiAlias = true
    }
    private val originPresetBorder = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        isAntiAlias = true
    }
    private val originPresetText = Paint().apply {
        color = Color.WHITE
        textSize = 14.5f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Dead Cells Character Origin Avatar Bitmaps & Reusable Objects (0 heap allocations in render loop)
    private val avatarBitmaps = mutableMapOf<String, Bitmap>()
    private val avatarFrameRect = RectF()
    private val avatarSrcRect = android.graphics.Rect()
    private val avatarClipPath = Path()
    private val avatarBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val avatarGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        isAntiAlias = true
    }
    private val avatarBitmapPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    private val avatarBadgeBg = Paint().apply {
        color = Color.parseColor("#CC090D16")
        isAntiAlias = true
    }
    private val avatarBadgeText = Paint().apply {
        color = Color.parseColor("#E2E8F0")
        textSize = 15f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val hudAvatarClipPath = Path()
    private val hudAvatarRect = RectF()
    private val hudAvatarBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val hudAvatarBgPaint = Paint().apply {
        color = Color.parseColor("#EE0B1120")
        isAntiAlias = true
    }

    init {
        val originIds = listOf(
            "shinobi",
            "vanguard",
            "executioner",
            "sorcerer",
            "forsaken",
            "paladin",
            "magic_swordsman",
            "nightblade",
            "ronin",
            "wanderer",
            "warrior"
        )
        for (id in originIds) {
            try {
                assetManager.open("characters/${id}_avatar.jpg").use { isStream ->
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val bmp = android.graphics.BitmapFactory.decodeStream(isStream, null, opts)
                    if (bmp != null) {
                        avatarBitmaps[id] = bmp
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }
    }

    // Reusable paths for zero-allocation 60 FPS combat rendering
    private val spikePath = Path()
    private val slashArcPath = Path()
    private val crescentPath = Path()
    private val coreCrescentPath = Path()
    private val crossCrescentPath = Path()
    private val crossCorePath = Path()

    // Calculated blade tip coordinates (0 allocations)
    private var tipX: Float = 0f
    private var tipY: Float = 0f
    private var tipAngleDeg: Float = 0f

    private fun computeKatanaTip(cx: Float, cy: Float, dir: Float, comboIndex: Int, progress: Float) {
        val (startDeg, endDeg, radius) = when (comboIndex) {
            1 -> Triple(-24f, 18f, 56f)
            2 -> Triple(68f, -102f, 66f)
            else -> Triple(-118f, 48f, 70f)
        }
        val t = ((progress - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val sweepFactor = sin(t * (PI.toFloat() / 2f))
        tipAngleDeg = startDeg + (endDeg - startDeg) * sweepFactor
        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
        tipX = cx + (cos(rad.toDouble()).toFloat() * radius * dir)
        tipY = cy + (sin(rad.toDouble()).toFloat() * radius)
    }

    private fun computeSwordTip(cx: Float, cy: Float, dir: Float, comboIndex: Int, progress: Float) {
        val (startDeg, endDeg, radius) = when (comboIndex) {
            1 -> Triple(-46f, 32f, 46f)
            2 -> Triple(42f, -54f, 52f)
            else -> Triple(-4f, 4f, 62f) // Thrust lunge
        }
        val t = ((progress - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val sweepFactor = sin(t * (PI.toFloat() / 2f))
        tipAngleDeg = startDeg + (endDeg - startDeg) * sweepFactor
        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
        tipX = cx + (cos(rad.toDouble()).toFloat() * radius * dir)
        tipY = cy + (sin(rad.toDouble()).toFloat() * radius)
    }

    private fun computeClubTip(cx: Float, cy: Float, dir: Float, comboIndex: Int, progress: Float) {
        val (startDeg, endDeg, radius) = when (comboIndex) {
            1 -> Triple(-75f, 36f, 46f)
            else -> Triple(-125f, 65f, 52f)
        }
        val t = ((progress - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val sweepFactor = sin(t * (PI.toFloat() / 2f))
        tipAngleDeg = startDeg + (endDeg - startDeg) * sweepFactor
        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
        tipX = cx + (cos(rad.toDouble()).toFloat() * radius * dir)
        tipY = cy + (sin(rad.toDouble()).toFloat() * radius)
    }

    private fun computeGreatswordTip(cx: Float, cy: Float, dir: Float, comboIndex: Int, progress: Float) {
        val (startDeg, endDeg, radius) = when (comboIndex) {
            1 -> Triple(-135f, 68f, 64f) // Downward Colossal Ground Slam
            else -> Triple(75f, -118f, 68f) // Upward Earth Cleave
        }
        val t = ((progress - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val sweepFactor = sin(t * (PI.toFloat() / 2f))
        tipAngleDeg = startDeg + (endDeg - startDeg) * sweepFactor
        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
        tipX = cx + (cos(rad.toDouble()).toFloat() * radius * dir)
        tipY = cy + (sin(rad.toDouble()).toFloat() * radius)
    }

    private fun buildCrescentPath(
        path: Path,
        cx: Float,
        cy: Float,
        dir: Float,
        startDeg: Float,
        endDeg: Float,
        radius: Float,
        ribbonWidth: Float,
        progress: Float,
        steps: Int = 16
    ) {
        path.reset()
        if (progress < 0.12f || progress > 0.88f) return

        val t = ((progress - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val sweepFactor = sin(t * (PI.toFloat() / 2f))
        val leadDeg = startDeg + (endDeg - startDeg) * sweepFactor

        val totalSpan = abs(endDeg - startDeg)
        val trailLength = totalSpan * 0.72f * sin(t * PI.toFloat())
        val trailDeg = if (endDeg > startDeg) (leadDeg - trailLength) else (leadDeg + trailLength)

        val degSpan = leadDeg - trailDeg
        if (abs(degSpan) < 2f) return

        val stepDeg = degSpan / steps

        // 1. Outer Arc from trail to lead
        for (i in 0..steps) {
            val currentDeg = trailDeg + (stepDeg * i)
            val rad = Math.toRadians(currentDeg.toDouble()).toFloat()
            val envelope = sin((i.toFloat() / steps.toFloat()) * PI.toFloat())
            val rOut = radius + (ribbonWidth * 0.5f * envelope)
            val px = cx + (cos(rad.toDouble()).toFloat() * rOut * dir)
            val py = cy + (sin(rad.toDouble()).toFloat() * rOut)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        // 2. Inner Arc from lead back to trail (tapered to needle points)
        for (i in steps downTo 0) {
            val currentDeg = trailDeg + (stepDeg * i)
            val rad = Math.toRadians(currentDeg.toDouble()).toFloat()
            val envelope = sin((i.toFloat() / steps.toFloat()) * PI.toFloat())
            val rIn = (radius - (ribbonWidth * 0.5f * envelope)).coerceAtLeast(8f)
            val px = cx + (cos(rad.toDouble()).toFloat() * rIn * dir)
            val py = cy + (sin(rad.toDouble()).toFloat() * rIn)
            path.lineTo(px, py)
        }

        path.close()
    }

    private fun drawBladeGlint(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        bladeGlintPaint.color = color
        canvas.drawCircle(x, y, size * 0.45f, bladeGlintPaint)
        canvas.drawRect(x - size, y - 1.5f, x + size, y + 1.5f, bladeGlintPaint)
        canvas.drawRect(x - 1.5f, y - size, x + 1.5f, y + size, bladeGlintPaint)
    }

    private fun drawAbyssalNeedleBurst(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        dir: Float,
        startDeg: Float,
        endDeg: Float,
        radius: Float,
        progress: Float,
        count: Int = 8,
        spikeLen: Float = 26f,
        colorCore: Int = Color.parseColor("#E9D5FF"),
        colorAura: Int = Color.parseColor("#9333EA")
    ) {
        if (progress < 0.14f || progress > 0.86f) return
        val life = sin(((progress - 0.14f) / 0.72f) * PI.toFloat())
        val degSpan = endDeg - startDeg
        val step = degSpan / (count - 1).coerceAtLeast(1)

        for (i in 0 until count) {
            val ang = startDeg + step * i
            val rad = Math.toRadians(ang.toDouble()).toFloat()
            val cosA = cos(rad.toDouble()).toFloat() * dir
            val sinA = sin(rad.toDouble()).toFloat()

            val len = spikeLen * life * (0.68f + 0.32f * sin((i * 1.8f).toDouble()).toFloat())
            val baseR = radius - 3f
            val tipR = baseR + len

            val baseX = cx + cosA * baseR
            val baseY = cy + sinA * baseR
            val nTipX = cx + cosA * tipR
            val nTipY = cy + sinA * tipR

            val perpX = -sinA * 2.8f * life
            val perpY = cosA * 2.8f * life

            needlePath.reset()
            needlePath.moveTo(nTipX, nTipY)
            needlePath.lineTo(baseX + perpX, baseY + perpY)
            needlePath.lineTo(cx + cosA * (baseR - 5f), cy + sinA * (baseR - 5f))
            needlePath.lineTo(baseX - perpX, baseY - perpY)
            needlePath.close()

            needlePaint.color = colorAura
            needlePaint.alpha = (life * 230f).toInt().coerceIn(0, 255)
            canvas.drawPath(needlePath, needlePaint)

            needleOutlinePaint.color = Color.parseColor("#180424")
            needleOutlinePaint.alpha = (life * 255f).toInt().coerceIn(0, 255)
            canvas.drawPath(needlePath, needleOutlinePaint)

            canvas.drawCircle(nTipX, nTipY, 1.6f, bladeGlintPaint)
        }
    }

    private fun drawHorizontalNeedleBurst(
        canvas: Canvas,
        startX: Float,
        baseY: Float,
        dir: Float,
        progress: Float,
        count: Int = 7,
        maxReach: Float = 62f,
        colorAura: Int = Color.parseColor("#A855F7")
    ) {
        if (progress < 0.14f || progress > 0.86f) return
        val life = sin(((progress - 0.14f) / 0.72f) * PI.toFloat())

        for (i in 0 until count) {
            val yOffset = ((i - (count / 2)) * 6.5f)
            val reach = maxReach * life * (0.6f + 0.4f * sin((i * 2.1f).toDouble()).toFloat())
            val nTipX = startX + dir * reach
            val nTipY = baseY + yOffset
            val nBaseX = startX + dir * (reach * 0.28f)

            needlePath.reset()
            needlePath.moveTo(nTipX, nTipY)
            needlePath.lineTo(nBaseX, nTipY - 2.5f * life)
            needlePath.lineTo(startX, nTipY)
            needlePath.lineTo(nBaseX, nTipY + 2.5f * life)
            needlePath.close()

            needlePaint.color = colorAura
            needlePaint.alpha = (life * 240f).toInt().coerceIn(0, 255)
            canvas.drawPath(needlePath, needlePaint)

            needleOutlinePaint.color = Color.parseColor("#180424")
            needleOutlinePaint.alpha = (life * 255f).toInt().coerceIn(0, 255)
            canvas.drawPath(needlePath, needleOutlinePaint)

            canvas.drawCircle(nTipX, nTipY, 1.8f, bladeGlintPaint)
        }
    }

    private fun drawShadowGroundSmoke(canvas: Canvas, groundX: Float, groundY: Float, progress: Float, dir: Float) {
        if (progress < 0.2f || progress > 0.85f) return
        val life = sin(((progress - 0.2f) / 0.65f) * PI.toFloat())
        val alpha = (life * 175f).toInt().coerceIn(0, 255)
        shadowSmokePaint.color = Color.argb(alpha, 24, 4, 36)

        for (i in 1..4) {
            val px = groundX + (dir * i * 13f * life)
            val py = groundY - (3f + i * 2f * life)
            val r = (6f + i * 3f) * life
            canvas.drawCircle(px, py, r, shadowSmokePaint)
        }
    }

    fun update(dt: Float, player: Player) {
        globalTime += dt
        if (shakeTimer > 0f) {
            shakeTimer -= dt
        }
        if (visceralEffectTimer > 0f) {
            visceralEffectTimer -= dt
        }
        if (lootModalTimer > 0f) {
            lootModalTimer -= dt
            if (lootModalTimer <= 0f) {
                activeLoot = null
            }
        }
        for (v in groundPoundVfxPool) {
            if (v.active) {
                v.timer -= dt
                if (v.timer <= 0f) v.active = false
            }
        }
        playerAnimator.update(dt, player)

        // 1. Update active Combat Sparks (0 allocations)
        for (i in sparkPool.indices) {
            val s = sparkPool[i]
            if (s.active) {
                s.x += s.vx * dt
                s.y += s.vy * dt
                s.vy += 320f * dt // gravity
                s.vx *= 0.94f // air resistance
                s.life -= dt
                if (s.life <= 0f) {
                    s.active = false
                }
            }
        }

        // 2. Dynamic Combat Spark emission trailing the weapon arc
        if (player.isAttacking) {
            val p = player.attackProgress
            if (p in 0.16f..0.72f) {
                val pb = player.bounds
                val cx = pb.centerX
                val cy = pb.centerY
                val dir = if (player.facingRight) 1f else -1f

                when (player.equippedWeapon.type) {
                    WeaponType.KATANA -> {
                        val isScarlet = player.equippedWeapon.id == "crimson_vein"
                        computeKatanaTip(cx, cy, dir, player.attackComboIndex, p)
                        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
                        for (k in 0..1) {
                            val speed = 280f + (Math.random().toFloat() * 320f)
                            val scatter = (Math.random().toFloat() - 0.5f) * 0.6f
                            val svx = (cos((rad + scatter).toDouble()).toFloat() * speed * dir)
                            val svy = (sin((rad + scatter).toDouble()).toFloat() * speed)
                            if (isScarlet) {
                                emitSpark(tipX, tipY, svx, svy, 239, 68, 68, strokeWidth = 3.2f)
                            } else if (player.attackComboIndex == 3 || k == 1) {
                                emitSpark(tipX, tipY, svx, svy, 233, 213, 255, strokeWidth = 3.2f) // Incandescent Lilac spark
                            } else {
                                emitSpark(tipX, tipY, svx, svy, 168, 85, 247, strokeWidth = 2.8f) // Deep Electric Violet spark
                            }
                        }
                    }
                    WeaponType.SWORD -> {
                        computeSwordTip(cx, cy, dir, player.attackComboIndex, p)
                        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
                        for (k in 0..1) {
                            val speed = 260f + (Math.random().toFloat() * 300f)
                            val scatter = (Math.random().toFloat() - 0.5f) * 0.6f
                            val svx = (cos((rad + scatter).toDouble()).toFloat() * speed * dir)
                            val svy = (sin((rad + scatter).toDouble()).toFloat() * speed)
                            emitSpark(tipX, tipY, svx, svy, 226, 232, 240, strokeWidth = 2.5f) // Silver spark
                        }
                    }
                    WeaponType.GREATSWORD -> {
                        computeGreatswordTip(cx, cy, dir, player.attackComboIndex, p)
                        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
                        for (k in 0..2) {
                            val speed = 300f + (Math.random().toFloat() * 380f)
                            val scatter = (Math.random().toFloat() - 0.5f) * 0.8f
                            val svx = (cos((rad + scatter).toDouble()).toFloat() * speed * dir)
                            val svy = (sin((rad + scatter).toDouble()).toFloat() * speed)
                            emitSpark(tipX, tipY, svx, svy, 245, 158, 11, strokeWidth = 4f, maxLife = 0.28f) // Gold spark
                        }
                    }
                    WeaponType.CLUB -> {
                        computeClubTip(cx, cy, dir, player.attackComboIndex, p)
                        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
                        for (k in 0..1) {
                            val speed = 240f + (Math.random().toFloat() * 260f)
                            val scatter = (Math.random().toFloat() - 0.5f) * 0.7f
                            val svx = (cos((rad + scatter).toDouble()).toFloat() * speed * dir)
                            val svy = (sin((rad + scatter).toDouble()).toFloat() * speed)
                            emitSpark(tipX, tipY, svx, svy, 217, 119, 6, strokeWidth = 3.6f) // Amber wood splinter spark
                        }
                    }
                    WeaponType.STAFF -> {
                        val tipStaffX = cx + (dir * 36f)
                        val tipStaffY = cy - 8f
                        if (Math.random() < 0.45) {
                            val speed = 120f + (Math.random().toFloat() * 160f)
                            val angle = Math.random().toFloat() * Math.PI.toFloat() * 2f
                            emitSpark(tipStaffX, tipStaffY, cos(angle.toDouble()).toFloat() * speed, sin(angle.toDouble()).toFloat() * speed, 192, 132, 252, strokeWidth = 2.5f)
                        }
                    }
                    WeaponType.BOW -> {
                        val tipBowX = cx + (dir * 26f)
                        val tipBowY = cy - 4f
                        if (Math.random() < 0.4) {
                            emitSpark(tipBowX, tipBowY, dir * 240f, (Math.random().toFloat() - 0.5f) * 60f, 251, 191, 36, strokeWidth = 2.4f)
                        }
                    }
                    WeaponType.SCYTHE -> {
                        computeGreatswordTip(cx, cy, dir, player.attackComboIndex, p)
                        val rad = Math.toRadians(tipAngleDeg.toDouble()).toFloat()
                        for (k in 0..1) {
                            val speed = 280f + (Math.random().toFloat() * 300f)
                            emitSpark(tipX, tipY, cos(rad.toDouble()).toFloat() * speed * dir, sin(rad.toDouble()).toFloat() * speed, 168, 85, 247, strokeWidth = 3.2f)
                        }
                    }
                    WeaponType.GAUNTLET -> {
                        val fistX = cx + (dir * 24f)
                        val fistY = cy - 4f
                        for (k in 0..1) {
                            emitSpark(fistX, fistY, dir * (220f + Math.random().toFloat() * 180f), (Math.random().toFloat() - 0.5f) * 140f, 249, 115, 22, strokeWidth = 3.0f)
                        }
                    }
                }
            }
        }
    }

    fun render(
        canvas: Canvas,
        level: Level,
        player: Player,
        particles: List<Particle>,
        projectiles: List<MagicProjectile>,
        cameraX: Float,
        cameraY: Float,
        screenWidth: Float,
        screenHeight: Float,
        scoreText: String
    ) {
        // 1. Draw sky
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, skyPaint)

        // 2. Parallax silhouettes
        drawParallaxBackground(canvas, cameraX, cameraY, screenWidth, screenHeight)

        // 3. Ambient fog
        drawAtmosphericLighting(canvas, screenWidth, screenHeight)

        // 4. Render World Elements with camera + screen shake
        val shakeX = if (shakeTimer > 0f) (Math.random().toFloat() - 0.5f) * shakeIntensity * 2f else 0f
        val shakeY = if (shakeTimer > 0f) (Math.random().toFloat() - 0.5f) * shakeIntensity * 2f else 0f

        canvas.save()
        canvas.translate(-cameraX + shakeX, -cameraY + shakeY)

        tilesetRenderer.drawZoneDecor(canvas, cameraX, cameraX + screenWidth)

        for (platform in level.platforms) {
            val b = platform.bounds
            when (platform.type) {
                PlatformType.NORMAL -> {
                    if (!tilesetRenderer.drawNormalPlatform(canvas, b, cameraX, cameraX + screenWidth)) {
                        canvas.drawRect(b.left, b.top + 8f, b.right, b.bottom + 8f, platformShadowPaint)
                        canvas.drawRect(b.left, b.top, b.right, b.bottom, platformCorePaint)
                        canvas.drawLine(b.left, b.top + 2f, b.right, b.top + 2f, platformSubTopPaint)
                        canvas.drawLine(b.left, b.top, b.right, b.top, platformTopPaint)
                    }
                }

                PlatformType.ONE_WAY -> {
                    if (!tilesetRenderer.drawOneWayPlatform(canvas, b)) {
                        // Dead Cells style semi-solid wooden/metal scaffold platform
                        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 4f, 4f, oneWayPlankPaint)
                        // Amber glowing upper tread rim
                        canvas.drawLine(b.left, b.top + 1.5f, b.right, b.top + 1.5f, oneWayTopRimPaint)
                        // Metal support struts
                        var gx = b.left + 24f
                        while (gx < b.right - 12f) {
                            canvas.drawLine(gx, b.top + 3f, gx, b.bottom - 2f, oneWayBracketPaint)
                            gx += 36f
                        }
                        // Centered drop-through chevron mark
                        val cx = (b.left + b.right) / 2f
                        val cy = (b.top + b.bottom) / 2f
                        canvas.drawLine(cx - 10f, cy - 3f, cx, cy + 3f, oneWayRunePaint)
                        canvas.drawLine(cx, cy + 3f, cx + 10f, cy - 3f, oneWayRunePaint)
                    }
                }

                PlatformType.HAZARD -> {
                    if (!tilesetRenderer.drawHazard(canvas, b)) {
                        val spikeWidth = 24f
                        var sx = b.left
                        while (sx < b.right) {
                            val currentW = minOf(spikeWidth, b.right - sx)
                            spikePath.reset()
                            spikePath.moveTo(sx, b.bottom)
                            spikePath.lineTo(sx + currentW / 2f, b.top)
                            spikePath.lineTo(sx + currentW, b.bottom)
                            spikePath.close()

                            canvas.drawPath(spikePath, spikeFillPaint)
                            canvas.drawPath(spikePath, spikeEdgePaint)
                            canvas.drawCircle(sx + currentW / 2f, b.top + 4f, 4f, spikeGlowPaint)
                            sx += spikeWidth
                        }
                    }
                }

                PlatformType.SWITCH -> {
                    if (!tilesetRenderer.drawSwitch(canvas, b, platform.isActivated)) {
                        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8f, 8f, switchBasePaint)
                        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8f, 8f, switchBorderPaint)
                        val corePaint = if (platform.isActivated) switchActiveCore else switchInactiveCore
                        val cx = b.centerX
                        val cy = b.centerY
                        canvas.drawCircle(cx, cy, 10f, corePaint)
                        if (platform.isActivated) {
                            canvas.drawLine(cx, cy, cx + 12f, cy - 12f, switchBorderPaint)
                        } else {
                            canvas.drawLine(cx, cy, cx - 12f, cy - 12f, switchBorderPaint)
                        }
                    }
                }

                PlatformType.DOOR -> {
                    if (!platform.isActivated) {
                        if (!tilesetRenderer.drawDoor(canvas, b)) {
                            canvas.drawRect(b.left, b.top, b.right, b.bottom, doorFramePaint)
                            val barStep = 10f
                            var bx = b.left + 5f
                            while (bx < b.right) {
                                canvas.drawLine(bx, b.top, bx, b.bottom, doorBarPaint)
                                bx += barStep
                            }
                            canvas.drawLine(b.left, b.top, b.right, b.top, platformTopPaint)
                        }
                    }
                }

                PlatformType.PRESSURE_PLATE -> {
                    val plateTop = if (platform.isActivated) b.top + 6f else b.top
                    canvas.drawRect(b.left, plateTop, b.right, b.bottom, plateBasePaint)
                    val edgePaint = if (platform.isActivated) plateTopActive else plateTopInactive
                    canvas.drawLine(b.left, plateTop, b.right, plateTop, edgePaint)
                }

                PlatformType.CHECKPOINT -> {
                    // If Grace Mimic has emerged and is fighting, do not render static sword-in-stone
                    if (platform.isGraceMimic && platform.isMimicAwakened && !platform.isMimicDefeated) {
                        continue
                    }
                    if (tilesetRenderer.drawCheckpoint(canvas, b, platform.isActivated)) {
                        if (platform.isActivated) {
                            canvas.drawCircle(b.centerX, b.bottom - 52f, 24f, exitGlowPaint)
                        } else if (platform.isGraceMimic) {
                            // Deceitful Grace ominous tell: faint violet flicker particle
                            val flickerY = b.bottom - 54f + sin(globalTime * 6f) * 4f
                            canvas.drawCircle(b.centerX, flickerY, 8f + sin(globalTime * 8f) * 3f, exitGlowPaint)
                        }
                    } else {
                        val midX = b.centerX
                        canvas.drawLine(midX, b.top + 16f, midX, b.bottom, checkpointPolePaint)
                        val lampPaint = if (platform.isActivated) checkpointActivePaint else checkpointInactivePaint
                        canvas.drawCircle(midX, b.top + 12f, 10f, lampPaint)
                        if (platform.isActivated) {
                            canvas.drawCircle(midX, b.top + 12f, 22f, exitGlowPaint)
                        }
                    }
                    val pDist = abs((player.bounds.x + player.width / 2f) - b.centerX)
                    if (pDist < 65f && abs(player.bounds.y - b.top) < 80f) {
                        val promptY = b.top - 22f + sin(globalTime * 5f) * 3f
                        val promptW = 126f
                        val promptH = 22f
                        canvas.drawRoundRect(b.centerX - promptW / 2f, promptY - promptH / 2f, b.centerX + promptW / 2f, promptY + promptH / 2f, 6f, 6f, chestPromptBgPaint)
                        canvas.drawRoundRect(b.centerX - promptW / 2f, promptY - promptH / 2f, b.centerX + promptW / 2f, promptY + promptH / 2f, 6f, 6f, chestBorderPaint)
                        canvas.drawText("✦ TOUCH GRACE", b.centerX, promptY + 5f, chestPromptPaint)
                    }
                }

                PlatformType.EXIT -> {
                    canvas.drawRect(b.left - 4f, b.top - 4f, b.right + 4f, b.bottom + 4f, exitGlowPaint)
                    canvas.drawRect(b.left, b.top, b.right, b.bottom, exitPaint)
                }
            }
        }

        // Pushable Boxes
        for (box in level.boxes) {
            val b = box.bounds
            if (!tilesetRenderer.drawBox(canvas, b)) {
                canvas.drawRect(b.left, b.top, b.right, b.bottom, boxPaint)
                canvas.drawRect(b.left, b.top, b.right, b.bottom, boxBorderPaint)
                canvas.drawLine(b.left + 6f, b.top + 6f, b.right - 6f, b.bottom - 6f, boxCrossPaint)
                canvas.drawLine(b.left + 6f, b.bottom - 6f, b.right - 6f, b.top + 6f, boxCrossPaint)
            }
        }

        // Souls Lost Essence Bloodstain
        renderBloodstain(canvas, level)

        // Souls Treasure Chests & Mimics
        renderChests(canvas, level.chests, player)

        // Magic Projectiles
        for (proj in projectiles) {
            canvas.drawCircle(proj.x, proj.y, proj.radius * 1.8f, spellAuraPaint)
            canvas.drawCircle(proj.x, proj.y, proj.radius, spellCorePaint)
        }

        // Atmospheric Dust Particles
        val playerCenterX = player.bounds.centerX
        val playerCenterY = player.bounds.centerY
        for (p in particles) {
            p.render(canvas, particlePaint, playerCenterX, playerCenterY)
        }

        // Render Enemies (Hollow Soldiers & Elite Guardians)
        renderEnemies(canvas, level.enemies, player)

        // Drinking Flask Healing / Mana Aura
        if (player.isDrinkingFlask) {
            val pb = player.bounds
            val auraColor = if (player.lastDrunkFlaskType == "CRIMSON") Color.parseColor("#55EF4444") else Color.parseColor("#5538BDF8")
            healingAuraPaint.color = auraColor
            val r = 36f + sin(globalTime * 14f) * 6f
            canvas.drawCircle(pb.centerX, pb.centerY, r, healingAuraPaint)
        }

        // Roll Shadow Ghost Trail
        if (player.isRolling) {
            val pb = player.bounds
            val trailOffset = if (player.facingRight) -30f else 30f
            canvas.drawRoundRect(pb.left + trailOffset, pb.top + 16f, pb.right + trailOffset, pb.bottom, 16f, 16f, rollTrailPaint)
        }

        // Player Character Sprite
        val drawn = playerAnimator.draw(canvas, player, null)
        if (!drawn) {
            renderProceduralPlayerSilhouette(canvas, player)
        }

        // Timed Parry / Deflect Golden-Cyan Aura
        if (player.isParrying) {
            val pb = player.bounds
            val shieldX = if (player.facingRight) pb.right + 6f else pb.left - 6f
            val shieldY = pb.centerY - 4f
            val parryArcPaint = Paint().apply {
                color = Color.parseColor("#FACC15")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            val parryGlowPaint = Paint().apply {
                color = Color.parseColor("#44FACC15")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(pb.centerX, pb.centerY, 34f, parryGlowPaint)
            val startDeg = if (player.facingRight) -65f else 115f
            canvas.drawArc(
                pb.centerX - 32f, pb.centerY - 34f,
                pb.centerX + 32f, pb.centerY + 30f,
                startDeg, 130f, false, parryArcPaint
            )
            canvas.drawCircle(shieldX, shieldY, 5f, parryArcPaint)
        }

        // Downward Smash Stomp Plunge VFX
        if (player.isGroundPounding) {
            drawGroundPoundPlungeVfx(canvas, player)
        }

        // Weapon Attack Slashes & Effects
        if (player.isAttacking) {
            drawWeaponAttackEffect(canvas, player)
        }

        // Weapon Skill Special VFX
        if (player.isPerformingSkill) {
            drawWeaponSkillEffect(canvas, player)
        }

        // Ground Pound Shockwaves on floor
        drawGroundPoundShockwaves(canvas)

        // 4. Render active Combat Sparks (0 allocations)
        for (i in sparkPool.indices) {
            val s = sparkPool[i]
            if (s.active) {
                val alpha = ((s.life / s.maxLife) * 255f).toInt().coerceIn(0, 255)
                sparkPaint.color = Color.argb(alpha, s.r, s.g, s.b)
                sparkPaint.strokeWidth = s.strokeWidth
                canvas.drawLine(s.x - s.vx * 0.032f, s.y - s.vy * 0.032f, s.x, s.y, sparkPaint)
            }
        }

        // 5. Visceral Execution Strike Flash & Shockwave
        if (visceralEffectTimer > 0f) {
            drawVisceralExecutionEffect(canvas, cameraX, cameraY, screenWidth, screenHeight)
        }

        canvas.restore()

        // 5. Dynamic Atmospheric Lighting & Vignette (0 allocations per frame)
        renderDynamicLighting(canvas, level, player, projectiles, cameraX, cameraY, screenWidth, screenHeight)

        // 7. Souls RPG HUD & Weapon Badge
        drawSoulsHud(canvas, player)
        drawWeaponBadge(canvas, player, screenWidth)
        drawArmorBadge(canvas, player, screenWidth)
        drawBossHealthBar(canvas, level, screenWidth, screenHeight)

        level.checkpointNotification?.let { notif ->
            canvas.drawText(notif, screenWidth / 2f - 140f, 60f, notifPaint)
        }

        // 8. Souls Loot Discovery Modal
        if (lootModalTimer > 0f && activeLoot != null) {
            drawLootDiscoveryModal(canvas, activeLoot!!, screenWidth, screenHeight)
        }
    }

    private fun drawGroundPoundPlungeVfx(canvas: Canvas, player: Player) {
        val pb = player.bounds
        val cx = pb.centerX
        val cy = pb.centerY
        // Downward motion blur speed lines
        skillLinePaint.color = Color.argb(190, 245, 158, 11)
        skillLinePaint.strokeWidth = 3f
        canvas.drawLine(cx - 10f, cy - 50f, cx - 10f, cy + 10f, skillLinePaint)
        canvas.drawLine(cx, cy - 70f, cx, cy + 20f, skillLinePaint)
        canvas.drawLine(cx + 10f, cy - 50f, cx + 10f, cy + 10f, skillLinePaint)

        // Amber concussive head cone
        skillAuraPaint.color = Color.argb(120, 253, 224, 71)
        val conePath = spikePath
        conePath.reset()
        conePath.moveTo(cx, pb.bottom + 16f)
        conePath.lineTo(pb.left - 4f, pb.centerY)
        conePath.lineTo(pb.right + 4f, pb.centerY)
        conePath.close()
        canvas.drawPath(conePath, skillAuraPaint)
    }

    private fun drawGroundPoundShockwaves(canvas: Canvas) {
        for (v in groundPoundVfxPool) {
            if (!v.active) continue
            val t = (1f - (v.timer / v.maxDuration)).coerceIn(0f, 1f)
            val maxR = 150f
            val r = t * maxR
            val alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)

            // Outer expanding concussive shock ring (elliptical perspective along floor)
            groundPoundShockRingPaint.color = Color.argb(alpha, 245, 158, 11)
            groundPoundShockRingPaint.strokeWidth = 6f * (1f - t * 0.5f)
            canvas.drawOval(v.x - r, v.y - r * 0.32f, v.x + r, v.y + r * 0.32f, groundPoundShockRingPaint)

            // Inner high-energy white/cyan ring
            groundPoundShockRingPaint.color = Color.argb((alpha * 0.8f).toInt(), 255, 255, 255)
            groundPoundShockRingPaint.strokeWidth = 3f
            val innerR = r * 0.65f
            canvas.drawOval(v.x - innerR, v.y - innerR * 0.32f, v.x + innerR, v.y + innerR * 0.32f, groundPoundShockRingPaint)

            // Central earth concussion flash
            if (t < 0.45f) {
                val coreAlpha = (((0.45f - t) / 0.45f) * 160f).toInt().coerceIn(0, 255)
                groundPoundCorePaint.color = Color.argb(coreAlpha, 253, 224, 71)
                canvas.drawCircle(v.x, v.y, 24f * (1f - t / 0.45f), groundPoundCorePaint)
            }
        }
    }

    private fun drawWeaponSkillEffect(canvas: Canvas, player: Player) {
        slashAnimator.drawSkillSlash(canvas, player)
        val pb = player.bounds
        val cx = pb.centerX
        val cy = pb.centerY
        val dir = if (player.facingRight) 1f else -1f
        val p = player.skillProgress
        val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)

        when (player.activeSkillId) {
            "katana" -> { // Phantom Dash: Shadow silhouettes trailing behind with cyan speed blades
                skillAuraPaint.color = Color.argb((alpha * 0.45f).toInt(), 0, 229, 255)
                val backDir = -dir
                for (step in 1..3) {
                    val offX = backDir * step * 38f * (1f - p * 0.4f)
                    canvas.drawRoundRect(pb.left + offX, pb.top + 6f, pb.right + offX, pb.bottom - 4f, 12f, 12f, skillAuraPaint)
                }
                // Razor cut lines
                skillLinePaint.color = Color.argb(alpha, 255, 255, 255)
                skillLinePaint.strokeWidth = 3.5f
                canvas.drawLine(cx - 70f * dir, cy - 12f, cx + 80f * dir, cy - 12f, skillLinePaint)
                canvas.drawLine(cx - 50f * dir, cy + 8f, cx + 100f * dir, cy + 8f, skillLinePaint)
            }
            "broadsword" -> { // Stalwart Guardbreaker: Golden hyper-armor crest & rising thrust plume
                skillAuraPaint.color = Color.argb((alpha * 0.5f).toInt(), 245, 158, 11)
                canvas.drawCircle(cx, cy, 38f + p * 16f, skillAuraPaint)
                // Rising thrust spear of light
                skillLinePaint.color = Color.argb(alpha, 253, 224, 71)
                skillLinePaint.strokeWidth = 6f
                canvas.drawLine(cx + dir * 14f, pb.bottom, cx + dir * 65f, pb.top - 36f, skillLinePaint)
                skillLinePaint.color = Color.argb(alpha, 255, 255, 255)
                skillLinePaint.strokeWidth = 2.5f
                canvas.drawLine(cx + dir * 14f, pb.bottom, cx + dir * 65f, pb.top - 36f, skillLinePaint)
            }
            "greatsword" -> { // Titan Slam: Fiery earth eruption
                val slamProgress = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                if (slamProgress > 0f) {
                    val slamAlpha = ((1f - slamProgress) * 255f).toInt().coerceIn(0, 255)
                    skillLinePaint.color = Color.argb(slamAlpha, 239, 68, 68)
                    skillLinePaint.strokeWidth = 5f
                    val impactX = cx + dir * 55f
                    canvas.drawLine(impactX, pb.bottom, impactX + dir * 75f, pb.bottom - 55f, skillLinePaint)
                    canvas.drawLine(impactX, pb.bottom, impactX - dir * 40f, pb.bottom - 45f, skillLinePaint)
                    skillAuraPaint.color = Color.argb((slamAlpha * 0.6f).toInt(), 245, 158, 11)
                    canvas.drawCircle(impactX, pb.bottom, 42f * slamProgress, skillAuraPaint)
                }
            }
            "staff" -> { // Supernova Burst: Concentric expanding cosmic rings
                val r = p * 130f
                skillLinePaint.color = Color.argb(alpha, 168, 85, 247)
                skillLinePaint.strokeWidth = 5f
                canvas.drawCircle(cx, cy, r, skillLinePaint)
                skillLinePaint.color = Color.argb((alpha * 0.7f).toInt(), 255, 255, 255)
                skillLinePaint.strokeWidth = 2f
                canvas.drawCircle(cx, cy, r * 0.75f, skillLinePaint)
                skillAuraPaint.color = Color.argb((alpha * 0.25f).toInt(), 147, 51, 234)
                canvas.drawCircle(cx, cy, r, skillAuraPaint)
            }
            "club" -> { // Earth Tremor: Bilateral shockwaves rippling left and right
                val tremorX = p * 110f
                skillLinePaint.color = Color.argb(alpha, 217, 119, 6)
                skillLinePaint.strokeWidth = 4f
                canvas.drawLine(cx - tremorX, pb.bottom - 16f, cx - tremorX + 18f, pb.bottom, skillLinePaint)
                canvas.drawLine(cx + tremorX - 18f, pb.bottom, cx + tremorX, pb.bottom - 16f, skillLinePaint)
            }
            "nightprowler_talon" -> { // Shadow Somersault: Crescent leap arc & evasive flip shadow
                skillLinePaint.color = Color.argb(alpha, 192, 132, 252)
                skillLinePaint.strokeWidth = 5f
                val somersaultArc = spikePath
                somersaultArc.reset()
                somersaultArc.moveTo(cx + dir * 30f, pb.bottom)
                somersaultArc.quadTo(cx, pb.top - 60f, cx - dir * 55f, pb.bottom - 10f)
                canvas.drawPath(somersaultArc, skillLinePaint)
            }
            "wyrm_wing_halberd" -> { // Vortical Tempest: Double cyclone vortex ring
                val angle = p * PI.toFloat() * 4f
                skillLinePaint.color = Color.argb(alpha, 245, 158, 11)
                skillLinePaint.strokeWidth = 4.5f
                canvas.drawOval(cx - 75f, cy - 25f, cx + 75f, cy + 25f, skillLinePaint)
                skillLinePaint.color = Color.argb((alpha * 0.65f).toInt(), 254, 240, 138)
                skillLinePaint.strokeWidth = 2.5f
                canvas.drawOval(cx - 55f, cy - 16f, cx + 55f, cy + 16f, skillLinePaint)
            }
            "pale_eclipse_gs" -> { // Frostfall Wave: Piercing celestial frost crystals
                val waveDist = p * 180f
                val wx = cx + dir * waveDist
                val wy = pb.bottom
                skillLinePaint.color = Color.argb(alpha, 56, 189, 248)
                skillLinePaint.strokeWidth = 4f
                canvas.drawLine(wx, wy, wx + dir * 14f, wy - 42f, skillLinePaint)
                canvas.drawLine(wx + dir * 14f, wy - 42f, wx + dir * 28f, wy, skillLinePaint)
                skillAuraPaint.color = Color.argb((alpha * 0.45f).toInt(), 224, 242, 254)
                canvas.drawCircle(wx + dir * 14f, wy - 22f, 16f, skillAuraPaint)
            }
            "crimson_vein" -> { // Scarlet Cascade: 5 rapid cross-slashes
                skillLinePaint.color = Color.argb(alpha, 239, 68, 68)
                skillLinePaint.strokeWidth = 4f
                val sx = cx + dir * 45f
                canvas.drawLine(sx - 35f, cy - 30f, sx + 35f, cy + 30f, skillLinePaint)
                canvas.drawLine(sx - 35f, cy + 30f, sx + 35f, cy - 30f, skillLinePaint)
                canvas.drawLine(sx - 45f, cy, sx + 45f, cy, skillLinePaint)
            }
        }
    }

    private fun drawWeaponAttackEffect(canvas: Canvas, player: Player) {
        val pb = player.bounds
        val cx = pb.centerX
        val cy = pb.centerY
        val dir = if (player.facingRight) 1f else -1f
        val p = player.attackProgress
        val weapon = player.equippedWeapon

        // Shinobi sprite sheet has purple shadow katana slash arcs directly animated when using signature katana
        if (player.currentOrigin.id == "shinobi" && weapon.id == "katana") return
        // Vanguard sprite sheet (FreeKnight) has broadsword cleave arcs directly animated when using a sword
        if (player.currentOrigin.id == "vanguard" && weapon.type == WeaponType.SWORD) return
        // Executioner sprite sheet (Fire Knight) has blazing greatsword cleaves directly animated when using greatsword blades
        if (player.currentOrigin.id == "executioner" && weapon.type == WeaponType.GREATSWORD && weapon.id != "battle_axe") return
        // Sorcerer sprite sheet (Wizard Pack) has arcane staff casts directly animated when using a staff
        if (player.currentOrigin.id == "sorcerer" && weapon.type == WeaponType.STAFF) return

        if (slashAnimator.drawAttackSlash(canvas, player)) return

        when (weapon.type) {
            WeaponType.KATANA -> {
                val isScarlet = weapon.id == "crimson_vein"
                val isDagger = weapon.id == "obsidian_dagger"
                val sizeScale = if (isDagger) 0.70f else 1.0f
                val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)

                // Void Obsidian / Jade Emerald (Dagger) / Scarlet Crimson (Crimson Vein)
                val voidColor = when {
                    isScarlet -> Color.argb((alpha * 0.95f).toInt(), 69, 10, 10)
                    isDagger -> Color.argb((alpha * 0.95f).toInt(), 6, 44, 34)
                    else -> Color.argb((alpha * 0.95f).toInt(), 24, 4, 36)
                }
                val auraColor = when {
                    isScarlet -> Color.argb((alpha * 0.90f).toInt(), 220, 38, 38)
                    isDagger -> Color.argb((alpha * 0.90f).toInt(), 16, 185, 129)
                    else -> Color.argb((alpha * 0.90f).toInt(), 147, 51, 234)
                }
                val edgeColor = when {
                    isScarlet -> Color.argb(alpha, 252, 165, 165)
                    isDagger -> Color.argb(alpha, 110, 231, 183)
                    else -> Color.argb(alpha, 216, 180, 254)
                }
                val glintColor = Color.argb(alpha, 255, 255, 255)

                when (player.attackComboIndex) {
                    1 -> {
                        val startDeg = -24f
                        val endDeg = 18f
                        val radius = 56f * sizeScale
                        val ribbonW = 20f * sizeScale

                        buildCrescentPath(crescentPath, cx, cy - 4f, dir, startDeg, endDeg, radius, ribbonW * 1.35f, p)
                        voidAuraPaint.color = voidColor
                        canvas.drawPath(crescentPath, voidAuraPaint)

                        buildCrescentPath(crescentPath, cx, cy - 4f, dir, startDeg, endDeg, radius, ribbonW, p)
                        crescentAuraPaint.color = auraColor
                        canvas.drawPath(crescentPath, crescentAuraPaint)

                        buildCrescentPath(coreCrescentPath, cx, cy - 4f, dir, startDeg, endDeg, radius, ribbonW * 0.38f, p)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        val burstX = cx + dir * (28f * sizeScale)
                        drawHorizontalNeedleBurst(
                            canvas,
                            burstX,
                            cy - 4f,
                            dir,
                            p,
                            count = if (isDagger) 4 else 7,
                            maxReach = 58f * sizeScale,
                            colorAura = when {
                                isScarlet -> Color.parseColor("#EF4444")
                                isDagger -> Color.parseColor("#10B981")
                                else -> Color.parseColor("#A855F7")
                            }
                        )

                        computeKatanaTip(cx, cy - 4f, dir, 1, p)
                        drawBladeGlint(canvas, tipX, tipY, 8f * sizeScale, glintColor)
                    }
                    2 -> {
                        val startDeg = 68f
                        val endDeg = -102f
                        val radius = 66f * sizeScale
                        val ribbonW = 26f * sizeScale

                        if (!isDagger) {
                            drawShadowGroundSmoke(canvas, cx + dir * 10f, pb.bottom, p, dir)
                        }

                        buildCrescentPath(crescentPath, cx, cy - 6f, dir, startDeg, endDeg, radius, ribbonW * 1.4f, p)
                        voidAuraPaint.color = voidColor
                        canvas.drawPath(crescentPath, voidAuraPaint)

                        buildCrescentPath(crescentPath, cx, cy - 6f, dir, startDeg, endDeg, radius, ribbonW, p)
                        crescentAuraPaint.color = auraColor
                        crescentEdgePaint.color = edgeColor
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)

                        buildCrescentPath(coreCrescentPath, cx, cy - 6f, dir, startDeg, endDeg, radius, ribbonW * 0.36f, p)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        drawAbyssalNeedleBurst(
                            canvas,
                            cx,
                            cy - 6f,
                            dir,
                            startDeg,
                            endDeg,
                            radius + ribbonW * 0.5f,
                            p,
                            count = if (isDagger) 5 else 9,
                            spikeLen = 26f * sizeScale,
                            colorCore = edgeColor,
                            colorAura = auraColor
                        )

                        computeKatanaTip(cx, cy - 6f, dir, 2, p)
                        drawBladeGlint(canvas, tipX, tipY, 11f * sizeScale, glintColor)
                    }
                    else -> {
                        val startDeg = -118f
                        val endDeg = 48f
                        val radius = 70f * sizeScale
                        val ribbonW = 28f * sizeScale

                        buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 1.35f, p)
                        voidAuraPaint.color = voidColor
                        canvas.drawPath(crescentPath, voidAuraPaint)

                        buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW, p)
                        crescentAuraPaint.color = auraColor
                        crescentEdgePaint.color = edgeColor
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)

                        buildCrescentPath(coreCrescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 0.40f, p)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        buildCrescentPath(crossCrescentPath, cx, cy - 8f, dir, 35f, -65f, 60f * sizeScale, 18f * sizeScale, p)
                        buildCrescentPath(crossCorePath, cx, cy - 8f, dir, 35f, -65f, 60f * sizeScale, 7f * sizeScale, p)
                        crescentAuraPaint.color = auraColor
                        canvas.drawPath(crossCrescentPath, crescentAuraPaint)
                        canvas.drawPath(crossCorePath, crescentCorePaint)

                        drawAbyssalNeedleBurst(
                            canvas,
                            cx,
                            cy - 8f,
                            dir,
                            startDeg,
                            endDeg,
                            radius + ribbonW * 0.5f,
                            p,
                            count = if (isDagger) 6 else 10,
                            spikeLen = 30f * sizeScale,
                            colorCore = edgeColor,
                            colorAura = auraColor
                        )

                        computeKatanaTip(cx, cy - 8f, dir, 3, p)
                        drawBladeGlint(canvas, tipX, tipY, 14f * sizeScale, glintColor)
                    }
                }
            }
            WeaponType.SWORD -> {
                val isSpear = weapon.id == "iron_spear" || weapon.id == "wyrm_wing_halberd"
                val swordScale = if (isSpear) 1.26f else 1.0f
                when (player.attackComboIndex) {
                    1 -> {
                        val startDeg = -46f
                        val endDeg = 32f
                        val radius = 46f * swordScale
                        val ribbonW = if (isSpear) 12f else 15f
                        buildCrescentPath(crescentPath, cx, cy - 5f, dir, startDeg, endDeg, radius, ribbonW, p)
                        buildCrescentPath(coreCrescentPath, cx, cy - 5f, dir, startDeg, endDeg, radius, ribbonW * 0.40f, p)

                        val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)
                        crescentAuraPaint.color = if (weapon.id == "wyrm_wing_halberd") Color.argb((alpha * 0.85f).toInt(), 245, 158, 11) else Color.argb((alpha * 0.85f).toInt(), 148, 163, 184)
                        crescentEdgePaint.color = Color.argb(alpha, 226, 232, 240)
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        computeSwordTip(cx, cy - 5f, dir, 1, p)
                        drawBladeGlint(canvas, tipX, tipY, 6f * swordScale, Color.argb(alpha, 255, 255, 255))
                    }
                    2 -> {
                        val startDeg = 42f
                        val endDeg = -54f
                        val radius = 52f * swordScale
                        val ribbonW = if (isSpear) 13f else 16f
                        buildCrescentPath(crescentPath, cx, cy - 3f, dir, startDeg, endDeg, radius, ribbonW, p)
                        buildCrescentPath(coreCrescentPath, cx, cy - 3f, dir, startDeg, endDeg, radius, ribbonW * 0.40f, p)

                        val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)
                        crescentAuraPaint.color = if (weapon.id == "wyrm_wing_halberd") Color.argb((alpha * 0.88f).toInt(), 245, 158, 11) else Color.argb((alpha * 0.88f).toInt(), 148, 163, 184)
                        crescentEdgePaint.color = Color.argb(alpha, 203, 213, 225)
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        computeSwordTip(cx, cy - 3f, dir, 2, p)
                        drawBladeGlint(canvas, tipX, tipY, 7f * swordScale, Color.argb(alpha, 255, 255, 255))
                    }
                    else -> {
                        computeSwordTip(cx, cy - 5f, dir, 3, p)
                        val thrustReachX = if (isSpear) tipX + dir * 20f else tipX
                        val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)

                        crescentEdgePaint.style = Paint.Style.STROKE
                        crescentEdgePaint.strokeWidth = 2.5f
                        crescentEdgePaint.color = Color.argb(alpha, 255, 255, 255)
                        val hx = cx + dir * 14f
                        canvas.drawLine(hx, cy - 3f, thrustReachX, tipY, crescentEdgePaint)

                        crescentAuraPaint.style = Paint.Style.STROKE
                        crescentAuraPaint.strokeWidth = if (isSpear) 7f else 5f
                        crescentAuraPaint.color = Color.argb((alpha * 0.6f).toInt(), 56, 189, 248)
                        canvas.drawLine(hx, cy - 3f, thrustReachX, tipY, crescentAuraPaint)

                        drawBladeGlint(canvas, thrustReachX, tipY, 11f * swordScale, Color.argb(alpha, 255, 255, 255))
                    }
                }
            }
            WeaponType.CLUB -> {
                val isHammer = weapon.id == "war_hammer"
                val isCrystalMace = weapon.id == "crystal_mace"
                val clubScale = when {
                    isHammer -> 1.15f
                    else -> 1.0f
                }
                val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)
                crescentAuraPaint.color = when {
                    isHammer -> Color.argb((alpha * 0.88f).toInt(), 217, 119, 6)
                    isCrystalMace -> Color.argb((alpha * 0.88f).toInt(), 56, 189, 248)
                    else -> Color.argb((alpha * 0.85f).toInt(), 180, 83, 9)
                }
                crescentEdgePaint.color = when {
                    isHammer -> Color.argb(alpha, 253, 224, 71)
                    isCrystalMace -> Color.argb(alpha, 192, 132, 252)
                    else -> Color.argb(alpha, 245, 158, 11)
                }

                if (player.attackComboIndex == 1) {
                    val startDeg = -75f
                    val endDeg = 36f
                    val radius = 46f * clubScale
                    val ribbonW = 18f * clubScale
                    buildCrescentPath(crescentPath, cx, cy - 6f, dir, startDeg, endDeg, radius, ribbonW, p)
                    buildCrescentPath(coreCrescentPath, cx, cy - 6f, dir, startDeg, endDeg, radius, ribbonW * 0.35f, p)
                    computeClubTip(cx, cy - 6f, dir, 1, p)
                } else {
                    val startDeg = -125f
                    val endDeg = 65f
                    val radius = 52f * clubScale
                    val ribbonW = 22f * clubScale
                    buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW, p)
                    buildCrescentPath(coreCrescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 0.35f, p)
                    computeClubTip(cx, cy - 8f, dir, 2, p)

                    if (p > 0.55f && player.isGrounded) {
                        val shockW = 34f * clubScale * ((p - 0.55f) / 0.45f)
                        canvas.drawLine(tipX - shockW, pb.bottom, tipX + shockW, pb.bottom, crescentEdgePaint)
                    }
                }
                canvas.drawPath(crescentPath, crescentAuraPaint)
                canvas.drawPath(crescentPath, crescentEdgePaint)
                canvas.drawPath(coreCrescentPath, crescentCorePaint)
                drawBladeGlint(canvas, tipX, tipY, 9f * clubScale, Color.argb(alpha, 255, 255, 255))
            }
            WeaponType.GREATSWORD -> {
                val isDarkmoon = weapon.id == "pale_eclipse_gs"
                val isBloodhound = weapon.id == "nightprowler_talon"
                val isBattleAxe = weapon.id == "battle_axe"
                val gsScale = if (isBattleAxe) 0.94f else 1.0f
                val alpha = (sin(p * PI.toFloat()) * 255f).toInt().coerceIn(0, 255)

                val (auraColor, edgeColor) = when {
                    isDarkmoon -> Pair(Color.argb((alpha * 0.95f).toInt(), 56, 189, 248), Color.argb(alpha, 240, 249, 255))
                    isBloodhound -> Pair(Color.argb((alpha * 0.95f).toInt(), 220, 38, 38), Color.argb(alpha, 248, 113, 113))
                    isBattleAxe -> Pair(Color.argb((alpha * 0.95f).toInt(), 225, 29, 72), Color.argb(alpha, 226, 232, 240))
                    else -> Pair(Color.argb((alpha * 0.95f).toInt(), 245, 158, 11), Color.argb(alpha, 253, 224, 71))
                }
                crescentAuraPaint.color = auraColor
                crescentEdgePaint.color = edgeColor

                when (player.attackComboIndex) {
                    1 -> {
                        val startDeg = -135f
                        val endDeg = 68f
                        val radius = 64f * gsScale
                        val ribbonW = 24f * gsScale
                        buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW, p)
                        buildCrescentPath(coreCrescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 0.45f, p)
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        computeGreatswordTip(cx, cy - 8f, dir, 1, p)
                        drawBladeGlint(canvas, tipX, tipY, 13f * gsScale, Color.argb(alpha, 255, 255, 255))

                        if (p > 0.52f && player.isGrounded) {
                            val slamProgress = ((p - 0.52f) / 0.48f).coerceIn(0f, 1f)
                            val shockW = 54f * slamProgress
                            val shockAlpha = ((1f - slamProgress) * 230f).toInt().coerceIn(0, 255)
                            crescentEdgePaint.color = Color.argb(shockAlpha, 253, 224, 71)
                            val groundY = pb.bottom
                            canvas.drawLine(tipX - shockW, groundY, tipX + shockW, groundY, crescentEdgePaint)
                        }
                    }
                    2 -> {
                        val startDeg = 75f
                        val endDeg = -118f
                        val radius = 68f * gsScale
                        val ribbonW = 26f * gsScale
                        buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW, p)
                        buildCrescentPath(coreCrescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 0.45f, p)
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        computeGreatswordTip(cx, cy - 8f, dir, 2, p)
                        drawBladeGlint(canvas, tipX, tipY, 16f * gsScale, Color.argb(alpha, 255, 255, 255))
                    }
                    else -> {
                        val startDeg = -150f
                        val endDeg = 82f
                        val radius = 74f * gsScale
                        val ribbonW = 28f * gsScale
                        buildCrescentPath(crescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW, p)
                        buildCrescentPath(coreCrescentPath, cx, cy - 8f, dir, startDeg, endDeg, radius, ribbonW * 0.48f, p)
                        canvas.drawPath(crescentPath, crescentAuraPaint)
                        canvas.drawPath(crescentPath, crescentEdgePaint)
                        canvas.drawPath(coreCrescentPath, crescentCorePaint)

                        computeGreatswordTip(cx, cy - 8f, dir, 1, p)
                        drawBladeGlint(canvas, tipX, tipY, 18f * gsScale, Color.argb(alpha, 255, 255, 255))
                    }
                }
            }
            WeaponType.STAFF -> {
                val tipStaffX = cx + (dir * 36f)
                val tipStaffY = cy - 8f
                val rot = globalTime * 6f

                // Outer and inner rotating arcane sigils
                canvas.drawCircle(tipStaffX, tipStaffY, 26f, spellAuraPaint)
                canvas.drawCircle(tipStaffX, tipStaffY, 14f, spellAuraPaint)

                // 4 Orbiting celestial energy nodes
                for (i in 0..3) {
                    val angle = rot + (i * (PI.toFloat() / 2f))
                    val nx = tipStaffX + cos(angle.toDouble()).toFloat() * 20f
                    val ny = tipStaffY + sin(angle.toDouble()).toFloat() * 20f
                    canvas.drawCircle(nx, ny, 4f, spellNodePaint)
                }

                // Expanding cast shockwave ring
                if (p in 0.25f..0.85f) {
                    val ringProgress = ((p - 0.25f) / 0.60f).coerceIn(0f, 1f)
                    val ringR = 12f + (ringProgress * 42f)
                    val ringAlpha = ((1f - ringProgress) * 220f).toInt().coerceIn(0, 255)
                    spellWavePaint.color = Color.argb(ringAlpha, 56, 189, 248)
                    canvas.drawCircle(tipStaffX, tipStaffY, ringR, spellWavePaint)
                }
                canvas.drawCircle(tipStaffX, tipStaffY, 6f, spellCorePaint)
            }
            WeaponType.BOW, WeaponType.SCYTHE, WeaponType.GAUNTLET -> {
                // Handled by SlashAnimator & particle sparks
            }
        }
    }

    private fun drawWeaponBadge(canvas: Canvas, player: Player, screenWidth: Float) {
        val weapon = player.equippedWeapon
        val damage = weapon.calculateDamage(player.stats).toInt()
        val penalty = weapon.calculateStatPenalty(player.stats).toInt()
        val hasReq = weapon.hasStatRequirementsMet(player.stats)
        val cardW = 260f
        val cardH = 48f
        val cardX = screenWidth - cardW - 36f - 56f
        val cardY = 30f

        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 8f, 8f, weaponCardPaint)
        weaponCardBorder.color = if (hasReq) Color.parseColor("#38BDF8") else Color.parseColor("#EF4444")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 8f, 8f, weaponCardBorder)

        weaponBadgeIconRect.set(cardX + 8f, cardY + 7f, cardX + 42f, cardY + 41f)
        val drewIcon = playerAnimator.weaponRenderer.drawIcon(canvas, weapon.id, weaponBadgeIconRect)
        val stanceBadge = if (weapon.isTwoHanded) "2H" else "1H"
        val dmgSubtext = if (hasReq) "DMG: $damage [$stanceBadge] 🔄" else "DMG: $damage (-$penalty PENALTY) 🔄"
        weaponDmgPaint.color = if (hasReq) Color.parseColor("#38BDF8") else Color.parseColor("#F87171")

        if (drewIcon) {
            canvas.drawText("${weapon.name} [$stanceBadge]", cardX + 48f, cardY + 22f, weaponNamePaint)
            canvas.drawText(dmgSubtext, cardX + 48f, cardY + 39f, weaponDmgPaint)
        } else {
            val icon = when (weapon.type) {
                WeaponType.KATANA -> "🗡"
                WeaponType.SWORD -> "⚔"
                WeaponType.GREATSWORD -> "🪓"
                WeaponType.STAFF -> "🔮"
                WeaponType.CLUB -> "🪵"
                WeaponType.BOW -> "🏹"
                WeaponType.SCYTHE -> "⚔"
                WeaponType.GAUNTLET -> "🥊"
            }
            canvas.drawText("$icon ${weapon.name} [$stanceBadge]", cardX + 12f, cardY + 22f, weaponNamePaint)
            canvas.drawText(dmgSubtext, cardX + 12f, cardY + 39f, weaponDmgPaint)
        }
    }

    val armorBadgeRect = RectF()
    private val armorNamePaint = Paint().apply {
        color = Color.WHITE
        textSize = 12f
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val armorDefPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 10.5f
        isAntiAlias = true
    }

    private fun drawArmorBadge(canvas: Canvas, player: Player, screenWidth: Float) {
        val armor = player.equippedArmor
        val cardW = 236f
        val cardH = 36f
        val cardX = screenWidth - cardW - 36f - 56f
        val cardY = 82f
        armorBadgeRect.set(cardX, cardY, cardX + cardW, cardY + cardH)

        canvas.drawRoundRect(armorBadgeRect, 8f, 8f, weaponCardPaint)
        canvas.drawRoundRect(armorBadgeRect, 8f, 8f, weaponCardBorder)

        val bmp = playerAnimator.armorRenderer.getBitmap(armor.assetFile)
        val iconSize = 26f
        val iconRect = RectF(cardX + 8f, cardY + 5f, cardX + 8f + iconSize, cardY + 5f + iconSize)
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, iconRect, null)
        }
        canvas.drawText(armor.name, cardX + 40f, cardY + 16f, armorNamePaint)
        canvas.drawText("DEF: +${armor.physicalDef.toInt()}%  •  POISE: +${armor.poise.toInt()}  [ARMOR]", cardX + 40f, cardY + 30f, armorDefPaint)
    }

    fun drawPauseButton(canvas: Canvas, screenWidth: Float, pauseBtnRect: RectF) {
        val size = 46f
        val px = screenWidth - 36f - size
        val py = 30f
        pauseBtnRect.set(px, py, px + size, py + size)

        pauseBtnBgPaint.color = Color.parseColor("#77101726")
        canvas.drawRoundRect(pauseBtnRect, 10f, 10f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(pauseBtnRect, 10f, 10f, pauseBtnBorderPaint)

        // Draw sleek vertical pause bars ❚❚
        val barW = 4.5f
        val barH = 18f
        val barGap = 6f
        val cx = pauseBtnRect.centerX()
        val cy = pauseBtnRect.centerY()
        pauseBtnIconPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(cx - barGap / 2f - barW, cy - barH / 2f, cx - barGap / 2f, cy + barH / 2f, 2f, 2f, pauseBtnIconPaint)
        canvas.drawRoundRect(cx + barGap / 2f, cy - barH / 2f, cx + barGap / 2f + barW, cy + barH / 2f, 2f, 2f, pauseBtnIconPaint)
    }

    fun drawPauseMenu(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        continueRect: RectF,
        equipmentRect: RectF,
        restartCheckpointRect: RectF,
        restartGameRect: RectF,
        saveRect: RectF,
        loadRect: RectF,
        closeRect: RectF,
        player: Player
    ) {
        // 1. Dark frosted glass background overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, pauseOverlayPaint)

        // 2. Centered Modal Box
        val modalW = 560f
        val modalH = 520f
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val left = cx - modalW / 2f
        val top = cy - modalH / 2f
        val right = cx + modalW / 2f
        val bottom = cy + modalH / 2f

        // Outer cyan glow & main modal container
        pauseModalGlowPaint.color = Color.argb(45, 56, 189, 248)
        canvas.drawRoundRect(left - 4f, top - 4f, right + 4f, bottom + 4f, 18f, 18f, pauseModalGlowPaint)
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, pauseModalBgPaint)
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, pauseModalBorderPaint)

        // Close '✕' button at top-right of modal
        closeRect.set(right - 44f, top + 14f, right - 14f, top + 44f)
        pauseBtnBgPaint.color = Color.parseColor("#152033")
        canvas.drawRoundRect(closeRect, 6f, 6f, pauseBtnBgPaint)
        pauseDividerPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(closeRect, 6f, 6f, pauseDividerPaint)
        pauseBadgePaint.color = Color.parseColor("#94A3B8")
        pauseBadgePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("✕", closeRect.centerX(), closeRect.centerY() + 4.5f, pauseBadgePaint)

        // Header Title & Subtitle
        canvas.drawText("✦  GAME PAUSED  ✦", cx, top + 34f, pauseTitlePaint)
        canvas.drawText("EXPEDITION SUSPENDED  •  SOULS OF THE ABYSS", cx, top + 51f, pauseSubTitlePaint)

        // Divider
        pauseDividerPaint.color = Color.parseColor("#334155")
        canvas.drawLine(left + 28f, top + 64f, right - 28f, top + 64f, pauseDividerPaint)

        // Player Snapshot info strip
        val infoY = top + 83f
        val originName = player.currentOrigin.name
        val weaponName = player.equippedWeapon.name
        val dmg = player.equippedWeapon.calculateDamage(player.stats).toInt()
        val penalty = player.equippedWeapon.calculateStatPenalty(player.stats).toInt()
        val penaltyStr = if (penalty > 0) " (-$penalty PENALTY)" else ""
        val infoText = "$originName  |  $weaponName [${player.equippedWeapon.stance.displayName}] (DMG: $dmg$penaltyStr)  |  ✦ ${player.stats.essence} Essence"
        pauseSubTitlePaint.color = Color.parseColor("#E2E8F0")
        canvas.drawText(infoText, cx, infoY, pauseSubTitlePaint)

        // Action Buttons
        val btnW = modalW - 56f
        val btnH = 46f
        val btnLeft = cx - btnW / 2f
        val btnRight = cx + btnW / 2f
        val startY = top + 98f
        val rowGap = 52f

        // 1. CONTINUE BUTTON (Interactive)
        continueRect.set(btnLeft, startY, btnRight, startY + btnH)
        pauseModalGlowPaint.color = Color.argb(40, 56, 189, 248)
        canvas.drawRoundRect(continueRect, 8f, 8f, pauseModalGlowPaint)
        pauseBtnBgPaint.color = Color.parseColor("#152238")
        canvas.drawRoundRect(continueRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(continueRect, 8f, 8f, pauseBtnBorderPaint)

        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("▶  CONTINUE", continueRect.left + 16f, continueRect.top + 21f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Resume your expedition through the shadows", continueRect.left + 16f, continueRect.top + 37f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#38BDF8")
        pauseBadgePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("RESUME ➔", continueRect.right - 16f, continueRect.centerY() + 4.5f, pauseBadgePaint)

        // 2. ARMOR & EQUIPMENT BUTTON (Interactive)
        val equipY = startY + rowGap
        equipmentRect.set(btnLeft, equipY, btnRight, equipY + btnH)
        pauseModalGlowPaint.color = Color.argb(40, 56, 189, 248)
        canvas.drawRoundRect(equipmentRect, 8f, 8f, pauseModalGlowPaint)
        pauseBtnBgPaint.color = Color.parseColor("#152238")
        canvas.drawRoundRect(equipmentRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(equipmentRect, 8f, 8f, pauseBtnBorderPaint)

        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("🛡️  ARMOR & EQUIPMENT", equipmentRect.left + 16f, equipmentRect.top + 21f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Equipped: ${player.equippedArmor.name} (+${player.equippedArmor.physicalDef.toInt()}% DEF)", equipmentRect.left + 16f, equipmentRect.top + 37f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#38BDF8")
        canvas.drawText("CUSTOMIZE ➔", equipmentRect.right - 16f, equipmentRect.centerY() + 4.5f, pauseBadgePaint)

        // 3. RESTART CHECKPOINT (Interactive)
        val restartY = equipY + rowGap
        restartCheckpointRect.set(btnLeft, restartY, btnRight, restartY + btnH)
        pauseModalGlowPaint.color = Color.argb(40, 245, 158, 11)
        canvas.drawRoundRect(restartCheckpointRect, 8f, 8f, pauseModalGlowPaint)
        pauseBtnBgPaint.color = Color.parseColor("#241812")
        canvas.drawRoundRect(restartCheckpointRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(restartCheckpointRect, 8f, 8f, pauseBtnBorderPaint)

        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("↺  RESTART CHECKPOINT", restartCheckpointRect.left + 16f, restartCheckpointRect.top + 21f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#FCD34D")
        canvas.drawText("Respawn at last safe altar & replenish flasks", restartCheckpointRect.left + 16f, restartCheckpointRect.top + 37f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#F59E0B")
        canvas.drawText("RESPAWN ↺", restartCheckpointRect.right - 16f, restartCheckpointRect.centerY() + 4.5f, pauseBadgePaint)

        // 4. RESTART GAME (Interactive)
        val restartGameY = restartY + rowGap
        restartGameRect.set(btnLeft, restartGameY, btnRight, restartGameY + btnH)
        pauseModalGlowPaint.color = Color.argb(40, 239, 68, 68)
        canvas.drawRoundRect(restartGameRect, 8f, 8f, pauseModalGlowPaint)
        pauseBtnBgPaint.color = Color.parseColor("#2A1215")
        canvas.drawRoundRect(restartGameRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#EF4444")
        canvas.drawRoundRect(restartGameRect, 8f, 8f, pauseBtnBorderPaint)

        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("⚡  RESTART GAME", restartGameRect.left + 16f, restartGameRect.top + 21f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#FCA5A5")
        canvas.drawText("Restart run & return to Character Creation", restartGameRect.left + 16f, restartGameRect.top + 37f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#EF4444")
        canvas.drawText("CHARACTER ↺", restartGameRect.right - 16f, restartGameRect.centerY() + 4.5f, pauseBadgePaint)

        // 5. SAVE & LOAD ROW (Interactive)
        val saveLoadY = restartGameY + rowGap
        val halfW = (btnW - 12f) / 2f
        saveRect.set(btnLeft, saveLoadY, btnLeft + halfW, saveLoadY + btnH)
        loadRect.set(btnLeft + halfW + 12f, saveLoadY, btnRight, saveLoadY + btnH)

        pauseBtnBgPaint.color = Color.parseColor("#0F1420")
        canvas.drawRoundRect(saveRect, 8f, 8f, pauseBtnBgPaint)
        pauseDividerPaint.color = Color.parseColor("#334155")
        canvas.drawRoundRect(saveRect, 8f, 8f, pauseDividerPaint)
        canvas.drawRoundRect(loadRect, 8f, 8f, pauseBtnBgPaint)
        canvas.drawRoundRect(loadRect, 8f, 8f, pauseDividerPaint)

        pauseItemTitlePaint.textSize = 13f
        pauseItemTitlePaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("💾  SAVE SOUL MEMORY", saveRect.left + 12f, saveRect.centerY() + 4.5f, pauseItemTitlePaint)
        canvas.drawText("📂  LOAD SOUL MEMORY", loadRect.left + 12f, loadRect.centerY() + 4.5f, pauseItemTitlePaint)

        // Footer hint
        val footerY = bottom - 14f
        pauseItemSubPaint.color = Color.parseColor("#64748B")
        pauseItemSubPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Progress is tethered to active Altars  •  Tap outside to resume", cx, footerY, pauseItemSubPaint)
        pauseItemSubPaint.textAlign = Paint.Align.LEFT
    }

    fun drawArmoryScreen(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        player: Player,
        selectedArmorIndex: Int,
        armorSlotRects: List<RectF>,
        equipBtnRect: RectF,
        closeBtnRect: RectF
    ) {
        playerAnimator.armorRenderer.drawArmoryScreen(
            canvas,
            screenWidth,
            screenHeight,
            player,
            selectedArmorIndex,
            armorSlotRects,
            equipBtnRect,
            closeBtnRect,
            globalTime
        )
    }

    private fun drawBossHealthBar(canvas: Canvas, level: Level, screenWidth: Float, screenHeight: Float) {
        val boss = level.enemies.firstOrNull {
            it.isAlive && it.isAggroed && (it.isZoneBoss || it.isDemonBossPhase || it.isShadowVesselAwakened || it.isShadowEcho)
        } ?: return

        val title = when {
            boss.bossTitle.isNotEmpty() -> boss.bossTitle
            boss.isDemonBossPhase -> "DEMON SLIME, ABYSSAL VESSEL"
            boss.isShadowVesselAwakened -> "AWAKENED SHADOW VESSEL"
            else -> "SHADOW BOSS"
        }

        val barW = (screenWidth * 0.50f).coerceIn(360f, 640f)
        val barH = 14f
        val barX = (screenWidth - barW) / 2f
        val barY = screenHeight - 62f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#FDE68A")
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }
        val bgPaint = Paint().apply {
            color = Color.parseColor("#CC0F172A")
            style = Paint.Style.FILL
        }
        val fillPaint = Paint().apply {
            color = if (boss.isShadowEcho) Color.parseColor("#A855F7") else Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        canvas.drawText("⚔  $title  ⚔", screenWidth / 2f, barY - 10f, titlePaint)
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 5f, 5f, bgPaint)
        val ratio = (boss.currentHp / boss.maxHp).coerceIn(0f, 1f)
        if (ratio > 0f) {
            canvas.drawRoundRect(barX, barY, barX + barW * ratio, barY + barH, 5f, 5f, fillPaint)
        }
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 5f, 5f, borderPaint)
    }

    fun drawGraceMenu(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        restRect: RectF,
        leaveRect: RectF,
        closeRect: RectF,
        attrRects: Array<RectF>,
        bloodPactRect: RectF,
        asceticPactRect: RectF,
        player: Player
    ) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, pauseOverlayPaint)

        val modalW = 580f
        val modalH = 502f
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val left = cx - modalW / 2f
        val top = cy - modalH / 2f
        val right = cx + modalW / 2f
        val bottom = cy + modalH / 2f

        pauseModalGlowPaint.color = Color.argb(55, 245, 158, 11)
        canvas.drawRoundRect(left - 4f, top - 4f, right + 4f, bottom + 4f, 18f, 18f, pauseModalGlowPaint)
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, pauseModalBgPaint)
        pauseModalBorderPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(left, top, right, bottom, 14f, 14f, pauseModalBorderPaint)
        pauseModalBorderPaint.color = Color.parseColor("#38BDF8")

        // Close '✕' button
        closeRect.set(right - 44f, top + 14f, right - 14f, top + 44f)
        pauseBtnBgPaint.color = Color.parseColor("#241812")
        canvas.drawRoundRect(closeRect, 6f, 6f, pauseBtnBgPaint)
        pauseDividerPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(closeRect, 6f, 6f, pauseDividerPaint)
        pauseBadgePaint.color = Color.parseColor("#FCD34D")
        pauseBadgePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("✕", closeRect.centerX(), closeRect.centerY() + 4.5f, pauseBadgePaint)

        // Header
        pauseTitlePaint.color = Color.parseColor("#FCD34D")
        canvas.drawText("✦  SITE OF GRACE  ✦", cx, top + 32f, pauseTitlePaint)
        pauseTitlePaint.color = Color.WHITE
        pauseSubTitlePaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("REST, ATTUNE & STRENGTHEN YOUR SOUL", cx, top + 49f, pauseSubTitlePaint)

        pauseDividerPaint.color = Color.parseColor("#334155")
        canvas.drawLine(left + 28f, top + 60f, right - 28f, top + 60f, pauseDividerPaint)

        // Player Status Strip
        val stats = player.stats
        val infoText = "LV ${stats.level}  •  ✦ ${stats.essence} Essence  •  Flasks: ${stats.currentCrimsonFlasks}/${stats.maxCrimsonFlasks} HP, ${stats.currentCeruleanFlasks}/${stats.maxCeruleanFlasks} FP"
        pauseSubTitlePaint.color = Color.parseColor("#E2E8F0")
        canvas.drawText(infoText, cx, top + 78f, pauseSubTitlePaint)

        // 1. REST AT GRACE
        val btnW = modalW - 56f
        val btnLeft = cx - btnW / 2f
        val btnRight = cx + btnW / 2f
        restRect.set(btnLeft, top + 92f, btnRight, top + 144f)
        pauseModalGlowPaint.color = Color.argb(45, 245, 158, 11)
        canvas.drawRoundRect(restRect, 10f, 10f, pauseModalGlowPaint)
        pauseBtnBgPaint.color = Color.parseColor("#241812")
        canvas.drawRoundRect(restRect, 10f, 10f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(restRect, 10f, 10f, pauseBtnBorderPaint)

        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("🕯  REST AT GRACE", restRect.left + 16f, restRect.top + 23f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#FCD34D")
        canvas.drawText("Restore HP, FP & Flasks  •  Respawns defeated foes", restRect.left + 16f, restRect.top + 41f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#F59E0B")
        pauseBadgePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("REST ↺", restRect.right - 16f, restRect.centerY() + 4.5f, pauseBadgePaint)

        // 2. LEVEL UP SECTION
        val cost = stats.costForNextLevel()
        val canAfford = stats.essence >= cost
        pauseSubTitlePaint.color = if (canAfford) Color.parseColor("#4ADE80") else Color.parseColor("#94A3B8")
        canvas.drawText("LEVEL UP  •  Next Level Cost: ✦ $cost Essence", cx, top + 164f, pauseSubTitlePaint)

        val colGap = 12f
        val colW = (btnW - colGap) / 2f
        val attrH = 44f
        val attrRowGap = 52f
        val attrStartY = top + 178f

        for (i in 0 until 5) {
            val row = i / 2
            val col = i % 2
            val aLeft = btnLeft + col * (colW + colGap)
            val aTop = attrStartY + row * attrRowGap
            val aRight = if (i == 4) btnRight else aLeft + colW
            attrRects[i].set(aLeft, aTop, aRight, aTop + attrH)

            pauseBtnBgPaint.color = if (canAfford) Color.parseColor("#13231D") else Color.parseColor("#0F1420")
            canvas.drawRoundRect(attrRects[i], 8f, 8f, pauseBtnBgPaint)
            pauseBtnBorderPaint.color = if (canAfford) Color.parseColor("#22C55E") else Color.parseColor("#334155")
            canvas.drawRoundRect(attrRects[i], 8f, 8f, pauseBtnBorderPaint)

            val label = when (i) {
                0 -> "VIGOR  ${stats.vigor}"
                1 -> "ENDURANCE  ${stats.endurance}"
                2 -> "STRENGTH  ${stats.strength}"
                3 -> "AGILITY  ${stats.agility}"
                else -> "INTELLIGENCE  ${stats.intelligence}"
            }
            val bonus = when (i) {
                0 -> "+15 Max HP"
                1 -> "+8 Max Stam"
                2 -> "+Melee DMG"
                3 -> "+Crit/Speed"
                else -> "+10 Max FP"
            }
            pauseItemTitlePaint.color = if (canAfford) Color.WHITE else Color.parseColor("#94A3B8")
            canvas.drawText(label, aLeft + 12f, aTop + 19f, pauseItemTitlePaint)
            pauseItemSubPaint.color = if (canAfford) Color.parseColor("#86EFAC") else Color.parseColor("#64748B")
            canvas.drawText(bonus, aLeft + 12f, aTop + 35f, pauseItemSubPaint)
            pauseBadgePaint.color = if (canAfford) Color.parseColor("#4ADE80") else Color.parseColor("#64748B")
            pauseBadgePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(if (canAfford) "+1 ▲" else "LOCKED", aRight - 12f, attrRects[i].centerY() + 4.5f, pauseBadgePaint)
        }

        // 2b. SHADOW PACTS (Risk-Reward Modifiers)
        val pactY = top + 340f
        val pactH = 48f
        bloodPactRect.set(btnLeft, pactY, btnLeft + colW, pactY + pactH)
        asceticPactRect.set(btnLeft + colW + colGap, pactY, btnRight, pactY + pactH)

        val bloodActive = stats.bloodPactActive
        pauseBtnBgPaint.color = if (bloodActive) Color.parseColor("#3B0A0A") else Color.parseColor("#0F1420")
        canvas.drawRoundRect(bloodPactRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = if (bloodActive) Color.parseColor("#EF4444") else Color.parseColor("#334155")
        canvas.drawRoundRect(bloodPactRect, 8f, 8f, pauseBtnBorderPaint)
        pauseItemTitlePaint.color = if (bloodActive) Color.parseColor("#FCA5A5") else Color.WHITE
        canvas.drawText("🩸 BLOOD PACT ${if (bloodActive) "[ON]" else "[OFF]"}", bloodPactRect.left + 10f, pactY + 19f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("+35% Enemy DMG • +60% Essence", bloodPactRect.left + 10f, pactY + 37f, pauseItemSubPaint)

        val asceticActive = stats.asceticPactActive
        pauseBtnBgPaint.color = if (asceticActive) Color.parseColor("#2E1065") else Color.parseColor("#0F1420")
        canvas.drawRoundRect(asceticPactRect, 8f, 8f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = if (asceticActive) Color.parseColor("#A855F7") else Color.parseColor("#334155")
        canvas.drawRoundRect(asceticPactRect, 8f, 8f, pauseBtnBorderPaint)
        pauseItemTitlePaint.color = if (asceticActive) Color.parseColor("#E9D5FF") else Color.WHITE
        canvas.drawText("🕯 ASCETIC PACT ${if (asceticActive) "[ON]" else "[OFF]"}", asceticPactRect.left + 10f, pactY + 19f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("No Flasks • -50% Skill FP Cost", asceticPactRect.left + 10f, pactY + 37f, pauseItemSubPaint)

        // 3. LEAVE GRACE
        leaveRect.set(btnLeft, top + 402f, btnRight, top + 452f)
        pauseBtnBgPaint.color = Color.parseColor("#152033")
        canvas.drawRoundRect(leaveRect, 10f, 10f, pauseBtnBgPaint)
        pauseBtnBorderPaint.color = Color.parseColor("#64748B")
        canvas.drawRoundRect(leaveRect, 10f, 10f, pauseBtnBorderPaint)
        pauseItemTitlePaint.color = Color.WHITE
        canvas.drawText("▶  STAND UP & LEAVE GRACE", leaveRect.left + 16f, leaveRect.top + 23f, pauseItemTitlePaint)
        pauseItemSubPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Return to the dungeon without resetting enemies", leaveRect.left + 16f, leaveRect.top + 40f, pauseItemSubPaint)
        pauseBadgePaint.color = Color.parseColor("#CBD5E1")
        pauseBadgePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("RESUME ➔", leaveRect.right - 16f, leaveRect.centerY() + 4.5f, pauseBadgePaint)

        // Footer hint
        pauseItemSubPaint.color = Color.parseColor("#64748B")
        pauseItemSubPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Grace sets your respawn point  •  Tap outside to stand up", cx, bottom - 14f, pauseItemSubPaint)
        pauseItemSubPaint.textAlign = Paint.Align.LEFT
    }

    private fun renderDynamicLighting(
        canvas: Canvas,
        level: Level,
        player: Player,
        projectiles: List<MagicProjectile>,
        cameraX: Float,
        cameraY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val bw = (screenWidth * lightScale).toInt().coerceAtLeast(1)
        val bh = (screenHeight * lightScale).toInt().coerceAtLeast(1)

        if (lightBitmap == null || lightBitmap?.width != bw || lightBitmap?.height != bh) {
            lightBitmap?.recycle()
            lightBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            lightCanvas = Canvas(lightBitmap!!)
        }

        val lCanvas = lightCanvas ?: return
        val lBmp = lightBitmap ?: return

        // 1. Fill mask with ambient darkness (0 allocations)
        lCanvas.drawColor(Color.argb(145, 4, 7, 14), PorterDuff.Mode.SRC)

        // 2. Player Lantern Aura via Pre-rendered Light Cookie (0 allocations)
        val playerCenterX = player.bounds.centerX
        val playerCenterY = player.bounds.centerY
        val pScreenX = (playerCenterX - cameraX) * lightScale
        val pScreenY = (playerCenterY - cameraY) * lightScale

        val flicker = (sin(globalTime * 4f) * 14f + sin(globalTime * 8.5f) * 6f) * lightScale
        val playerLightRadius = (680f * lightScale) + flicker

        lightDestRect.set(
            pScreenX - playerLightRadius,
            pScreenY - playerLightRadius,
            pScreenX + playerLightRadius,
            pScreenY + playerLightRadius
        )
        lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)

        // Weapon strike light flare in the dark mask (illuminates dark corridors during slash)
        if (player.isAttacking) {
            val p = player.attackProgress
            val dir = if (player.facingRight) 1f else -1f
            val flashRadius = (380f + 160f * sin(p * PI.toFloat())) * lightScale
            val atkX = (playerCenterX + (dir * 55f) - cameraX) * lightScale
            val atkY = (playerCenterY - 8f - cameraY) * lightScale
            lightDestRect.set(atkX - flashRadius, atkY - flashRadius, atkX + flashRadius, atkY + flashRadius)
            lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
        }

        // 3. Magic Projectiles cast light through the dark
        for (proj in projectiles) {
            val px = (proj.x - cameraX) * lightScale
            val py = (proj.y - cameraY) * lightScale
            val projRadius = 180f * lightScale
            lightDestRect.set(px - projRadius, py - projRadius, px + projRadius, py + projRadius)
            lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
        }

        // 4. Interactive World Lights (Checkpoints, Switches, Spikes, Portal)
        val bwF = bw.toFloat()
        val bhF = bh.toFloat()
        for (platform in level.platforms) {
            val cx = (platform.bounds.centerX - cameraX) * lightScale
            val cy = (platform.bounds.centerY - cameraY) * lightScale

            if (cx < -300f || cx > bwF + 300f || cy < -300f || cy > bhF + 300f) continue

            when (platform.type) {
                PlatformType.CHECKPOINT -> {
                    if (platform.isGraceMimic && platform.isMimicAwakened && !platform.isMimicDefeated) {
                        // Static platform light suspended while fighting
                    } else {
                        val radius = (if (platform.isActivated) 320f else 160f) * lightScale
                        lightDestRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                        lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
                    }
                }

                PlatformType.SWITCH -> {
                    val radius = (if (platform.isActivated) 260f else 180f) * lightScale
                    lightDestRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                    lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
                }

                PlatformType.PRESSURE_PLATE -> {
                    if (platform.isActivated) {
                        val radius = 200f * lightScale
                        lightDestRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                        lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
                    }
                }

                PlatformType.EXIT -> {
                    val radius = (500f + sin(globalTime * 2f) * 30f) * lightScale
                    lightDestRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                    lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
                }

                PlatformType.HAZARD -> {
                    val radius = 120f * lightScale
                    lightDestRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
                    lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
                }

                else -> {}
            }
        }

        // Souls Bloodstain Light
        if (level.hasBloodstain) {
            val bsX = (level.bloodstainX - cameraX) * lightScale
            val bsY = (level.bloodstainY - cameraY) * lightScale
            val radius = 260f * lightScale
            lightDestRect.set(bsX - radius, bsY - radius, bsX + radius, bsY + radius)
            lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, lightCutoutPaint)
        }

        // Subtle Enemy Rim Visibility Cutout
        for (enemy in level.enemies) {
            if (!enemy.isAlive && enemy.deathFadeTimer > 1.3f) continue
            val ex = (enemy.bounds.centerX - cameraX) * lightScale
            val ey = (enemy.bounds.centerY - cameraY) * lightScale
            val radius = (if (enemy.type == com.example.shadowescape.model.EnemyType.ELITE_GUARDIAN) 150f else 105f) * lightScale
            lightDestRect.set(ex - radius, ey - radius, ex + radius, ey + radius)
            lCanvas.drawBitmap(lightCookieBitmap, null, lightDestRect, enemyLightCutoutPaint)
        }

        // 5. Blit darkness mask to main screen (GPU texture quad)
        screenRect.set(0f, 0f, screenWidth, screenHeight)
        canvas.drawBitmap(lBmp, null, screenRect, lightBlendPaint)

        // Souls Bloodstain Golden Bloom
        if (level.hasBloodstain) {
            val bx = level.bloodstainX - cameraX
            val by = level.bloodstainY - cameraY
            lightDestRect.set(bx - 140f, by - 140f, bx + 140f, by + 140f)
            canvas.drawBitmap(amberBloomCookie, null, lightDestRect, bloomPaint)
        }

        // 6. Soft Colored Blooms via pre-rendered bloom cookies
        val playerScreenX = playerCenterX - cameraX
        val playerScreenY = playerCenterY - cameraY
        val pBloomRadius = 380f
        lightDestRect.set(playerScreenX - pBloomRadius, playerScreenY - pBloomRadius, playerScreenX + pBloomRadius, playerScreenY + pBloomRadius)
        canvas.drawBitmap(playerLanternCookie, null, lightDestRect, bloomPaint)

        // Weapon attack color bloom
        if (player.isAttacking) {
            val p = player.attackProgress
            val dir = if (player.facingRight) 1f else -1f
            val atkScreenX = playerCenterX + (dir * 55f) - cameraX
            val atkScreenY = playerCenterY - 8f - cameraY
            val bloomR = 260f * sin(p * PI.toFloat())
            if (bloomR > 8f) {
                lightDestRect.set(atkScreenX - bloomR, atkScreenY - bloomR, atkScreenX + bloomR, atkScreenY + bloomR)
                val cookie = when (player.equippedWeapon.type) {
                    WeaponType.KATANA, WeaponType.SWORD, WeaponType.SCYTHE -> cyanBloomCookie
                    WeaponType.GREATSWORD, WeaponType.CLUB, WeaponType.BOW, WeaponType.GAUNTLET -> amberBloomCookie
                    WeaponType.STAFF -> purpleBloomCookie
                }
                canvas.drawBitmap(cookie, null, lightDestRect, bloomPaint)
            }
        }

        for (platform in level.platforms) {
            val ox = platform.bounds.centerX - cameraX
            val oy = platform.bounds.centerY - cameraY

            if (ox < -250f || ox > screenWidth + 250f || oy < -250f || oy > screenHeight + 250f) continue

            when (platform.type) {
                PlatformType.CHECKPOINT -> {
                    if (platform.isGraceMimic && platform.isMimicAwakened && !platform.isMimicDefeated) {
                        // Static platform bloom suspended while fighting
                    } else {
                        val r = 200f
                        lightDestRect.set(ox - r, oy - r, ox + r, oy + r)
                        val cookie = if (platform.isGraceMimic && !platform.isMimicDefeated) purpleBloomCookie else cyanBloomCookie
                        canvas.drawBitmap(cookie, null, lightDestRect, bloomPaint)
                    }
                }

                PlatformType.SWITCH -> {
                    val r = 150f
                    lightDestRect.set(ox - r, oy - r, ox + r, oy + r)
                    val cookie = if (platform.isActivated) cyanBloomCookie else amberBloomCookie
                    canvas.drawBitmap(cookie, null, lightDestRect, bloomPaint)
                }

                PlatformType.EXIT -> {
                    val r = 300f
                    lightDestRect.set(ox - r, oy - r, ox + r, oy + r)
                    canvas.drawBitmap(purpleBloomCookie, null, lightDestRect, bloomPaint)
                }

                PlatformType.HAZARD -> {
                    val r = 90f
                    lightDestRect.set(ox - r, oy - r, ox + r, oy + r)
                    canvas.drawBitmap(redBloomCookie, null, lightDestRect, bloomPaint)
                }

                else -> {}
            }
        }
    }

    private fun drawGlowingEyes(canvas: Canvas, player: Player, cameraX: Float, cameraY: Float) {
        val pb = player.bounds
        val eyeX = (if (player.facingRight) pb.x + 22f else pb.x + 14f) - cameraX
        val eyeY = (pb.y + 17f) - cameraY

        when (player.currentOrigin.id) {
            "vanguard" -> {
                playerEyeGlowPaint.color = Color.argb(160, 245, 158, 11)
                playerEyeCorePaint.color = Color.parseColor("#FDE047")
            }
            "executioner" -> {
                playerEyeGlowPaint.color = Color.argb(170, 239, 68, 68)
                playerEyeCorePaint.color = Color.parseColor("#EF4444")
            }
            "sorcerer" -> {
                playerEyeGlowPaint.color = Color.argb(160, 224, 242, 254)
                playerEyeCorePaint.color = Color.parseColor("#E0F2FE")
            }
            "forsaken" -> {
                playerEyeGlowPaint.color = Color.argb(160, 245, 158, 11)
                playerEyeCorePaint.color = Color.parseColor("#F59E0B")
            }
            else -> {
                playerEyeGlowPaint.color = Color.argb(150, 0, 229, 255)
                playerEyeCorePaint.color = Color.parseColor("#00E5FF")
            }
        }

        canvas.drawCircle(eyeX, eyeY, 6f, playerEyeGlowPaint)
        canvas.drawCircle(eyeX, eyeY, 2.2f, playerEyeCorePaint)
    }

    private fun drawSoulsHud(canvas: Canvas, player: Player) {
        val stats = player.stats
        val startX = 32f
        val startY = 24f
        val barH = 10f

        // Draw Player Origin Avatar Emblem Crest
        val origin = player.currentOrigin
        val avatarBmp = avatarBitmaps[origin.avatarId] ?: avatarBitmaps[origin.id]
        val emblemSize = 52f
        val emblemX = startX
        val emblemY = startY

        val originThemeColor = try {
            Color.parseColor(origin.themeColorHex)
        } catch (_: Exception) {
            Color.parseColor("#38BDF8")
        }

        hudAvatarRect.set(emblemX, emblemY, emblemX + emblemSize, emblemY + emblemSize)
        canvas.drawCircle(hudAvatarRect.centerX(), hudAvatarRect.centerY(), emblemSize / 2f, hudAvatarBgPaint)

        if (avatarBmp != null) {
            canvas.save()
            hudAvatarClipPath.reset()
            hudAvatarClipPath.addCircle(hudAvatarRect.centerX(), hudAvatarRect.centerY(), emblemSize / 2f - 2f, Path.Direction.CW)
            canvas.clipPath(hudAvatarClipPath)
            avatarSrcRect.set(0, 0, avatarBmp.width, avatarBmp.height)
            canvas.drawBitmap(avatarBmp, avatarSrcRect, hudAvatarRect, avatarBitmapPaint)
            canvas.restore()
        }

        hudAvatarBorderPaint.color = originThemeColor
        canvas.drawCircle(hudAvatarRect.centerX(), hudAvatarRect.centerY(), emblemSize / 2f, hudAvatarBorderPaint)

        val barsX = emblemX + emblemSize + 14f
        var currentBarY = startY + 4f

        // 1. HP Bar (Red)
        val hpMaxW = 200f + (stats.vigor - 10) * 12f
        val hpCurrentW = (stats.currentHp / stats.maxHp).coerceIn(0f, 1f) * hpMaxW
        canvas.drawRoundRect(barsX, currentBarY, barsX + hpMaxW, currentBarY + barH, 3f, 3f, barBgPaint)
        if (hpCurrentW > 0f) {
            canvas.drawRoundRect(barsX, currentBarY, barsX + hpCurrentW, currentBarY + barH, 3f, 3f, hpFillPaint)
            canvas.drawLine(barsX + 2f, currentBarY + 2f, barsX + hpCurrentW - 2f, currentBarY + 2f, hpSheenPaint)
        }
        canvas.drawRoundRect(barsX, currentBarY, barsX + hpMaxW, currentBarY + barH, 3f, 3f, barFramePaint)

        // 2. FP / Mana Bar (Blue)
        currentBarY += barH + 5f
        val manaMaxW = 160f + (stats.intelligence - 10) * 10f
        val manaCurrentW = (stats.currentMana / stats.maxMana).coerceIn(0f, 1f) * manaMaxW
        canvas.drawRoundRect(barsX, currentBarY, barsX + manaMaxW, currentBarY + barH, 3f, 3f, barBgPaint)
        if (manaCurrentW > 0f) {
            canvas.drawRoundRect(barsX, currentBarY, barsX + manaCurrentW, currentBarY + barH, 3f, 3f, manaFillPaint)
            canvas.drawLine(barsX + 2f, currentBarY + 2f, barsX + manaCurrentW - 2f, currentBarY + 2f, manaSheenPaint)
        }
        canvas.drawRoundRect(barsX, currentBarY, barsX + manaMaxW, currentBarY + barH, 3f, 3f, barFramePaint)

        // 3. Stamina Bar (Green)
        currentBarY += barH + 5f
        val staminaMaxW = 180f + (stats.endurance - 10) * 10f
        val staminaCurrentW = (stats.currentStamina / stats.maxStamina).coerceIn(0f, 1f) * staminaMaxW
        canvas.drawRoundRect(barsX, currentBarY, barsX + staminaMaxW, currentBarY + barH, 3f, 3f, barBgPaint)
        if (staminaCurrentW > 0f) {
            canvas.drawRoundRect(barsX, currentBarY, barsX + staminaCurrentW, currentBarY + barH, 3f, 3f, staminaFillPaint)
            canvas.drawLine(barsX + 2f, currentBarY + 2f, barsX + staminaCurrentW - 2f, currentBarY + 2f, staminaSheenPaint)
        }
        canvas.drawRoundRect(barsX, currentBarY, barsX + staminaMaxW, currentBarY + barH, 3f, 3f, barFramePaint)

        // 4. Level & Essence Rune Counter
        val subInfoY = emblemY + emblemSize + 18f
        canvas.drawText("Lv. ${stats.level}", startX, subInfoY, hudStatTextPaint)
        canvas.drawCircle(startX + 65f, subInfoY - 5f, 5f, essenceIconPaint)
        canvas.drawText("${stats.essence} Essence", startX + 76f, subInfoY, essenceTextPaint)

        // 5. Crimson & Cerulean Flask Badges
        val flaskY = subInfoY + 20f
        canvas.drawText("🧪 ${stats.currentCrimsonFlasks}/${stats.maxCrimsonFlasks}", startX, flaskY, crimsonFlaskTextPaint)
        canvas.drawText("💧 ${stats.currentCeruleanFlasks}/${stats.maxCeruleanFlasks}", startX + 72f, flaskY, ceruleanFlaskTextPaint)
    }

    private fun renderEnemies(canvas: Canvas, enemies: List<com.example.shadowescape.model.Enemy>, player: Player) {
        for (enemy in enemies) {
            if (!enemy.isAlive && enemy.deathFadeTimer > 1.3f) continue

            val eb = enemy.bounds
            val cx = eb.centerX
            val dir = if (enemy.facingRight) 1f else -1f

            canvas.save()

            // Dynamic Shadow Vessel / Shadow Echo / Zone Boss Aura
            if (enemy.isAlive && (enemy.isShadowVessel || enemy.isShadowEcho || enemy.isZoneBoss)) {
                val auraPaint = Paint().apply {
                    color = when {
                        enemy.isZoneBoss -> Color.argb(if (enemy.bossPhase2) 95 else 55, 220, 38, 38)
                        enemy.isShadowEcho -> Color.argb(85, 168, 85, 247)
                        enemy.isShadowVesselAwakened -> Color.argb(95, 192, 38, 211)
                        else -> Color.argb(45, 168, 85, 247)
                    }
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val pulseR = (eb.width.coerceAtLeast(eb.height) * 0.65f) + sin(globalTime * 5f) * 4f
                canvas.drawCircle(cx, eb.centerY, pulseR, auraPaint)
            }

            val spriteAlpha = if (!enemy.isAlive) {
                val fadeOut = (1f - ((enemy.deathFadeTimer - 0.65f) / 0.65f).coerceIn(0f, 1f))
                (fadeOut * 255).toInt()
            } else {
                255
            }

            // Elite Guardian & Demon Slime Boss subtle crimson glow beneath sprite
            if (enemy.isAlive && (enemy.type == com.example.shadowescape.model.EnemyType.ELITE_GUARDIAN || enemy.isDemonBossPhase)) {
                lightDestRect.set(cx - 56f, eb.centerY - 56f, cx + 56f, eb.centerY + 56f)
                canvas.drawBitmap(redBloomCookie, null, lightDestRect, bloomPaint)
            }

            val drewSprite = enemyAnimator.draw(canvas, enemy, spriteAlpha)

            if (!drewSprite) {
                if (!enemy.isAlive) {
                    val fade = (1f - enemy.deathFadeTimer / 1.2f).coerceIn(0f, 1f)
                    canvas.scale(1f - (1f - fade) * 0.3f, fade, cx, eb.bottom)
                    enemyBodyPaint.alpha = (fade * 255).toInt()
                } else {
                    enemyBodyPaint.alpha = 255
                }

                // Hurt flash
                val bodyPaint = if (enemy.hurtTimer > 0f) enemyHurtPaint else enemyBodyPaint

                // 1. Enemy Silhouette Body
                if (enemy.type == com.example.shadowescape.model.EnemyType.ELITE_GUARDIAN) {
                    // Heavy Armor silhouette
                    canvas.drawRoundRect(eb.left, eb.top + 16f, eb.right, eb.bottom, 6f, 6f, bodyPaint)
                    canvas.drawCircle(cx, eb.top + 12f, 13f, bodyPaint)
                    canvas.drawLine(cx - 10f, eb.top + 6f, cx - 16f, eb.top - 4f, enemyArmorPaint)
                    canvas.drawLine(cx + 10f, eb.top + 6f, cx + 16f, eb.top - 4f, enemyArmorPaint)
                } else {
                    // Tattered Hollow Soldier silhouette
                    canvas.drawCircle(cx, eb.top + 11f, 10f, bodyPaint)
                    canvas.drawRect(eb.left + 4f, eb.top + 18f, eb.right - 4f, eb.bottom, bodyPaint)
                }

                // 2. Piercing Glowing Red Eyes
                if (enemy.isAlive) {
                    val eyeX = cx + (dir * 6f)
                    val eyeY = eb.top + 11f
                    canvas.drawCircle(eyeX, eyeY, 5f, enemyEyeGlowPaint)
                    canvas.drawCircle(eyeX, eyeY, 2f, enemyEyeCorePaint)
                }
            }

            if (enemy.isAlive) {
                // 3. Attack windup telegraph glint (kept subtle over pixel sprite)
                if (enemy.attackWindupTimer > 0f) {
                    val eyeX = cx + (dir * 10f)
                    val eyeY = eb.top + eb.height * 0.28f
                    val flareR = 5f + (1f - enemy.attackWindupTimer / enemy.windupDuration) * 7f
                    canvas.drawCircle(eyeX, eyeY, flareR, enemyEyeGlowPaint)
                } else if (!drewSprite && enemy.isAttacking) {
                    val slashX = cx + (dir * 30f)
                    canvas.drawLine(cx, eb.top + 20f, slashX, eb.bottom - 10f, enemyBladeSlashPaint)
                    canvas.drawCircle(slashX, eb.centerY, 10f, enemyEyeGlowPaint)
                }

                // Universal Attack Telegraph Glint ("!" flash before strike, works for all sprites!)
                if (enemy.attackWindupTimer > 0f) {
                    val progress = (1f - enemy.attackWindupTimer / enemy.windupDuration.coerceAtLeast(0.05f)).coerceIn(0f, 1f)
                    val flashR = 8f + progress * 8f
                    val glintY = eb.top - 24f
                    val glintPaint = Paint().apply {
                        color = if (progress > 0.55f) Color.parseColor("#EF4444") else Color.parseColor("#FACC15")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val glintTextPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawCircle(cx, glintY, flashR, glintPaint)
                    canvas.drawText("!", cx, glintY + 5f, glintTextPaint)
                }

                // 4. Stagger stars / poise break halo & Visceral Target Reticle
                if (enemy.isStaggered) {
                    val starAngle = globalTime * 8f
                    for (i in 0..2) {
                        val sa = starAngle + (i * PI.toFloat() * 2f / 3f)
                        val sx = cx + cos(sa.toDouble()).toFloat() * 16f
                        val sy = eb.top - 8f + sin(sa.toDouble()).toFloat() * 6f
                        canvas.drawCircle(sx, sy, 3.5f, enemyStaggerPaint)
                    }

                    // Pulsing Crimson Visceral Execution Reticle
                    val reticleR = 20f + sin(globalTime * 9f) * 3f
                    val coreY = eb.centerY
                    canvas.drawCircle(cx, coreY, reticleR, visceralReticlePaint)
                    canvas.drawLine(cx - reticleR - 5f, coreY, cx - reticleR + 4f, coreY, visceralReticlePaint)
                    canvas.drawLine(cx + reticleR - 4f, coreY, cx + reticleR + 5f, coreY, visceralReticlePaint)
                    canvas.drawLine(cx, coreY - reticleR - 5f, cx, coreY - reticleR + 4f, visceralReticlePaint)
                    canvas.drawLine(cx, coreY + reticleR - 4f, cx, coreY + reticleR + 5f, visceralReticlePaint)
                    canvas.drawCircle(cx, coreY, 4f, visceralCorePaint)

                    // Floating Visceral Strike prompt when player in range
                    val isNearStaggered = abs(player.bounds.centerX - cx) < 95f && abs(player.bounds.centerY - coreY) < 80f
                    val promptY = eb.top - 28f + sin(globalTime * 6f) * 2f
                    val promptW = 100f
                    val promptH = 20f
                    canvas.drawRoundRect(cx - promptW / 2f, promptY - promptH / 2f, cx + promptW / 2f, promptY + promptH / 2f, 6f, 6f, visceralPromptBg)
                    canvas.drawRoundRect(cx - promptW / 2f, promptY - promptH / 2f, cx + promptW / 2f, promptY + promptH / 2f, 6f, 6f, visceralReticlePaint)
                    val text = if (isNearStaggered) "⚔ VISCERAL [TAP]" else "✦ STAGGERED ✦"
                    canvas.drawText(text, cx, promptY + 4f, visceralPromptPaint)
                }

                // 5. Mini Health & Poise Bar above head
                if (enemy.currentHp < enemy.maxHp || enemy.isAggroed) {
                    val barW = eb.width * 1.2f
                    val barX = cx - barW / 2f
                    val barY = eb.top - 14f

                    // HP Bar
                    canvas.drawRoundRect(barX, barY, barX + barW, barY + 4f, 2f, 2f, enemyHpBarBg)
                    val hpW = (enemy.currentHp / enemy.maxHp).coerceIn(0f, 1f) * barW
                    if (hpW > 0f) {
                        canvas.drawRoundRect(barX, barY, barX + hpW, barY + 4f, 2f, 2f, enemyHpBarFill)
                    }

                    // Poise Bar (amber line under HP)
                    if (enemy.currentPoise < enemy.maxPoise && !enemy.isStaggered) {
                        val poiseW = (enemy.currentPoise / enemy.maxPoise).coerceIn(0f, 1f) * barW
                        canvas.drawRect(barX, barY + 4.5f, barX + poiseW, barY + 6.5f, enemyPoiseBarFill)
                    }

                    // Active Status Effect Badges
                    val statusIcons = buildString {
                        if (enemy.frostTimer > 0f) append("❄")
                        if (enemy.burnTimer > 0f) append("🔥")
                        if (enemy.poisonTimer > 0f) append("☠")
                    }
                    if (statusIcons.isNotEmpty()) {
                        val stPaint = Paint().apply {
                            color = Color.WHITE
                            textSize = 13f
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText(statusIcons, cx, barY - 4f, stPaint)
                    }
                }
            }

            canvas.restore()
        }
    }

    private fun renderProceduralPlayerSilhouette(canvas: Canvas, player: Player) {
        val pb = player.bounds
        val cx = pb.centerX

        canvas.save()
        val isMoving = player.vx != 0f && player.isGrounded
        val isJumping = !player.isGrounded && player.vy < 0f
        val isFalling = !player.isGrounded && player.vy >= 0f

        when {
            isJumping -> {
                canvas.drawCircle(cx, pb.top + 12f, 10f, playerBodyPaint)
                canvas.drawRect(pb.left + 6f, pb.top + 22f, pb.right - 6f, pb.bottom - 12f, playerBodyPaint)
                canvas.drawLine(cx, pb.bottom - 12f, cx - 10f, pb.bottom - 4f, playerRimPaint)
                canvas.drawLine(cx, pb.bottom - 12f, cx + 10f, pb.bottom - 4f, playerRimPaint)
            }
            isFalling -> {
                canvas.drawCircle(cx, pb.top + 12f, 10f, playerBodyPaint)
                canvas.drawRect(pb.left + 6f, pb.top + 22f, pb.right - 6f, pb.bottom - 10f, playerBodyPaint)
                canvas.drawLine(cx, pb.bottom - 10f, cx - 8f, pb.bottom, playerRimPaint)
                canvas.drawLine(cx, pb.bottom - 10f, cx + 8f, pb.bottom, playerRimPaint)
            }
            isMoving -> {
                val swing = sin(player.animTime) * 12f
                val headBob = kotlin.math.abs(sin(player.animTime)) * 2f
                canvas.drawCircle(cx, pb.top + 12f - headBob, 10f, playerBodyPaint)
                canvas.drawRect(pb.left + 6f, pb.top + 22f, pb.right - 6f, pb.bottom - 12f, playerBodyPaint)
                canvas.drawLine(cx, pb.bottom - 12f, cx + swing, pb.bottom, playerRimPaint)
                canvas.drawLine(cx, pb.bottom - 12f, cx - swing, pb.bottom, playerRimPaint)
                canvas.drawLine(cx, pb.top + 26f, cx - swing * 0.7f, pb.top + 42f, playerRimPaint)
                canvas.drawLine(cx, pb.top + 26f, cx + swing * 0.7f, pb.top + 42f, playerRimPaint)
            }
            else -> {
                val breath = sin(player.animTime) * 1.5f
                canvas.drawCircle(cx, pb.top + 12f + breath, 10f, playerBodyPaint)
                canvas.drawRect(pb.left + 6f, pb.top + 22f + breath, pb.right - 6f, pb.bottom - 12f, playerBodyPaint)
                canvas.drawLine(cx - 4f, pb.bottom - 12f, cx - 6f, pb.bottom, playerRimPaint)
                canvas.drawLine(cx + 4f, pb.bottom - 12f, cx + 6f, pb.bottom, playerRimPaint)
            }
        }

        canvas.restore()
    }

    private fun drawParallaxBackground(canvas: Canvas, cameraX: Float, cameraY: Float, width: Float, height: Float) {
        val p1X = -(cameraX * 0.15f) % width
        canvas.save()
        canvas.translate(p1X, -cameraY * 0.1f)
        for (i in -1..4) {
            val x = i * width * 0.75f
            canvas.drawRect(x, height * 0.15f, x + 140f, height, bgSilhouettePaint)
            canvas.drawRect(x + 25f, height * 0.35f, x + 115f, height * 0.85f, skyPaint)
        }
        canvas.restore()

        val p2X = -(cameraX * 0.35f) % width
        canvas.save()
        canvas.translate(p2X, -cameraY * 0.2f)
        for (i in -1..5) {
            val x = i * width * 0.45f
            canvas.drawRect(x + 80f, 0f, x + 180f, height * 0.48f, midSilhouettePaint)
        }
        canvas.restore()
    }

    private fun drawAtmosphericLighting(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            shader = RadialGradient(
                width * 0.35f, height * 0.3f, width * 0.65f,
                Color.parseColor("#152033"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    fun drawOriginSelectionScreen(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        vigor: Int,
        endurance: Int,
        strength: Int,
        agility: Int,
        intelligence: Int,
        totalPoints: Int,
        statMinusBtnRects: Array<RectF>,
        statPlusBtnRects: Array<RectF>,
        resetBtnRect: RectF,
        awakenBtnRect: RectF
    ) {
        val origin = com.example.shadowescape.model.CharacterOrigins.deriveClass(vigor, endurance, strength, agility, intelligence)
        val spentPoints = (vigor - 8) + (endurance - 8) + (strength - 8) + (agility - 8) + (intelligence - 8)
        val remainingPoints = (totalPoints - spentPoints).coerceAtLeast(0)
        val originThemeColor = try {
            Color.parseColor(origin.themeColorHex)
        } catch (_: Exception) {
            Color.parseColor("#38BDF8")
        }

        // 1. Dark Atmospheric Overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, originOverlayPaint)

        // 2. Header
        val headerY = screenHeight * 0.052f
        canvas.drawText("✦  C H A R A C T E R   C R E A T I O N  ✦", screenWidth / 2f, headerY, originHeaderTitle)
        canvas.drawText("Allocate your attribute points to forge your class & destiny", screenWidth / 2f, headerY + 18f, originHeaderSub)

        // 3. Points Remaining Pill
        val pillY = headerY + 28f
        val pillW = 380f
        val pillH = 26f
        val pillRect = RectF(screenWidth / 2f - pillW / 2f, pillY, screenWidth / 2f + pillW / 2f, pillY + pillH)
        val hasPoints = remainingPoints > 0
        originPointsPillBorder.color = if (hasPoints) Color.parseColor("#F59E0B") else Color.parseColor("#10B981")
        originPointsPillText.color = if (hasPoints) Color.parseColor("#FEF08A") else Color.parseColor("#A7F3D0")
        canvas.drawRoundRect(pillRect, 13f, 13f, originPointsPillBg)
        canvas.drawRoundRect(pillRect, 13f, 13f, originPointsPillBorder)
        val pillMsg = if (hasPoints) "✦ $remainingPoints ATTRIBUTE POINTS REMAINING ✦" else "✔ ALL POINTS ALLOCATED • CLASS FORGED"
        canvas.drawText(pillMsg, pillRect.centerX(), pillRect.centerY() + 5f, originPointsPillText)

        // 4. Main Two-Column Showcase Area
        val contentY = pillY + pillH + 12f
        val bottomNavH = 62f
        val contentH = (screenHeight - contentY - bottomNavH).coerceAtLeast(180f)
        val marginX = 22f
        val colGap = 16f
        val colW = (screenWidth - 2 * marginX - colGap) / 2f

        // ==========================================
        // LEFT CARD: Derived Class & Weapon Showcase
        // ==========================================
        val leftCardRect = RectF(marginX, contentY, marginX + colW, contentY + contentH)
        canvas.drawRoundRect(leftCardRect, 12f, 12f, originCardBg)
        originCardBorder.color = (originThemeColor and 0x00FFFFFF) or 0x66000000
        canvas.drawRoundRect(leftCardRect, 12f, 12f, originCardBorder)

        // Avatar Portrait Frame (4:5 / 3:4 golden portrait card ratio)
        val pad = 14f
        val portraitW = minOf(colW * 0.40f, 360f)
        val portraitH = minOf(contentH * 0.62f, portraitW * 1.28f)
        val portraitX = leftCardRect.left + pad
        val portraitY = leftCardRect.top + pad

        avatarFrameRect.set(portraitX, portraitY, portraitX + portraitW, portraitY + portraitH)
        avatarGlowPaint.color = (originThemeColor and 0x00FFFFFF) or 0x33000000
        canvas.drawRoundRect(avatarFrameRect, 14f, 14f, avatarGlowPaint)

        val bmp = avatarBitmaps[origin.avatarId] ?: avatarBitmaps[origin.id]
        if (bmp != null) {
            canvas.save()
            avatarClipPath.reset()
            avatarClipPath.addRoundRect(avatarFrameRect, 10f, 10f, Path.Direction.CW)
            canvas.clipPath(avatarClipPath)

            // Aspect-ratio preserving center-crop: perfectly prevents vertical/horizontal squeezing!
            val frameAspect = avatarFrameRect.width() / avatarFrameRect.height()
            val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
            if (bmpAspect > frameAspect) {
                val cropW = (bmp.height * frameAspect).toInt()
                val cropX = (bmp.width - cropW) / 2
                avatarSrcRect.set(cropX, 0, cropX + cropW, bmp.height)
            } else {
                val cropH = (bmp.width / frameAspect).toInt()
                val cropY = (bmp.height - cropH) / 2
                avatarSrcRect.set(0, cropY, bmp.width, cropY + cropH)
            }
            canvas.drawBitmap(bmp, avatarSrcRect, avatarFrameRect, avatarBitmapPaint)

            val badgeH = 32f
            canvas.drawRect(avatarFrameRect.left, avatarFrameRect.bottom - badgeH, avatarFrameRect.right, avatarFrameRect.bottom, avatarBadgeBg)
            canvas.restore()
        } else {
            canvas.drawRoundRect(avatarFrameRect, 10f, 10f, originTabInactiveBg)
        }

        avatarBorderPaint.color = originThemeColor
        canvas.drawRoundRect(avatarFrameRect, 10f, 10f, avatarBorderPaint)
        canvas.drawText("${origin.icon} ${origin.title}", avatarFrameRect.centerX(), avatarFrameRect.bottom - 10f, avatarBadgeText)

        // Tactical Playstyle Box placed neatly directly below the portrait in the left column
        val tacticsBoxRect = RectF(portraitX, portraitY + portraitH + 12f, portraitX + portraitW, leftCardRect.bottom - pad)
        if (tacticsBoxRect.height() >= 40f) {
            canvas.drawRoundRect(tacticsBoxRect, 8f, 8f, originTabInactiveBg)
            canvas.drawRoundRect(tacticsBoxRect, 8f, 8f, originCardBorder)
            originBadgePaint.textSize = 14f
            originBadgePaint.color = originThemeColor
            canvas.drawText("✦ TACTICAL PLAYSTYLE", tacticsBoxRect.left + 12f, tacticsBoxRect.top + 20f, originBadgePaint)
            originDescText.color = Color.parseColor("#CBD5E1")
            originDescText.textSize = 13.5f
            val tacticsWords = origin.tacticalPlaystyle.split(" ")
            var tLine = ""
            var tY = tacticsBoxRect.top + 40f
            val tMaxW = tacticsBoxRect.width() - 24f
            for (w in tacticsWords) {
                if (originDescText.measureText("$tLine $w") < tMaxW) {
                    tLine = if (tLine.isEmpty()) w else "$tLine $w"
                } else {
                    canvas.drawText(tLine, tacticsBoxRect.left + 12f, tY, originDescText)
                    tY += 19f
                    tLine = w
                }
            }
            if (tLine.isNotEmpty() && tY <= tacticsBoxRect.bottom - 6f) {
                canvas.drawText(tLine, tacticsBoxRect.left + 12f, tY, originDescText)
            }
        }

        // Info Section (To the right of portrait)
        val infoX = portraitX + portraitW + 18f
        val infoMaxW = leftCardRect.right - pad - infoX
        var lY = leftCardRect.top + 34f

        originBadgePaint.textSize = 26f
        originBadgePaint.color = originThemeColor
        canvas.drawText("${origin.icon}  ${origin.name.uppercase()}", infoX, lY, originBadgePaint)

        lY += 24f
        originDescText.color = Color.parseColor("#94A3B8")
        originDescText.textSize = 15.5f
        canvas.drawText("ARCHETYPE: ${origin.title}   •   START LEVEL: ${origin.startingLevel}", infoX, lY, originDescText)

        lY += 24f
        originDescText.color = Color.parseColor("#E2E8F0")
        originDescText.textSize = 14.5f
        val words = origin.description.split(" ")
        var line = ""
        for (w in words) {
            if (originDescText.measureText("$line $w") < infoMaxW) {
                line = if (line.isEmpty()) w else "$line $w"
            } else {
                canvas.drawText(line, infoX, lY, originDescText)
                lY += 21f
                line = w
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, infoX, lY, originDescText)
            lY += 22f
        }

        // Weapon Showcase Sub-card
        val weaponBoxRect = RectF(infoX, lY, leftCardRect.right - pad, lY + 112f)
        canvas.drawRoundRect(weaponBoxRect, 8f, 8f, originTabInactiveBg)
        canvas.drawRoundRect(weaponBoxRect, 8f, 8f, originCardBorder)

        val wpn = origin.startingWeapon
        originBadgePaint.textSize = 16.5f
        originBadgePaint.color = Color.parseColor("#38BDF8")
        canvas.drawText("WEAPON: ${wpn.name} [${wpn.stance.displayName.uppercase()}]", weaponBoxRect.left + 14f, weaponBoxRect.top + 24f, originBadgePaint)

        originDescText.textSize = 14.5f
        originDescText.color = Color.parseColor("#CBD5E1")
        canvas.drawText("DMG: ${wpn.baseDamage.toInt()}   •   REQ: ${wpn.getRequirementString()}   •   Stamina: ${wpn.staminaCost.toInt()}", weaponBoxRect.left + 14f, weaponBoxRect.top + 48f, originDescText)

        originBadgePaint.textSize = 14.5f
        originBadgePaint.color = Color.parseColor("#FDE047")
        canvas.drawText("SCALING:  STR [ ${wpn.strGrade} ]   AGI [ ${wpn.agiGrade} ]   INT [ ${wpn.intGrade} ]", weaponBoxRect.left + 14f, weaponBoxRect.top + 72f, originBadgePaint)

        originBadgePaint.textSize = 14f
        originBadgePaint.color = Color.parseColor("#A7F3D0")
        canvas.drawText("SKILL: ${wpn.skillName} • ${wpn.skillDescription}", weaponBoxRect.left + 14f, weaponBoxRect.top + 96f, originBadgePaint)

        // Archetype Passives
        lY = weaponBoxRect.bottom + 20f
        originBadgePaint.textSize = 15.5f
        originBadgePaint.color = originThemeColor
        canvas.drawText("✦ ARCHETYPE PASSIVES & TRAITS", infoX, lY, originBadgePaint)

        lY += 20f
        for (trait in origin.traits) {
            originDescText.color = Color.parseColor("#E2E8F0")
            originDescText.textSize = 14f
            canvas.drawText(trait, infoX, lY, originDescText)
            lY += 20f
        }

        // ==========================================
        // RIGHT CARD: Interactive Attribute Terminal
        // ==========================================
        val rightCardRect = RectF(marginX + colW + colGap, contentY, screenWidth - marginX, contentY + contentH)
        canvas.drawRoundRect(rightCardRect, 12f, 12f, originCardBg)
        canvas.drawRoundRect(rightCardRect, 12f, 12f, originCardBorder)

        var rY = rightCardRect.top + 28f
        val rX = rightCardRect.left + 22f
        val rRight = rightCardRect.right - 22f
        originBadgePaint.textSize = 18.5f
        originBadgePaint.color = Color.parseColor("#F8FAFC")
        canvas.drawText("✦ ATTRIBUTE ALLOCATION & ATTUNEMENT", rX, rY, originBadgePaint)

        val statsData = listOf(
            Triple("❤️ VIGOR", vigor, Color.parseColor("#EF4444")),
            Triple("⚡ ENDURANCE", endurance, Color.parseColor("#22C55E")),
            Triple("⚔️ STRENGTH", strength, Color.parseColor("#F59E0B")),
            Triple("💨 AGILITY", agility, Color.parseColor("#06B6D4")),
            Triple("🔮 INTELLIGENCE", intelligence, Color.parseColor("#3B82F6"))
        )

        val statRowGap = 48f
        val btnW = 34f
        val btnH = 30f
        val btnMinusX = rX + 160f
        val btnPlusX = btnMinusX + btnW + 48f
        val barStartX = btnPlusX + btnW + 18f
        val barMaxW = (rRight - barStartX).coerceAtLeast(80f)

        for (i in statsData.indices) {
            val (name, value, color) = statsData[i]
            rY += statRowGap
            val rowCenterY = rY - 4f

            // 1. Stat Name
            originStatLabel.color = Color.parseColor("#F1F5F9")
            originStatLabel.textSize = 16f
            canvas.drawText(name, rX, rowCenterY + 5f, originStatLabel)

            // 2. Minus Button [-]
            val minusRect = statMinusBtnRects[i]
            minusRect.set(btnMinusX, rowCenterY - btnH / 2f, btnMinusX + btnW, rowCenterY + btnH / 2f)
            val canMinus = value > 8
            originStatBtnBorder.color = if (canMinus) Color.parseColor("#38BDF8") else Color.parseColor("#475569")
            originStatBtnText.color = if (canMinus) Color.WHITE else Color.parseColor("#64748B")
            canvas.drawRoundRect(minusRect, 6f, 6f, originStatBtnBg)
            canvas.drawRoundRect(minusRect, 6f, 6f, originStatBtnBorder)
            canvas.drawText("-", minusRect.centerX(), minusRect.centerY() + 7f, originStatBtnText)

            // 3. Stat Value
            originStatValue.color = Color.WHITE
            originStatValue.textSize = 20f
            canvas.drawText("$value", btnMinusX + btnW + 24f, rowCenterY + 6f, originStatValue)

            // 4. Plus Button [+]
            val plusRect = statPlusBtnRects[i]
            plusRect.set(btnPlusX, rowCenterY - btnH / 2f, btnPlusX + btnW, rowCenterY + btnH / 2f)
            val canPlus = remainingPoints > 0 && value < 20
            originStatBtnBorder.color = if (canPlus) Color.parseColor("#00E5FF") else Color.parseColor("#475569")
            originStatBtnText.color = if (canPlus) Color.WHITE else Color.parseColor("#64748B")
            canvas.drawRoundRect(plusRect, 6f, 6f, originStatBtnBg)
            canvas.drawRoundRect(plusRect, 6f, 6f, originStatBtnBorder)
            canvas.drawText("+", plusRect.centerX(), plusRect.centerY() + 7f, originStatBtnText)

            // 5. Stat Progress Bar
            val barY = rowCenterY - 6f
            val barH = 12f
            canvas.drawRoundRect(barStartX, barY, barStartX + barMaxW, barY + barH, 4f, 4f, originStatBarBg)

            val fillRatio = ((value - 8) / 12f).coerceIn(0.04f, 1f)
            val fillW = barMaxW * fillRatio
            crescentAuraPaint.color = color
            canvas.drawRoundRect(barStartX, barY, barStartX + fillW, barY + barH, 4f, 4f, crescentAuraPaint)

            // 6. Subtext description
            val desc = when (i) {
                0 -> "HP: ${100 + (vigor - 10) * 15}  •  Vitality"
                1 -> "Stamina: ${80 + (endurance - 10) * 8}  •  Recovery"
                2 -> "Scaling: Heavy Blades & Poise"
                3 -> "Scaling: Katana Speed & Dodging"
                else -> "Mana: ${50 + (intelligence - 10) * 10}  •  Sorcery"
            }
            originDescText.textSize = 13.5f
            originDescText.color = Color.parseColor("#94A3B8")
            canvas.drawText(desc, barStartX, rowCenterY + 20f, originDescText)
        }

        // Guidance Box for Dynamic Classes
        val guideTop = rY + 44f
        val guideBottom = rightCardRect.bottom - 54f
        if (guideBottom > guideTop + 36f) {
            val guideBoxRect = RectF(rX, guideTop, rRight, guideBottom)
            canvas.drawRoundRect(guideBoxRect, 8f, 8f, originTabInactiveBg)
            canvas.drawRoundRect(guideBoxRect, 8f, 8f, originCardBorder)

            originBadgePaint.textSize = 14.5f
            originBadgePaint.color = Color.parseColor("#38BDF8")
            canvas.drawText("✦ DYNAMIC CLASS ATTUNEMENTS", guideBoxRect.left + 14f, guideBoxRect.top + 20f, originBadgePaint)

            originDescText.textSize = 13.5f
            originDescText.color = Color.parseColor("#CBD5E1")
            canvas.drawText("• Pure Strength ➔ Warrior   • Pure Int ➔ Astral Sorcerer   • Pure Agi ➔ Shadow Shinobi", guideBoxRect.left + 14f, guideBoxRect.top + 40f, originDescText)
            canvas.drawText("• STR + INT ➔ Paladin / Magic Swordsman   • AGI + INT ➔ Nightblade   • STR + AGI ➔ Ronin", guideBoxRect.left + 14f, guideBoxRect.top + 60f, originDescText)
            canvas.drawText("• High VIG + END ➔ Iron Vanguard   • Balanced Attributes ➔ Wanderer", guideBoxRect.left + 14f, guideBoxRect.top + 80f, originDescText)
        }

        // Reset Points Button
        val resetW = 190f
        val resetH = 32f
        resetBtnRect.set(rRight - resetW, rightCardRect.bottom - resetH - 12f, rRight, rightCardRect.bottom - 12f)
        canvas.drawRoundRect(resetBtnRect, 6f, 6f, originStatBtnBg)
        originStatBtnBorder.color = Color.parseColor("#64748B")
        canvas.drawRoundRect(resetBtnRect, 6f, 6f, originStatBtnBorder)
        originPresetText.color = Color.parseColor("#E2E8F0")
        originPresetText.textSize = 14f
        canvas.drawText("↺  RESET ALL POINTS", resetBtnRect.centerX(), resetBtnRect.centerY() + 4.5f, originPresetText)

        // 5. Bottom Navigation & Awaken Button
        val bY = screenHeight - 56f
        val awakenW = 420f
        val awakenH = 46f
        val cx = screenWidth / 2f

        awakenBtnRect.set(cx - awakenW / 2f, bY - awakenH / 2f, cx + awakenW / 2f, bY + awakenH / 2f)
        val pulseAura = sin(globalTime * 4f) * 3f
        canvas.drawRoundRect(awakenBtnRect.left - pulseAura, awakenBtnRect.top - pulseAura, awakenBtnRect.right + pulseAura, awakenBtnRect.bottom + pulseAura, 10f, 10f, avatarGlowPaint)
        canvas.drawRoundRect(awakenBtnRect, 10f, 10f, originAwakenBtnBg)
        originAwakenBtnBorder.color = originThemeColor
        canvas.drawRoundRect(awakenBtnRect, 10f, 10f, originAwakenBtnBorder)
        canvas.drawText("✦  AWAKEN IN THE SHADOWS  ✦", awakenBtnRect.centerX(), awakenBtnRect.centerY() + 6.5f, originAwakenBtnText)
    }

    private fun renderChests(canvas: Canvas, chests: List<TreasureChest>, player: Player) {
        for (chest in chests) {
            val b = chest.bounds
            val isNear = abs(player.bounds.centerX - b.centerX) < 70f && abs(player.bounds.centerY - b.centerY) < 70f

            // Handle Awakened Mimic Monster!
            if (chest.isMimic && chest.isAwakened) {
                if (chest.isDefeated) {
                    val fade = (1f - chest.deathFadeTimer / 1.5f).coerceIn(0f, 1f)
                    if (fade > 0f) {
                        chestBasePaint.color = Color.argb((fade * 180).toInt(), 30, 41, 59)
                        canvas.drawRoundRect(b.left, b.top + 16f, b.right, b.bottom, 4f, 4f, chestBasePaint)
                    }
                    continue
                }

                // Awakened living Mimic in combat!
                val hurtFlash = chest.mimicHurtTimer > 0f
                val bodyColor = if (hurtFlash) Color.WHITE else Color.parseColor("#1E293B")
                val mawColor = if (hurtFlash) Color.RED else Color.parseColor("#450A0A")
                chestBasePaint.color = bodyColor
                mimicMawPaint.color = mawColor

                // Lower chest jaw / body
                canvas.drawRoundRect(b.left, b.top + 12f, b.right, b.bottom, 4f, 4f, chestBasePaint)
                canvas.drawRect(b.left + 4f, b.top + 12f, b.right - 4f, b.bottom - 4f, mimicMawPaint)

                // Lower row of sharp upward fangs
                val numTeeth = 5
                val toothW = (b.width - 8f) / numTeeth
                for (t in 0 until numTeeth) {
                    val tx = b.left + 4f + t * toothW
                    mimicPath.reset()
                    mimicPath.moveTo(tx, b.bottom - 4f)
                    mimicPath.lineTo(tx + toothW / 2f, b.top + 14f)
                    mimicPath.lineTo(tx + toothW, b.bottom - 4f)
                    mimicPath.close()
                    canvas.drawPath(mimicPath, mimicTeethPaint)
                }

                // Upper jaw (open wide!)
                val openBiteY = b.top - 20f + sin(globalTime * 8f) * 3f
                canvas.drawRoundRect(b.left - 2f, openBiteY - 14f, b.right + 2f, openBiteY, 4f, 4f, chestBasePaint)
                // Upper row of sharp downward fangs
                for (t in 0 until numTeeth) {
                    val tx = b.left + 4f + t * toothW
                    mimicPath.reset()
                    mimicPath.moveTo(tx, openBiteY)
                    mimicPath.lineTo(tx + toothW / 2f, openBiteY + 12f)
                    mimicPath.lineTo(tx + toothW, openBiteY)
                    mimicPath.close()
                    canvas.drawPath(mimicPath, mimicTeethPaint)
                }

                // Piercing glowing red eyes deep in the throat
                val eyeL = b.centerX - 10f
                val eyeR = b.centerX + 10f
                val eyeY = b.top + 8f
                canvas.drawCircle(eyeL, eyeY, 4.5f, mimicEyePaint)
                canvas.drawCircle(eyeR, eyeY, 4.5f, mimicEyePaint)
                canvas.drawCircle(eyeL, eyeY, 1.8f, enemyEyeCorePaint)
                canvas.drawCircle(eyeR, eyeY, 1.8f, enemyEyeCorePaint)

                // Spindly Shadow Claw Arms reaching out
                val armSwing = sin(globalTime * 12f) * 8f
                val armDir = if (chest.mimicFacingRight) 1f else -1f
                canvas.drawLine(b.left, b.top + 18f, b.left - 18f * armDir, b.top + 28f + armSwing, mimicArmPaint)
                canvas.drawLine(b.right, b.top + 18f, b.right + 18f * armDir, b.top + 28f - armSwing, mimicArmPaint)

                // Mimic Boss Mini HP Bar
                val barW = 65f
                val barX = b.centerX - barW / 2f
                val barY = b.top - 36f
                canvas.drawRoundRect(barX, barY, barX + barW, barY + 5f, 2f, 2f, mimicHpBg)
                val hpRatio = (chest.currentMimicHp / chest.maxMimicHp).coerceIn(0f, 1f)
                canvas.drawRoundRect(barX, barY, barX + (barW * hpRatio), barY + 5f, 2f, 2f, mimicHpFill)
                canvas.drawText("✦ MIMIC TRAP ✦", b.centerX, barY - 4f, visceralPromptPaint)
                continue
            }

            // Normal Chest & Closed Mimic Chest
            val drewSpriteChest = tilesetRenderer.drawChest(canvas, chest)
            val (baseColor, borderColor, trimColor) = when (chest.type) {
                ChestType.WOODEN -> Triple(Color.parseColor("#3E2723"), Color.parseColor("#795548"), Color.parseColor("#BCAAA4"))
                ChestType.IRON -> Triple(Color.parseColor("#1E293B"), Color.parseColor("#475569"), Color.parseColor("#94A3B8"))
                ChestType.GILDED -> Triple(Color.parseColor("#78350F"), Color.parseColor("#D97706"), Color.parseColor("#FDE68A"))
            }

            if (!drewSpriteChest) {
                // Chest Base Box
                chestBasePaint.color = baseColor
                chestBorderPaint.color = borderColor
                canvas.drawRoundRect(b.left, b.top + 10f, b.right, b.bottom, 4f, 4f, chestBasePaint)
                canvas.drawRoundRect(b.left, b.top + 10f, b.right, b.bottom, 4f, 4f, chestBorderPaint)

                // Metallic decorative bands
                chestBorderPaint.color = trimColor
                canvas.drawLine(b.left + 12f, b.top + 10f, b.left + 12f, b.bottom, chestBorderPaint)
                canvas.drawLine(b.right - 12f, b.top + 10f, b.right - 12f, b.bottom, chestBorderPaint)
            }

            // Chest Lid
            val lidHeight = 14f
            if (chest.state == ChestState.CLOSED) {
                if (!drewSpriteChest) {
                    // If it's a Mimic, subtle breathing tell!
                    val breatheOffset = if (chest.isMimic) sin(globalTime * 3.5f) * 2.6f else 0f
                    val lidTop = b.top - breatheOffset

                    // Closed Lid
                    canvas.drawRoundRect(b.left - 2f, lidTop, b.right + 2f, lidTop + lidHeight, 4f, 4f, chestBasePaint)
                    canvas.drawRoundRect(b.left - 2f, lidTop, b.right + 2f, lidTop + lidHeight, 4f, 4f, chestBorderPaint)

                    // Lock Keyhole / Gem
                    chestLockPaint.color = if (chest.type == ChestType.GILDED) Color.parseColor("#F59E0B") else Color.parseColor("#E2E8F0")
                    canvas.drawCircle(b.centerX, lidTop + 12f, 4f, chestLockPaint)
                }

                // If Mimic, subtle tell: tiny fangs and glowing red eyes in the breathing slit!
                if (chest.isMimic) {
                    canvas.drawRect(b.left + 6f, b.top + 9f, b.right - 6f, b.top + 11f, mimicMawPaint)
                    for (i in 0..3) {
                        val tx = b.left + 14f + i * 9f
                        canvas.drawLine(tx, b.top + 9f, tx + 2f, b.top + 12f, mimicTeethPaint)
                    }
                    if (sin(globalTime * 4.5f) > 0.3f) {
                        canvas.drawCircle(b.centerX - 6f, b.top + 10f, 1.8f, mimicEyePaint)
                        canvas.drawCircle(b.centerX + 6f, b.top + 10f, 1.8f, mimicEyePaint)
                    }
                }

                // Floating interaction prompt when near
                if (isNear) {
                    val promptY = b.top - 18f + sin(globalTime * 6f) * 3f
                    val promptW = 76f
                    val promptH = 22f
                    canvas.drawRoundRect(b.centerX - promptW / 2f, promptY - promptH / 2f, b.centerX + promptW / 2f, promptY + promptH / 2f, 6f, 6f, chestPromptBgPaint)
                    canvas.drawRoundRect(b.centerX - promptW / 2f, promptY - promptH / 2f, b.centerX + promptW / 2f, promptY + promptH / 2f, 6f, 6f, chestBorderPaint)
                    canvas.drawText("🖐 OPEN", b.centerX, promptY + 5f, chestPromptPaint)
                }
            } else {
                // OPENING or OPENED
                val openRatio = chest.openProgress
                val beamAlpha = ((1f - (chest.openProgress - 0.4f).coerceAtLeast(0f) * 0.5f) * 160f).toInt().coerceIn(0, 255)
                val beamColor = when (chest.type) {
                    ChestType.WOODEN -> Color.argb(beamAlpha, 56, 189, 248)
                    ChestType.IRON -> Color.argb(beamAlpha, 168, 85, 247)
                    ChestType.GILDED -> Color.argb(beamAlpha, 245, 158, 11)
                }
                chestBeamPaint.color = beamColor
                val beamPath = spikePath
                beamPath.reset()
                beamPath.moveTo(b.left + 4f, b.top + 10f)
                beamPath.lineTo(b.right - 4f, b.top + 10f)
                beamPath.lineTo(b.right + 24f, b.top - 180f)
                beamPath.lineTo(b.left - 24f, b.top - 180f)
                beamPath.close()
                canvas.drawPath(beamPath, chestBeamPaint)

                if (!drewSpriteChest) {
                    // Open angled lid
                    val lidAngleY = b.top - (lidHeight * openRatio * 1.6f)
                    canvas.drawRoundRect(b.left - 2f, lidAngleY, b.right + 2f, lidAngleY + 8f, 4f, 4f, chestBasePaint)
                    canvas.drawRoundRect(b.left - 2f, lidAngleY, b.right + 2f, lidAngleY + 8f, 4f, 4f, chestBorderPaint)
                }
            }
        }
    }

    private fun renderBloodstain(canvas: Canvas, level: Level) {
        if (!level.hasBloodstain) return

        val bx = level.bloodstainX
        val by = level.bloodstainY

        // 1. Translucent golden celestial beam rising to sky
        val beamAlpha = (85 + sin(globalTime * 3.5f) * 25f).toInt().coerceIn(0, 255)
        bloodstainBeamPaint.color = Color.argb(beamAlpha, 245, 158, 11)
        val beamW = 26f + sin(globalTime * 2f) * 4f
        canvas.drawRect(bx - beamW / 2f, by - 160f, bx + beamW / 2f, by, bloodstainBeamPaint)

        // Inner bright beam core
        bloodstainBeamPaint.color = Color.argb((beamAlpha * 1.3f).toInt().coerceIn(0, 255), 253, 230, 138)
        canvas.drawRect(bx - beamW * 0.22f, by - 160f, bx + beamW * 0.22f, by, bloodstainBeamPaint)

        // 2. Pulsing concentric ground runes
        val innerR = 14f + sin(globalTime * 3.2f) * 3f
        val outerR = 26f + cos(globalTime * 2.8f) * 4f
        bloodstainRunePaint.color = Color.parseColor("#F59E0B")
        canvas.drawOval(bx - innerR, by - 4f, bx + innerR, by + 4f, bloodstainRunePaint)
        bloodstainRunePaint.color = Color.parseColor("#B45309")
        canvas.drawOval(bx - outerR, by - 7f, bx + outerR, by + 7f, bloodstainRunePaint)

        // 3. Floating sparkling essence orb
        val orbY = by - 26f + sin(globalTime * 3.8f) * 5f
        bloodstainOrbPaint.color = Color.argb(90, 245, 158, 11)
        canvas.drawCircle(bx, orbY, 14f, bloodstainOrbPaint)
        bloodstainOrbPaint.color = Color.parseColor("#FDE68A")
        canvas.drawCircle(bx, orbY, 6f, bloodstainOrbPaint)

        // 4. Overhead callout badge
        val badgeY = by - 62f + sin(globalTime * 3.8f) * 3f
        val badgeW = 124f
        val badgeH = 22f
        canvas.drawRoundRect(bx - badgeW / 2f, badgeY - badgeH / 2f, bx + badgeW / 2f, badgeY + badgeH / 2f, 6f, 6f, bloodstainBadgeBgPaint)
        canvas.drawRoundRect(bx - badgeW / 2f, badgeY - badgeH / 2f, bx + badgeW / 2f, badgeY + badgeH / 2f, 6f, 6f, bloodstainRunePaint)
        canvas.drawText("✦ ${level.bloodstainEssence} ESSENCE ✦", bx, badgeY + 4.5f, bloodstainTextPaint)
    }

    private fun drawVisceralExecutionEffect(canvas: Canvas, cameraX: Float, cameraY: Float, screenWidth: Float, screenHeight: Float) {
        val progress = (visceralEffectTimer / 0.35f).coerceIn(0f, 1f)
        val vx = visceralEffectX
        val vy = visceralEffectY

        // 1. Screen-wide crimson vignette impact
        val vignetteAlpha = (progress * 110f).toInt().coerceIn(0, 255)
        visceralCorePaint.color = Color.argb(vignetteAlpha, 185, 28, 28)
        canvas.drawRect(cameraX, cameraY, cameraX + screenWidth, cameraY + screenHeight, visceralCorePaint)

        // 2. Twin Cross Execution Slashes
        val slashLen = 78f * (1f + (1f - progress) * 0.4f)
        // Diagonal slash 1
        canvas.drawLine(vx - slashLen, vy - slashLen * 0.7f, vx + slashLen, vy + slashLen * 0.7f, visceralSlashPaint)
        canvas.drawLine(vx - slashLen, vy - slashLen * 0.7f, vx + slashLen, vy + slashLen * 0.7f, visceralSlashCorePaint)
        // Diagonal slash 2
        canvas.drawLine(vx + slashLen, vy - slashLen * 0.7f, vx - slashLen, vy + slashLen * 0.7f, visceralSlashPaint)
        canvas.drawLine(vx + slashLen, vy - slashLen * 0.7f, vx - slashLen, vy + slashLen * 0.7f, visceralSlashCorePaint)

        // 3. Central explosion core
        visceralCorePaint.color = Color.argb((progress * 255f).toInt(), 255, 255, 255)
        canvas.drawCircle(vx, vy, 16f * progress, visceralCorePaint)
        visceralCorePaint.color = Color.argb((progress * 180f).toInt(), 239, 68, 68)
        canvas.drawCircle(vx, vy, 32f * progress, visceralCorePaint)
    }

    private fun drawLootDiscoveryModal(canvas: Canvas, loot: LootReward, screenWidth: Float, screenHeight: Float) {
        // Non-blocking bottom-center Souls pickup toast (keeps combat 100% visible!)
        val fadeAlpha = ((lootModalTimer / 0.5f).coerceIn(0f, 1f) * 255).toInt()
        val hasWeapon = loot.weapon != null
        val modalW = minOf(520f, screenWidth - 360f).coerceAtLeast(320f)
        val modalH = if (hasWeapon) 114f else 92f
        val left = (screenWidth - modalW) / 2f
        val bottom = screenHeight - 24f
        val top = bottom - modalH
        val right = left + modalW
        val centerX = screenWidth / 2f

        val rarityColor = Color.parseColor(loot.rarity.colorHex)
        lootModalCardBg.alpha = ((fadeAlpha * 230) / 255)
        lootModalBorder.color = rarityColor
        lootModalBorder.alpha = fadeAlpha
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, lootModalCardBg)
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, lootModalBorder)

        // Header + Item Title
        lootHeaderPaint.color = rarityColor
        lootHeaderPaint.alpha = fadeAlpha
        canvas.drawText("✦  ${loot.title.uppercase()}  ✦", centerX, top + 28f, lootHeaderPaint)

        // Subtitle
        lootSubtitlePaint.alpha = fadeAlpha
        canvas.drawText(loot.subtitle, centerX, top + 50f, lootSubtitlePaint)

        // Compact inline weapon scaling badges
        if (hasWeapon) {
            val w = loot.weapon!!
            val badgeW = 74f
            val badgeH = 22f
            val gap = 12f
            val totalBadgesW = badgeW * 3 + gap * 2
            val startBadgeX = centerX - totalBadgesW / 2f
            val badgeY = top + 60f

            lootScalingBadgeBg.alpha = fadeAlpha
            lootScalingBadgeBorder.alpha = fadeAlpha

            val grades = arrayOf(
                Pair("STR", w.strGrade),
                Pair("AGI", w.agiGrade),
                Pair("INT", w.intGrade)
            )
            for (i in grades.indices) {
                val (stat, grade) = grades[i]
                val bx = startBadgeX + (badgeW + gap) * i
                canvas.drawRoundRect(bx, badgeY, bx + badgeW, badgeY + badgeH, 5f, 5f, lootScalingBadgeBg)
                canvas.drawRoundRect(bx, badgeY, bx + badgeW, badgeY + badgeH, 5f, 5f, lootScalingBadgeBorder)

                lootScalingText.color = Color.parseColor("#94A3B8")
                lootScalingText.alpha = fadeAlpha
                canvas.drawText("$stat:", bx + 22f, badgeY + 15f, lootScalingText)
                lootScalingText.color = if (grade == "S" || grade == "A") Color.parseColor("#F59E0B") else Color.WHITE
                lootScalingText.alpha = fadeAlpha
                canvas.drawText(grade, bx + 53f, badgeY + 15f, lootScalingText)
            }
        }

        // Compact footer hint
        val footerText = if (hasWeapon) {
            "Added to Arsenal • Tap Weapon Card to Swap"
        } else if (loot.isPrimordialSeed) {
            "Max Crimson Flasks +1 • Rest at Grace to refill"
        } else {
            "Essence absorbed into your spirit"
        }
        lootFooterPaint.alpha = fadeAlpha
        canvas.drawText(footerText, centerX, bottom - 12f, lootFooterPaint)

        // Restore default alphas
        lootModalCardBg.alpha = 235
        lootModalBorder.alpha = 255
        lootHeaderPaint.alpha = 255
        lootSubtitlePaint.alpha = 255
        lootFooterPaint.alpha = 255
    }
}
