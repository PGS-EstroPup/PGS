package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawText
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
    private var selecting = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0
    private var cursor = 0
    private var selectionAnchor: Int? = null
    private var scrollLine = 0

    @JvmStatic
    fun init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Toggle Notepad",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register {
            while (toggleKey.consumeClick()) {
                Settings.general.notepadRenderMode = if (Settings.general.notepadRenderMode == RENDER_OFF) RENDER_EVERYWHERE else RENDER_OFF
                if (Settings.general.notepadRenderMode == RENDER_OFF) focused = false
                Settings.save()
            }
        }

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("pgs_addons", "notepad")) { context, _ ->
            if (mc.screen == null && shouldRenderInHud()) render(context, -1, -1, 0f)
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
                !mouseClicked(click, false)
            }
            ScreenMouseEvents.allowMouseDrag(screen).register { _, click, offsetX, offsetY ->
                !mouseDragged(click, offsetX, offsetY)
            }
            ScreenMouseEvents.allowMouseRelease(screen).register { _, click ->
                !mouseReleased(click)
            }
            ScreenMouseEvents.allowMouseScroll(screen).register { _, mouseX, mouseY, _, verticalAmount ->
                !mouseScrolled(mouseX, mouseY, verticalAmount)
            }
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

        val newCursor = cursorFromMouse(mx, my)
        if (GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            if (selectionAnchor == null) selectionAnchor = cursor
        } else {
            selectionAnchor = null
        }
        cursor = newCursor
        selecting = true
        return true
    }

    @JvmStatic
    fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        if (!shouldRenderOnScreen() || (!dragging && !resizing && !selecting)) return false

        if (dragging) {
            Settings.general.notepadX = (click.x() - dragOffsetX).toInt().coerceAtLeast(0)
            Settings.general.notepadY = (click.y() - dragOffsetY).toInt().coerceAtLeast(0)
            Settings.save()
            return true
        }

        if (selecting) {
            if (selectionAnchor == null) selectionAnchor = cursor
            cursor = cursorFromMouse(click.x(), click.y())
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
        selecting = false
        return handled
    }

    @JvmStatic
    fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        if (!shouldRenderOnScreen()) return false
        val x = Settings.general.notepadX
        val y = Settings.general.notepadY
        val w = Settings.general.notepadWidth
        val h = Settings.general.notepadHeight
        if (!contains(mouseX, mouseY, x, y, w, h)) return false

        val visibleLines = visibleLineCount()
        val maxScroll = (visualLines().size - visibleLines).coerceAtLeast(0)
        scrollLine = (scrollLine - verticalAmount.toInt()).coerceIn(0, maxScroll)
        return true
    }

    @JvmStatic
    fun keyPressed(input: KeyInput): Boolean {
        if (!shouldRenderOnScreen() || !focused) return false
        val ctrl = input.modifiers() and GLFW.GLFW_MOD_CONTROL != 0 || input.modifiers() and GLFW.GLFW_MOD_SUPER != 0
        val shift = input.modifiers() and GLFW.GLFW_MOD_SHIFT != 0

        if (ctrl) {
            return when (input.key()) {
                GLFW.GLFW_KEY_A -> {
                    selectionAnchor = 0
                    cursor = Settings.general.notepadText.length
                    ensureCursorVisible()
                    true
                }
                GLFW.GLFW_KEY_C -> {
                    selectedText()?.let { mc.keyboardHandler.setClipboard(it) }
                    true
                }
                GLFW.GLFW_KEY_X -> {
                    selectedText()?.let {
                        mc.keyboardHandler.setClipboard(it)
                        replaceSelection("")
                    }
                    true
                }
                GLFW.GLFW_KEY_V -> {
                    insertText(mc.keyboardHandler.getClipboard())
                    true
                }
                else -> true
            }
        }

        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    replaceSelection("")
                } else if (cursor > 0) {
                    val text = Settings.general.notepadText
                    Settings.general.notepadText = text.removeRange(cursor - 1, cursor)
                    cursor--
                    Settings.save()
                }
                ensureCursorVisible()
                true
            }
            GLFW.GLFW_KEY_DELETE -> {
                val text = Settings.general.notepadText
                if (hasSelection()) {
                    replaceSelection("")
                } else if (cursor < text.length) {
                    Settings.general.notepadText = text.removeRange(cursor, cursor + 1)
                    Settings.save()
                }
                ensureCursorVisible()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insertText("\n")
                true
            }
            GLFW.GLFW_KEY_LEFT -> {
                moveCursor((cursor - 1).coerceAtLeast(0), shift)
                true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                moveCursor((cursor + 1).coerceAtMost(Settings.general.notepadText.length), shift)
                true
            }
            GLFW.GLFW_KEY_UP -> {
                moveCursor(cursorVertical(-1), shift)
                true
            }
            GLFW.GLFW_KEY_DOWN -> {
                moveCursor(cursorVertical(1), shift)
                true
            }
            GLFW.GLFW_KEY_HOME -> {
                moveCursor(visualLineStart(cursor), shift)
                true
            }
            GLFW.GLFW_KEY_END -> {
                moveCursor(visualLineEnd(cursor), shift)
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                false
            }
            else -> true
        }
    }

    @JvmStatic
    fun charTyped(input: CharInput): Boolean {
        if (!shouldRenderOnScreen() || !focused || !input.isAllowedChatCharacter()) return false

        insertText(input.codepointAsString())
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
        context.drawText(mc.font, Text.literal("Notepad"), x + 6, y + 6, 0xFFFFFFFF.toInt(), true)

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
        val lines = visualLines()
        val visibleLines = visibleLineCount()
        scrollLine = scrollLine.coerceIn(0, (lines.size - visibleLines).coerceAtLeast(0))
        for (line in lines.drop(scrollLine).take(visibleLines)) {
            drawSelection(context, line, left, y)
            context.drawText(mc.font, line.text.ifEmpty { " " }, left, y, 0xFFECECEC.toInt(), true)
            y += LINE_HEIGHT
            if (y > Settings.general.notepadY + Settings.general.notepadHeight) break
        }
    }

    private fun drawCursor(context: DrawContext, left: Int, top: Int) {
        val cursorPoint = cursorPoint()
        val cursorX = left + mc.font .width(cursorPoint.textBeforeCursor)
        val cursorY = top + (cursorPoint.lineIndex - scrollLine) * LINE_HEIGHT
        if (cursorY < top || cursorY > Settings.general.notepadY + Settings.general.notepadHeight - PADDING) return
        context.fill(cursorX, cursorY, cursorX + 1, cursorY + 10, 0xFFFFFFFF.toInt())
    }

    private fun insertText(value: String) {
        if (value.isEmpty()) return
        if (hasSelection()) {
            replaceSelection(value)
            return
        }
        val text = Settings.general.notepadText
        Settings.general.notepadText = text.substring(0, cursor) + value + text.substring(cursor)
        cursor += value.length
        selectionAnchor = null
        ensureCursorVisible()
        Settings.save()
    }

    private fun cursorFromMouse(mouseX: Double, mouseY: Double): Int {
        val lines = visualLines()
        val lineIndex = (scrollLine + ((mouseY.toInt() - Settings.general.notepadY - HEADER_HEIGHT - 5) / LINE_HEIGHT)).coerceIn(0, max(0, lines.size - 1))
        val line = lines.getOrElse(lineIndex) { VisualLine("", Settings.general.notepadText.length) }
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (i in 0..line.text.length) {
            val px = Settings.general.notepadX + PADDING + mc.font .width(line.text.substring(0, i))
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
        val maxWidth = (Settings.general.notepadWidth - PADDING * 2).coerceAtLeast(20)
        text.split('\n').forEachIndexed { index, paragraph ->
            addWrappedLines(paragraph, start, maxWidth, lines)
            start += paragraph.length
            if (index < text.count { it == '\n' }) {
                if (paragraph.isEmpty()) lines.add(VisualLine("", start))
                start++
            }
        }
        return lines
    }

    private fun addWrappedLines(paragraph: String, paragraphStart: Int, maxWidth: Int, out: MutableList<VisualLine>) {
        if (paragraph.isEmpty()) {
            out.add(VisualLine("", paragraphStart))
            return
        }

        var lineStart = 0
        while (lineStart < paragraph.length) {
            var bestEnd = lineStart + 1
            var lastBreak = -1
            var i = lineStart + 1
            while (i <= paragraph.length) {
                val candidate = paragraph.substring(lineStart, i)
                if (mc.font.width(candidate) > maxWidth) break
                bestEnd = i
                if (i < paragraph.length && paragraph[i].isWhitespace()) lastBreak = i + 1
                i++
            }

            val end = if (bestEnd < paragraph.length && lastBreak > lineStart) lastBreak else bestEnd
            out.add(VisualLine(paragraph.substring(lineStart, end).trimEnd(), paragraphStart + lineStart))
            lineStart = end
        }
    }

    private fun drawSelection(context: DrawContext, line: VisualLine, left: Int, y: Int) {
        val range = selectionRange() ?: return
        val lineStart = line.startIndex
        val lineEnd = line.startIndex + line.text.length
        val start = range.first.coerceIn(lineStart, lineEnd)
        val end = range.second.coerceIn(lineStart, lineEnd)
        if (start >= end) return

        val x1 = left + mc.font.width(line.text.substring(0, start - lineStart))
        val x2 = left + mc.font.width(line.text.substring(0, end - lineStart))
        context.fill(x1, y, x2.coerceAtLeast(x1 + 1), y + LINE_HEIGHT, 0x885A8DEE.toInt())
    }

    private fun moveCursor(newCursor: Int, keepSelection: Boolean) {
        if (keepSelection) {
            if (selectionAnchor == null) selectionAnchor = cursor
        } else {
            selectionAnchor = null
        }
        cursor = newCursor.coerceIn(0, Settings.general.notepadText.length)
        ensureCursorVisible()
    }

    private fun cursorVertical(delta: Int): Int {
        val point = cursorPoint()
        val lines = visualLines()
        val targetIndex = (point.lineIndex + delta).coerceIn(0, lines.lastIndex.coerceAtLeast(0))
        val targetLine = lines.getOrElse(targetIndex) { return cursor }
        val currentX = mc.font.width(point.textBeforeCursor)
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (i in 0..targetLine.text.length) {
            val distance = kotlin.math.abs(mc.font.width(targetLine.text.substring(0, i)) - currentX)
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return (targetLine.startIndex + best).coerceIn(0, Settings.general.notepadText.length)
    }

    private fun visualLineStart(index: Int): Int {
        val line = visualLines().firstOrNull { index in it.startIndex..(it.startIndex + it.text.length) }
        return line?.startIndex ?: lineStart(index)
    }

    private fun visualLineEnd(index: Int): Int {
        val line = visualLines().firstOrNull { index in it.startIndex..(it.startIndex + it.text.length) }
        return line?.let { it.startIndex + it.text.length } ?: lineEnd(index)
    }

    private fun selectedText(): String? {
        val range = selectionRange() ?: return null
        return Settings.general.notepadText.substring(range.first, range.second)
    }

    private fun replaceSelection(value: String) {
        val range = selectionRange() ?: return
        val text = Settings.general.notepadText
        Settings.general.notepadText = text.substring(0, range.first) + value + text.substring(range.second)
        cursor = range.first + value.length
        selectionAnchor = null
        ensureCursorVisible()
        Settings.save()
    }

    private fun hasSelection(): Boolean = selectionRange() != null

    private fun selectionRange(): Pair<Int, Int>? {
        val anchor = selectionAnchor ?: return null
        if (anchor == cursor) return null
        val start = min(anchor, cursor).coerceIn(0, Settings.general.notepadText.length)
        val end = max(anchor, cursor).coerceIn(0, Settings.general.notepadText.length)
        return start to end
    }

    private fun visibleLineCount(): Int {
        return ((Settings.general.notepadHeight - HEADER_HEIGHT - PADDING - 5) / LINE_HEIGHT).coerceAtLeast(1)
    }

    private fun ensureCursorVisible() {
        cursor = cursor.coerceIn(0, Settings.general.notepadText.length)
        val lineIndex = cursorPoint().lineIndex
        val visible = visibleLineCount()
        if (lineIndex < scrollLine) scrollLine = lineIndex
        if (lineIndex >= scrollLine + visible) scrollLine = lineIndex - visible + 1
        scrollLine = scrollLine.coerceAtLeast(0)
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
            RENDER_WORLD -> mc.level != null
            RENDER_EVERYWHERE -> true
            else -> false
        }
    }

    private fun shouldRenderOnScreen(): Boolean {
        return when (Settings.general.notepadRenderMode) {
            RENDER_WORLD -> mc.level != null
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





