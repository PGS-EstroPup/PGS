package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import com.pgs.pgsaddons.utils.PgsSliderWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class MiningSettingsScreen(private val parent: Screen) : Screen(Text.empty()) {
    private val reminderSeparator = "§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Powder Chest ESP"
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    override fun init() {
        groups.clear()
        SettingsTabs.create(SettingsTab.MINING, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun option(name: String, widget: ClickableWidget, description: String? = null): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget, description)
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

        fun numberInput(name: String, initial: String, onChange: (String) -> Unit): TextFieldWidget {
            val input = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal(name))
            input.text = initial
            input.setTextPredicate { it.isEmpty() || it.all(Char::isDigit) }
            input.setChangedListener {
                onChange(it)
                Settings.save()
            }
            return input
        }

        fun group(name: String, options: List<SettingsOptionRow>) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options))
        }

        group("Powder Chest ESP", listOf(
            option("Main Toggle", toggle(Settings.general.ChestHighlightEnabled) { Settings.general.ChestHighlightEnabled = it }),
            option("Tracer", toggle(Settings.general.chestHighlightTracersEnabled) { Settings.general.chestHighlightTracersEnabled = it }),
            option("HUD", toggle(Settings.general.powderChestHudEnabled) { Settings.general.powderChestHudEnabled = it })
        ))

        group("Zero Tick Hardstone", listOf(
            option("Main Toggle", toggle(Settings.general.zeroTickHardstoneEnabled) { Settings.general.zeroTickHardstoneEnabled = it })
        ))

        group("Pingless Mining", listOf(
            option("Main Toggle", toggle(Settings.general.pinglessMiningEnabled) {
                Settings.general.pinglessMiningEnabled = it
                if (it) showPinglessMiningReminder()
            }),
            option("Mining Tick Override", PgsSliderWidget(0, 0, 100, 20, "Ticks", 0.0, 2.0, Settings.general.miningTickOverride.toDouble()) {
                Settings.general.miningTickOverride = it.toInt()
                Settings.save()
            }),
            option("Extra Ore Speed", numberInput("Extra Ore Speed", Settings.general.extraOreSpeed) { Settings.general.extraOreSpeed = it }, "Enter Extra speed that doesnt show up in tab, EX -> 250 from goblin"),
            option("Extra Block Speed", numberInput("Extra Block Speed", Settings.general.extraBlockSpeed) { Settings.general.extraBlockSpeed = it }, "Enter Extra speed that doesnt show up in tab, EX -> ... is there any?"),
            option("Extra Gemstone Speed", numberInput("Extra Gemstone Speed", Settings.general.extraGemstoneSpeed) { Settings.general.extraGemstoneSpeed = it }, "Enter Extra speed that doesnt show up in tab, EX -> 750/755 from professional, 100 from Lapidary, 800 from gemstone drills"),
            option("Extra Dwarven Metal Speed", numberInput("Extra Dwarven Metal Speed", Settings.general.extraDwarvenMetalSpeed) { Settings.general.extraDwarvenMetalSpeed = it }, "Enter Extra speed that doesnt show up in tab, EX -> 500/505 From StrongArm")
        ))

        val littlefootColor = PgsColorPickerWidget(0, 0, 100, 18, Settings.general.littlefootEspColor) { color ->
            Settings.general.littlefootEspColor = color
            Settings.save()
        }
        group("Littlefoot ESP", listOf(
            option("Main Toggle", toggle(Settings.general.littlefootEspEnabled) { Settings.general.littlefootEspEnabled = it }),
            option("Tracer", toggle(Settings.general.littlefootEspTracersEnabled) { Settings.general.littlefootEspTracersEnabled = it }),
            option("Color", littlefootColor)
        ))

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

    private fun showPinglessMiningReminder() {
        val player = client?.player ?: return
        player.sendSystemMessage(Text.literal(reminderSeparator))
        player.sendSystemMessage(Text.literal("§e§lMake sure mining speed is visible on tab"))
        player.sendSystemMessage(Text.literal(reminderSeparator))
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Mining Settings", width, startX, startY, panelWidth, panelHeight)
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







