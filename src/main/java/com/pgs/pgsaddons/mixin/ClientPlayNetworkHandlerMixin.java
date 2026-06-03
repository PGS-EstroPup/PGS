package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.LotusWormholeDetector;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleParticleEvent", at = @At("HEAD"), remap = false)
    private void pgsaddons$onParticleEvent(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        LotusWormholeDetector.INSTANCE.onParticlePacket(packet);
    }
}
