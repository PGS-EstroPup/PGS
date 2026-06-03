package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.mixin.KeyBindingAccessor as KeyMappingAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

object AttackDestroyMode {
    private val mc = MinecraftClient.getInstance()
    lateinit var toggleModeKey: KeyBinding

    private var toggledAttackDown = false
    private var lastAttackClickWasToggle = false

    fun init() {
        toggleModeKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Toggle Attack / Destroy Mode",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleModeKey.consumeClick()) toggleMode()
            applyAttackDestroyMode(client)
        }
    }

    fun toggleMode() {
        Settings.general.attackDestroyToggleMode = !Settings.general.attackDestroyToggleMode
        Settings.save()
        setMinecraftControlMode(Settings.general.attackDestroyToggleMode)
        if (!Settings.general.attackDestroyToggleMode) {
            toggledAttackDown = false
            lastAttackClickWasToggle = false
            net.minecraft.client.KeyMapping.set((mc.options.keyAttack as KeyMappingAccessor).`pgsAddons$getBoundKey`(), false)
        }
        val mode = if (Settings.general.attackDestroyToggleMode) "Toggle" else "Hold"
        mc.player?.sendSystemMessage(Text.literal("\u00A7b[PGS] \u00A77Attack / Destroy: \u00A7a$mode"))
    }

    fun modeLabel(): String {
        return if (Settings.general.attackDestroyToggleMode) "\u00A7aToggle" else "\u00A7eHold"
    }

    fun pressAttackForAutomation(): Boolean {
        return Settings.general.attackDestroyToggleMode
    }

    private fun setMinecraftControlMode(toggle: Boolean) {
        setBooleanOption(listOf("toggleAttack", "getAttackToggled"), toggle)
        setBooleanOption(listOf("toggleUse", "getUseToggled"), toggle)
        mc.options.save()
    }

    private fun setBooleanOption(optionMethods: List<String>, value: Boolean) {
        val option = optionMethods.firstNotNullOfOrNull { methodName ->
            runCatching { mc.options.javaClass.getMethod(methodName).invoke(mc.options) }.getOrNull()
        } ?: return

        val setter = listOf("set", "setValue").firstNotNullOfOrNull { methodName ->
            option.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 1 }
        } ?: return
        setter.invoke(option, value)
    }

    private fun applyAttackDestroyMode(client: MinecraftClient) {
        if (!Settings.general.attackDestroyToggleMode || client.player == null) return
        if (AutoFarm2.isRunning()) return
        if (client.screen != null) {
            toggledAttackDown = false
            lastAttackClickWasToggle = false
            return
        }

        val attackKey = client.options.keyAttack
        while (attackKey.consumeClick()) {
            toggledAttackDown = !toggledAttackDown
            lastAttackClickWasToggle = true
        }

        if (lastAttackClickWasToggle && !attackKey.isDown) {
            lastAttackClickWasToggle = false
        }

        if (toggledAttackDown) {
            net.minecraft.client.KeyMapping.set((attackKey as KeyMappingAccessor).`pgsAddons$getBoundKey`(), true)
        }
    }
}
