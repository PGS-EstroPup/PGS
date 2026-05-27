package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.Settings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void pgsAddons_showOwnNametag(PlayerLikeEntity playerLikeEntity, double sqDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!Settings.general.showOwnNametag) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && playerLikeEntity == client.player) {
            // Usually, Minecraft hides the label if you're the main player. 
            // By returning true here, we force it to show up.
            cir.setReturnValue(true);
        }
    }
}
