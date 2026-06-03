package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.NotepadOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, remap = false)
    private void pgsAddons$notepadCharTyped(long window, CharacterEvent input, CallbackInfo ci) {
        if (NotepadOverlay.INSTANCE.charTyped(input)) {
            ci.cancel();
        }
    }
}
