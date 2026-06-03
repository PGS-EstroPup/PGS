package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.Settings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1500)
public abstract class StopSwimmingPlayerMixin {
    @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void pgsAddons$disableSwimming(CallbackInfoReturnable<Boolean> cir) {
        if (Settings.general.stopSwimmingEnabled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"), require = 0, remap = false)
    private void pgsAddons$disableSprintInWater(CallbackInfo ci) {
        if (!Settings.general.stopSwimmingEnabled) return;

        Player player = (Player) (Object) this;
        if (player.isInWater() && player.isSprinting()) {
            player.setSprinting(false);
        }
    }
}
