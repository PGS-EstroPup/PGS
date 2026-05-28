package com.pgs.pgsaddons

import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

enum class SettingsTab {
    GENERAL,
    FISHING,
    COMBAT,
    NODES,
    MINING,
    NOTEPAD,
    DEBUG
}

object SettingsTabs {
    private val tabs = listOf(
        SettingsTab.GENERAL to "\u00A7lGeneral",
        SettingsTab.FISHING to "\u00A7lFishing",
        SettingsTab.COMBAT to "\u00A7lCombat",
        SettingsTab.NODES to "\u00A7lFarming",
        SettingsTab.MINING to "\u00A7lMining",
        SettingsTab.NOTEPAD to "\u00A7lNotes",
        SettingsTab.DEBUG to "\u00A7lDebug"
    )

    fun create(
        active: SettingsTab,
        parent: Screen,
        startX: Int,
        startY: Int,
        panelWidth: Int,
        navigate: (Screen) -> Unit
    ): List<PgsButtonWidget> {
        val leftBoxX1 = ThreePaneSettingsLayout.navLeft(startX)
        val leftBoxX2 = ThreePaneSettingsLayout.navRight(startX)
        val availableWidth = leftBoxX2 - leftBoxX1

        val tabWidth = 66
        val calculatedX = leftBoxX1 + (availableWidth - tabWidth) / 2
        val tabGap = 6
        val startYOffset = ThreePaneSettingsLayout.topY(startY) + 10
        val tabHeight = 20

        return tabs.mapIndexed { index, (tab, label) ->
            PgsButtonWidget(
                calculatedX,
                startYOffset + (tabHeight + tabGap) * index,
                tabWidth,
                tabHeight,
                Text.literal(label),
                tab == active
            ) {
                if (tab != active) {
                    navigate(screenFor(tab, parent))
                }
            }
        }
    }

    private fun screenFor(tab: SettingsTab, parent: Screen): Screen {
        return when (tab) {
            SettingsTab.GENERAL -> SettingsScreen()
            SettingsTab.FISHING -> FishingSettingsScreen(parent)
            SettingsTab.COMBAT -> CombatSettingsScreen(parent)
            SettingsTab.NODES -> NodesSettingsScreen(parent)
            SettingsTab.MINING -> MiningSettingsScreen(parent)
            SettingsTab.NOTEPAD -> NotepadScreen(parent)
            SettingsTab.DEBUG -> DebugSettingsScreen(parent)
        }
    }
}
