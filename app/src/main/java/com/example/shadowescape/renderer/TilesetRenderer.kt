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

class TilesetRenderer(assetManager: AssetManager) {

    private val dungeonStone: Bitmap? = loadBitmap(assetManager, "tiles/dungeon_stone.png")
    private val sewerGreen: Bitmap? = loadBitmap(assetManager, "tiles/sewer_green.png")
    private val crimsonAbyss: Bitmap? = loadBitmap(assetManager, "tiles/crimson_abyss.png")
    private val dungeonProps: Bitmap? = loadBitmap(assetManager, "tiles/dungeon_props.png")

    private val srcRect = Rect()
    private val destRect = RectF()

    private val tilePaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    private val gildedChestPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(255, 225, 130), Color.rgb(35, 20, 0))
    }

    private val ironChestPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        colorFilter = LightingColorFilter(Color.rgb(200, 215, 235), Color.rgb(10, 15, 25))
    }

    private val activeSwordPaint = Paint().apply {
        isFilterBitmap = false
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

    private fun sheetForX(x: Float): Bitmap? {
        return when {
            x >= 2750f -> crimsonAbyss ?: dungeonStone
            x >= 1380f -> sewerGreen ?: dungeonStone
            else -> dungeonStone
        }
    }

    fun drawNormalPlatform(canvas: Canvas, b: AABB, viewLeft: Float, viewRight: Float): Boolean {
        if (dungeonStone == null) return false

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
            val sheet = sheetForX(x + w * 0.5f) ?: continue

            var row = 0
            var y = b.top
            while (y < maxY) {
                val h = minOf(tileSize, maxY - y)
                val variant = (col % 3) * 16
                if (!isCeiling && row == 0) {
                    // Top surface brick edge (cols 1..3, row 0)
                    val sx = 16 + variant
                    srcRect.set(sx, 0, sx + 16, 16)
                } else {
                    // Inner brick fill (cols 1..2, row 4..5)
                    val sx = 16 + (col % 2) * 16
                    val sy = 64 + (row % 2) * 16
                    srcRect.set(sx, sy, sx + 16, sy + 16)
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
        val sheet = sheetForX(b.centerX) ?: return false
        val tileSize = 36f
        var x = b.left
        var col = 0
        val totalCols = ((b.width / tileSize).toInt()).coerceAtLeast(1)

        while (x < b.right) {
            val w = minOf(tileSize, b.right - x)
            val sx = when {
                col == 0 -> 96
                x + tileSize >= b.right -> 144
                else -> 112 + (col % 2) * 16
            }
            srcRect.set(sx, 128, sx + 16, 144)
            destRect.set(x, b.top, x + w, b.top + 30f)
            canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)

            // Corner support struts under left/right ends
            if (col == 0 || x + tileSize >= b.right) {
                val strutSx = if (col == 0) 96 else 144
                srcRect.set(strutSx, 144, strutSx + 16, 160)
                destRect.set(x, b.top + 26f, x + w, b.top + 54f)
                canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
            }

            x += tileSize
            col++
            if (col > totalCols + 4) break
        }
        return true
    }

    fun drawCheckpoint(canvas: Canvas, b: AABB, isActivated: Boolean): Boolean {
        val sheet = if (b.centerX >= 2750f) (crimsonAbyss ?: dungeonStone) else dungeonStone
        sheet ?: return false
        srcRect.set(160, 32, 176, 64)
        destRect.set(b.centerX - 22f, b.bottom - 88f, b.centerX + 22f, b.bottom)
        val p = if (isActivated) activeSwordPaint else tilePaint
        canvas.drawBitmap(sheet, srcRect, destRect, p)
        return true
    }

    fun drawHazard(canvas: Canvas, b: AABB): Boolean {
        val sheet = if (b.centerX >= 2750f) crimsonAbyss ?: dungeonStone else dungeonStone
        sheet ?: return false
        val spikeW = 48f
        var x = b.left
        while (x < b.right) {
            val w = minOf(spikeW, b.right - x)
            srcRect.set(224, 112, 256, 128)
            destRect.set(x, b.bottom - 28f, x + w, b.bottom)
            canvas.drawBitmap(sheet, srcRect, destRect, tilePaint)
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

        // Zone 1: Dungeon Stone barrels, pots, skulls
        dungeonStone?.let { stone ->
            drawPropIfVisible(canvas, stone, 128, 80, 160, 128, 210f, groundY, 72f, 108f, viewLeft, viewRight)
            drawPropIfVisible(canvas, stone, 160, 112, 176, 128, 530f, groundY, 36f, 36f, viewLeft, viewRight)
            drawPropIfVisible(canvas, stone, 224, 32, 256, 48, 1020f, groundY, 68f, 34f, viewLeft, viewRight)
            drawPropIfVisible(canvas, stone, 192, 64, 208, 128, 550f, 180f, 32f, 140f, viewLeft, viewRight)
            drawPropIfVisible(canvas, stone, 192, 64, 208, 128, 1180f, 160f, 32f, 140f, viewLeft, viewRight)
        }

        // Zone 2: Sewer Green grates & pipes
        sewerGreen?.let { sewer ->
            drawPropIfVisible(canvas, sewer, 128, 160, 192, 208, 1580f, groundY, 130f, 96f, viewLeft, viewRight)
            drawPropIfVisible(canvas, sewer, 128, 160, 192, 208, 2320f, groundY, 130f, 96f, viewLeft, viewRight)
            drawPropIfVisible(canvas, sewer, 96, 0, 128, 80, 1850f, 220f, 64f, 160f, viewLeft, viewRight)
        }

        // Zone 3: Crimson Abyss skulls & flesh spires
        crimsonAbyss?.let { abyss ->
            drawPropIfVisible(canvas, abyss, 224, 32, 256, 48, 2940f, groundY, 72f, 36f, viewLeft, viewRight)
            drawPropIfVisible(canvas, abyss, 128, 48, 160, 96, 3240f, groundY, 70f, 102f, viewLeft, viewRight)
            drawPropIfVisible(canvas, abyss, 224, 32, 256, 48, 3680f, groundY, 72f, 36f, viewLeft, viewRight)
            drawPropIfVisible(canvas, abyss, 224, 0, 256, 24, 3480f, 680f, 64f, 48f, viewLeft, viewRight)
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
