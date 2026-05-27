package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.TpsSync;
import com.pgs.pgsaddons.features.TPMazeTracer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
   @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
   private void pgsAddons$onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
      TpsSync.TICK_RATE.onWorldTimeUpdate();
   }

   @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
   private void pgsAddons$onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      TPMazeTracer.INSTANCE.onPlayerPositionLook(packet);
   }
}
