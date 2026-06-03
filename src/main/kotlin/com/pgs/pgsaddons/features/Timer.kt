package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil

object Timer {
    lateinit var startStopKey: KeyBinding

    private val mc = MinecraftClient.getInstance()
    private var endTimeMillis = 0L

    @JvmStatic
    fun init() {
        startStopKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Timer Start / Stop",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (startStopKey.consumeClick()) {
                toggle(client)
            }

            if (endTimeMillis > 0L && remainingSeconds() <= 0) {
                finish(client)
            }
        }

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("pgs_addons", "timer")
        ) { context, _ ->
            onRenderHud(context)
        }
    }

    private fun toggle(client: MinecraftClient) {
        if (isRunning()) {
            stop()
            client.player?.sendSystemMessage(Text.literal("\u00A7c[Timer] Stopped"))
            return
        }

        val seconds = parseDurationSeconds(Settings.general.timerDuration)
        if (seconds <= 0) {
            client.player?.sendSystemMessage(Text.literal("\u00A7c[Timer] Enter a time like 2m 30s."))
            return
        }

        endTimeMillis = System.currentTimeMillis() + seconds * 1000L
        client.player?.sendSystemMessage(Text.literal("\u00A7a[Timer] Started for ${formatSeconds(seconds)}"))
    }

    private fun stop() {
        endTimeMillis = 0L
    }

    private fun finish(client: MinecraftClient) {
        stop()
        val message = Text.literal("TIMER ENDED").withStyle(Formatting.RED, Formatting.BOLD)
        client.gui.setTitle(message)
        client.gui.setTitleTicks(5, 40, 10)
        client.player?.sendMessage(message, false)
        runCommand(client)
    }

    private fun runCommand(client: MinecraftClient) {
        val command = Settings.general.timerCommand.trim().removePrefix("/")
        if (command.isEmpty()) return
        client.player?.connection?.sendChatCommand(command)
    }

    private fun isRunning(): Boolean {
        return endTimeMillis > System.currentTimeMillis()
    }

    private fun remainingSeconds(): Long {
        if (endTimeMillis <= 0L) return 0L
        val remainingMillis = endTimeMillis - System.currentTimeMillis()
        return ceil(remainingMillis / 1000.0).toLong().coerceAtLeast(0L)
    }

    private fun parseDurationSeconds(input: String?): Long {
        val text = input?.trim()?.lowercase().orEmpty()
        if (text.isEmpty()) return 0L

        val matcher = Regex("""(\d+)\s*([hms])?""").findAll(text)
        var total = 0L
        var matched = false

        for (match in matcher) {
            matched = true
            val value = match.groupValues[1].toLongOrNull() ?: continue
            total += when (match.groupValues[2]) {
                "h" -> value * 3600L
                "m" -> value * 60L
                else -> value
            }
        }

        return if (matched) total else 0L
    }

    private fun onRenderHud(context: DrawContext) {
        if (!isRunning()) return
        drawHud(context, false)
    }

    fun drawHud(context: DrawContext, mockup: Boolean) {
        val x = Settings.general.timerHudX
        val y = Settings.general.timerHudY
        val label = if (mockup) "Timer 02:30" else "Timer ${formatSeconds(remainingSeconds())}"
        val width = (mc.font.width("\u00A7l$label") + HudPanel.PADDING * 2).coerceAtLeast(84)
        HudPanel.drawTextPanel(context, x, y, width, 24, label, 0xFFFFFF55.toInt(), bold = true)
    }

    fun mockupWidth(): Int {
        return (mc.font.width("\u00A7lTimer 02:30") + HudPanel.PADDING * 2).coerceAtLeast(84)
    }

    private fun formatSeconds(seconds: Long): String {
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        val secs = seconds % 60L
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }
}
