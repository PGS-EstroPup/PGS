package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.util.Identifier

object ArrowTypeTracker {
    private val mc = MinecraftClient.getInstance()
    private val inventorySlots = (0..35).toList()
    private var selectedArrowType: String? = null

    @JvmStatic
    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("pgs_addons", "arrow_type_tracker")
        ) { context, _ ->
            onRenderHud(context)
        }
    }

    private fun onClientTick(client: MinecraftClient) {
        if (!Settings.general.arrowTypeTrackerEnabled) {
            selectedArrowType = null
            return
        }

        val player = client.player ?: run {
            selectedArrowType = null
            return
        }

        selectedArrowType = null
        for (slot in inventorySlots) {
            val stack = player.inventory.getItem(slot)
            val arrowType = getSelectedArrowType(stack)
            if (arrowType != null) {
                selectedArrowType = arrowType
                return
            }
        }
    }

    private fun onRenderHud(context: DrawContext) {
        if (!Settings.general.arrowTypeTrackerEnabled) return
        if (selectedArrowType == null) return
        drawHud(context, false)
    }

    fun drawHud(context: DrawContext, mockup: Boolean) {
        val x = Settings.general.arrowTypeTrackerX
        val y = Settings.general.arrowTypeTrackerY
        val arrowType = if (mockup) "Armorshred Arrow" else selectedArrowType ?: return
        val text = "\u00A7e\u00A7l[ $arrowType ]"

        val width = hudWidth(text)
        val bodyY = HudPanel.draw(context, x, y, width, 38, "Arrow Type")
        HudPanel.drawCenteredLine(context, text, x, width, bodyY, 0xFF55FFFF.toInt())
    }

    fun mockupWidth(): Int = hudWidth("\u00A7e\u00A7l[ Armorshred Arrow ]")

    private fun hudWidth(text: String): Int {
        val titleWidth = mc.font.width("Arrow Type")
        val textWidth = mc.font.width(text)
        return (maxOf(titleWidth, textWidth) + HudPanel.PADDING * 2).coerceAtLeast(1)
    }

    private fun getSelectedArrowType(stack: ItemStack): String? {
        if (stack.isEmpty()) return null
        if (!itemNameContains(stack, "Arrow Swapper")) return null

        val tooltip = stack.getTooltipLines(
            net.minecraft.world.item.Item.TooltipContext.EMPTY,
            mc.player,
            TooltipType.NORMAL
        )

        for (line in tooltip) {
            val cleanLine = stripFormatting(line.string)
            if (cleanLine.contains("Selected:", ignoreCase = true)) {
                return cleanLine.substringAfter("Selected:").trim().takeIf { it.isNotEmpty() }
            }
        }

        return null
    }

    private fun itemNameContains(stack: ItemStack, needle: String): Boolean {
        return normalizeText(stack.name.string).contains(normalizeText(needle))
    }

    private fun stripFormatting(text: String): String {
        return text.replace(Regex("\u00A7."), "")
    }

    private fun normalizeText(text: String): String {
        return stripFormatting(text)
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
    }
}
