package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import com.pgs.pgsaddons.utils.PgsSliderWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class FishingSettingsScreen(private val parent: Screen) : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Autofish"
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    override fun init() {
        groups.clear()
        SettingsTabs.create(SettingsTab.FISHING, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun option(name: String, widget: ClickableWidget, description: String? = null): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget, description)
        }

        fun toggle(initial: Boolean, onToggle: (Boolean) -> Unit): PgsButtonWidget {
            var state = initial
            return PgsButtonWidget(0, 0, 100, 20, Text.literal(onOff(state))) { button ->
                state = !state
                onToggle(state)
                Settings.save()
                button.setMessage(Text.literal(onOff(state)))
            }
        }

        fun group(name: String, options: List<SettingsOptionRow>) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options))
        }

        var autofishButton: PgsButtonWidget? = null
        var autofishWithKillerButton: PgsButtonWidget? = null
        autofishButton = PgsButtonWidget(0, 0, 100, 20, Text.literal(onOff(Settings.general.autofish))) { button ->
            Settings.general.autofish = !Settings.general.autofish
            if (Settings.general.autofish) {
                Settings.general.autofishWithKillerEnabled = false
                autofishWithKillerButton?.setMessage(Text.literal(onOff(false)))
            }
            Settings.save()
            button.setMessage(Text.literal(onOff(Settings.general.autofish)))
        }
        autofishWithKillerButton = PgsButtonWidget(0, 0, 100, 20, Text.literal(onOff(Settings.general.autofishWithKillerEnabled))) { button ->
            Settings.general.autofishWithKillerEnabled = !Settings.general.autofishWithKillerEnabled
            if (Settings.general.autofishWithKillerEnabled) {
                Settings.general.autofish = false
                autofishButton?.setMessage(Text.literal(onOff(false)))
            }
            Settings.save()
            button.setMessage(Text.literal(onOff(Settings.general.autofishWithKillerEnabled)))
        }

        group("Autofish", listOf(
            option("Autofish", autofishButton, "Enables normal autofish. It pauses while a GUI is open."),
            option("Autofish Range", PgsSliderWidget(0, 0, 100, 20, "Range", 1.0, 30.0, Settings.general.autofishRange.toDouble()) {
                Settings.general.autofishRange = it.toInt()
                Settings.save()
            }, "Detection range for the bite indicator armor stand."),
            option("Leap Fish", toggle(Settings.general.autofishJumpBeforeCatch) { Settings.general.autofishJumpBeforeCatch = it }, "Jumps when a bite is detected, then waits 2-4 ticks before reeling in."),
            option("Autofish With Killer", autofishWithKillerButton, "Enables the fishing mode that swaps to a weapon, attacks, then swaps back to rod."),
            option("Killing Slot", PgsSliderWidget(0, 0, 100, 20, "Slot", 1.0, 9.0, Settings.general.killingItemSlot.toDouble()) {
                Settings.general.killingItemSlot = it.toInt()
                Settings.save()
            }, "Hotbar slot selected for killer swings."),
            option("Rod Slot", PgsSliderWidget(0, 0, 100, 20, "Slot", 1.0, 9.0, Settings.general.autofishRodSlot.toDouble()) {
                Settings.general.autofishRodSlot = it.toInt()
                Settings.save()
            }, "Hotbar slot selected again after killer swings."),
            option("Killing Swing Count", PgsSliderWidget(0, 0, 100, 20, "Count", 1.0, 5.0, Settings.general.killingSwingCount.toDouble()) {
                Settings.general.killingSwingCount = it.toInt()
                Settings.save()
            }, "Number of interact swings to send with the killing item.")
        ))

        val macroText = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("Macro Check Alert Text"))
        macroText.text = Settings.general.macroCheckAlertText
        macroText.setChangedListener {
            Settings.general.macroCheckAlertText = it
            Settings.save()
        }
        group("Macro Check", listOf(
            option("Main Toggle", toggle(Settings.general.macroCheckEnabled) { Settings.general.macroCheckEnabled = it }, "Enables the macro-check alert system for fishing."),
            option("Macro Check Alert Text", macroText, "Text shown when the macro-check alert triggers.")
        ))

        addDrawableChild(PgsButtonWidget(width / 2 - 185, startY + panelHeight - 24, 175, 18, Text.literal("§bMove HUD")) {
            client?.setScreen(HudEditorScreen(this))
        })
        addDrawableChild(PgsButtonWidget(width / 2 + 10, startY + panelHeight - 24, 175, 18, Text.literal("Done")) { client?.setScreen(null) })
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    private fun onOff(value: Boolean): String = if (value) "§aON" else "§cOFF"

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Fishing Settings", width, startX, startY, panelWidth, panelHeight)
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
