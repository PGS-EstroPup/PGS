package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object ChestHighlight {

    private const val TREASURE_CHEST_MESSAGE = "You uncovered a treasure chest!"
    private const val CHEST_SCAN_RADIUS = 8
    private const val CHEST_SCAN_TICKS = 40
    private const val WIRE_ALPHA = 1.0f
    private const val FILL_ALPHA = 0.2f

    private val highlightedChests = mutableSetOf<BlockPos>()
    private var pendingChestScanTicks = 0

    fun registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            onGameMessage(message.string)
        }

        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            try {
                renderChests(context)
            } catch (e: IllegalStateException) {
                highlightedChests.clear()
                System.err.println("[pgs_addons] Disabled stale chest highlights after render failure: $e")
            }
        }

        HudElementRegistry.addLast(
            Identifier.of("pgs_addons", "powder_chest_hud")
        ) { context, _ ->
            onRenderHud(context)
        }
    }

    private fun onClientTick(client: MinecraftClient) {
        if (!Settings.general.ChestHighlightEnabled || !LocationUtils.isInCrystalHollows()) {
            highlightedChests.clear()
            pendingChestScanTicks = 0
            return
        }

        val world = client.world ?: return

        if (pendingChestScanTicks > 0) {
            scanNearbyChests()
            pendingChestScanTicks--
        }

        highlightedChests.removeIf { pos -> world.isChunkLoaded(pos) && !isChestAt(pos) }
    }

    private fun onGameMessage(rawMessage: String) {
        if (!Settings.general.ChestHighlightEnabled || !LocationUtils.isInCrystalHollows()) return

        val message = rawMessage.replace(Regex("\\u00A7."), "").trim()
        if (!message.contains(TREASURE_CHEST_MESSAGE, ignoreCase = true)) return

        pendingChestScanTicks = CHEST_SCAN_TICKS
        scanNearbyChests()
    }

    private fun scanNearbyChests() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val world = client.world ?: return
        val center = player.blockPos

        for (x in center.x - CHEST_SCAN_RADIUS..center.x + CHEST_SCAN_RADIUS) {
            for (y in center.y - CHEST_SCAN_RADIUS..center.y + CHEST_SCAN_RADIUS) {
                for (z in center.z - CHEST_SCAN_RADIUS..center.z + CHEST_SCAN_RADIUS) {
                    val pos = BlockPos(x, y, z)
                    if (!world.isChunkLoaded(pos)) continue
                    if (isChestAt(pos)) highlightedChests.add(pos)
                }
            }
        }
    }

    private fun isChestAt(pos: BlockPos): Boolean {
        val world = MinecraftClient.getInstance().world ?: return false
        val block = world.getBlockState(pos).block
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
    }

    private fun renderChests(context: WorldRenderContext) {
        val client = MinecraftClient.getInstance()
        if (!Settings.general.ChestHighlightEnabled || !LocationUtils.isInCrystalHollows()) return

        val cameraObject = client.gameRenderer.camera
        val camera = cameraObject.cameraPos
        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return

        val buffer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)
        matrices.push()
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z)

            val fillBuffer = consumers.getBuffer(EspRenderLayers.FILLED_ESP)
            for (pos in highlightedChests) {
                EspRenderer.drawFilledBox(
                    matrices.peek(),
                    fillBuffer,
                    Box(pos),
                    0f, 1f, 0f, FILL_ALPHA
                )
            }

            for (pos in highlightedChests) {
                val renderBox = Box(pos)
                if (Settings.general.chestHighlightTracersEnabled) {
                    EspRenderer.drawTracer(
                        matrices.peek(),
                        buffer,
                        cameraObject,
                        renderBox.center,
                        0f, 1f, 0f, WIRE_ALPHA
                    )
                }
                EspRenderer.drawWireframeBox(
                    matrices.peek(),
                    buffer,
                    renderBox,
                    0f, 1f, 0f, WIRE_ALPHA
                )
            }
        } finally {
            matrices.pop()
        }
    }

    private fun onRenderHud(context: DrawContext) {
        if (!Settings.general.powderChestHudEnabled) return
        if (!Settings.general.ChestHighlightEnabled) return
        if (!LocationUtils.isInCrystalHollows()) return

        drawHud(context, false)
    }

    fun drawHud(context: DrawContext, mockup: Boolean) {
        val x = Settings.general.powderChestHudX
        val y = Settings.general.powderChestHudY
        val count = if (mockup) 3 else highlightedChests.size
        val color = if (count > 0) 0xFF55FF55.toInt() else 0xFFAAAAAA.toInt()

        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("&a&lPowder Chest HUD: $count"),
            x,
            y,
            0xFFFFFF,
            true
        )
    }

}
