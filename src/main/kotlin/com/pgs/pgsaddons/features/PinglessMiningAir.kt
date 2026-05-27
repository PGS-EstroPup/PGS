package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.utils.MiningUtils
import com.pgs.pgsaddons.utils.MiningUtils.getBlockTicks
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos

object PinglessMiningAir {

    private val playerSpeed: Int
        get() = MiningUtils.getMiningSpeed()

    private data class PendingMine(
        val pos: BlockPos,
        val originalBlock: Block,
        var remainingTicks: Float,
        var missedActiveTicks: Int = 0
    )

    private var pendingMine: PendingMine? = null
    private val recentRemovals = mutableMapOf<BlockPos, Int>()

    private val glassList = setOf(
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
        Blocks.LIME_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS_PANE,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS_PANE,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS_PANE,
        Blocks.BLACK_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS_PANE,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS_PANE,
        Blocks.RED_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS_PANE,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS_PANE,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS_PANE,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.WHITE_STAINED_GLASS_PANE,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS_PANE,
        Blocks.PURPLE_STAINED_GLASS_PANE,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.PACKED_ICE
    )

    fun registeredEvents() {

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->

            if (!Settings.general.pinglessMiningEnabled) {
                PinglessMiningDebug.log("Air", "pingless mining disabled", pos)
                return@register ActionResult.PASS
            }

            if (!world.isClient) {
                PinglessMiningDebug.log("Air", "not client world", pos)
                return@register ActionResult.PASS
            }

            val client = MinecraftClient.getInstance()

            if (client.player != player) {
                PinglessMiningDebug.log("Air", "callback player is not client player", pos)
                return@register ActionResult.PASS
            }

            val blockState = world.getBlockState(pos)

            if (blockState.block !in glassList) {
                PinglessMiningDebug.log("Air", "block is not a supported air-mining block", pos, blockState.block)
                return@register ActionResult.PASS
            }

            val immutablePos = pos.toImmutable()
            if (immutablePos in recentRemovals) {
                PinglessMiningDebug.log("Air", "recently removed this block, waiting for cooldown", immutablePos, blockState.block)
                return@register ActionResult.PASS
            }

            val blockName = getMiningBlockName(blockState.block) ?: run {
                PinglessMiningDebug.log("Air", "no mining block name mapping", immutablePos, blockState.block)
                return@register ActionResult.PASS
            }
            val speed = playerSpeed + getExtraSpeed(blockName)
            val baseTicks = getBlockTicks(blockName, speed) ?: run {
                PinglessMiningDebug.log("Air", "no tick data for $blockName at speed $speed", immutablePos, blockState.block)
                return@register ActionResult.PASS
            }
            val blockTicks = baseTicks + Settings.general.miningTickOverride

            if (pendingMine?.pos != immutablePos) {
                pendingMine = PendingMine(immutablePos, blockState.block, blockTicks.toFloat())
                PinglessMiningDebug.log("Air", "created pending mine for $blockName ticks=$blockTicks speed=$speed", immutablePos, blockState.block)
            } else {
                PinglessMiningDebug.log("Air", "already pending this block", immutablePos, blockState.block)
            }
            ActionResult.PASS
        }
    }

    fun reset() {
        pendingMine = null
        recentRemovals.clear()
        PinglessMiningDebug.clear()
    }

    private fun onClientTick(client: MinecraftClient) {
        if (!Settings.general.pinglessMiningEnabled) {
            pendingMine = null
            recentRemovals.clear()
            return
        }

        updateRecentRemovals()

        val mine = pendingMine ?: return
        val world = client.world ?: run {
            PinglessMiningDebug.log("Air", "cleared pending mine because world is null", mine.pos, mine.originalBlock)
            pendingMine = null
            return
        }

        if (!world.isChunkLoaded(mine.pos)) {
            PinglessMiningDebug.log("Air", "cleared pending mine because chunk is not loaded", mine.pos, mine.originalBlock)
            pendingMine = null
            return
        }

        if (world.getBlockState(mine.pos).block != mine.originalBlock) {
            PinglessMiningDebug.log("Air", "cleared pending mine because block changed", mine.pos, mine.originalBlock)
            pendingMine = null
            return
        }

        if (!isActivelyMining(client, mine.pos)) {
            mine.missedActiveTicks++
            if (mine.missedActiveTicks > 2) {
                PinglessMiningDebug.log("Air", "cleared pending mine because player stopped mining", mine.pos, mine.originalBlock)
                pendingMine = null
            }
            return
        }

        mine.missedActiveTicks = 0
        mine.remainingTicks -= TpsSync.getServerTicksPerClientTick()
        if (mine.remainingTicks > 0f) return

        world.removeBlock(mine.pos, false)
        PinglessMiningDebug.log("Air", "removed block client-side", mine.pos, mine.originalBlock)
        recentRemovals[mine.pos] = 5
        pendingMine = null
    }

    private fun updateRecentRemovals() {
        val iterator = recentRemovals.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remainingTicks = entry.value - 1
            if (remainingTicks <= 0) {
                iterator.remove()
            } else {
                entry.setValue(remainingTicks)
            }
        }
    }

    private fun getMiningBlockName(block: Block): String? {
        return when (block) {
            Blocks.RED_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS_PANE -> "Ruby"
            Blocks.GREEN_STAINED_GLASS,
            Blocks.GREEN_STAINED_GLASS_PANE -> "Peridot"
            Blocks.ORANGE_STAINED_GLASS,
            Blocks.ORANGE_STAINED_GLASS_PANE -> "Amber"
            Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS_PANE -> "Sapphire"
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS_PANE -> "Amethyst"
            Blocks.YELLOW_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS_PANE -> "Topaz"
            Blocks.MAGENTA_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS_PANE -> "Jasper"
            Blocks.BLACK_STAINED_GLASS,
            Blocks.BLACK_STAINED_GLASS_PANE -> "Onyx"
            Blocks.BLUE_STAINED_GLASS,
            Blocks.BLUE_STAINED_GLASS_PANE -> "Aquamarine"
            Blocks.LIME_STAINED_GLASS,
            Blocks.LIME_STAINED_GLASS_PANE -> "Jade"
            Blocks.WHITE_STAINED_GLASS,
            Blocks.WHITE_STAINED_GLASS_PANE -> "Opal"
            Blocks.BROWN_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS_PANE -> "Citrine"
            Blocks.PACKED_ICE -> "Glacite"
            else -> null
        }
    }

    private fun getExtraSpeed(blockName: String): Int {
        return when (blockName) {
            "Glacite" -> Settings.general.extraDwarvenMetalSpeed.toIntOrNull() ?: 0
            else -> Settings.general.extraGemstoneSpeed.toIntOrNull() ?: 0
        }
    }

    private fun isActivelyMining(client: MinecraftClient, pos: BlockPos): Boolean {
        if (!client.options.attackKey.isPressed) return false

        val target = client.crosshairTarget
        if (target?.type != HitResult.Type.BLOCK) return false

        return (target as BlockHitResult).blockPos == pos
    }
}
