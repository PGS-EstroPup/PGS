package com.pgs.pgsaddons.features

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.screen.slot.SlotActionType

object AutoHarp {

    private val songNames = listOf(
        "Hymn to the Joy", "Frère Jacques", "Amazing Grace", "Brahm's Lullaby",
        "Happy Birthday to You", "Greensleeves", "Geothermy?", "Minuet",
        "Joy to the World", "Godly Imagination", "La Vie en Rose"
    )

    private val detectedTicks = mutableMapOf<Int, Float>()
    private val clickedSlots = mutableSetOf<Int>()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!com.pgs.pgsaddons.Settings.general.autoHarpEnabled) {
                if (detectedTicks.isNotEmpty()) detectedTicks.clear()
                if (clickedSlots.isNotEmpty()) clickedSlots.clear()
                return@register
            }
            val screen = client.currentScreen
            if (screen is HandledScreen<*>) {
                val title = screen.title.string
                if (title.startsWith("Harp -", ignoreCase = true) || songNames.any { title.contains(it, ignoreCase = true) }) {
                    val handler = screen.screenHandler
                    val player = client.player ?: return@register

                    for (slotId in 37..43) {
                        if (slotId >= handler.slots.size) continue

                        val slot = handler.getSlot(slotId)
                        val stack = slot.stack
                        val translationKey = stack.item.translationKey

                        if (translationKey.contains("quartz", ignoreCase = true)) {
                            val elapsedTicks = detectedTicks[slotId] ?: 0f
                            if (!clickedSlots.contains(slotId)) {
                                if (elapsedTicks >= com.pgs.pgsaddons.Settings.general.autoHarpCooldown) {
                                    client.interactionManager?.clickSlot(
                                        handler.syncId,
                                        slotId,
                                        0,
                                        SlotActionType.THROW,
                                        player
                                    )
                                    clickedSlots.add(slotId)
                                } else {
                                    detectedTicks[slotId] = elapsedTicks + TpsSync.getServerTicksPerClientTick()
                                }
                            }
                        } else {
                            detectedTicks.remove(slotId)
                            clickedSlots.remove(slotId)
                        }
                    }
                }
            } else {
                if (detectedTicks.isNotEmpty()) detectedTicks.clear()
                if (clickedSlots.isNotEmpty()) clickedSlots.clear()
            }
        }
    }
}
