package com.pgs.pgsaddons.utils

import net.minecraft.client.MinecraftClient
import com.pgs.pgsaddons.features.playerList

object MiningUtils {

    private val formattingCodeRegex = Regex("\\u00A7[0-9a-fk-or]", RegexOption.IGNORE_CASE)
    private val miningSpeedRegex = Regex("^Mining Speed\\s*:\\s*\\D*([0-9][0-9,]*)", RegexOption.IGNORE_CASE)
    private var cachedMiningSpeed = 0
    private var lastMiningSpeedUpdateNanos = 0L

    fun stripFormatting(str: String): String {
        return str
            .replace(formattingCodeRegex, "")
            .replace("\u00C2", "")
    }

    fun getBlockTicks(name: String, speed: Int): Int? {

        val data = MiningSpeedData.getMiningData(name) ?: return null

        var bestTick: Int? = null

        for (entry in data) {

            val ticks = entry[0]
            val requiredSpeed = entry[1]

            if (speed >= requiredSpeed) {
                bestTick = ticks
            }
        }

        return bestTick
    }

    fun getMiningSpeed(): Int {
        val now = System.nanoTime()
        if (now - lastMiningSpeedUpdateNanos < 500_000_000L) {
            return cachedMiningSpeed
        }
        lastMiningSpeedUpdateNanos = now

        val client = MinecraftClient.getInstance()
        val handler = client?.player?.connection ?: run {
            clearCachedMiningSpeed()
            return 0
        }

        var bestSpeed: Int? = null
        for (entry in handler.playerList) {
            val texts = listOfNotNull(
                entry.tabListDisplayName?.string,
                entry.profile.name
            )

            for (rawText in texts) {
                val text = stripFormatting(rawText)
                val speed = parseMiningSpeed(text)
                if (speed != null && speed > (bestSpeed ?: 0)) {
                    bestSpeed = speed
                }
            }
        }
        bestSpeed?.let {
            cachedMiningSpeed = it
            return it
        }
        return cachedMiningSpeed
    }

    fun clearCachedMiningSpeed() {
        cachedMiningSpeed = 0
        lastMiningSpeedUpdateNanos = 0L
    }

    private fun parseMiningSpeed(text: String): Int? {
        val normalizedText = text
            .trim()
            .trimStart { !it.isLetterOrDigit() }

        val speedText = miningSpeedRegex
            .find(normalizedText)
            ?.groupValues
            ?.get(1)
            ?.replace(",", "")

        return speedText?.toIntOrNull()
    }
}


