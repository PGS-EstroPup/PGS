package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.ListEditorScreen
import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.util.concurrent.ThreadLocalRandom

object AutoSell {
    private val mc = MinecraftClient.getInstance()
    lateinit var executeKey: KeyBinding

    private var state = State.IDLE
    private var delayTicks = 0f
    private var targets = emptyList<String>()
    private var initialMatches = emptyList<String>()
    private var sellSlots = emptyList<Int>()
    private var sellIndex = 0
    private var retryCount = 0
    private var completeCallback: (() -> Unit)? = null

    private const val OPEN_MENU_DELAY_TICKS = 8
    private const val MIN_SELL_DELAY_TICKS = 2
    private const val MAX_SELL_DELAY_TICKS = 5
    private const val VERIFY_DELAY_TICKS = 8
    private const val MAX_RETRIES = 2

    private enum class State {
        IDLE,
        WAITING_FOR_TRADES,
        SELLING,
        VERIFYING
    }

    fun init() {
        executeKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Execute AutoSell",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (executeKey.consumeClick()) execute()
            tick(client)
        }
    }

    fun execute(onComplete: (() -> Unit)? = null) {
        val player = mc.player ?: return
        if (!Settings.general.autoSellEnabled) {
            player.sendSystemMessage(Text.literal("§c[AutoSell] Enable AutoSell first."))
            onComplete?.invoke()
            return
        }

        targets = ListEditorScreen.fromCommaList(Settings.general.autoSellNames).map { it.lowercase() }
        if (targets.isEmpty()) {
            player.sendSystemMessage(Text.literal("§c[AutoSell] Add item names to the AutoSell list first."))
            onComplete?.invoke()
            return
        }

        initialMatches = matchingInventoryStacks().map { displayName(it) }
        if (initialMatches.isEmpty()) {
            player.sendSystemMessage(Text.literal("§e[AutoSell] No matching items found."))
            reset()
            onComplete?.invoke()
            return
        }

        completeCallback = onComplete
        player.connection?.sendChatCommand("trades")
        state = State.WAITING_FOR_TRADES
        delayTicks = OPEN_MENU_DELAY_TICKS.toFloat()
        retryCount = 0
        player.sendSystemMessage(Text.literal("§b[AutoSell] §7Selling ${initialMatches.size} matching stack(s)."))
    }

    private fun tick(client: MinecraftClient) {
        if (state == State.IDLE) return

        if (delayTicks > 0) {
            delayTicks -= TpsSync.getServerTicksPerClientTick()
            return
        }

        when (state) {
            State.IDLE -> {}
            State.WAITING_FOR_TRADES -> startSelling(client)
            State.SELLING -> sellNext(client)
            State.VERIFYING -> verify(client)
        }
    }

    private fun startSelling(client: MinecraftClient) {
        val screen = client.screen as? HandledScreen<*>
        if (screen == null) {
            delayTicks = OPEN_MENU_DELAY_TICKS.toFloat()
            return
        }

        sellSlots = matchingTradeInventorySlots(screen)
        sellIndex = 0
        state = State.SELLING
        delayTicks = randomSellDelayTicks()
    }

    private fun sellNext(client: MinecraftClient) {
        val screen = client.screen as? HandledScreen<*>
        if (screen == null) {
            state = State.VERIFYING
            delayTicks = VERIFY_DELAY_TICKS.toFloat()
            return
        }

        if (sellIndex >= sellSlots.size) {
            state = State.VERIFYING
            delayTicks = VERIFY_DELAY_TICKS.toFloat()
            return
        }

        val slotId = sellSlots[sellIndex]
        if (slotId in screen.menu.slots.indices && matchesTarget(screen.menu.slots[slotId].item)) {
            guiDrop(screen.menu.syncId, slotId)
        }
        sellIndex++
        delayTicks = randomSellDelayTicks()
    }

    private fun verify(client: MinecraftClient) {
        val missed = matchingInventoryStacks()
        if (missed.isNotEmpty() && retryCount < MAX_RETRIES && client.screen is HandledScreen<*>) {
            retryCount++
            startSelling(client)
            return
        }

        client.player?.closeContainer()
        if (missed.isEmpty()) {
            client.player?.sendSystemMessage(Text.literal("§a[AutoSell] Sold all matching items."))
        } else {
            val names = missed.take(4).joinToString(", ") { displayName(it) }
            val extra = if (missed.size > 4) " +${missed.size - 4} more" else ""
            client.player?.sendSystemMessage(Text.literal("§c[AutoSell] Missed ${missed.size} stack(s): $names$extra"))
        }
        val callback = completeCallback
        reset()
        callback?.invoke()
    }

    private fun matchingInventoryStacks(): List<ItemStack> {
        val player = mc.player ?: return emptyList()
        return player.inventory.getNonEquipmentItems()
            .filter { !it.isEmpty && matchesTarget(it) }
    }

    private fun matchingTradeInventorySlots(screen: HandledScreen<*>): List<Int> {
        val player = mc.player ?: return emptyList()
        val inventory = player.inventory
        return screen.menu.slots
            .mapIndexedNotNull { index, slot ->
                if (slot.container === inventory && matchesTarget(slot.item)) index else null
            }
    }

    private fun matchesTarget(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val name = displayName(stack).lowercase()
        val itemId = stack.item.toString().lowercase()
        return targets.any { target -> name.contains(target) || itemId.contains(target) }
    }

    private fun displayName(stack: ItemStack): String {
        return stack.name.string
    }

    private fun guiDrop(syncId: Int, slotId: Int) {
        val player = mc.player ?: return
        mc.gameMode?.clickSlot(syncId, slotId, 0, SlotActionType.THROW, player)
    }

    private fun randomSellDelayTicks(): Float {
        return ThreadLocalRandom.current().nextInt(MIN_SELL_DELAY_TICKS, MAX_SELL_DELAY_TICKS + 1).toFloat()
    }

    private fun reset() {
        state = State.IDLE
        delayTicks = 0f
        targets = emptyList()
        initialMatches = emptyList()
        sellSlots = emptyList()
        sellIndex = 0
        retryCount = 0
        completeCallback = null
    }
}
