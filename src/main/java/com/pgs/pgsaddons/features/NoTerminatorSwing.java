package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * No Terminator Swing: Prevents arm swing animation when holding a Terminator.
 * Based on OdinClient's Animations#noTermSwing logic.
 */
public class NoTerminatorSwing {

    public static void onClientTick(MinecraftClient client) {
        if (!Settings.general.noTerminatorSwingEnabled) return;
        if (client.player == null) return;

        ItemStack held = client.player.getMainHandStack();
        if (held != null && !held.isEmpty()) {
            String name = held.getName().getString().toUpperCase();
            if (name.contains("TERMINATOR")) {
                // Reset swing state fields, mirroring OdinClient's approach:
                // handSwinging = false, handSwingProgress = 0, lastHandSwingProgress = -1
                client.player.handSwinging = false;
                client.player.handSwingTicks = 0;
                client.player.handSwingProgress = 0.0F;
                client.player.lastHandSwingProgress = 0.0F;
            }
        }
    }
}
