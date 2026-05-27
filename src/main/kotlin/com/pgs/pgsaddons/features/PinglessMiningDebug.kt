package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

object PinglessMiningDebug {
    private const val MESSAGE_COOLDOWN_MS = 750L
    private val lastMessages = mutableMapOf<String, Long>()

    fun log(feature: String, reason: String, pos: BlockPos? = null, block: Block? = null) {
        if (!Settings.general.pinglessMiningDebugEnabled) return

        val now = System.currentTimeMillis()
        val key = "$feature|$reason|${pos?.toShortString()}|${block?.translationKey}"
        val last = lastMessages[key] ?: 0L
        if (now - last < MESSAGE_COOLDOWN_MS) return
        lastMessages[key] = now

        val details = buildString {
            append("[PinglessMining:")
            append(feature)
            append("] ")
            append(reason)
            if (pos != null) {
                append(" pos=")
                append(pos.toShortString())
            }
            if (block != null) {
                append(" block=")
                append(block.translationKey.removePrefix("block.minecraft."))
            }
        }

        System.out.println("[pgs_addons] $details")
        MinecraftClient.getInstance().player?.sendMessage(Text.literal("§7$details"), false)
    }

    fun clear() {
        lastMessages.clear()
    }
}
