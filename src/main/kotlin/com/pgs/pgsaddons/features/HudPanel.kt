package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawText
import net.minecraft.text.Text

object HudPanel {
    private const val HEADER_HEIGHT = 20
    const val PADDING = 6
    const val LINE_HEIGHT = 11

    fun drawTextPanel(context: DrawContext, x: Int, y: Int, width: Int, height: Int, text: String, color: Int = 0xFFECECEC.toInt(), bold: Boolean = false) {
        val accent = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        val font = MinecraftClient.getInstance().font
        val message = Text.literal(if (bold) "\u00A7l$text" else text)
        val textX = x + ((width - font.width(message)) / 2).coerceAtLeast(PADDING)
        val textY = y + ((height - 9) / 2).coerceAtLeast(1)

        context.fill(x, y, x + width, y + height, 0xDD080808.toInt())
        drawBorder(context, x, y, x + width, y + height, accent)
        context.drawText(font, message, textX, textY, color, true)
    }

    fun draw(context: DrawContext, x: Int, y: Int, width: Int, height: Int, title: String): Int {
        val accent = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)

        context.fill(x, y, x + width, y + height, 0xDD080808.toInt())
        context.fill(x, y, x + width, y + HEADER_HEIGHT, 0xEE151515.toInt())
        drawBorder(context, x, y, x + width, y + height, accent)
        context.drawText(MinecraftClient.getInstance().font, Text.literal(title), x + PADDING, y + 6, 0xFFFFFFFF.toInt(), true)
        return y + HEADER_HEIGHT + 5
    }

    fun drawLine(context: DrawContext, text: String, x: Int, y: Int, color: Int = 0xFFECECEC.toInt()) {
        context.drawText(MinecraftClient.getInstance().font, Text.literal(text), x, y, color, true)
    }

    fun drawCenteredLine(context: DrawContext, text: String, x: Int, width: Int, y: Int, color: Int = 0xFFECECEC.toInt()) {
        val font = MinecraftClient.getInstance().font
        val message = Text.literal(text)
        val textX = x + ((width - font.width(message)) / 2).coerceAtLeast(PADDING)
        context.drawText(font, message, textX, y, color, true)
    }

    private fun drawBorder(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        context.fill(left, top, right, top + 1, color)
        context.fill(left, bottom - 1, right, bottom, color)
        context.fill(left, top, left + 1, bottom, color)
        context.fill(right - 1, top, right, bottom, color)
    }
}
