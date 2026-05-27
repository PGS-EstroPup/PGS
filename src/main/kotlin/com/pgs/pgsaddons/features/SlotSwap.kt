package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.mixin.HandledScreenAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.client.gui.DrawContext
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.util.Identifier
import net.minecraft.text.Text
import net.minecraft.item.ItemStack
import org.lwjgl.glfw.GLFW

object SlotSwap {
    private val mc = MinecraftClient.getInstance()
    lateinit var executeSwapKey: KeyBinding
    lateinit var recordSwapKey: KeyBinding

    private var isWaitingForEqMenu = false
    private var isEqMenuOpen = false
    private var swapCallback: (() -> Unit)? = null
    private var swapSlots = emptyList<Int>()
    private var swapSlotIndex = 0
    private var swapDelayTicks = 0f
    private var isSwapRunning = false
    private var isWaitingForClose = false
    private var skipSwapIfSqueakyEquipment = false

    private const val INITIAL_SWAP_DELAY_TICKS = 2
    private const val BETWEEN_SWAP_DELAY_TICKS = 6

    fun triggerSwap(onComplete: (() -> Unit)? = null, skipIfSqueakyEquipment: Boolean = false) {
        if (!Settings.general.slotSwapEnabled) {
            onComplete?.invoke()
        } else if (Settings.general.savedSwapSlots.isEmpty()) {
            mc.player?.sendMessage(
                Text.literal("§c[SlotSwap] No slots saved. Record slots in /pgs first."),
                false
            )
            onComplete?.invoke()
        } else if (skipIfSqueakyEquipment && EquipmentStatsHud.hasCachedSqueakyEquipment()) {
            onComplete?.invoke()
        } else {
            resetSwapState(clearCallback = false)
            mc.networkHandler?.sendChatCommand("eq")
            isWaitingForEqMenu = true
            swapCallback = onComplete
            skipSwapIfSqueakyEquipment = skipIfSqueakyEquipment
        }
    }

    @JvmStatic
    fun init() {
        HudElementRegistry.addLast(
            Identifier.of("pgs_addons", "slot_swap_hud")
        ) { context, _ ->
            onRenderHud(context)
        }

        executeSwapKey =
                KeyBindingHelper.registerKeyBinding(
                        KeyBinding(
                                "PGS Execute Slot Swap (/eq)",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_UNKNOWN,
                                KeyBinding.Category.MISC
                        )
                )
        recordSwapKey =
                KeyBindingHelper.registerKeyBinding(
                        KeyBinding(
                                "PGS Toggle Record Slots",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_UNKNOWN,
                                KeyBinding.Category.MISC
                        )
                )

        ClientTickEvents.END_CLIENT_TICK.register(
                ClientTickEvents.EndTick { client ->
                    if (!Settings.general.slotSwapEnabled) {
                        isWaitingForEqMenu = false
                        isEqMenuOpen = false
                        resetSwapState()
                        return@EndTick
                    }

                    if (recordSwapKey.wasPressed()) {
                        Settings.general.slotSwapRecordMode = !Settings.general.slotSwapRecordMode
                        Settings.save()
                        val status = if (Settings.general.slotSwapRecordMode) "§aON" else "§cOFF"
                        client.player?.sendMessage(
                                Text.literal("§b[SlotSwap] §7Record Mode $status"),
                                false
                        )
                    }

                    if (executeSwapKey.wasPressed()) {
                        triggerSwap()
                    }

                    if (isEqMenuOpen) {
                        isEqMenuOpen = false
                        val screen = client.currentScreen
                        if (screen is HandledScreen<*>) {
                            startTickSwap()
                        }
                    }

                    handleTickSwap(client)
                    handleCloseWait(client)
                }
        )

        ScreenEvents.AFTER_INIT.register(
                ScreenEvents.AfterInit { client, screen, _, _ ->
                    if (screen is HandledScreen<*>) {
                        if (Settings.general.slotSwapEnabled && isWaitingForEqMenu) {
                            isWaitingForEqMenu = false
                            isEqMenuOpen = true
                        }

                        ScreenMouseEvents.allowMouseClick(screen)
                                .register(
                                        ScreenMouseEvents.AllowMouseClick { currentScreen, click ->
                                            if (!Settings.general.slotSwapEnabled)
                                                    return@AllowMouseClick true

                                            if (Settings.general.slotSwapRecordMode &&
                                                            currentScreen is HandledScreen<*>
                                            ) {
                                                val button = click.button()
                                                if (button == 0 || button == 1) {
                                                    val accessor =
                                                            currentScreen as HandledScreenAccessor
                                                    val slot =
                                                            accessor.invokeGetSlotAt(
                                                                    click.x(),
                                                                    click.y()
                                                            )

                                                    if (slot != null && client.player != null) {
                                                        val slotId = slot.id
                                                        if (!Settings.general.savedSwapSlots
                                                                        .contains(slotId)
                                                        ) {
                                                            Settings.general.savedSwapSlots.add(
                                                                    slotId
                                                            )
                                                            Settings.save()
                                                            client.player?.sendMessage(
                                                                    Text.literal(
                                                                            "§a[SlotSwap] Saved slot $slotId"
                                                                    ),
                                                                    false
                                                            )
                                                        } else {
                                                            client.player?.sendMessage(
                                                                    Text.literal(
                                                                            "§c[SlotSwap] Slot $slotId is already saved!"
                                                                    ),
                                                                    false
                                                            )
                                                        }
                                                    }
                                                    return@AllowMouseClick false
                                                }
                                            }
                                            true
                                        }
                                )
                    }
                }
        )
    }

    private fun startTickSwap() {
        val screen = mc.currentScreen as? HandledScreen<*>
        val hasSqueakyEquipment =
                if (screen != null) EquipmentStatsHud.hasSqueakyEquipment(screen)
                else EquipmentStatsHud.hasCachedSqueakyEquipment()

        if (skipSwapIfSqueakyEquipment && hasSqueakyEquipment) {
            finishTickSwap(mc)
            return
        }

        swapSlots = Settings.general.savedSwapSlots.toList()
        swapSlotIndex = 0
        swapDelayTicks = INITIAL_SWAP_DELAY_TICKS.toFloat()
        isSwapRunning = true
    }

    private fun handleTickSwap(client: MinecraftClient) {
        if (!isSwapRunning) return

        if (swapDelayTicks > 0) {
            swapDelayTicks -= TpsSync.getServerTicksPerClientTick()
            return
        }

        val screen = client.currentScreen
        if (screen !is HandledScreen<*>) {
            finishTickSwap(client)
            return
        }

        if (swapSlotIndex >= swapSlots.size) {
            finishTickSwap(client)
            return
        }

        guiClick(screen.screenHandler.syncId, swapSlots[swapSlotIndex] + 45)
        swapSlotIndex++
        swapDelayTicks = BETWEEN_SWAP_DELAY_TICKS.toFloat()
    }

    private fun finishTickSwap(client: MinecraftClient) {
        client.player?.closeHandledScreen()
        isSwapRunning = false
        isWaitingForClose = true
    }

    private fun resetSwapState(clearCallback: Boolean = true) {
        isSwapRunning = false
        isWaitingForClose = false
        swapSlots = emptyList()
        swapSlotIndex = 0
        swapDelayTicks = 0f
        skipSwapIfSqueakyEquipment = false
        if (clearCallback) swapCallback = null
    }

    private fun handleCloseWait(client: MinecraftClient) {
        if (!isWaitingForClose || client.currentScreen is HandledScreen<*>) return

        val callback = swapCallback
        resetSwapState()
        callback?.invoke()
    }

    private fun guiClick(
            id: Int,
            index: Int,
            button: Int = 0,
            clickType: SlotActionType = SlotActionType.THROW
    ) {
        val player = mc.player ?: return
        mc.interactionManager?.clickSlot(id, index, button, clickType, player)
    }

    private fun onRenderHud(context: DrawContext) {
        if (!Settings.general.slotSwapEnabled || !Settings.general.slotSwapHudEnabled) return
        drawHud(context, false)
    }

    fun drawHud(context: DrawContext, mockup: Boolean) {
        val x = Settings.general.slotSwapHudX
        val y = Settings.general.slotSwapHudY
        val color = 0xFF55FFFF.toInt() // Aqua
        
        context.drawText(mc.textRenderer, "§b[Slot Swap HUD]", x, y, color, true)

        if (mockup) {
            // Draw a few placeholder boxes to represent item slots
            for (i in 0 until 3) {
                val dx = x + (i * 20)
                val dy = y + 12
                context.fill(dx, dy, dx + 16, dy + 16, 0x44FFFFFF.toInt())
                // Use fill for border since drawBorder is unresolved
                context.fill(dx, dy, dx + 16, dy + 1, 0xFFAAAAAA.toInt())
                context.fill(dx, dy + 15, dx + 16, dy + 16, 0xFFAAAAAA.toInt())
                context.fill(dx, dy + 1, dx + 1, dy + 15, 0xFFAAAAAA.toInt())
                context.fill(dx + 15, dy + 1, dx + 16, dy + 15, 0xFFAAAAAA.toInt())
            }
            return
        }

        if (Settings.general.savedSwapSlots.isEmpty()) {
            context.drawText(mc.textRenderer, "§cNo Slots Recorded", x, y + 12, 0xFFFFFFFF.toInt(), true)
            return
        }

        var currentX = x
        Settings.general.savedSwapSlots.forEach { slotId ->
            val player = mc.player
            if (player != null) {
                val handler = player.playerScreenHandler
                if (slotId >= 0 && slotId < handler.slots.size) {
                    val slot = handler.getSlot(slotId)
                    val stack = slot.stack
                    if (!stack.isEmpty) {
                        context.drawItem(stack, currentX, y + 12)
                    } else {
                        // Show empty slot placeholder with slot ID
                        context.drawText(mc.textRenderer, "($slotId)", currentX, y + 16, 0xFF888888.toInt(), true)
                    }
                } else {
                    // Slot ID out of bounds for player screen handler
                    context.drawText(mc.textRenderer, "!", currentX + 6, y + 16, 0xFFFF5555.toInt(), true)
                }
            }
            currentX += 22 // Spacing for items
        }
    }
}
