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


object PinglessMiningDwarven {

    val playerSpeed: Int
        get() = MiningUtils.getMiningSpeed()

    data class PendingMine(
        val originalBlock: Block,
        var remainingTicks: Float,
        val removeBlock: Boolean,
        var missedActiveTicks: Int = 0
    )

    val pendingMines = mutableMapOf<BlockPos, PendingMine>()

    private val blockNames = setOf(
        Blocks.DARK_PRISMARINE,
        Blocks.PRISMARINE,
        Blocks.LIGHT_BLUE_WOOL,
        Blocks.GRAY_WOOL,
        Blocks.COAL_BLOCK,
        Blocks.DIAMOND_BLOCK,
        Blocks.GOLD_BLOCK,
        Blocks.IRON_BLOCK,
        Blocks.LAPIS_BLOCK,
        Blocks.EMERALD_BLOCK,
        Blocks.QUARTZ_BLOCK,
        Blocks.REDSTONE_BLOCK,
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
                PinglessMiningDebug.log("Dwarven", "pingless mining disabled", pos)
                return@register ActionResult.PASS
            }

            if (!world.isClient) {
                PinglessMiningDebug.log("Dwarven", "not client world", pos)
                return@register ActionResult.PASS
            }

            val removeBlock = LocationUtils.isInCrystalHollows()
            if (!LocationUtils.isInDwarvenMines() && !removeBlock && !LocationUtils.isInTheEnd()) {
                PinglessMiningDebug.log("Dwarven", "not in Dwarven Mines, Crystal Hollows, or The End", pos)
                return@register ActionResult.PASS
            }

            val client = MinecraftClient.getInstance()

            if (client.player != player) {
                PinglessMiningDebug.log("Dwarven", "callback player is not client player", pos)
                return@register ActionResult.PASS
            }

            val blockState = world.getBlockState(pos)

            if (blockState.block !in blockNames) {
                PinglessMiningDebug.log("Dwarven", "block is not a supported Dwarven block", pos, blockState.block)
                return@register ActionResult.PASS
            }

            val blockName = getMiningBlockName(blockState.block) ?: run {
                PinglessMiningDebug.log("Dwarven", "no mining block name mapping", pos, blockState.block)
                return@register ActionResult.PASS
            }
            val speed = playerSpeed + getExtraSpeed(blockName)
            val baseTicks = getBlockTicks(blockName, speed) ?: run {
                PinglessMiningDebug.log("Dwarven", "no tick data for $blockName at speed $speed", pos, blockState.block)
                return@register ActionResult.PASS
            }
            val blockTicks = baseTicks + Settings.general.miningTickOverride
            val immutablePos = pos.toImmutable()

            if (immutablePos !in pendingMines) {
                pendingMines.clear()
                pendingMines[immutablePos] = PendingMine(blockState.block, blockTicks.toFloat(), removeBlock)
                PinglessMiningDebug.log("Dwarven", "created pending mine for $blockName ticks=$blockTicks speed=$speed removeBlock=$removeBlock", immutablePos, blockState.block)
            } else {
                PinglessMiningDebug.log("Dwarven", "already pending this block", immutablePos, blockState.block)
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
            PinglessMiningDebug.log("Dwarven", "cleared pending mines because world is null")
            pendingMines.clear()
            return
        }

        pendingMines.entries.removeIf { (pos, mine) ->
            if (!world.isChunkLoaded(pos)) {
                PinglessMiningDebug.log("Dwarven", "cleared pending mine because chunk is not loaded", pos, mine.originalBlock)
                return@removeIf true
            }

            if (!isActivelyMining(client, pos)) {
                mine.missedActiveTicks++
                if (mine.missedActiveTicks > 2) {
                    PinglessMiningDebug.log("Dwarven", "cleared pending mine because player stopped mining", pos, mine.originalBlock)
                }
                return@removeIf mine.missedActiveTicks > 2
            }

            mine.missedActiveTicks = 0
            mine.remainingTicks -= TpsSync.getServerTicksPerClientTick()
            if (mine.remainingTicks > 0f) {
                return@removeIf false
            }

            if (mine.removeBlock) {
                world.removeBlock(pos, false)
                PinglessMiningDebug.log("Dwarven", "removed block client-side", pos, mine.originalBlock)
            } else {
                world.setBlockState(pos, Blocks.BEDROCK.defaultState, 3)
                PinglessMiningDebug.log("Dwarven", "set block to bedrock client-side", pos, mine.originalBlock)
            }
            true
        }
    }

    private fun getMiningBlockName(block: Block): String? {
        return when (block) {
            Blocks.DARK_PRISMARINE,
            Blocks.PRISMARINE -> "Prismarine Mithril"
            Blocks.LIGHT_BLUE_WOOL -> "Blue Mithril"
            Blocks.GRAY_WOOL -> "Gray Mithril"
            Blocks.COAL_BLOCK -> "Pure Coal"
            Blocks.DIAMOND_BLOCK -> "Pure Diamond"
            Blocks.GOLD_BLOCK -> "Pure Gold"
            Blocks.IRON_BLOCK -> "Pure Iron"
            Blocks.LAPIS_BLOCK -> "Pure Lapis"
            Blocks.EMERALD_BLOCK -> "Pure Emerald"
            Blocks.QUARTZ_BLOCK -> "Pure Quartz"
            Blocks.REDSTONE_BLOCK -> "Pure Redstone"
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
            "Pure Coal", "Pure Diamond", "Pure Gold", "Pure Iron", "Pure Lapis", "Pure Emerald", "Pure Quartz", "Pure Redstone" ->
                Settings.general.extraOreSpeed.toIntOrNull() ?: 0
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
