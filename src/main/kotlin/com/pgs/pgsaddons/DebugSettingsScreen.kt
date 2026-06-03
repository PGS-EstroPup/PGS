package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class DebugSettingsScreen(private val parent: Screen) : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Pingless Mining Debug"
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    override fun init() {
        groups.clear()
        SettingsTabs.create(SettingsTab.DEBUG, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun option(name: String, widget: ClickableWidget): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget)
        }

        fun toggle(initial: Boolean, onToggle: (Boolean) -> Unit): PgsButtonWidget {
            var state = initial
            return PgsButtonWidget(0, 0, 100, 20, onOff(state)) { button ->
                state = !state
                onToggle(state)
                Settings.save()
                button.setMessage(onOff(state))
            }
        }

        fun group(name: String, setting: Boolean, onToggle: (Boolean) -> Unit) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, listOf(option("Main Toggle", toggle(setting, onToggle)))))
        }

        group("Pingless Mining Debug", Settings.general.pinglessMiningDebugEnabled) { Settings.general.pinglessMiningDebugEnabled = it }
        group("TPS Sync", Settings.general.tpsSyncEnabled) { Settings.general.tpsSyncEnabled = it }

        addDrawableChild(PgsButtonWidget(width / 2 - 185, startY + panelHeight - 24, 175, 18, Text.literal("§bMove HUD")) {
            client?.setScreen(HudEditorScreen(this))
        })
        addDrawableChild(PgsButtonWidget(width / 2 + 10, startY + panelHeight - 24, 175, 18, Text.literal("Done")) { client?.setScreen(null) })
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    private fun onOff(value: Boolean): Text {
        return Text.literal(if (value) "ON" else "OFF").withStyle(if (value) Formatting.GREEN else Formatting.RED)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Debug Settings", width, startX, startY, panelWidth, panelHeight)
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

    override fun shouldPause(): Boolean = true
}




