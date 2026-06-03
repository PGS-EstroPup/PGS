package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import java.awt.Desktop
import java.net.URI

class CombatSettingsScreen(private val parent: Screen) : Screen(Text.empty()) {
    private var scrollY = 0.0
    private var maxScroll = 0.0
    private var featureScrollX = 0.0
    private var maxFeatureScrollX = 0.0
    private var activeGroup = "Combat"
    private val groups = mutableListOf<SettingsFunctionGroup>()

    private val panelWidth get() = ThreePaneSettingsLayout.panelWidth(width)
    private val panelHeight get() = ThreePaneSettingsLayout.panelHeight(height)
    private val startX get() = (width - panelWidth) / 2
    private val startY get() = ((height - panelHeight) / 2).coerceAtLeast(10)

    companion object {
        private const val AUTO_DUNGEONS_EDIT_URL = "https://www.youtube.com/watch?v=mDUOibsW36w"
    }

    override fun init() {
        groups.clear()
        SettingsTabs.create(SettingsTab.COMBAT, parent, startX, startY, panelWidth) { client?.setScreen(it) }.forEach { addDrawableChild(it) }

        fun toggle(initial: Boolean, onToggle: (Boolean) -> Unit): PgsButtonWidget {
            var state = initial
            return PgsButtonWidget(0, 0, 100, 20, Text.literal(onOff(state))) { button ->
                state = !state
                onToggle(state)
                Settings.save()
                button.setMessage(Text.literal(onOff(state)))
            }
        }

        fun option(name: String, widget: ClickableWidget): SettingsOptionRow {
            addDrawableChild(widget)
            return SettingsOptionRow(name, widget)
        }

        fun group(name: String, options: List<SettingsOptionRow>) {
            val button = PgsButtonWidget(0, 0, 118, 34, Text.literal(name)) { activeGroup = name; scrollY = 0.0 }
            addDrawableChild(button)
            groups.add(SettingsFunctionGroup(name, button, options))
        }

        val starColor = PgsColorPickerWidget(0, 0, 100, 18, Settings.general.starredMobEspColor) { color ->
            Settings.general.starredMobEspColor = color
            Settings.save()
        }
        val autoDungeonsEditButton = PgsButtonWidget(0, 0, 100, 20, Text.literal("Edit")) {
            openAutoDungeonsLink()
        }

        group("Combat", listOf(
            option("No Term Swing", toggle(Settings.general.noTerminatorSwingEnabled) { Settings.general.noTerminatorSwingEnabled = it }),
            option("I Hate Diorite", toggle(Settings.general.iHateDioriteEnabled) { Settings.general.iHateDioriteEnabled = it }),
            option("Arrow Tracker HUD", toggle(Settings.general.arrowTypeTrackerEnabled) { Settings.general.arrowTypeTrackerEnabled = it }),
            option("TP Maze Tracer", toggle(Settings.general.tpMazeTracerEnabled) { Settings.general.tpMazeTracerEnabled = it })
        ))
        group("Auto Dungeons", listOf(
            option("Auto Dungeons", autoDungeonsEditButton)
        ))
        group("Starred Mobs ESP", listOf(
            option("Main Toggle", toggle(Settings.general.starredMobEspEnabled) { Settings.general.starredMobEspEnabled = it }),
            option("Tracer", toggle(Settings.general.starredMobEspTracersEnabled) { Settings.general.starredMobEspTracersEnabled = it }),
            option("Color", starColor)
        ))
        group("Door Highlight", listOf(
            option("Wither Door Toggle", toggle(Settings.general.witherDoorEspEnabled) { Settings.general.witherDoorEspEnabled = it }),
            option("Wither Door Tracer", toggle(Settings.general.witherDoorEspTracersEnabled) { Settings.general.witherDoorEspTracersEnabled = it }),
            option("Blood Door Toggle", toggle(Settings.general.bloodDoorEspEnabled) { Settings.general.bloodDoorEspEnabled = it }),
            option("Blood Door Tracer", toggle(Settings.general.bloodDoorEspTracersEnabled) { Settings.general.bloodDoorEspTracersEnabled = it })
        ))
        group("Key Highlight", listOf(
            option("Main Toggle", toggle(Settings.general.keyHighlightEnabled) { Settings.general.keyHighlightEnabled = it }),
            option("Tracer", toggle(Settings.general.keyHighlightTracersEnabled) { Settings.general.keyHighlightTracersEnabled = it })
        ))

        addDrawableChild(PgsButtonWidget(width / 2 - 185, startY + panelHeight - 24, 175, 18, Text.literal("Â§bMove HUD")) {
            client?.setScreen(HudEditorScreen(this))
        })
        addDrawableChild(PgsButtonWidget(width / 2 + 10, startY + panelHeight - 24, 175, 18, Text.literal("Done")) { client?.setScreen(null) })
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x88000000.toInt())
    }

    private fun onOff(value: Boolean): String = if (value) "Â§aON" else "Â§cOFF"

    private fun openAutoDungeonsLink() {
        val rawUrl = AUTO_DUNGEONS_EDIT_URL.trim()
        if (rawUrl.isEmpty()) {
            client?.player?.sendSystemMessage(Text.literal("\u00A7c[PGS] Auto Dungeons link is not set yet."))
            return
        }

        val url = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        try {
            Desktop.getDesktop().browse(URI.create(url))
        } catch (_: Exception) {
            client?.player?.sendSystemMessage(Text.literal("\u00A7c[PGS] Could not open Auto Dungeons link."))
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        ThreePaneSettingsLayout.hide(groups)
        ThreePaneSettingsLayout.drawChrome(context, textRenderer, "Combat Settings", width, startX, startY, panelWidth, panelHeight)
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
