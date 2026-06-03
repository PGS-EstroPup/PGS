package net.minecraft.client.gui

import net.minecraft.network.chat.Component

typealias DrawContext = GuiGraphicsExtractor
typealias Click = net.minecraft.client.input.MouseButtonEvent

fun DrawContext.drawText(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean = false): Int {
    this.text(font, text, x, y, color, shadow)
    return font.width(text)
}

fun DrawContext.drawText(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean = false): Int {
    this.text(font, text, x, y, color, shadow)
    return font.width(text)
}

fun DrawContext.drawTextWithShadow(font: Font, text: Component, x: Int, y: Int, color: Int): Int {
    this.text(font, text, x, y, color, true)
    return font.width(text)
}

fun DrawContext.drawTextWithShadow(font: Font, text: String, x: Int, y: Int, color: Int): Int {
    this.text(font, text, x, y, color, true)
    return font.width(text)
}

fun DrawContext.drawCenteredTextWithShadow(font: Font, text: Component, centerX: Int, y: Int, color: Int) {
    this.centeredText(font, text, centerX, y, color)
}

fun DrawContext.drawCenteredTextWithShadow(font: Font, text: String, centerX: Int, y: Int, color: Int) {
    this.centeredText(font, text, centerX, y, color)
}

fun DrawContext.drawTooltip(font: Font, lines: List<Component>, x: Int, y: Int) {
    this.setComponentTooltipForNextFrame(font, lines, x, y)
}
