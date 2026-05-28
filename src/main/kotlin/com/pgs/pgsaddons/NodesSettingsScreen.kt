package com.pgs.pgsaddons

import com.pgs.pgsaddons.features.DrawNodes
import com.pgs.pgsaddons.features.NodeManager
import com.pgs.pgsaddons.features.AutoFarm2
import com.pgs.pgsaddons.utils.PgsButtonWidget
import com.pgs.pgsaddons.utils.PgsSliderWidget
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class NodesSettingsScreen(private val parent: Screen) : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Node Placement"
    private var listeningRow: KeybindRow? = null
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private data class KeybindRow(
        val binding: KeyBinding,
        val button: PgsButtonWidget
    )

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    override fun init() {
        groups.clear()
        listeningRow = null
        SettingsTabs.create(SettingsTab.NODES, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun option(name: String, widget: ClickableWidget, description: String? = null): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget, description)
        }

        fun group(name: String, options: List<SettingsOptionRow>) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) {
                activeGroup = name
                scrollY = 0.0
            }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options))
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

        fun nodeRenderModeButton(): PgsButtonWidget {
            fun label(mode: Int): String = when (mode.coerceIn(0, 2)) {
                1 -> "Render Close"
                2 -> "Never Render"
                else -> "Always Render"
            }

            return PgsButtonWidget(0, 0, 100, 20, Text.literal(label(Settings.general.nodeRenderMode))) { button ->
                Settings.general.nodeRenderMode = (Settings.general.nodeRenderMode + 1) % 3
                Settings.save()
                button.setMessage(Text.literal(label(Settings.general.nodeRenderMode)))
            }
        }

        fun profileButton(current: () -> String, onSelect: (String) -> Unit): PgsButtonWidget {
            return PgsButtonWidget(0, 0, 100, 20, Text.literal(current())) { button ->
                val next = nextProfile(current())
                onSelect(next)
                button.setMessage(Text.literal(next))
                client?.setScreen(NodesSettingsScreen(parent))
            }
        }

        fun keybindOption(name: String, binding: KeyBinding, description: String? = null): SettingsOptionRow {
            lateinit var row: KeybindRow
            val button = PgsButtonWidget(0, 0, 120, 20, binding.getBoundKeyLocalizedText()) {
                listeningRow = row
                row.button.setMessage(Text.literal("> Press key <"))
                row.button.isActive = true
            }
            row = KeybindRow(binding, button)
            return option(name, button, description)
        }

        group("Node Placement", listOf(
            option("Node Profile", profileButton({ Settings.general.nodeActiveProfile }) { NodeManager.switchProfile(it) }, "Switches the placed-node preset."),
            keybindOption("Toggle Node Placement", DrawNodes.toggleKey, "Right click to place, Right click again to configure"),
            option("Node Render", nodeRenderModeButton(), "Controls whether placed nodes render always, only within 30 blocks, or never.")
        ))

        val pestEspColor = PgsColorPickerWidget(0, 0, 100, 18, Settings.general.pestEspColor) { color ->
            Settings.general.pestEspColor = color
            Settings.save()
        }
        group("Garden Pest ESP", listOf(
            option("Main Toggle", toggle(Settings.general.pestEspEnabled) { Settings.general.pestEspEnabled = it }),
            option("Tracer", toggle(Settings.general.pestEspTracersEnabled) { Settings.general.pestEspTracersEnabled = it }),
            option("Color", pestEspColor)
        ))

        val plotInput = textInput("Plot", Settings.general.autoFarm2PlotName) {
            Settings.general.autoFarm2PlotName = it
            Settings.save()
        }
        val armor1Slider = armorSlotSlider(Settings.general.autoFarm2ArmorSlot1) { Settings.general.autoFarm2ArmorSlot1 = it.toString() }
        val armor2Slider = armorSlotSlider(Settings.general.autoFarm2ArmorSlot2) { Settings.general.autoFarm2ArmorSlot2 = it.toString() }
        val armor3Slider = armorSlotSlider(Settings.general.autoFarm2ArmorSlot3) { Settings.general.autoFarm2ArmorSlot3 = it.toString() }
        val pestOffsetInput = textInput("Pest offset", Settings.general.autoFarm2PestSpawnOffsetSeconds.takeIf { it > 0 }?.let { formatDuration(it) } ?: "") {
            parseDuration(it)?.let { seconds ->
                Settings.general.autoFarm2PestSpawnOffsetSeconds = seconds
                Settings.save()
            }
        }
        pestOffsetInput.setPlaceholder(Text.literal("2m 45s"))
        group("Auto Farm 2.0", listOf(
            option("AutoFarm Profile", profileButton({ Settings.general.autoFarm2ActiveProfile }) { Settings.switchAutoFarm2Profile(it) }, "Switches Auto Farm slots, plot, wardrobe slots, pest offset, and action order."),
            option("Main Toggle", toggle(Settings.general.autoFarm2Enabled) {
                if (it != Settings.general.autoFarm2Enabled) AutoFarm2.toggle()
            }, "Starts or pauses Auto Farm 2.0. Restarting continues from the last unfinished action unless Stop Action reset it."),
            option("Edit Order", PgsButtonWidget(0, 0, 100, 20, Text.literal("Edit Order")) {
                client?.setScreen(AutoFarmOrderScreen(this))
            }, "Opens the action order editor for C1, C2, and C3."),
            keybindOption("Start / Stop", AutoFarm2.toggleKey),
            option("Pest Tracker HUD", toggle(Settings.general.pestTimersEnabled) { Settings.general.pestTimersEnabled = it }, "Shows garden pest timer lines and drives the C2 start timing."),
            option("Hoe Slot", slotSlider(Settings.general.autoFarm2HoeSlot) { Settings.general.autoFarm2HoeSlot = it }, "Hotbar slot selected by the Hold Hoe action."),
            option("Mousemat Slot", slotSlider(Settings.general.autoFarm2MousematSlot) { Settings.general.autoFarm2MousematSlot = it }, "Hotbar slot used by the Use Mousemat action."),
            option("Rod Slot", slotSlider(Settings.general.autoFarm2RodSlot) { Settings.general.autoFarm2RodSlot = it }, "Hotbar slot used by the Use Rod action."),
            option("Vacuum Slot", slotSlider(Settings.general.autoFarm2VacuumSlot) { Settings.general.autoFarm2VacuumSlot = it }, "Hotbar slot used by Tracking Vacuum and Vacuum Interact actions."),
            option("Pest Offset", pestOffsetInput, "Subtracts this time from the pest cooldown. C2 starts when the adjusted countdown reaches zero."),
            option("Plot Name", plotInput, "Plot name used by the TP To Plot action."),
            option("Armor Slot 1", armor1Slider, "Wardrobe slot used by the Armor Slot 1 action."),
            option("Armor Slot 2", armor2Slider, "Wardrobe slot used by the Armor Slot 2 action."),
            option("Armor Slot 3", armor3Slider, "Wardrobe slot used by the Armor Slot 3 action.")
        ))

        addDrawableChild(PgsButtonWidget(width / 2 + 10, startY + panelHeight - 24, 175, 18, Text.literal("Done")) {
            client?.setScreen(null)
        })
    }

    private fun textInput(label: String, value: String, onChange: (String) -> Unit): TextFieldWidget {
        val input = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal(label))
        input.text = value
        input.setChangedListener(onChange)
        return input
    }

    private fun slotSlider(value: Int, onChange: (Int) -> Unit): PgsSliderWidget {
        return PgsSliderWidget(0, 0, 100, 20, "Slot", 1.0, 9.0, value.toDouble()) {
            onChange(it.toInt())
            Settings.save()
        }
    }

    private fun armorSlotSlider(value: String, onChange: (Int) -> Unit): PgsSliderWidget {
        val slot = value.toIntOrNull()?.coerceIn(1, 9) ?: 1
        return PgsSliderWidget(0, 0, 100, 20, "Slot", 1.0, 9.0, slot.toDouble()) {
            onChange(it.toInt().coerceIn(1, 9))
            Settings.save()
        }
    }

    private fun onOff(value: Boolean): String = if (value) "§aON" else "§cOFF"

    private fun nextProfile(current: String): String {
        val index = PROFILE_NAMES.indexOf(current).takeIf { it >= 0 } ?: 0
        return PROFILE_NAMES[(index + 1) % PROFILE_NAMES.size]
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Farming", width, startX, startY, panelWidth, panelHeight)
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

    override fun keyPressed(input: KeyInput): Boolean {
        val row = listeningRow
        if (row != null) {
            val key = if (input.key() == GLFW.GLFW_KEY_ESCAPE) InputUtil.UNKNOWN_KEY else InputUtil.fromKeyCode(input)
            setBinding(row, key)
            return true
        }
        return super.keyPressed(input)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val row = listeningRow
        if (row != null) {
            setBinding(row, InputUtil.Type.MOUSE.createFromCode(click.button()))
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    private fun setBinding(row: KeybindRow, key: InputUtil.Key) {
        row.binding.setBoundKey(key)
        KeyBinding.updateKeysByCode()
        client?.options?.write()
        listeningRow = null
        row.button.isActive = false
        row.button.setMessage(row.binding.getBoundKeyLocalizedText())
    }

    override fun shouldPause(): Boolean = true

    private fun parseDuration(value: String): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0
        if (trimmed.all { it.isDigit() }) return trimmed.toIntOrNull()
        var seconds = 0
        Regex("(\\d+)\\s*h", RegexOption.IGNORE_CASE).find(trimmed)?.let { seconds += it.groupValues[1].toInt() * 3600 }
        Regex("(\\d+)\\s*m", RegexOption.IGNORE_CASE).find(trimmed)?.let { seconds += it.groupValues[1].toInt() * 60 }
        Regex("(\\d+)\\s*s", RegexOption.IGNORE_CASE).find(trimmed)?.let { seconds += it.groupValues[1].toInt() }
        return seconds.takeIf { it > 0 || trimmed == "0" }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return when {
            minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    companion object {
        private val PROFILE_NAMES = listOf("Default", "Profile 2", "Profile 3", "Profile 4", "Profile 5")
    }
}
