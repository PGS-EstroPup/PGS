package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawText
import net.minecraft.client.gui.drawTextWithShadow
import net.minecraft.client.gui.drawCenteredTextWithShadow
import net.minecraft.client.gui.drawTooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting

data class SettingsOptionRow(
    val name: String,
    val widget: ClickableWidget,
    val description: String? = null
)

data class SettingsFunctionGroup(
    val name: String,
    val button: PgsButtonWidget,
    val options: List<SettingsOptionRow>,
    val description: String? = null
)

object ThreePaneSettingsLayout {
    private const val NAV_WIDTH = 78
    private const val GAP = 7
    private const val TOP_HEIGHT = 40
    private const val BUTTON_HEIGHT = 20
    private const val FOOTER_HEIGHT = 34
    private const val ROW_GAP = 12
    private const val BOX_FILL = 0xCC080808.toInt()

    fun panelWidth(width: Int): Int = minOf(520, width - 20).coerceAtLeast(340)
    fun panelHeight(height: Int): Int = minOf(330, height - 20).coerceAtLeast(210)

    fun navWidth(): Int = NAV_WIDTH

    fun navLeft(startX: Int): Int = startX + GAP
    fun navRight(startX: Int): Int = startX + GAP + NAV_WIDTH

    // --- FIXED: Removed the extra 'startX +' so the panel doesn't fly off screen ---
    fun contentLeft(startX: Int): Int = navRight(startX) + GAP

    fun topY(startY: Int): Int = startY + 46
    fun optionsTop(startY: Int): Int = topY(startY) + TOP_HEIGHT + GAP
    fun optionsBottom(startY: Int, panelHeight: Int): Int = startY + panelHeight - FOOTER_HEIGHT
    fun functionLeft(startX: Int): Int = contentLeft(startX) + 8
    fun functionRight(startX: Int, panelWidth: Int): Int = startX + panelWidth - GAP - 8
    fun functionButtonTop(startY: Int): Int = topY(startY) + (TOP_HEIGHT - BUTTON_HEIGHT) / 2

    fun isInFunctionArea(mouseX: Double, mouseY: Double, startX: Int, startY: Int, panelWidth: Int): Boolean {
        return mouseX >= contentLeft(startX) &&
                mouseX <= startX + panelWidth - GAP &&
                mouseY >= topY(startY) &&
                mouseY <= topY(startY) + TOP_HEIGHT
    }

    fun drawChrome(
        context: DrawContext,
        textRenderer: Font,
        title: String,
        screenWidth: Int,
        startX: Int,
        startY: Int,
        panelWidth: Int,
        panelHeight: Int
    ) {
        val accentColor = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("PGS ADDONS").withStyle(Formatting.BOLD),
            screenWidth / 2,
            startY + 14,
            accentColor
        )

        val top = topY(startY)
        val right = startX + panelWidth - GAP
        val bottom = optionsBottom(startY, panelHeight)

        context.fill(navLeft(startX), top, navRight(startX), bottom, BOX_FILL)
        drawBorder(context, navLeft(startX), top, navRight(startX), bottom, accentColor)

        val left = contentLeft(startX)
        context.fill(left, top, right, top + TOP_HEIGHT, BOX_FILL)
        drawBorder(context, left, top, right, top + TOP_HEIGHT, accentColor)

        context.fill(left, optionsTop(startY), right, bottom, BOX_FILL)
        drawBorder(context, left, optionsTop(startY), right, bottom, accentColor)
    }

    fun prepareNavButtons(
        navButtons: List<ClickableWidget>,
        startX: Int,
        startY: Int
    ) {
        val leftBound = navLeft(startX)
        val rightBound = navRight(startX)
        val totalNavWidth = rightBound - leftBound

        val buttonWidth = 66
        val buttonHeight = BUTTON_HEIGHT
        val startYOffset = topY(startY) + 12
        val verticalGap = 8

        navButtons.forEachIndexed { index, button ->
            button.x = leftBound + (totalNavWidth - buttonWidth) / 2
            button.y = startYOffset + index * (buttonHeight + verticalGap)
            button.setWidth(buttonWidth)
            button.height = buttonHeight
        }
    }

    fun prepareFunctionButtons(
        groups: List<SettingsFunctionGroup>,
        activeGroup: String,
        startX: Int,
        startY: Int,
        panelWidth: Int,
        scrollX: Double
    ): Double {
        val left = functionLeft(startX)
        val top = functionButtonTop(startY)
        val right = functionRight(startX, panelWidth)
        val buttonGap = 8
        val buttonWidths = groups.map { functionButtonWidth(it.name) }

        groups.forEachIndexed { index, group ->
            val xOffset = buttonWidths.take(index).sum() + index * buttonGap
            val buttonWidth = buttonWidths[index]
            group.button.x = left + xOffset - scrollX.toInt()
            group.button.y = top
            group.button.setWidth(buttonWidth)
            group.button.height = BUTTON_HEIGHT
            group.button.selected = group.name == activeGroup
            group.button.visible = group.button.x + group.button.width > left && group.button.x < right
        }
        val totalWidth = buttonWidths.sum() + (groups.size - 1).coerceAtLeast(0) * buttonGap
        val visibleWidth = right - left
        return if (totalWidth > visibleWidth) (totalWidth - visibleWidth).toDouble() else 0.0
    }

    fun renderOptions(
        context: DrawContext,
        textRenderer: Font,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        groups: List<SettingsFunctionGroup>,
        activeGroup: String,
        startX: Int,
        startY: Int,
        panelWidth: Int,
        panelHeight: Int,
        scrollY: Double
    ): Double {
        groups.flatMap { it.options }.forEach { it.widget.visible = false }

        val activeOptions = groups.firstOrNull { it.name == activeGroup }?.options.orEmpty()
        val left = contentLeft(startX) + 20
        val top = optionsTop(startY) + 14
        val bottom = optionsBottom(startY, panelHeight) - 8
        val labelX = left
        var tooltipLines: List<Text>? = null

        // --- FIXED: Keeps option toggle buttons neatly tucked inside the new background frame position ---
        val rightBoxRightEdge = startX + panelWidth - GAP
        val widgetX = rightBoxRightEdge - 130
        val labelHoverRight = widgetX - 8

        context.enableScissor(contentLeft(startX), optionsTop(startY), startX + panelWidth - GAP, optionsBottom(startY, panelHeight))

        var currentY = top - scrollY.toInt()
        for (row in activeOptions) {
            row.widget.x = widgetX
            row.widget.y = currentY
            row.widget.setWidth(110)
            row.widget.height = if (row.widget is PgsColorPickerWidget) row.widget.preferredHeight else 18

            if (currentY + 20 > optionsTop(startY) && currentY < bottom) {
                row.widget.visible = true
                context.drawTextWithShadow(textRenderer, row.name, labelX, currentY + 6, 0xFFFFFFFF.toInt())
                if (!row.description.isNullOrBlank()) {
                    val labelHovered = mouseX >= labelX &&
                            mouseX <= labelHoverRight &&
                            mouseY >= currentY &&
                            mouseY <= currentY + row.widget.height
                    if (labelHovered) {
                        tooltipLines = wrapTooltip(row.description, textRenderer, 220)
                    }
                }
                row.widget.extractRenderState(context, mouseX, mouseY, delta)
            }
            currentY += row.widget.height + ROW_GAP
        }

        context.disableScissor()
        tooltipLines?.let { context.drawTooltip(textRenderer, it, mouseX, mouseY) }

        val totalHeight = activeOptions.sumOf { option ->
            (if (option.widget is PgsColorPickerWidget) option.widget.preferredHeight else 18) + ROW_GAP
        }
        val visibleHeight = bottom - top
        return if (totalHeight > visibleHeight) (totalHeight - visibleHeight).toDouble() else 0.0
    }

    fun hide(groups: List<SettingsFunctionGroup>) {
        groups.forEach { group ->
            group.button.visible = false
            group.options.forEach { it.widget.visible = false }
        }
    }

    fun hideFunctionButtons(groups: List<SettingsFunctionGroup>) {
        groups.forEach { it.button.visible = false }
    }

    fun renderFunctionButtons(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        groups: List<SettingsFunctionGroup>,
        startX: Int,
        startY: Int,
        panelWidth: Int
    ) {
        val left = functionLeft(startX)
        val right = functionRight(startX, panelWidth)
        val top = topY(startY)
        val bottom = top + TOP_HEIGHT
        val textRenderer = MinecraftClient.getInstance().font
        var tooltipLines: List<Text>? = null

        context.enableScissor(left, top, right, bottom)
        groups.forEach { group ->
            val overlapsViewport = group.button.x + group.button.width > left && group.button.x < right
            group.button.visible = overlapsViewport
            if (overlapsViewport) {
                group.button.extractRenderState(context, mouseX, mouseY, delta)
                if (!group.description.isNullOrBlank() &&
                    mouseX >= group.button.x &&
                    mouseX <= group.button.x + group.button.width &&
                    mouseY >= group.button.y &&
                    mouseY <= group.button.y + group.button.height
                ) {
                    tooltipLines = wrapTooltip(group.description, textRenderer, 220)
                }
            }
        }
        context.disableScissor()
        tooltipLines?.let { context.drawTooltip(textRenderer, it, mouseX, mouseY) }
    }

    private fun drawBorder(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        context.fill(left, top, right, top + 1, color)
        context.fill(left, bottom - 1, right, bottom, color)
        context.fill(left, top, left + 1, bottom, color)
        context.fill(right - 1, top, right, bottom, color)
    }

    private fun functionButtonWidth(name: String): Int {
        return (name.length * 6 + 24).coerceIn(72, 140)
    }

    private fun wrapTooltip(text: String, textRenderer: Font, maxWidth: Int): List<Text> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        var current = ""

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textRenderer.width(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines.add(current)
                current = word
            }
        }

        if (current.isNotEmpty()) lines.add(current)
        return lines.map { Text.literal(it) }
    }
}








