package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.ceil

object FarmingTracker {
    private val pestTimers = mutableMapOf<String, String>()
    private var lastTabUpdate = 0L
    private var countdownEndMillis = 0L
    private var lastCooldownSeconds: Int? = null
    private var cycle2TriggeredForThisCooldown = false
    private var alertUntilMillis = 0L
    private var chimeTicksRemaining = 0
    private var cooldownReady = false

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { onTick() }
        HudElementRegistry.addLast(Identifier.of("pgs_addons", "pest_timers")) { context, _ ->
            onRenderHud(context)
        }
    }

    private fun onTick() {
        val client = MinecraftClient.getInstance()
        if (client.world == null || client.player == null) return
        if (!Settings.general.pestTimersEnabled) return

        val now = System.currentTimeMillis()
        if (chimeTicksRemaining > 0) {
            client.player?.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.0f)
            chimeTicksRemaining--
        }

        if (LocationUtils.isInGarden() && countdownEndMillis > 0L && remainingOffsetSeconds() <= 0) {
            triggerCycle2IfNeeded(now)
        }

        if (now - lastTabUpdate < 500L) return
        lastTabUpdate = now

        if (!LocationUtils.isInGarden()) {
            pestTimers.clear()
            countdownEndMillis = 0L
            lastCooldownSeconds = null
            cycle2TriggeredForThisCooldown = false
            cooldownReady = false
            return
        }

        updatePestTimers(client)
    }

    private fun updatePestTimers(client: MinecraftClient) {
        val entries = client.networkHandler?.playerList ?: return
        val newTimers = mutableMapOf<String, String>()

        for (entry in entries) {
            val text = stripFormatting(entry.displayName?.string ?: continue)
            when {
                text.contains("Alive:") -> newTimers["Alive"] = text.substringAfter("Alive:").trim()
                text.contains("Spray:") -> newTimers["Spray"] = text.substringAfter("Spray:").trim()
                text.contains("Bonus:") -> newTimers["Bonus"] = text.substringAfter("Bonus:").trim()
                text.contains("Cooldown:") -> newTimers["Cooldown"] = text.substringAfter("Cooldown:").trim()
            }
        }

        pestTimers.clear()
        pestTimers.putAll(newTimers)
        newTimers["Alive"]?.let { parseAliveCount(it)?.let(AutoFarm2::updatePestAliveCount) }

        val cooldownText = newTimers["Cooldown"].orEmpty()
        val cooldownSeconds = parseTimeToSeconds(cooldownText)
        if (cooldownSeconds != null) {
            syncCountdown(cooldownSeconds)
        } else if (cooldownText.equals("Ready", ignoreCase = true)) {
            countdownEndMillis = System.currentTimeMillis()
            cooldownReady = true
            triggerCycle2IfNeeded(System.currentTimeMillis())
        }
    }

    private fun syncCountdown(cooldownSeconds: Int) {
        val offsetSeconds = Settings.general.autoFarm2PestSpawnOffsetSeconds
        val targetSeconds = (cooldownSeconds - offsetSeconds).coerceAtLeast(0)
        val now = System.currentTimeMillis()

        if (lastCooldownSeconds == null || cooldownSeconds > (lastCooldownSeconds ?: 0) + 10) {
            cycle2TriggeredForThisCooldown = false
            cooldownReady = false
        }

        lastCooldownSeconds = cooldownSeconds
        countdownEndMillis = now + targetSeconds * 1000L
        if (targetSeconds <= 0) {
            cooldownReady = true
            triggerCycle2IfNeeded(now)
        }
    }

    private fun triggerCycle2IfNeeded(now: Long) {
        if (cycle2TriggeredForThisCooldown) return
        cycle2TriggeredForThisCooldown = true
        alertUntilMillis = now + 4000L
        chimeTicksRemaining = 5
        AutoFarm2.startCycle2FromPestCooldown()
    }

    fun isCycle2Ready(): Boolean {
        return cooldownReady || (countdownEndMillis > 0L && remainingOffsetSeconds() <= 0)
    }

    private fun onRenderHud(context: DrawContext) {
        if (!Settings.general.pestTimersEnabled) return
        if (!LocationUtils.isInGarden()) return
        drawHud(context, false)
    }

    @JvmStatic
    fun drawHud(context: DrawContext, mockup: Boolean) {
        val client = MinecraftClient.getInstance()
        val x = Settings.general.pestTimersX
        val y = Settings.general.pestTimersY
        val spacing = 12
        var row = 0

        context.drawText(client.textRenderer, "§6[Pest Tracker]", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
        row++

        if (mockup) {
            context.drawText(client.textRenderer, "§6Spray: 15m 00s", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
            row++
            context.drawText(client.textRenderer, "§6Bonus: 20m 00s", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
            row++
            context.drawText(client.textRenderer, "§6Alive: 3", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
            row++
            context.drawText(client.textRenderer, "§6Cooldown: 5m 00s", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
            row++
            context.drawText(client.textRenderer, "§e  Countdown: 4m 30s", x, y + row * spacing, 0xFFFFFF55.toInt(), true)
            return
        }

        for (key in listOf("Spray", "Bonus", "Alive", "Cooldown")) {
            pestTimers[key]?.let {
                context.drawText(client.textRenderer, "§6$key: $it", x, y + row * spacing, 0xFFFFAA00.toInt(), true)
                row++
            }
        }

        if (countdownEndMillis > 0L) {
            val remaining = remainingOffsetSeconds()
            val label = if (remaining <= 0) "§2Pest Spawn Ready" else formatSeconds(remaining)
            context.drawText(client.textRenderer, "§e  Countdown: $label", x, y + row * spacing, 0xFFFFFF55.toInt(), true)
        }

        if (alertUntilMillis > System.currentTimeMillis()) {
            val msg = Text.literal("§c§lPEST COOLDOWN READY")
            val textWidth = client.textRenderer.getWidth(msg)
            context.drawText(
                client.textRenderer,
                msg,
                (client.window.scaledWidth - textWidth) / 2,
                client.window.scaledHeight / 2 - 30,
                0xFFFF4444.toInt(),
                true
            )
        }
    }

    private fun remainingOffsetSeconds(): Long {
        if (countdownEndMillis <= 0L) return -1L
        return ceil((countdownEndMillis - System.currentTimeMillis()) / 1000.0).toLong().coerceAtLeast(0L)
    }

    private fun parseTimeToSeconds(input: String?): Int? {
        val text = input?.trim()?.lowercase().orEmpty()
        if (text.isEmpty()) return null

        val minutes = Regex("(\\d+)\\s*m").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val seconds = Regex("(\\d+)\\s*s").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        if (minutes > 0 || seconds > 0) return minutes * 60 + seconds

        return text.takeIf { it.all(Char::isDigit) }?.toIntOrNull()
    }

    private fun parseAliveCount(input: String): Int? {
        return Regex("\\d+").find(input)?.value?.toIntOrNull()
    }

    private fun formatSeconds(seconds: Long): String {
        val minutes = seconds / 60L
        val secs = seconds % 60L
        return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
    }

    private fun stripFormatting(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE), "")
    }
}
