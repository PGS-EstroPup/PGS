package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.FrozenBlazeFishing;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class FrozenBlazeMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), remap = false)
    private void pgsAddons$frozenBlazeMouseButton(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (action != 0) {
            FrozenBlazeFishing.INSTANCE.onUserInput();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), remap = false)
    private void pgsAddons$frozenBlazeMouseMove(long window, double x, double y, CallbackInfo ci) {
        FrozenBlazeFishing.INSTANCE.onUserInput();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), remap = false)
    private void pgsAddons$frozenBlazeMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        FrozenBlazeFishing.INSTANCE.onUserInput();
    }
}
