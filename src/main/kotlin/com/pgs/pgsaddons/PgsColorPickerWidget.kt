package com.pgs.pgsaddons

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.Locale

class PgsColorPickerWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    initialColor: Int,
    private val onUpdate: (Int) -> Unit
) : ClickableWidget(x, y, width, height, Text.literal("")) {
    var expanded = false
        private set

    val preferredHeight: Int
        get() = if (expanded) 116 else 18

    private var h = 0f
    private var s = 0f
    private var b = 0f
    private var hexText = ""
    private var hexFocused = false
    private var draggingHue = false
    private var draggingSv = false
    private var rgb = initialColor and 0xFFFFFF

    init {
        setFromRgb(rgb, notify = false)
    }

    override fun onClick(click: Click, doubled: Boolean) {
        if (click.button() != 0) return

        if (click.y() <= y + 18) {
            expanded = !expanded
            hexFocused = false
            return
        }

        if (!expanded) return

        val pickerTop = y + 24
        val pickerSize = 66
        val hueX = x
        val svX = x + 16
        val svW = width - 16
        val hexY = pickerTop + pickerSize + 6

        hexFocused = click.x() >= x && click.x() <= x + width && click.y() >= hexY && click.y() <= hexY + 18
        draggingHue = click.x() >= hueX && click.x() <= hueX + 10 && click.y() >= pickerTop && click.y() <= pickerTop + pickerSize
        draggingSv = click.x() >= svX && click.x() <= svX + svW && click.y() >= pickerTop && click.y() <= pickerTop + pickerSize

        updateDrag(click.x(), click.y())
    }

    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        if (!visible || (!draggingHue && !draggingSv)) return false
        updateDrag(click.x(), click.y())
        return true
    }

    override fun mouseReleased(click: Click): Boolean {
        draggingHue = false
        draggingSv = false
        return super.mouseReleased(click)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!expanded || !hexFocused) return false

        when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (hexText.isNotEmpty()) {
                    hexText = hexText.dropLast(1)
                    updateFromHexIfReady()
                }
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ESCAPE -> hexFocused = false
        }
        return true
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!expanded || !hexFocused || !input.isValidChar) return false

        input.asString().forEach { char ->
            if (char in '0'..'9' || char.lowercaseChar() in 'a'..'f') {
                if (hexText.length < 6) {
                    hexText += char.uppercaseChar()
                    updateFromHexIfReady()
                }
            }
        }
        return true
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        val accentColor = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        val borderColor = accentColor

        context.fill(x, y, x + width, y + 18, if (hovered) 0xDD242424.toInt() else 0xCC151515.toInt())
        drawBorder(context, x, y, x + width, y + 18, borderColor)
        context.fill(x + width - 18, y + 3, x + width - 4, y + 15, 0xFF000000.toInt() or rgb)
        drawBorder(context, x + width - 18, y + 3, x + width - 4, y + 15, accentColor)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        context.drawText(textRenderer, "#$hexText", x + 5, y + 5, 0xFFFFFFFF.toInt(), true)

        if (!expanded) return

        val pickerTop = y + 24
        val pickerSize = 66
        val hueX = x
        val svX = x + 16
        val svW = width - 16

        drawHueBar(context, hueX, pickerTop, 10, pickerSize)
        drawSvBox(context, svX, pickerTop, svW, pickerSize)

        val hexY = pickerTop + pickerSize + 6
        context.fill(x, hexY, x + width, hexY + 18, 0xDD080808.toInt())
        drawBorder(context, x, hexY, x + width, hexY + 18, accentColor)
        val cursor = if (hexFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) "|" else ""
        context.drawText(textRenderer, "#$hexText$cursor", x + 5, hexY + 5, 0xFFFFFFFF.toInt(), true)
    }

    private fun updateDrag(mouseX: Double, mouseY: Double) {
        val pickerTop = y + 24
        val pickerSize = 66
        val svX = x + 16
        val svW = width - 16

        if (draggingHue) h = ((mouseY - pickerTop) / pickerSize.toDouble()).toFloat().coerceIn(0f, 1f)
        if (draggingSv) {
            s = ((mouseX - svX) / svW.toDouble()).toFloat().coerceIn(0f, 1f)
            b = (1f - ((mouseY - pickerTop) / pickerSize.toDouble()).toFloat()).coerceIn(0f, 1f)
        }

        if (draggingHue || draggingSv) {
            rgb = Color.HSBtoRGB(h, s, b) and 0xFFFFFF
            updateHexText()
            onUpdate(rgb)
        }
    }

    private fun setFromRgb(color: Int, notify: Boolean) {
        rgb = color and 0xFFFFFF
        val hsb = Color.RGBtoHSB((rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255, null)
        h = hsb[0]
        s = hsb[1]
        b = hsb[2]
        updateHexText()
        if (notify) onUpdate(rgb)
    }

    private fun updateHexText() {
        hexText = String.format(Locale.US, "%06X", rgb)
    }

    private fun updateFromHexIfReady() {
        if (hexText.length == 6) {
            runCatching { setFromRgb(hexText.toInt(16), notify = true) }
        }
    }

    private fun drawHueBar(context: DrawContext, left: Int, top: Int, barWidth: Int, barHeight: Int) {
        val segments = 12
        for (i in 0 until segments) {
            val y1 = top + i * barHeight / segments
            val y2 = top + (i + 1) * barHeight / segments
            val c1 = 0xFF000000.toInt() or (Color.HSBtoRGB(i / segments.toFloat(), 1f, 1f) and 0xFFFFFF)
            val c2 = 0xFF000000.toInt() or (Color.HSBtoRGB((i + 1) / segments.toFloat(), 1f, 1f) and 0xFFFFFF)
            context.fillGradient(left, y1, left + barWidth, y2 + 1, c1, c2)
        }
        val markerY = top + (h * barHeight).toInt()
        context.fill(left - 1, markerY - 1, left + barWidth + 1, markerY + 1, 0xFFFFFFFF.toInt())
    }

    private fun drawSvBox(context: DrawContext, left: Int, top: Int, boxWidth: Int, boxHeight: Int) {
        val hueColor = Color.HSBtoRGB(h, 1f, 1f) and 0xFFFFFF

        context.fill(left, top, left + boxWidth, top + boxHeight, 0xFF000000.toInt() or hueColor)
        drawHorizontalGradient(context, left, top, boxWidth, boxHeight, 0xFFFFFFFF.toInt(), 0x00FFFFFF)
        context.fillGradient(left, top, left + boxWidth, top + boxHeight, 0x00000000, 0xFF000000.toInt())
        drawBorder(context, left, top, left + boxWidth, top + boxHeight, 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF))

        val markerX = left + (s * boxWidth).toInt()
        val markerY = top + ((1f - b) * boxHeight).toInt()
        context.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFFFFFFFF.toInt())
        context.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFF000000.toInt() or hueColor)
    }

    private fun drawHorizontalGradient(context: DrawContext, left: Int, top: Int, width: Int, height: Int, leftColor: Int, rightColor: Int) {
        val matrices = context.matrices
        matrices.pushMatrix()
        matrices.translate(left.toFloat(), (top + height).toFloat())
        matrices.rotate((-Math.PI / 2.0).toFloat())
        context.fillGradient(0, 0, height, width, leftColor, rightColor)
        matrices.popMatrix()
    }

    private fun drawBorder(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        context.fill(left, top, right, top + 1, color)
        context.fill(left, bottom - 1, right, bottom, color)
        context.fill(left, top, left + 1, bottom, color)
        context.fill(right - 1, top, right, bottom, color)
    }
}
