package com.pgs.pgsaddons

import com.pgs.pgsaddons.features.AutoFarmAction
import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawText
import net.minecraft.client.gui.drawCenteredTextWithShadow
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class AutoFarmOrderScreen(private val parent: Screen) : Screen(Text.literal("Edit Order")) {
    private enum class ActionCategory(val title: String, val color: Int) {
        INTERACTION("🖱 Actions", 0xFF55FF88.toInt()),
        TELEPORT("🏠 Teleports", 0xFFFFDD55.toInt()),
        FLOW("⛏ Flows", 0xFF66CCFF.toInt())
    }

    private val paletteGroups = listOf(
        ActionCategory.INTERACTION to listOf(
            AutoFarmAction.ARMOR_SLOT_1, 
            AutoFarmAction.ARMOR_SLOT_2,
            AutoFarmAction.ARMOR_SLOT_3,
            AutoFarmAction.INTERACT_MOUSEMAT,
            AutoFarmAction.INTERACT_ROD,
            AutoFarmAction.AUTO_SPRAY,
            AutoFarmAction.INTERACT_VACUUM_UNTIL_0_PESTS,
            AutoFarmAction.HOLD_VACUUM_5S,
            AutoFarmAction.HOLD_HOE,
            AutoFarmAction.SLOT_SWAP,
            AutoFarmAction.AUTO_SELL
        ),
        ActionCategory.TELEPORT to listOf(
            AutoFarmAction.SET_SPAWN,
            AutoFarmAction.WARP_SPAWN,
            AutoFarmAction.TPTOPLOT
        ),
        ActionCategory.FLOW to listOf(
            AutoFarmAction.REPEAT,
            AutoFarmAction.START_MOVEMENT,
            AutoFarmAction.STOP_MOVEMENT,
            AutoFarmAction.START_FARM,
            AutoFarmAction.STOP_FARM,
            AutoFarmAction.STOP_ACTION
        )
    )

    private data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun contains(mx: Double, my: Double): Boolean = mx >= x && mx <= x + w && my >= y && my <= y + h
    }

    private data class DragState(
        val action: AutoFarmAction,
        val sourceCycle: Int?,
        val sourceIndex: Int,
        var x: Int,
        var y: Int
    )

    private var dragging: DragState? = null
    private val paletteScroll = IntArray(3)

    private val cycleLists: List<MutableList<String>>
        get() = listOf(
            Settings.general.autoFarm2Cycle1,
            Settings.general.autoFarm2Cycle2,
            Settings.general.autoFarm2Cycle3
        )

    override fun init() {
        clearChildren()
        val layout = layout()
        addDrawableChild(PgsButtonWidget(layout.done.x, layout.done.y, layout.done.w, layout.done.h, Text.literal("Done")) {
            client?.setScreen(parent)
        })
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        val layout = layout()

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Edit Order"), width / 2, 12, 0xFFFFFFFF.toInt())
        layout.trashes.forEach { trash ->
            drawTrash(context, trash, dragging != null && trash.contains(mouseX.toDouble(), mouseY.toDouble()))
        }

        layout.cycles.forEachIndexed { cycle, rect ->
            drawBox(context, rect, if (dropCycle(mouseX.toDouble(), mouseY.toDouble()) == cycle) 0xFF55FFFF.toInt() else 0xFF555555.toInt())
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Cycle ${cycle + 1}"), rect.x + rect.w / 2, rect.y + 6, 0xFFFFFFFF.toInt())
            drawClearButton(context, clearButtonRect(rect), cycleLists[cycle].isNotEmpty())
            cycleLists[cycle].forEachIndexed { index, id ->
                val action = AutoFarmAction.fromId(id) ?: return@forEachIndexed
                val item = cycleItemRect(rect, index)
                if (dragging?.sourceCycle == cycle && dragging?.sourceIndex == index) {
                    drawAction(context, item, action, 0x55333333, 0xFF777777.toInt())
                } else {
                    drawAction(context, item, action, 0xCC202020.toInt(), actionColor(action))
                }
            }
        }

        drawBox(context, layout.palette, 0xFF777777.toInt())
        paletteGroups.forEachIndexed { groupIndex, (category, actions) ->
            val group = paletteGroupRect(layout.palette, groupIndex)
            context.drawText(textRenderer, Text.literal(category.title), group.x, group.y - 11, category.color, true)
            val visibleActions = visiblePaletteActions(groupIndex, group)
            visibleActions.forEachIndexed { actionIndex, action ->
                drawAction(context, paletteItemRect(group, actionIndex), action, 0xCC161616.toInt(), actionColor(action))
            }
            drawPaletteScrollHint(context, groupIndex, group)
        }

        dragging?.let {
            drawAction(context, Rect(it.x - 54, it.y - 9, 108, 18), it.action, 0xEE252525.toInt(), actionColor(it.action))
        }

        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val layout = layout()
        paletteGroups.forEachIndexed { groupIndex, (_, actions) ->
            val group = paletteGroupRect(layout.palette, groupIndex)
            if (group.contains(mouseX, mouseY)) {
                val maxScroll = maxPaletteScroll(groupIndex, group)
                if (maxScroll <= 0) return true

                val direction = if (verticalAmount > 0.0) -1 else 1
                paletteScroll[groupIndex] = (paletteScroll[groupIndex] + direction).coerceIn(0, maxScroll)
                return true
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() != 0) return super.mouseClicked(click, doubled)
        val mx = click.x()
        val my = click.y()
        val layout = layout()

        layout.cycles.forEachIndexed { cycle, rect ->
            if (clearButtonRect(rect).contains(mx, my)) {
                if (cycleLists[cycle].isNotEmpty()) {
                    cycleLists[cycle].clear()
                    Settings.save()
                }
                return true
            }
        }

        layout.cycles.forEachIndexed { cycle, rect ->
            cycleLists[cycle].forEachIndexed { index, id ->
                val action = AutoFarmAction.fromId(id) ?: return@forEachIndexed
                if (cycleItemRect(rect, index).contains(mx, my)) {
                    dragging = DragState(action, cycle, index, mx.toInt(), my.toInt())
                    return true
                }
            }
        }

        paletteGroups.forEachIndexed { groupIndex, (_, _) ->
            val group = paletteGroupRect(layout.palette, groupIndex)
            visiblePaletteActions(groupIndex, group).forEachIndexed { actionIndex, action ->
                if (paletteItemRect(group, actionIndex).contains(mx, my)) {
                    dragging = DragState(action, null, -1, mx.toInt(), my.toInt())
                    return true
                }
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        dragging?.let {
            it.x = click.x().toInt()
            it.y = click.y().toInt()
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: Click): Boolean {
        val drag = dragging ?: return super.mouseReleased(click)
        dragging = null

        val layout = layout()
        if (layout.trashes.any { it.contains(click.x(), click.y()) }) {
            if (drag.sourceCycle != null) {
                cycleLists[drag.sourceCycle].removeAt(drag.sourceIndex)
                Settings.save()
            }
            return true
        }

        val targetCycle = dropCycle(click.x(), click.y())
        if (targetCycle != null) {
            val target = cycleLists[targetCycle]
            var insertIndex = dropIndex(layout.cycles[targetCycle], click.y())

            if (drag.sourceCycle != null) {
                val source = cycleLists[drag.sourceCycle]
                source.removeAt(drag.sourceIndex)
                if (drag.sourceCycle == targetCycle && drag.sourceIndex < insertIndex) insertIndex--
            }

            target.add(insertIndex.coerceIn(0, target.size), drag.action.id)
            Settings.save()
            return true
        }

        return true
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private data class Layout(val cycles: List<Rect>, val palette: Rect, val trashes: List<Rect>, val done: Rect)

    private fun layout(): Layout {
        val gap = 6
        val trashW = 32
        val cycleTop = 38
        val paletteH = 126
        val maxContentW = 620.coerceAtMost(width - 48)
        val cycleW = ((maxContentW - trashW * 2 - gap * 4) / 3).coerceIn(96, 180)
        val contentW = cycleW * 3 + trashW * 2 + gap * 4
        val contentStart = (width - contentW) / 2
        val cyclesStart = contentStart + trashW + gap
        val done = Rect(contentStart + contentW - 90, height - 24, 90, 18)
        val palette = Rect(contentStart, height - paletteH - 34, contentW, paletteH)
        val cycleH = (palette.y - cycleTop - 16).coerceIn(116, 260)
        val cycles = List(3) { i -> Rect(cyclesStart + i * (cycleW + gap), cycleTop, cycleW, cycleH) }
        val trashes = listOf(
            Rect(contentStart, cycleTop, trashW, cycleH),
            Rect(cycles.last().x + cycleW + gap, cycleTop, trashW, cycleH)
        )
        return Layout(cycles, palette, trashes, done)
    }

    private fun cycleItemRect(cycle: Rect, index: Int): Rect {
        return Rect(cycle.x + 6, cycle.y + 22 + index * 20, cycle.w - 12, 18)
    }

    private fun clearButtonRect(cycle: Rect): Rect {
        return Rect(cycle.x + cycle.w - 40, cycle.y + 5, 34, 12)
    }

    private fun paletteGroupRect(palette: Rect, index: Int): Rect {
        val groupW = (palette.w - 24) / 3
        return Rect(palette.x + 6 + index * (groupW + 6), palette.y + 20, groupW, palette.h - 26)
    }

    private fun paletteItemRect(group: Rect, index: Int): Rect {
        val x = group.x
        val y = group.y + index * 17
        val itemW = group.w
        return Rect(x, y, itemW, 15)
    }

    private fun maxPaletteItems(group: Rect): Int = (group.h / 17).coerceAtLeast(1)

    private fun maxPaletteScroll(groupIndex: Int, group: Rect): Int {
        return (paletteGroups[groupIndex].second.size - maxPaletteItems(group)).coerceAtLeast(0)
    }

    private fun visiblePaletteActions(groupIndex: Int, group: Rect): List<AutoFarmAction> {
        val actions = paletteGroups[groupIndex].second
        val scroll = paletteScroll[groupIndex].coerceIn(0, maxPaletteScroll(groupIndex, group))
        paletteScroll[groupIndex] = scroll
        return actions.drop(scroll).take(maxPaletteItems(group))
    }

    private fun dropCycle(mx: Double, my: Double): Int? {
        return layout().cycles.indexOfFirst { it.contains(mx, my) }.takeIf { it >= 0 }
    }

    private fun dropIndex(cycle: Rect, my: Double): Int {
        return (((my.toInt() - cycle.y - 22) + 10) / 20).coerceAtLeast(0)
    }

    private fun drawAction(context: DrawContext, rect: Rect, action: AutoFarmAction, fill: Int, border: Int) {
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill)
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + 1, border)
        context.fill(rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, border)
        context.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.h, border)
        context.fill(rect.x + rect.w - 1, rect.y, rect.x + rect.w, rect.y + rect.h, border)
        context.drawText(textRenderer, Text.literal(action.label.take(18)), rect.x + 4, rect.y + 4, 0xFFFFFFFF.toInt(), true)
    }

    private fun drawPaletteScrollHint(context: DrawContext, groupIndex: Int, group: Rect) {
        val actions = paletteGroups[groupIndex].second
        val maxVisible = maxPaletteItems(group)
        if (actions.size <= maxVisible) return

        val first = paletteScroll[groupIndex] + 1
        val last = (paletteScroll[groupIndex] + maxVisible).coerceAtMost(actions.size)
        val text = Text.literal("$first-$last/${actions.size}")
        context.drawText(textRenderer, text, group.x + group.w - textRenderer .width(text), group.y - 11, 0xFFAAAAAA.toInt(), true)
    }

    private fun drawClearButton(context: DrawContext, rect: Rect, enabled: Boolean) {
        val border = if (enabled) 0xFFFF5555.toInt() else 0xFF555555.toInt()
        val fill = if (enabled) 0x99221111.toInt() else 0x66333333
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill)
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + 1, border)
        context.fill(rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, border)
        context.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.h, border)
        context.fill(rect.x + rect.w - 1, rect.y, rect.x + rect.w, rect.y + rect.h, border)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Clear"), rect.x + rect.w / 2, rect.y + 2, 0xFFFFFFFF.toInt())
    }

    private fun actionColor(action: AutoFarmAction): Int {
        return when (action) {
            AutoFarmAction.SET_SPAWN,
            AutoFarmAction.WARP_SPAWN,
            AutoFarmAction.TPTOPLOT -> ActionCategory.TELEPORT.color
            AutoFarmAction.REPEAT,
            AutoFarmAction.START_MOVEMENT,
            AutoFarmAction.STOP_MOVEMENT,
            AutoFarmAction.START_FARM,
            AutoFarmAction.STOP_FARM,
            AutoFarmAction.STOP_ACTION -> ActionCategory.FLOW.color
            else -> ActionCategory.INTERACTION.color
        }
    }

    private fun drawTrash(context: DrawContext, rect: Rect, hot: Boolean) {
        val color = if (hot) 0xFFFF5555.toInt() else 0xFFAA3333.toInt()
        drawBox(context, rect, color)
        val cx = rect.x + rect.w / 2
        val cy = rect.y + rect.h / 2
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("DEL"), cx, cy - 12, 0xFFFFFFFF.toInt())
        context.fill(cx - 8, cy + 4, cx + 8, cy + 7, color)
        context.fill(cx - 6, cy + 8, cx + 6, cy + 22, color)
        context.fill(cx - 4, cy + 1, cx + 4, cy + 3, color)
    }

    private fun drawBox(context: DrawContext, rect: Rect, color: Int) {
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 0xCC111111.toInt())
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + 1, color)
        context.fill(rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, color)
        context.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.h, color)
        context.fill(rect.x + rect.w - 1, rect.y, rect.x + rect.w, rect.y + rect.h, color)
    }
}




