package com.example.shadowescape.renderer

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.shadowescape.model.ArmorPiece
import com.example.shadowescape.model.Armors
import com.example.shadowescape.model.Player
import kotlin.math.sin

class ArmorRenderer(private val assetManager: AssetManager) {

    private val armorBitmaps = mutableMapOf<String, Bitmap>()

    // Paints
    private val armorPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private val screenOverlayPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(225, 7, 10, 18)
        isAntiAlias = true
    }

    private val modalBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0C111E")
        isAntiAlias = true
    }

    private val cardBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#151D2F")
        isAntiAlias = true
    }

    private val cardBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        color = Color.parseColor("#334155")
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val textPrimary = Paint().apply {
        color = Color.WHITE
        textSize = 15f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val textSecondary = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 12f
        isAntiAlias = true
    }

    private val textMuted = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 11f
        isAntiAlias = true
    }

    private val statValuePaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        textSize = 13.5f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val buttonBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
        isAntiAlias = true
    }

    private val buttonBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }

    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 13f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    fun getBitmap(assetFile: String): Bitmap? {
        if (armorBitmaps.containsKey(assetFile)) {
            return armorBitmaps[assetFile]
        }
        return try {
            assetManager.open("armors/$assetFile").use { isStream ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeStream(isStream, null, opts)
                if (bmp != null) {
                    armorBitmaps[assetFile] = bmp
                }
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Draws the equipped armor directly onto the player's torso bone during gameplay.
     */
    fun drawOnPlayer(
        canvas: Canvas,
        player: Player,
        chestX: Float,
        chestY: Float,
        flipH: Boolean,
        scaleMultiplier: Float = 1.0f
    ) {
        val armor = player.equippedArmor
        val bmp = getBitmap(armor.assetFile) ?: return

        canvas.save()
        if (flipH) {
            canvas.scale(-1f, 1f, chestX, chestY)
        }

        val baseW = 28f * scaleMultiplier
        val baseH = 30f * scaleMultiplier
        val dest = RectF(chestX - baseW / 2f, chestY - baseH / 2f, chestX + baseW / 2f, chestY + baseH / 2f)

        canvas.drawBitmap(bmp, null, dest, armorPaint)
        canvas.restore()
    }

    /**
     * Draws an individual armor slot icon with border, tier glow, and equipped badge.
     */
    fun drawArmorSlot(
        canvas: Canvas,
        armor: ArmorPiece,
        rect: RectF,
        isEquipped: Boolean,
        isSelected: Boolean = false
    ) {
        // Slot background
        val rarityColor = try {
            Color.parseColor(armor.rarityColorHex)
        } catch (_: Exception) {
            Color.parseColor("#38BDF8")
        }

        cardBgPaint.color = if (isSelected) Color.parseColor("#1E2D4A") else Color.parseColor("#111827")
        canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)

        // Border & glow
        if (isEquipped) {
            glowPaint.color = rarityColor
            glowPaint.strokeWidth = 2.4f
            canvas.drawRoundRect(rect, 8f, 8f, glowPaint)
        } else if (isSelected) {
            glowPaint.color = Color.parseColor("#38BDF8")
            glowPaint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 8f, 8f, glowPaint)
        } else {
            cardBorderPaint.color = Color.parseColor("#374151")
            canvas.drawRoundRect(rect, 8f, 8f, cardBorderPaint)
        }

        // Draw armor image
        val bmp = getBitmap(armor.assetFile)
        val iconPad = 8f
        val iconRect = RectF(rect.left + iconPad, rect.top + iconPad, rect.right - iconPad, rect.bottom - iconPad)
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, iconRect, armorPaint)
        }

        // Equipped indicator badge
        if (isEquipped) {
            val badgeH = 14f
            val badgeRect = RectF(rect.left, rect.bottom - badgeH, rect.right, rect.bottom)
            glowPaint.color = rarityColor
            glowPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(badgeRect, 0f, 0f, glowPaint)
            glowPaint.style = Paint.Style.STROKE

            buttonTextPaint.textSize = 9.5f
            buttonTextPaint.color = Color.parseColor("#0C111E")
            canvas.drawText("EQUIPPED", badgeRect.centerX(), badgeRect.centerY() + 3.5f, buttonTextPaint)
        }
    }

    /**
     * Draws the complete full-screen Armory & Visual Customization terminal.
     */
    fun drawArmoryScreen(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        player: Player,
        selectedArmorIndex: Int,
        armorSlotRects: List<RectF>,
        equipBtnRect: RectF,
        closeBtnRect: RectF,
        globalTime: Float
    ) {
        // 1. Frosted overlay
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, screenOverlayPaint)

        // 2. Main Terminal Modal Container
        val marginX = screenWidth * 0.05f
        val marginY = screenHeight * 0.045f
        val modalRect = RectF(marginX, marginY, screenWidth - marginX, screenHeight - marginY)

        canvas.drawRoundRect(modalRect, 14f, 14f, modalBgPaint)
        cardBorderPaint.color = Color.parseColor("#38BDF8")
        cardBorderPaint.strokeWidth = 2f
        canvas.drawRoundRect(modalRect, 14f, 14f, cardBorderPaint)

        // Close button '✕'
        closeBtnRect.set(modalRect.right - 44f, modalRect.top + 14f, modalRect.right - 14f, modalRect.top + 44f)
        buttonBgPaint.color = Color.parseColor("#1F2937")
        canvas.drawRoundRect(closeBtnRect, 6f, 6f, buttonBgPaint)
        buttonBorderPaint.color = Color.parseColor("#64748B")
        canvas.drawRoundRect(closeBtnRect, 6f, 6f, buttonBorderPaint)
        buttonTextPaint.textSize = 15f
        buttonTextPaint.color = Color.WHITE
        canvas.drawText("✕", closeBtnRect.centerX(), closeBtnRect.centerY() + 5f, buttonTextPaint)

        // Header Title
        val headY = modalRect.top + 34f
        canvas.drawText("✦   A R M O R Y   &   E Q U I P M E N T   ✦", modalRect.centerX(), headY, titlePaint)
        canvas.drawText("CUSTOMIZE ATTIRE  •  PHYSICAL DEFENSE & POISE ATTUNEMENT", modalRect.centerX(), headY + 18f, subtitlePaint)

        val contentTop = headY + 34f
        val contentBottom = modalRect.bottom - 18f
        val contentW = modalRect.width()
        val splitX = modalRect.left + contentW * 0.38f

        // ==========================================
        // LEFT PANEL: Selected / Equipped Armor Details
        // ==========================================
        val leftPanelRect = RectF(modalRect.left + 16f, contentTop, splitX - 10f, contentBottom)
        cardBgPaint.color = Color.parseColor("#111827")
        canvas.drawRoundRect(leftPanelRect, 10f, 10f, cardBgPaint)
        cardBorderPaint.color = Color.parseColor("#1F2937")
        canvas.drawRoundRect(leftPanelRect, 10f, 10f, cardBorderPaint)

        val armorsList = player.inventoryArmors.ifEmpty { Armors.ALL_ARMORS }
        val inspectedIndex = selectedArmorIndex.coerceIn(0, armorsList.size - 1)
        val inspectedArmor = armorsList[inspectedIndex]
        val isEquipped = player.equippedArmor.id == inspectedArmor.id

        val lPad = 18f
        var curY = leftPanelRect.top + 26f
        val lX = leftPanelRect.left + lPad
        val lRight = leftPanelRect.right - lPad

        // Large showcase icon frame
        val previewSize = 100f
        val previewRect = RectF(
            leftPanelRect.centerX() - previewSize / 2f,
            curY,
            leftPanelRect.centerX() + previewSize / 2f,
            curY + previewSize
        )
        val previewBg = RectF(previewRect.left - 4f, previewRect.top - 4f, previewRect.right + 4f, previewRect.bottom + 4f)
        cardBgPaint.color = Color.parseColor("#0B0F19")
        canvas.drawRoundRect(previewBg, 12f, 12f, cardBgPaint)

        val rarityColor = try {
            Color.parseColor(inspectedArmor.rarityColorHex)
        } catch (_: Exception) {
            Color.parseColor("#38BDF8")
        }
        glowPaint.color = (rarityColor and 0x00FFFFFF) or (0x88 shl 24)
        canvas.drawRoundRect(previewBg, 12f, 12f, glowPaint)

        val bmp = getBitmap(inspectedArmor.assetFile)
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, previewRect, armorPaint)
        }

        curY = previewRect.bottom + 22f

        // Armor Name
        textPrimary.textSize = 17f
        textPrimary.color = rarityColor
        textPrimary.textAlign = Paint.Align.CENTER
        canvas.drawText(inspectedArmor.name, leftPanelRect.centerX(), curY, textPrimary)

        curY += 18f
        textMuted.textAlign = Paint.Align.CENTER
        canvas.drawText("${inspectedArmor.category.displayName.uppercase()}  •  AFFINITY: ${inspectedArmor.classAffinity.uppercase()}", leftPanelRect.centerX(), curY, textMuted)

        curY += 20f
        // Stat Rows
        textSecondary.textAlign = Paint.Align.LEFT
        statValuePaint.textAlign = Paint.Align.RIGHT

        val stats = listOf(
            Triple("🛡️ Physical Defense", "+${inspectedArmor.physicalDef.toInt()}%", Color.parseColor("#22C55E")),
            Triple("🔮 Magic Negation", "+${inspectedArmor.magicNegation.toInt()}%", Color.parseColor("#38BDF8")),
            Triple("⚡ Poise Defense", "+${inspectedArmor.poise.toInt()}", Color.parseColor("#F59E0B")),
            Triple("⚖️ Equipment Weight", "${inspectedArmor.weight} kg", Color.parseColor("#94A3B8"))
        )

        for ((label, value, col) in stats) {
            canvas.drawText(label, lX, curY, textSecondary)
            statValuePaint.color = col
            canvas.drawText(value, lRight, curY, statValuePaint)
            curY += 22f
        }

        curY += 6f
        // Description Box
        val descBoxRect = RectF(lX, curY, lRight, curY + 68f)
        cardBgPaint.color = Color.parseColor("#0B0F19")
        canvas.drawRoundRect(descBoxRect, 6f, 6f, cardBgPaint)

        textMuted.textAlign = Paint.Align.LEFT
        textMuted.textSize = 11f
        textMuted.color = Color.parseColor("#CBD5E1")
        val words = inspectedArmor.description.split(" ")
        var line = ""
        var descY = descBoxRect.top + 18f
        val maxDescW = descBoxRect.width() - 16f
        for (w in words) {
            if (textMuted.measureText("$line $w") < maxDescW) {
                line = if (line.isEmpty()) w else "$line $w"
            } else {
                canvas.drawText(line, descBoxRect.left + 8f, descY, textMuted)
                descY += 16f
                line = w
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, descBoxRect.left + 8f, descY, textMuted)
        }

        // Equip / Equipped Button at bottom of left panel
        val equipH = 38f
        equipBtnRect.set(lX, leftPanelRect.bottom - equipH - 12f, lRight, leftPanelRect.bottom - 12f)
        if (isEquipped) {
            buttonBgPaint.color = Color.parseColor("#065F46")
            buttonBorderPaint.color = Color.parseColor("#10B981")
            canvas.drawRoundRect(equipBtnRect, 8f, 8f, buttonBgPaint)
            canvas.drawRoundRect(equipBtnRect, 8f, 8f, buttonBorderPaint)
            buttonTextPaint.color = Color.parseColor("#A7F3D0")
            buttonTextPaint.textSize = 13.5f
            buttonTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("✔  CURRENTLY EQUIPPED", equipBtnRect.centerX(), equipBtnRect.centerY() + 5f, buttonTextPaint)
        } else {
            val pulse = sin(globalTime * 5f) * 2f
            buttonBgPaint.color = Color.parseColor("#0284C7")
            buttonBorderPaint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(
                equipBtnRect.left - pulse,
                equipBtnRect.top - pulse,
                equipBtnRect.right + pulse,
                equipBtnRect.bottom + pulse,
                8f,
                8f,
                buttonBgPaint
            )
            canvas.drawRoundRect(equipBtnRect, 8f, 8f, buttonBorderPaint)
            buttonTextPaint.color = Color.WHITE
            buttonTextPaint.textSize = 13.5f
            buttonTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("✦  EQUIP THIS ARMOR  ✦", equipBtnRect.centerX(), equipBtnRect.centerY() + 5f, buttonTextPaint)
        }

        // ==========================================
        // RIGHT PANEL: Armor Armory Grid
        // ==========================================
        val rightPanelRect = RectF(splitX + 10f, contentTop, modalRect.right - 16f, contentBottom)
        cardBgPaint.color = Color.parseColor("#111827")
        canvas.drawRoundRect(rightPanelRect, 10f, 10f, cardBgPaint)
        cardBorderPaint.color = Color.parseColor("#1F2937")
        canvas.drawRoundRect(rightPanelRect, 10f, 10f, cardBorderPaint)

        // Grid Title & Summary
        var rY = rightPanelRect.top + 24f
        textPrimary.textSize = 15f
        textPrimary.color = Color.WHITE
        textPrimary.textAlign = Paint.Align.LEFT
        canvas.drawText("✦ AVAILABLE ARMORS (${armorsList.size} IN REPOSITORY)", rightPanelRect.left + 16f, rY, textPrimary)

        // Player current stats summary banner
        val currDef = player.equippedArmor.physicalDef.toInt()
        val currPoise = (player.stats.endurance * 2 + player.equippedArmor.poise).toInt()
        textMuted.textAlign = Paint.Align.RIGHT
        textMuted.color = Color.parseColor("#38BDF8")
        canvas.drawText("ACTIVE DEFENSE: $currDef%  •  TOTAL POISE: $currPoise", rightPanelRect.right - 16f, rY, textMuted)

        rY += 16f
        // Grid Layout: 5 columns x 3 rows (or as many as available)
        val gridPadX = 16f
        val gridPadY = 10f
        val gridStartX = rightPanelRect.left + gridPadX
        val gridStartY = rY + gridPadY
        val gridAvailW = rightPanelRect.width() - gridPadX * 2f

        val cols = 5
        val gap = 12f
        val slotW = (gridAvailW - (cols - 1) * gap) / cols
        val slotH = slotW

        for (i in armorsList.indices) {
            val c = i % cols
            val r = i / cols
            val sx = gridStartX + c * (slotW + gap)
            val sy = gridStartY + r * (slotH + gap)

            if (sy + slotH > rightPanelRect.bottom - 12f) break

            val sRect = if (i < armorSlotRects.size) armorSlotRects[i] else RectF()
            sRect.set(sx, sy, sx + slotW, sy + slotH)

            val armorItem = armorsList[i]
            val isSlotEquipped = player.equippedArmor.id == armorItem.id
            val isSlotSelected = inspectedIndex == i

            drawArmorSlot(canvas, armorItem, sRect, isSlotEquipped, isSlotSelected)
        }
    }
}
