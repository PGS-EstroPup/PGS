package com.pgs.pgsaddons.utils

import com.pgs.pgsaddons.Settings
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

class PgsButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    var selected: Boolean = false,
    private val onPressAction: (PgsButtonWidget) -> Unit
) : ClickableWidget(x, y, width, height, message) {

    override fun onClick(click: Click, doubled: Boolean) {
        onPressAction(this)
    }

    override fun updateWidgetNarration(builder: NarrationMessageBuilder) {
        defaultButtonNarrationText(builder)
    }

    override fun extractWidgetRenderState(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!visible) return

        val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        
        val accentColor = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        val accentGlow = ((if (hovered) 0x77 else 0x55) shl 24) or (Settings.general.menuColor and 0xFFFFFF)
        val bgColor = when {
            selected -> accentGlow
            hovered -> 0xDD242424.toInt()
            else -> 0xCC151515.toInt()
        }
        context.fill(x, y, x + width, y + height, bgColor)

        val borderColor = if (hovered || selected) accentColor else 0xFF1F1F1F.toInt()
        
        // Draw custom 1px border
        context.fill(x, y, x + width, y + 1, borderColor) // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor) // Bottom
        context.fill(x, y, x + 1, y + height, borderColor) // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor) // Right

        // Text rendering - Always White
        val textColor = 0xFFFFFFFF.toInt()
        val textWidth = MinecraftClient.getInstance().font.width(this.message)
        context.text(
            MinecraftClient.getInstance().font,
            this.message,
            this.x + (this.width - textWidth) / 2,
            this.y + (this.height - 9) / 2,
            textColor,
            true
        )
    }
}
