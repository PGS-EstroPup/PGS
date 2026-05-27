package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.utils.MiningUtils.getBlockTicks
import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.utils.LocationUtils
import com.pgs.pgsaddons.utils.MiningUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.Block
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos


object PinglessMiningMineshaft {

    private val playerSpeed: Int
        get() = MiningUtils.getMiningSpeed()

    private data class PendingMine(
        val originalBlock: Block,
        var remainingTicks: Float,
        var missedActiveTicks: Int = 0
    )

    private val pendingMines = mutableMapOf<BlockPos, PendingMine>()

    private val blockNames = setOf(
        Blocks.DARK_PRISMARINE,
        Blocks.PRISMARINE,
        Blocks.LIGHT_BLUE_WOOL,
        Blocks.GRAY_WOOL,
        Blocks.GOLD_BLOCK,
        Blocks.OBSIDIAN,
        Blocks.SMOOTH_RED_SANDSTONE,
        Blocks.TERRACOTTA,
        Blocks.BROWN_TERRACOTTA,
        Blocks.CLAY,
        Blocks.COBBLESTONE
    )

    fun registeredEvents() {

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->

            if (!Settings.general.pinglessMiningEnabled) {
                PinglessMiningDebug.log("Mineshaft", "pingless mining disabled", pos)
                return@register ActionResult.PASS
            }

            if (!world.isClient) {
                PinglessMiningDebug.log("Mineshaft", "not client world", pos)
                return@register ActionResult.PASS
            }

            if (!LocationUtils.isInMineshaft()) {
                PinglessMiningDebug.log("Mineshaft", "not in Mineshaft", pos)
                return@register ActionResult.PASS
            }

            val client = MinecraftClient.getInstance()

            if (client.player != player) {
                PinglessMiningDebug.log("Mineshaft", "callback player is not client player", pos)
                return@register ActionResult.PASS
            }

            val blockState = world.getBlockState(pos)

            if (blockState.block !in blockNames) {
                PinglessMiningDebug.log("Mineshaft", "block is not a supported Mineshaft block", pos, blockState.block)
                return@register ActionResult.PASS
            }

            val blockName = getMiningBlockName(blockState.block) ?: run {
                PinglessMiningDebug.log("Mineshaft", "no mining block name mapping", pos, blockState.block)
                return@register ActionResult.PASS
            }
            val speed = playerSpeed + getExtraSpeed(blockName)
            val baseTicks = getBlockTicks(blockName, speed) ?: run {
                PinglessMiningDebug.log("Mineshaft", "no tick data for $blockName at speed $speed", pos, blockState.block)
                return@register ActionResult.PASS
            }
            val blockTicks = baseTicks + Settings.general.miningTickOverride
            val immutablePos = pos.toImmutable()

            if (immutablePos !in pendingMines) {
                pendingMines.clear()
                pendingMines[immutablePos] = PendingMine(blockState.block, blockTicks.toFloat())
                PinglessMiningDebug.log("Mineshaft", "created pending mine for $blockName ticks=$blockTicks speed=$speed", immutablePos, blockState.block)
            } else {
                PinglessMiningDebug.log("Mineshaft", "already pending this block", immutablePos, blockState.block)
            }
            ActionResult.PASS
        }
    }

    fun reset() {
        pendingMines.clear()
        PinglessMiningDebug.clear()
    }

    private fun onClientTick(client: MinecraftClient) {
        if (pendingMines.isEmpty()) return
        val world = client.world ?: run {
            PinglessMiningDebug.log("Mineshaft", "cleared pending mines because world is null")
            pendingMines.clear()
            return
        }

        pendingMines.entries.removeIf { (pos, mine) ->
            if (!world.isChunkLoaded(pos)) {
                PinglessMiningDebug.log("Mineshaft", "cleared pending mine because chunk is not loaded", pos, mine.originalBlock)
                return@removeIf true
            }

            if (world.getBlockState(pos).block != mine.originalBlock) {
                PinglessMiningDebug.log("Mineshaft", "cleared pending mine because block changed", pos, mine.originalBlock)
                return@removeIf true
            }

            if (!isActivelyMining(client, pos)) {
                mine.missedActiveTicks++
                if (mine.missedActiveTicks > 2) {
                    PinglessMiningDebug.log("Mineshaft", "cleared pending mine because player stopped mining", pos, mine.originalBlock)
                }
                return@removeIf mine.missedActiveTicks > 2
            }

            mine.missedActiveTicks = 0
            mine.remainingTicks -= TpsSync.getServerTicksPerClientTick()
            if (mine.remainingTicks > 0f) {
                return@removeIf false
            }

            world.removeBlock(pos, false)
            PinglessMiningDebug.log("Mineshaft", "removed block client-side", pos, mine.originalBlock)
            true
        }
    }

    private fun getMiningBlockName(block: Block): String? {
        return when (block) {
            Blocks.DARK_PRISMARINE,
            Blocks.PRISMARINE -> "Prismarine Mithril"
            Blocks.LIGHT_BLUE_WOOL -> "Blue Mithril"
            Blocks.GRAY_WOOL -> "Gray Mithril"
            Blocks.GOLD_BLOCK -> "Pure Gold"
            Blocks.OBSIDIAN -> "Obsidian"
            Blocks.SMOOTH_RED_SANDSTONE -> "Umber"
            Blocks.TERRACOTTA -> "Umber"
            Blocks.BROWN_TERRACOTTA -> "Umber"
            Blocks.CLAY -> "Tungsten"
            Blocks.COBBLESTONE -> "Tungsten"
            else -> null
        }
    }

    private fun getExtraSpeed(blockName: String): Int {
        return when (blockName) {
            "Obsidian" -> Settings.general.extraBlockSpeed.toIntOrNull() ?: 0
            "Pure Gold" -> Settings.general.extraOreSpeed.toIntOrNull() ?: 0
            else -> Settings.general.extraDwarvenMetalSpeed.toIntOrNull() ?: 0
        }
    }

    private fun isActivelyMining(client: MinecraftClient, pos: BlockPos): Boolean {
        if (!client.options.attackKey.isPressed) return false

        val target = client.crosshairTarget
        if (target?.type != HitResult.Type.BLOCK) return false

        return (target as BlockHitResult).blockPos == pos
    }
}
