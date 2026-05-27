package com.pgs.pgsaddons.utils

import com.pgs.pgsaddons.Settings
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.text.Text

class PgsSliderWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val prefix: String,
    private val min: Double,
    private val max: Double,
    initialValue: Double,
    private val onUpdate: (Double) -> Unit
) : SliderWidget(x, y, width, height, Text.literal(""), (initialValue - min) / (max - min)) {

    init {
        updateMessage()
    }

    override fun updateMessage() {
        val currentValue = min + (value * (max - min))
        message = Text.literal("$prefix: ${currentValue.toInt()}")
    }

    override fun applyValue() {
        val currentValue = min + (value * (max - min))
        onUpdate(currentValue)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!visible) return

        val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        
        val bgColor = if (hovered) 0xDD242424.toInt() else 0xCC151515.toInt()
        context.fill(x, y, x + width, y + height, bgColor)

        // Slider Handle - Cyan/Blue glow
        val handleWidth = 8
        val handleX = x + (value * (width - handleWidth)).toInt()
        val accentColor = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        val handleColor = accentColor
        context.fill(handleX, y, handleX + handleWidth, y + height, handleColor)

        val borderColor = accentColor
        
        // Draw custom 1px border
        context.fill(x, y, x + width, y + 1, borderColor) // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor) // Bottom
        context.fill(x, y, x + 1, y + height, borderColor) // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor) // Right

        // Text rendering - Always White
        val textColor = 0xFFFFFFFF.toInt()
        val textWidth = MinecraftClient.getInstance().textRenderer.getWidth(this.message)
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            this.message,
            this.x + (this.width - textWidth) / 2,
            this.y + (this.height - 9) / 2,
            textColor,
            true
        )
    }
}
