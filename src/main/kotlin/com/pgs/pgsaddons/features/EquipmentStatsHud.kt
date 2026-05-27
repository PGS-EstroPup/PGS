package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

object EquipmentStatsHud {
    private val mc = MinecraftClient.getInstance()
    private val equipmentSlots = listOf(10, 19, 28, 37)
    private val armorSlots = listOf(11, 20, 29, 38)
    private var cachedEquipmentStacks = List(equipmentSlots.size) { ItemStack.EMPTY }
    private var cachedArmorStacks = List(armorSlots.size) { ItemStack.EMPTY }
    private var hasSeenEquipmentStats = false
    private const val TARGET_TITLE = "Your Equipments and Stats"

    @JvmStatic
    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            (client.currentScreen as? HandledScreen<*>)?.let { screen ->
                if (isTargetEquipmentScreen(screen)) {
                    cacheFromScreen(screen)
                }
            }
        }

        HudElementRegistry.addLast(
            Identifier.of("pgs_addons", "equipment_stats_hud")
        ) { context, _ ->
            onRenderHud(context)
        }
    }

    private fun onRenderHud(context: DrawContext) {
        if (!Settings.general.equipmentStatsHudEnabled) return
        if (!hasSeenEquipmentStats) return

        drawHud(context, false)
    }

    fun drawHud(context: DrawContext, mockup: Boolean) {
        val x = Settings.general.equipmentStatsHudX
        val y = Settings.general.equipmentStatsHudY

        context.drawText(mc.textRenderer, "§b[Equipment Stats HUD]", x, y, 0xFF55FFFF.toInt(), true)
        drawGroup(context, "§eEquipments", equipmentSlots, cachedEquipmentStacks, x, y + 14, mockup)
        drawGroup(context, "§aArmor", armorSlots, cachedArmorStacks, x, y + 48, mockup)
    }

    private fun drawGroup(
        context: DrawContext,
        label: String,
        slots: List<Int>,
        cachedStacks: List<ItemStack>,
        x: Int,
        y: Int,
        mockup: Boolean
    ) {
        context.drawText(mc.textRenderer, label, x, y, 0xFFFFFFFF.toInt(), true)

        val stacks = if (mockup) {
            List(slots.size) { ItemStack.EMPTY }
        } else {
            cachedStacks
        }

        var currentX = x
        stacks.forEachIndexed { index, stack ->
            drawSlotFrame(context, currentX, y + 11)
            if (!stack.isEmpty) {
                context.drawItem(stack, currentX, y + 11)
            } else if (!mockup) {
                context.drawText(mc.textRenderer, "(${slots[index]})", currentX, y + 15, 0xFF888888.toInt(), true)
            }
            currentX += 22
        }
    }

    private fun drawSlotFrame(context: DrawContext, x: Int, y: Int) {
        context.fill(x, y, x + 16, y + 16, 0x44000000.toInt())
        context.fill(x, y, x + 16, y + 1, 0xFFAAAAAA.toInt())
        context.fill(x, y + 15, x + 16, y + 16, 0xFFAAAAAA.toInt())
        context.fill(x, y + 1, x + 1, y + 15, 0xFFAAAAAA.toInt())
        context.fill(x + 15, y + 1, x + 16, y + 15, 0xFFAAAAAA.toInt())
    }

    fun hasSqueakyEquipment(screen: HandledScreen<*>): Boolean {
        if (!isTargetEquipmentScreen(screen)) return false
        cacheFromScreen(screen)
        return hasCachedSqueakyEquipment()
    }

    fun hasCachedSqueakyEquipment(): Boolean {
        return cachedEquipmentStacks.any { stack ->
            !stack.isEmpty && itemNameContains(stack, "squeaky")
        }
    }

    private fun cacheFromScreen(screen: HandledScreen<*>) {
        cachedEquipmentStacks = copySlots(screen, equipmentSlots)
        cachedArmorStacks = copySlots(screen, armorSlots)
        hasSeenEquipmentStats = true
    }

    private fun copySlots(screen: HandledScreen<*>, slots: List<Int>): List<ItemStack> {
        return slots.map { slotId ->
            if (slotId !in 0 until screen.screenHandler.slots.size) {
                ItemStack.EMPTY
            } else {
                screen.screenHandler.getSlot(slotId).stack.copy()
            }
        }
    }

    private fun isTargetEquipmentScreen(screen: HandledScreen<*>): Boolean {
        val normalizedTitle = normalizeText(screen.title.string)
        return normalizedTitle.contains(normalizeText(TARGET_TITLE)) ||
            (normalizedTitle.contains("equipment") && normalizedTitle.contains("stats"))
    }

    private fun itemNameContains(stack: ItemStack, needle: String): Boolean {
        val normalizedName = normalizeText(stack.name.string)
        return normalizedName.contains(normalizeText(needle))
    }

    private fun normalizeText(text: String): String {
        return text
            .replace(Regex("§."), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
    }
}
