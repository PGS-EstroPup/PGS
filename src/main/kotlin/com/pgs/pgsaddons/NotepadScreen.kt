package com.pgs.pgsaddons

import com.pgs.pgsaddons.features.NotepadOverlay
import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

class NotepadScreen(private val parent: Screen) : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Notepad"
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    override fun init() {
        groups.clear()
        SettingsTabs.create(SettingsTab.GENERAL, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun option(name: String, widget: ClickableWidget, description: String? = null): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget, description)
        }

        fun group(name: String, options: List<SettingsOptionRow>) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options))
        }

        val renderModeButton = PgsButtonWidget(0, 0, 100, 20, Text.literal(renderModeLabel(Settings.general.notepadRenderMode))) { button ->
            Settings.general.notepadRenderMode = (Settings.general.notepadRenderMode + 1) % 3
            Settings.save()
            button.setMessage(Text.literal(renderModeLabel(Settings.general.notepadRenderMode)))
        }
        val resetButton = PgsButtonWidget(0, 0, 100, 20, Text.literal("Reset")) {
            NotepadOverlay.resetWindow()
        }
        val clearButton = PgsButtonWidget(0, 0, 100, 20, Text.literal("Clear")) {
            Settings.general.notepadText = ""
            Settings.save()
        }

        group("Notepad", listOf(
            option("Render Mode", renderModeButton, "Controls where the notepad HUD window is rendered."),
            option("Reset Window", resetButton, "Moves the notepad back to its default size and position."),
            option("Clear Notes", clearButton, "Deletes the current notepad text.")
        ))

        addDrawableChild(PgsButtonWidget(width / 2 - 185, startY + panelHeight - 24, 175, 18, Text.literal("Â§bMove HUD")) {
            client?.setScreen(HudEditorScreen(this))
        })
        addDrawableChild(PgsButtonWidget(width / 2 + 10, startY + panelHeight - 24, 175, 18, Text.literal("Done")) { client?.setScreen(null) })
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Notepad Settings", width, startX, startY, panelWidth, panelHeight)
        maxFeatureScrollX = ThreePaneSettingsLayout.prepareFunctionButtons(groups, activeGroup, startX, startY, panelWidth, featureScrollX)
        ThreePaneSettingsLayout.hideFunctionButtons(groups)
        super.render(context, mouseX, mouseY, delta)
        ThreePaneSettingsLayout.renderFunctionButtons(context, mouseX, mouseY, delta, groups, startX, startY, panelWidth)
        maxScroll = ThreePaneSettingsLayout.renderOptions(context, textRenderer, mouseX, mouseY, delta, groups, activeGroup, startX, startY, panelWidth, panelHeight, scrollY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (ThreePaneSettingsLayout.isInFunctionArea(mouseX, mouseY, startX, startY, panelWidth)) {
            featureScrollX = (featureScrollX - verticalAmount * 28.0 - horizontalAmount * 28.0).coerceIn(0.0, maxFeatureScrollX)
        } else {
            scrollY = (scrollY - verticalAmount * 20.0).coerceIn(0.0, maxScroll)
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun onOff(value: Boolean): String = if (value) "Â§aON" else "Â§cOFF"

    private fun renderModeLabel(value: Int): String {
        return when (value.coerceIn(0, 2)) {
            1 -> "\u00A7aWorld"
            2 -> "\u00A7aEverywhere"
            else -> "\u00A7cOFF"
        }
    }

    override fun shouldPause(): Boolean = true
}



