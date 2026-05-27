package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.Settings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true)
    private void pgsAddons_showOwnNametag(Entity entity, double sqDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!Settings.general.showOwnNametag)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && entity == client.player) {
            cir.setReturnValue(true);
        }
    }
}
