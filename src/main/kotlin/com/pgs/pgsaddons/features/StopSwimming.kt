package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.lang.reflect.Method

object StopSwimming {
    lateinit var toggleKey: KeyBinding

    private val mc = MinecraftClient.getInstance()
    private val methods = mutableMapOf<String, Method?>()

    fun init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Toggle Stop Swimming",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleKey.consumeClick()) toggle()
            if (Settings.general.stopSwimmingEnabled) apply(client)
        }
    }

    fun toggle() {
        Settings.general.stopSwimmingEnabled = !Settings.general.stopSwimmingEnabled
        Settings.save()
        mc.player?.sendSystemMessage(Text.literal("\u00A7b[PGS] \u00A77Stop Swimming: ${if (Settings.general.stopSwimmingEnabled) "\u00A7aON" else "\u00A7cOFF"}"))
    }

    private fun apply(client: MinecraftClient) {
        val player = client.player ?: return
        callBoolean(player, listOf("setSwimming"), false)

        if (isInFluid(player)) {
            callBoolean(player, listOf("setSprinting"), false)
        }
    }

    private fun isInFluid(player: Any): Boolean {
        return callBooleanResult(player, listOf("isInWater", "isUnderWater", "isInFluidType")) == true
    }

    private fun callBoolean(target: Any, names: List<String>, value: Boolean) {
        val method = findMethod(target, names, Boolean::class.javaPrimitiveType)
        method?.invoke(target, value)
    }

    private fun callBooleanResult(target: Any, names: List<String>): Boolean? {
        val method = findMethod(target, names)
        return method?.invoke(target) as? Boolean
    }

    private fun findMethod(target: Any, names: List<String>, vararg parameterTypes: Class<*>?): Method? {
        val key = target.javaClass.name + "#" + names.joinToString("|") + parameterTypes.joinToString(prefix = "(") { it?.name ?: "null" }
        if (methods.containsKey(key)) return methods[key]

        val method = target.javaClass.methods.firstOrNull { method ->
            method.name in names &&
                    method.parameterCount == parameterTypes.size &&
                    method.parameterTypes.zip(parameterTypes).all { (actual, expected) -> expected == null || actual == expected }
        }
        methods[key] = method
        return method
    }
}
