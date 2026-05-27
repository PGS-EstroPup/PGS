package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.item.Items
import net.minecraft.util.ActionResult
import com.pgs.pgsaddons.utils.MiningUtils.stripFormatting
import net.minecraft.util.math.BlockPos

object ZeroTickHardstone {

    private data class PendingBreak(val pos: BlockPos, var ticksLeft: Int)

    private const val BREAK_DELAY_TICKS = 2
    private val pendingBreaks = mutableMapOf<BlockPos, PendingBreak>()

    fun registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->
            if (!Settings.general.zeroTickHardstoneEnabled) return@register ActionResult.PASS
            if (!world.isClient) return@register ActionResult.PASS
            if (!LocationUtils.isInCrystalHollows()) return@register ActionResult.PASS

            val client = MinecraftClient.getInstance()
            if (client.player != player) return@register ActionResult.PASS

            val blockState = world.getBlockState(pos)
            if (blockState.block != Blocks.STONE) return@register ActionResult.PASS

            val stack = player.getStackInHand(hand)
            val item = stack.item
            var isAllowedItem = false

            if (item == Items.PRISMARINE_SHARD) {
                isAllowedItem = true
            } else if (item == Items.WOODEN_PICKAXE || item == Items.PLAYER_HEAD) {
                val name = stripFormatting(stack.name.string).lowercase()
                if (item == Items.WOODEN_PICKAXE && name.contains("jungle pickaxe")) {
                    isAllowedItem = true
                } else if (item == Items.PLAYER_HEAD && name.contains("gemstone gauntlet")) {
                    isAllowedItem = true
                }
            }

            if (isAllowedItem) {
                pendingBreaks[pos.toImmutable()] = PendingBreak(pos.toImmutable(), BREAK_DELAY_TICKS)
            }

            ActionResult.PASS
        }
    }

    private fun onClientTick(client: MinecraftClient) {
        val world = client.world ?: run {
            pendingBreaks.clear()
            return
        }

        pendingBreaks.entries.removeIf { (_, pending) ->
            pending.ticksLeft--
            if (pending.ticksLeft > 0) return@removeIf false

            if (Settings.general.zeroTickHardstoneEnabled && world.getBlockState(pending.pos).block == Blocks.STONE) {
                world.removeBlock(pending.pos, false)
            }
            true
        }
    }
}
