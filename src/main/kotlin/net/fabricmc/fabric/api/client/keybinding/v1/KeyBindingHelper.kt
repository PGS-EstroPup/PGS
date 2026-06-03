package net.fabricmc.fabric.api.client.keybinding.v1

import net.minecraft.client.KeyMapping

object KeyBindingHelper {
    @Suppress("UNCHECKED_CAST")
    fun <T : KeyMapping> registerKeyBinding(keyBinding: T): T {
        return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(keyBinding) as T
    }
}
