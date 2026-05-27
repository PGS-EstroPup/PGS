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
    DEBUG
}

object SettingsTabs {
    private val tabs = listOf(
        SettingsTab.GENERAL to "§lGeneral",
        SettingsTab.FISHING to "§lFishing",
        SettingsTab.COMBAT to "§lCombat",
        SettingsTab.NODES to "§lFarming",
        SettingsTab.MINING to "§lMining",
        SettingsTab.DEBUG to "§lDebug"
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
            SettingsTab.DEBUG -> DebugSettingsScreen(parent)
        }
    }
}
