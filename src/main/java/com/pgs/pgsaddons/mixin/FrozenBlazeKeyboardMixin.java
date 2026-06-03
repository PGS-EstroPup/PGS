package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.FrozenBlazeFishing;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class FrozenBlazeKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), remap = false)
    private void pgsAddons$frozenBlazeKeyboardInput(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != 0) {
            FrozenBlazeFishing.INSTANCE.onUserInput();
        }
    }
}
