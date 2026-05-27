package com.pgs.pgsaddons

import com.pgs.pgsaddons.features.SlotSwap
import com.pgs.pgsaddons.features.Timer
import com.pgs.pgsaddons.features.CustomEsp
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

class SettingsScreen : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "General"
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
        SettingsTabs.create(SettingsTab.GENERAL, this, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

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

        fun group(name: String, options: List<SettingsOptionRow>, description: String? = null) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options, description))
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

        val menuColor = PgsColorPickerWidget(0, 0, 100, 18, Settings.general.menuColor) { color ->
            Settings.general.menuColor = color
            Settings.save()
        }
        group("General", listOf(
            option("Show Own Nametag", toggle(Settings.general.showOwnNametag) { Settings.general.showOwnNametag = it }),
            option("Deployables Tracker", toggle(Settings.general.deployablesTrackerEnabled) { Settings.general.deployablesTrackerEnabled = it }),
            option("Keep this on <3", toggle(Settings.general.minireenasOverlayEnabled) { Settings.general.minireenasOverlayEnabled = it }),
            option("Menu Color", menuColor),
            keybindOption("Attack / Destroy", client!!.options.attackKey, "Changes Minecraft's Controls > Attack/Destroy binding.")
        ))

        val timerInput = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("Set timer"))
        timerInput.setPlaceholder(Text.literal("ex 2m 30s"))
        timerInput.text = Settings.general.timerDuration
        timerInput.setTextPredicate { value ->
            value.all { it.isDigit() || it.isWhitespace() || it.lowercaseChar() in listOf('h', 'm', 's') }
        }
        timerInput.setChangedListener {
            Settings.general.timerDuration = it
            Settings.save()
        }
        val timerCommandInput = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("Timer command"))
        timerCommandInput.setPlaceholder(Text.literal("ex warp garden"))
        timerCommandInput.text = Settings.general.timerCommand
        timerCommandInput.setChangedListener {
            Settings.general.timerCommand = it
            Settings.save()
        }
        group("Timer", listOf(
            option("Set timer", timerInput, "How long the timer runs before ending. Supports formats like 2m 30s, 90s, or 1h."),
            option("Command", timerCommandInput, "Optional command to execute when the timer reaches zero. You can type it with or without a leading slash."),
            keybindOption("Start / Stop", Timer.startStopKey)
        ))

        val customEspNamesInput = TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("Names"))
        customEspNamesInput.setPlaceholder(Text.literal("[ mob_one, mob_two ]"))
        customEspNamesInput.text = Settings.general.customEspNames
        customEspNamesInput.setChangedListener {
            Settings.general.customEspNames = it
            Settings.save()
        }
        val customEspColor = PgsColorPickerWidget(0, 0, 100, 18, Settings.general.customEspColor) { color ->
            Settings.general.customEspColor = color
            Settings.save()
        }
        group("Custom Esp", listOf(
            option("Main Toggle", toggle(Settings.general.customEspEnabled) { Settings.general.customEspEnabled = it }),
            option("Tracer", toggle(Settings.general.customEspTracersEnabled) { Settings.general.customEspTracersEnabled = it }),
            option("Color", customEspColor),
            option("Names", customEspNamesInput, "Enter name(s) of the entitiy")
        ))

        val clearSwapsButton = PgsButtonWidget(0, 0, 100, 20, Text.literal("Clear")) {
            Settings.general.savedSwapSlots.clear()
            Settings.save()
            client?.player?.sendMessage(Text.literal("§a[SlotSwap] Cleared all recorded slots!"), false)
        }
        group("Slot Swap", listOf(
            option("Main Toggle", toggle(Settings.general.slotSwapEnabled) { Settings.general.slotSwapEnabled = it }, "Enables recorded equipment slot swapping through the swap key or Auto Farm action."),
            option("Show Swap HUD", toggle(Settings.general.slotSwapHudEnabled) { Settings.general.slotSwapHudEnabled = it }, "Shows the currently recorded slot swap items on the HUD."),
            option("Equipment Stats HUD", toggle(Settings.general.equipmentStatsHudEnabled) { Settings.general.equipmentStatsHudEnabled = it }, "Shows cached equipment stats and helps slot swap avoid incompatible equipment when needed."),
            option("Clear Recorded Swaps", clearSwapsButton, "Deletes all recorded swap slots so you can record a fresh set."),
            keybindOption("Execute Slot Swap", SlotSwap.executeSwapKey),
            keybindOption("Toggle Record Slots", SlotSwap.recordSwapKey)
        ), "Pick slots you like to swap with your current equipment/armor(s). Ex. Farming eq, sorrow swap, Dungeon Masks")

        group("Auto Harp", listOf(
            option("Main Toggle", toggle(Settings.general.autoHarpEnabled) { Settings.general.autoHarpEnabled = it }),
            option("Harp Cooldown", PgsSliderWidget(0, 0, 100, 20, "Cooldown", 0.0, 5.0, Settings.general.autoHarpCooldown.toDouble()) {
                Settings.general.autoHarpCooldown = it.toInt()
                Settings.save()
            })
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
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "PGS Addons Settings", width, startX, startY, panelWidth, panelHeight)
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
}
