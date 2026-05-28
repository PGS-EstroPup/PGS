package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.NotepadOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void pgsAddons$renderNotepad(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NotepadOverlay.INSTANCE.renderOnScreen(context, mouseX, mouseY, delta);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void pgsAddons$notepadKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.keyPressed(input)) {
            cir.setReturnValue(true);
        }
    }
}
