package com.example.shadowescape.renderer

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.shadowescape.model.ChestState
import com.example.shadowescape.model.ChestType
import com.example.shadowescape.model.TreasureChest
import com.example.shadowescape.physics.AABB
import kotlin.math.sin

class TilesetRenderer(assetManager: AssetManager) {

    // 3 Progressive Elden Ring-style area tilesets
    private val ashenSepulcher: Bitmap? = loadBitmap(assetManager, "tiles/ashen_sepulcher.png")
    private val glintstoneCrypt: Bitmap? = loadBitmap(assetManager, "tiles/glintstone_crypt.png")
    private val crucibleBastion: Bitmap? = loadBitmap(assetManager, "tiles/crucible_bastion.png")
    private val dungeonProps: Bitmap? = loadBitmap(assetManager, "tiles/dungeon_props.png")
    private val dungeonStone: Bitmap? = loadBitmap(assetManager, "tiles/dungeon_stone.png")

    private val srcRect = Rect()
    private val destRect = RectF()

    private val tilePaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false
    }

    private val gildedChestPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(255, 225, 130), Color.rgb(35, 20, 0))
    }

    private val ironChestPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(200, 215, 235), Color.rgb(10, 15, 25))
    }

    private val activeSwordPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(255, 255, 255), Color.rgb(25, 45, 55))
    }

    private fun loadBitmap(assets: AssetManager, path: String): Bitmap? {
        return try {
            assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    enum class AreaZone {
        ASHEN_SEPULCHER,
        GLINTSTONE_CRYPT,
        CRUCIBLE_BASTION
    }

    fun zoneForX(x: Float): AreaZone {
        return when {
            x >= 2800f -> AreaZone.CRUCIBLE_BASTION
            x >= 1400f -> AreaZone.GLINTSTONE_CRYPT
            else -> AreaZone.ASHEN_SEPULCHER
        }
    }

    private fun sheetForZone(zone: AreaZone): Bitmap? {
        return when (zone) {
            AreaZone.CRUCIBLE_BASTION -> crucibleBastion ?: dungeonStone
            AreaZone.GLINTSTONE_CRYPT -> glintstoneCrypt ?: dungeonStone
            AreaZone.ASHEN_SEPULCHER -> ashenSepulcher ?: dungeonStone
        }
    }

    fun drawNormalPlatform(canvas: Canvas, b: AABB, viewLeft: Float, viewRight: Float): Boolean {
        val tileSize = 40f
        val startX = maxOf(b.left, viewLeft - tileSize)
        val endX = minOf(b.right, viewRight + tileSize)
        if (startX >= endX) return true

        val isCeiling = b.top < 100f
        val maxY = minOf(b.bottom, b.top + 280f)
        val firstCol = ((startX - b.left) / tileSize).toInt().coerceAtLeast(0)
        val lastCol = ((endX - b.left) / tileSize).toInt().coerceAtLeast(0)

        for (col in firstCol..lastCol) {
            val x = b.left + col * tileSize
            if (x >= b.right) break
            val w = minOf(tileSize, b.right - x)
            val zone = zoneForX(x + w * 0.5f)
            val sheet = sheetForZone(zone) ?: continue

            var row = 0
            var y = b.top
            while (y < maxY) {
                val h = minOf(tileSize, maxY - y)

                when (zone) {
                    AreaZone.ASHEN_SEPULCHER -> {
                        if (!isCeiling && row == 0) {
                            // Top surface stone flagstone (128x128 tiles at y=256 in 1024x1024 sheet)
                            val variant = (col % 4) * 128
                            srcRect.set(variant, 256, variant + 128, 384)
                        } else {
                            // Inner dark masonry brick fill (y=768)
                            val variant = (col % 2) * 128
                            val sy = if (row % 2 == 0) 768 else 896
                            srcRect.set(variant, sy, variant + 128, sy + 128)
                        }
                    }

                    AreaZone.GLINTSTONE_CRYPT -> {
                        if (!isCeiling && row == 0) {
                            // Submerged aquatic brick with cyan highlight (y=76, step=64)
                            val sx = 16 + (col % 3) * 64
                            srcRect.set(sx, 76, sx + 64, 140)
                        } else {
                            // Dark aqueduct slate bricks (y=250)
                            val sx = 16 + (col % 2) * 64
                            val sy = 250 + (row % 2) * 64
                            srcRect.set(sx, sy, sx + 64, sy + 64)
                        }
                    }

                    AreaZone.CRUCIBLE_BASTION -> {
                        if (!isCeiling && row == 0) {
                            // Glowing molten rock platform surface with fiery magma mortar (y=96)
                            val sx = 48 + (col % 3) * 64
                            srcRect.set(sx, 96, sx + 64, 160)
                        } else {
                            // Dark volcanic charcoal basalt fill (y=160)
                            val sx = 48 + (col % 2) * 64
                            val sy = 160 + (row % 2) * 64
                            srcRect.set(sx, sy, sx + 64, sy + 64)
                        }
                    }
                }

                destRect.set(x, y, x + w, y + h)
                canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
                y += tileSize
                row++
            }
        }
        return true
    }

    fun drawOneWayPlatform(canvas: Canvas, b: AABB): Boolean {
        val zone = zoneForX(b.centerX)
        val sheet = sheetForZone(zone) ?: return false
        val tileSize = 36f
        var x = b.left
        var col = 0
        val totalCols = ((b.width / tileSize).toInt()).coerceAtLeast(1)

        while (x < b.right) {
            val w = minOf(tileSize, b.right - x)

            when (zone) {
                AreaZone.ASHEN_SEPULCHER -> {
                    // Carved stone beam ledge
                    val sx = 128 + (col % 2) * 128
                    srcRect.set(sx, 384, sx + 128, 448)
                }

                AreaZone.GLINTSTONE_CRYPT -> {
                    // Carved marble cornice platform
                    val sx = 40 + (col % 3) * 64
                    srcRect.set(sx, 376, sx + 64, 436)
                }

                AreaZone.CRUCIBLE_BASTION -> {
                    // Heated iron floor grate
                    val sx = 250 + (col % 2) * 64
                    srcRect.set(sx, 560, sx + 64, 624)
                }
            }

            destRect.set(x, b.top, x + w, b.top + 30f)
            canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)

            x += tileSize
            col++
            if (col > totalCols + 4) break
        }
        return true
    }

    fun drawCheckpoint(canvas: Canvas, b: AABB, isActivated: Boolean): Boolean {
        val sheet = dungeonStone ?: return false
        srcRect.set(160, 32, 176, 64)
        destRect.set(b.centerX - 22f, b.bottom - 88f, b.centerX + 22f, b.bottom)
        val p = if (isActivated) activeSwordPaint else tilePaint
        canvas.drawBitmap(sheet, srcRect, destRect, p)
        return true
    }

    fun drawHazard(canvas: Canvas, b: AABB): Boolean {
        val zone = zoneForX(b.centerX)
        val sheet = sheetForZone(zone) ?: return false
        val spikeW = 44f
        var x = b.left

        while (x < b.right) {
            val w = minOf(spikeW, b.right - x)
            when (zone) {
                AreaZone.CRUCIBLE_BASTION -> {
                    // Glowing bubbling molten lava pool hazard
                    srcRect.set(750, 460, 950, 500)
                    destRect.set(x, b.bottom - 32f, x + w, b.bottom + 4f)
                }

                AreaZone.GLINTSTONE_CRYPT -> {
                    // Cyan crystal / icicle stalagmites
                    srcRect.set(510, 450, 620, 500)
                    destRect.set(x, b.bottom - 30f, x + w, b.bottom)
                }

                AreaZone.ASHEN_SEPULCHER -> {
                    // Classical sharp stone / iron ground spikes
                    dungeonStone?.let { ds ->
                        srcRect.set(224, 112, 256, 128)
                        destRect.set(x, b.bottom - 28f, x + w, b.bottom)
                        canvas.drawBitmap(ds, srcRect, destRect, tilePaint)
                    } ?: run {
                        srcRect.set(0, 0, 64, 64)
                        destRect.set(x, b.bottom - 28f, x + w, b.bottom)
                        canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
                    }
                }
            }
            if (zone != AreaZone.ASHEN_SEPULCHER || dungeonStone == null) {
                canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
            }
            x += spikeW
        }
        return true
    }

    fun drawBox(canvas: Canvas, b: AABB): Boolean {
        val sheet = dungeonStone ?: return false
        srcRect.set(32, 192, 64, 224)
        destRect.set(b.left, b.top, b.right, b.bottom)
        canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
        return true
    }

    fun drawDoor(canvas: Canvas, b: AABB): Boolean {
        val sheet = dungeonProps ?: return false
        srcRect.set(64, 0, 96, 32)
        var y = b.top
        val tileH = 48f
        while (y < b.bottom) {
            val h = minOf(tileH, b.bottom - y)
            destRect.set(b.left - 4f, y, b.right + 4f, y + h)
            canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
            y += tileH
        }
        return true
    }

    fun drawSwitch(canvas: Canvas, b: AABB, isActivated: Boolean): Boolean {
        val sheet = dungeonStone ?: return false
        srcRect.set(192, 0, 224, 16)
        destRect.set(b.left - 8f, b.bottom - 32f, b.right + 8f, b.bottom)
        canvas.save()
        if (isActivated) {
            canvas.scale(-1f, 1f, b.centerX, b.centerY)
        }
        canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
        canvas.restore()
        return true
    }

    fun drawChest(canvas: Canvas, chest: TreasureChest): Boolean {
        val sheet = dungeonProps ?: return false
        val b = chest.bounds
        val isOpen = chest.state != ChestState.CLOSED
        if (isOpen) {
            srcRect.set(224, 32, 256, 64)
        } else {
            srcRect.set(160, 32, 192, 64)
        }
        destRect.set(b.left - 6f, b.top - 12f, b.right + 6f, b.bottom + 2f)
        val p = when (chest.type) {
            ChestType.GILDED -> gildedChestPaint
            ChestType.IRON -> ironChestPaint
            ChestType.WOODEN -> tilePaint
        }
        canvas.drawBitmap(sheet, srcRect, destRect, p)
        return true
    }

    fun drawZoneDecor(canvas: Canvas, viewLeft: Float, viewRight: Float) {
        val groundY = 880f

        // ========================================================
        // AREA 1: Ashen Sepulcher (0..1400f)
        // Ancient gravestones, wall torches, fluted pillars, chains
        // ========================================================
        ashenSepulcher?.let { ashen ->
            // Lit Wall-mounted iron torches (with subtle flickering light)
            drawPropIfVisible(canvas, ashen, 520, 750, 610, 870, 180f, 620f, 44f, 72f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 520, 750, 610, 870, 580f, 620f, 44f, 72f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 520, 750, 610, 870, 940f, 620f, 44f, 72f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 520, 750, 610, 870, 1260f, 620f, 44f, 72f, viewLeft, viewRight)

            // Ancient Stone Gravestones with runic inscriptions
            drawPropIfVisible(canvas, ashen, 740, 505, 875, 625, 420f, groundY, 56f, 72f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 740, 505, 875, 625, 860f, groundY, 56f, 72f, viewLeft, viewRight)

            // Tall fluted catacomb pillars
            drawPropIfVisible(canvas, ashen, 840, 750, 920, 990, 310f, groundY, 48f, 130f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 840, 750, 920, 990, 740f, groundY, 48f, 130f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 840, 750, 920, 990, 1120f, groundY, 48f, 130f, viewLeft, viewRight)

            // Hanging iron chains from upper arches
            drawPropIfVisible(canvas, ashen, 896, 512, 980, 768, 640f, 260f, 36f, 120f, viewLeft, viewRight)
            drawPropIfVisible(canvas, ashen, 896, 512, 980, 768, 1020f, 260f, 36f, 120f, viewLeft, viewRight)
        }

        // ========================================================
        // AREA 2: Glintstone Crypt (1400..2800f)
        // Glowing cyan crystal clusters, dripping stalactites, arches
        // ========================================================
        glintstoneCrypt?.let { glint ->
            // Bioluminescent Glintstone crystal clusters sprouting from rocks
            drawPropIfVisible(canvas, glint, 630, 80, 700, 150, 1520f, groundY, 52f, 52f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 710, 80, 790, 150, 1880f, groundY, 64f, 56f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 890, 80, 970, 150, 2150f, groundY, 68f, 58f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 710, 80, 790, 150, 2480f, groundY, 64f, 56f, viewLeft, viewRight)

            // Dripping ceiling stalactites
            drawPropIfVisible(canvas, glint, 505, 340, 625, 460, 1680f, 260f, 56f, 96f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 505, 340, 625, 460, 2040f, 260f, 56f, 96f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 505, 340, 625, 460, 2380f, 260f, 56f, 96f, viewLeft, viewRight)

            // Submerged crystal colonnade pillars
            drawPropIfVisible(canvas, glint, 380, 690, 430, 810, 1780f, groundY, 44f, 110f, viewLeft, viewRight)
            drawPropIfVisible(canvas, glint, 380, 690, 430, 810, 2260f, groundY, 44f, 110f, viewLeft, viewRight)
        }

        // ========================================================
        // AREA 3: Crucible Bastion (2800..4200f)
        // Stone gargoyles, glowing runic pillars, blazing braziers
        // ========================================================
        crucibleBastion?.let { crucible ->
            // Carved stone gargoyle statues perched on bastions
            drawPropIfVisible(canvas, crucible, 500, 500, 595, 620, 2960f, groundY, 68f, 78f, viewLeft, viewRight)
            drawPropIfVisible(canvas, crucible, 500, 500, 595, 620, 3520f, groundY, 68f, 78f, viewLeft, viewRight)
            drawPropIfVisible(canvas, crucible, 500, 500, 595, 620, 3880f, groundY, 72f, 82f, viewLeft, viewRight)

            // Fiery Runic Pillars inscribed with glowing molten glyphs
            drawPropIfVisible(canvas, crucible, 600, 320, 660, 500, 3120f, groundY, 48f, 130f, viewLeft, viewRight)
            drawPropIfVisible(canvas, crucible, 600, 320, 660, 500, 3700f, groundY, 48f, 130f, viewLeft, viewRight)

            // Blazing ritual braziers with crackling fire
            drawPropIfVisible(canvas, crucible, 505, 630, 575, 720, 3320f, groundY, 52f, 62f, viewLeft, viewRight)
            drawPropIfVisible(canvas, crucible, 505, 630, 575, 720, 3960f, groundY, 52f, 62f, viewLeft, viewRight)

            // Tattered war banners of the Crucible
            drawPropIfVisible(canvas, crucible, 600, 810, 650, 920, 3420f, 680f, 38f, 84f, viewLeft, viewRight)
        }
    }

    private fun drawPropIfVisible(
        canvas: Canvas,
        sheet: Bitmap,
        sx1: Int, sy1: Int, sx2: Int, sy2: Int,
        worldX: Float, bottomY: Float,
        w: Float, h: Float,
        viewLeft: Float, viewRight: Float
    ) {
        if (worldX + w < viewLeft || worldX > viewRight) return
        srcRect.set(sx1, sy1, sx2, sy2)
        destRect.set(worldX, bottomY - h, worldX + w, bottomY)
        canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
    }
}
