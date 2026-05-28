package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min

object NotepadOverlay {
    lateinit var toggleKey: KeyBinding

    private val mc = MinecraftClient.getInstance()
    private var focused = false
    private var dragging = false
    private var resizing = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0
    private var cursor = 0

    @JvmStatic
    fun init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Toggle Notepad",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register {
            while (toggleKey.wasPressed()) {
                Settings.general.notepadRenderMode = if (Settings.general.notepadRenderMode == RENDER_OFF) RENDER_EVERYWHERE else RENDER_OFF
                if (Settings.general.notepadRenderMode == RENDER_OFF) focused = false
                Settings.save()
            }
        }

        HudElementRegistry.addLast(Identifier.of("pgs_addons", "notepad")) { context, _ ->
            if (mc.currentScreen == null && shouldRenderInHud()) render(context, -1, -1, 0f)
        }
    }

    @JvmStatic
    fun renderOnScreen(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!shouldRenderOnScreen()) return
        render(context, mouseX, mouseY, delta)
    }

    fun resetWindow() {
        Settings.general.notepadX = 20
        Settings.general.notepadY = 20
        Settings.general.notepadWidth = 180
        Settings.general.notepadHeight = 120
        Settings.save()
    }

    @JvmStatic
    fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (!shouldRenderOnScreen() || click.button() != 0) return false

        val mx = click.x()
        val my = click.y()
        val x = Settings.general.notepadX
        val y = Settings.general.notepadY
        val w = Settings.general.notepadWidth
        val h = Settings.general.notepadHeight

        focused = contains(mx, my, x, y, w, h)
        if (!focused) return false

        if (isResizeHandle(mx, my, x, y, w, h)) {
            resizing = true
            return true
        }

        if (my <= y + HEADER_HEIGHT) {
            dragging = true
            dragOffsetX = mx - x
            dragOffsetY = my - y
            return true
        }

        cursor = cursorFromMouse(mx, my)
        return true
    }

    @JvmStatic
    fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        if (!shouldRenderOnScreen() || (!dragging && !resizing)) return false

        if (dragging) {
            Settings.general.notepadX = (click.x() - dragOffsetX).toInt().coerceAtLeast(0)
            Settings.general.notepadY = (click.y() - dragOffsetY).toInt().coerceAtLeast(0)
            Settings.save()
            return true
        }

        val newWidth = (click.x() - Settings.general.notepadX).toInt().coerceAtLeast(MIN_WIDTH)
        val newHeight = (click.y() - Settings.general.notepadY).toInt().coerceAtLeast(MIN_HEIGHT)
        Settings.general.notepadWidth = newWidth
        Settings.general.notepadHeight = newHeight
        Settings.save()
        return true
    }

    @JvmStatic
    fun mouseReleased(click: Click): Boolean {
        val handled = dragging || resizing
        dragging = false
        resizing = false
        return handled
    }

    @JvmStatic
    fun keyPressed(input: KeyInput): Boolean {
        if (!shouldRenderOnScreen() || !focused) return false

        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    val text = Settings.general.notepadText
                    Settings.general.notepadText = text.removeRange(cursor - 1, cursor)
                    cursor--
                    Settings.save()
                }
                true
            }
            GLFW.GLFW_KEY_DELETE -> {
                val text = Settings.general.notepadText
                if (cursor < text.length) {
                    Settings.general.notepadText = text.removeRange(cursor, cursor + 1)
                    Settings.save()
                }
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insertText("\n")
                true
            }
            GLFW.GLFW_KEY_LEFT -> {
                cursor = (cursor - 1).coerceAtLeast(0)
                true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                cursor = (cursor + 1).coerceAtMost(Settings.general.notepadText.length)
                true
            }
            GLFW.GLFW_KEY_HOME -> {
                cursor = lineStart(cursor)
                true
            }
            GLFW.GLFW_KEY_END -> {
                cursor = lineEnd(cursor)
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                false
            }
            else -> false
        }
    }

    @JvmStatic
    fun charTyped(input: CharInput): Boolean {
        if (!shouldRenderOnScreen() || !focused || !input.isValidChar) return false

        insertText(input.asString())
        return true
    }

    private fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val x = Settings.general.notepadX
        val y = Settings.general.notepadY
        val w = Settings.general.notepadWidth
        val h = Settings.general.notepadHeight
        val accent = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)

        context.fill(x, y, x + w, y + h, 0xDD080808.toInt())
        context.fill(x, y, x + w, y + HEADER_HEIGHT, 0xEE151515.toInt())
        drawBorder(context, x, y, x + w, y + h, accent)
        context.drawText(mc.textRenderer, Text.literal("Notepad"), x + 6, y + 6, 0xFFFFFFFF.toInt(), true)

        val bodyLeft = x + PADDING
        val bodyTop = y + HEADER_HEIGHT + 5
        val bodyRight = x + w - PADDING
        val bodyBottom = y + h - PADDING

        context.enableScissor(bodyLeft, bodyTop, bodyRight, bodyBottom)
        drawText(context, bodyLeft, bodyTop, bodyRight - bodyLeft)
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            drawCursor(context, bodyLeft, bodyTop)
        }
        context.disableScissor()

        val handleColor = if (isResizeHandle(mouseX.toDouble(), mouseY.toDouble(), x, y, w, h)) accent else 0xFFAAAAAA.toInt()
        context.fill(x + w - 9, y + h - 3, x + w - 3, y + h - 2, handleColor)
        context.fill(x + w - 6, y + h - 6, x + w - 3, y + h - 5, handleColor)
    }

    private fun drawText(context: DrawContext, left: Int, top: Int, maxWidth: Int) {
        var y = top
        for (line in visualLines()) {
            context.drawText(mc.textRenderer, line.text.ifEmpty { " " }, left, y, 0xFFECECEC.toInt(), true)
            y += LINE_HEIGHT
            if (y > Settings.general.notepadY + Settings.general.notepadHeight) break
        }
    }

    private fun drawCursor(context: DrawContext, left: Int, top: Int) {
        val cursorPoint = cursorPoint()
        val cursorX = left + mc.textRenderer.getWidth(cursorPoint.textBeforeCursor)
        val cursorY = top + cursorPoint.lineIndex * LINE_HEIGHT
        context.fill(cursorX, cursorY, cursorX + 1, cursorY + 10, 0xFFFFFFFF.toInt())
    }

    private fun insertText(value: String) {
        val text = Settings.general.notepadText
        Settings.general.notepadText = text.substring(0, cursor) + value + text.substring(cursor)
        cursor += value.length
        Settings.save()
    }

    private fun cursorFromMouse(mouseX: Double, mouseY: Double): Int {
        val lines = visualLines()
        val lineIndex = ((mouseY.toInt() - Settings.general.notepadY - HEADER_HEIGHT - 5) / LINE_HEIGHT).coerceIn(0, max(0, lines.size - 1))
        val line = lines.getOrElse(lineIndex) { VisualLine("", Settings.general.notepadText.length) }
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (i in 0..line.text.length) {
            val px = Settings.general.notepadX + PADDING + mc.textRenderer.getWidth(line.text.substring(0, i))
            val distance = kotlin.math.abs(px - mouseX.toInt())
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return (line.startIndex + best).coerceIn(0, Settings.general.notepadText.length)
    }

    private fun cursorPoint(): CursorPoint {
        val lines = visualLines()
        for ((index, line) in lines.withIndex()) {
            val end = line.startIndex + line.text.length
            if (cursor in line.startIndex..end) {
                return CursorPoint(index, line.text.substring(0, cursor - line.startIndex))
            }
        }
        val last = lines.lastOrNull() ?: VisualLine("", 0)
        return CursorPoint(max(0, lines.size - 1), last.text)
    }

    private fun visualLines(): List<VisualLine> {
        val text = Settings.general.notepadText
        if (text.isEmpty()) return listOf(VisualLine("", 0))

        val lines = mutableListOf<VisualLine>()
        var start = 0
        text.split('\n').forEachIndexed { index, line ->
            lines.add(VisualLine(line, start))
            start += line.length
            if (index < text.count { it == '\n' }) start++
        }
        return lines
    }

    private fun lineStart(index: Int): Int {
        return Settings.general.notepadText.lastIndexOf('\n', (index - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    }

    private fun lineEnd(index: Int): Int {
        return Settings.general.notepadText.indexOf('\n', index).let { if (it < 0) Settings.general.notepadText.length else it }
    }

    private fun contains(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int): Boolean {
        return mx >= x && mx <= x + w && my >= y && my <= y + h
    }

    private fun isResizeHandle(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int): Boolean {
        return mx >= x + w - 14 && mx <= x + w && my >= y + h - 14 && my <= y + h
    }

    private fun drawBorder(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        context.fill(left, top, right, top + 1, color)
        context.fill(left, bottom - 1, right, bottom, color)
        context.fill(left, top, left + 1, bottom, color)
        context.fill(right - 1, top, right, bottom, color)
    }

    private fun shouldRenderInHud(): Boolean {
        return when (Settings.general.notepadRenderMode) {
            RENDER_WORLD -> mc.world != null
            RENDER_EVERYWHERE -> true
            else -> false
        }
    }

    private fun shouldRenderOnScreen(): Boolean {
        return when (Settings.general.notepadRenderMode) {
            RENDER_WORLD -> mc.world != null
            RENDER_EVERYWHERE -> true
            else -> false
        }
    }

    private data class VisualLine(val text: String, val startIndex: Int)
    private data class CursorPoint(val lineIndex: Int, val textBeforeCursor: String)

    private const val HEADER_HEIGHT = 20
    private const val PADDING = 6
    private const val LINE_HEIGHT = 11
    private const val MIN_WIDTH = 100
    private const val MIN_HEIGHT = 70
    private const val RENDER_OFF = 0
    private const val RENDER_WORLD = 1
    private const val RENDER_EVERYWHERE = 2
}
