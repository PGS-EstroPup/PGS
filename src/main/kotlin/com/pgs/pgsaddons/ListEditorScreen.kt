package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawCenteredTextWithShadow
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ListEditorScreen(
    private val parent: Screen,
    private val titleText: String,
    values: List<String>,
    private val onSave: (List<String>) -> Unit
) : Screen(Text.literal(titleText)) {
    private val entries = values.toMutableList()
    private val rows = mutableListOf<Row>()
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private lateinit var addButton: PgsButtonWidget
    private lateinit var doneButton: PgsButtonWidget

    private data class Row(
        val input: TextFieldWidget,
        val removeButton: PgsButtonWidget
    )

    override fun init() {
        rows.clear()
        entries.forEachIndexed { index, value -> addRow(index, value) }

        addButton = PgsButtonWidget(0, 0, 90, 20, Text.literal("Add")) {
            entries.add("")
            save()
            client?.setScreen(ListEditorScreen(parent, titleText, entries, onSave))
        }
        doneButton = PgsButtonWidget(0, 0, 90, 20, Text.literal("Done")) {
            save()
            client?.setScreen(parent)
        }
        addDrawableChild(addButton)
        addDrawableChild(doneButton)
    }

    private fun addRow(index: Int, value: String) {
        val input = TextFieldWidget(textRenderer, 0, 0, 220, 20, Text.literal("Entry ${index + 1}"))
        input.text = value
        input.setMaxLength(120)
        input.setChangedListener {
            if (index in entries.indices) {
                entries[index] = it
                save()
            }
        }

        val removeButton = PgsButtonWidget(0, 0, 64, 20, Text.literal("Remove")) {
            if (index in entries.indices) {
                entries.removeAt(index)
                save()
                client?.setScreen(ListEditorScreen(parent, titleText, entries, onSave))
            }
        }

        rows.add(Row(input, removeButton))
        addDrawableChild(input)
        addDrawableChild(removeButton)
    }

    private fun save() {
        onSave(cleanEntries(entries))
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        val panelWidth = minOf(420, width - 24).coerceAtLeast(300)
        val panelHeight = minOf(300, height - 24).coerceAtLeast(190)
        val panelX = (width - panelWidth) / 2
        val panelY = (height - panelHeight) / 2
        val accentColor = 0xFF000000.toInt() or (Settings.general.menuColor and 0xFFFFFF)
        val contentLeft = panelX + 18
        val contentRight = panelX + panelWidth - 18
        val listTop = panelY + 46
        val listBottom = panelY + panelHeight - 42

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC080808.toInt())
        drawBorder(context, panelX, panelY, panelX + panelWidth, panelY + panelHeight, accentColor)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(titleText), width / 2, panelY + 16, accentColor)

        var rowY = listTop - scrollY.toInt()
        rows.forEachIndexed { index, row ->
            row.input.x = contentLeft
            row.input.y = rowY
            row.input.setWidth((contentRight - contentLeft - 74).coerceAtLeast(120))
            row.input.height = 20
            row.removeButton.x = contentRight - 64
            row.removeButton.y = rowY
            row.removeButton.setWidth(64)
            row.removeButton.height = 20

            val visible = rowY + 20 > listTop && rowY < listBottom
            row.input.visible = visible
            row.removeButton.visible = visible
            rowY += 28
        }

        addButton.x = contentLeft
        addButton.y = panelY + panelHeight - 28
        addButton.setWidth(90)
        addButton.height = 20
        doneButton.x = contentRight - 90
        doneButton.y = panelY + panelHeight - 28
        doneButton.setWidth(90)
        doneButton.height = 20

        maxScroll = ((rows.size * 28) - (listBottom - listTop)).coerceAtLeast(0).toDouble()
        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scrollY = (scrollY - verticalAmount * 20.0).coerceIn(0.0, maxScroll)
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun close() {
        save()
        client?.setScreen(parent)
    }

    override fun shouldPause(): Boolean = true

    private fun drawBorder(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        context.fill(left, top, right, top + 1, color)
        context.fill(left, bottom - 1, right, bottom, color)
        context.fill(left, top, left + 1, bottom, color)
        context.fill(right - 1, top, right, bottom, color)
    }

    companion object {
        fun fromCommaList(value: String): List<String> {
            return value
                .trim()
                .removePrefix("[")
                .removeSuffix("]")
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        fun toCommaList(values: List<String>): String {
            return cleanEntries(values).joinToString(", ")
        }

        fun cleanEntries(values: List<String>): List<String> {
            return values.map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
