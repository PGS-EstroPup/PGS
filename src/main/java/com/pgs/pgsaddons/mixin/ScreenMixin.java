package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.NotepadOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false)
    private void pgsAddons$renderNotepad(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NotepadOverlay.INSTANCE.renderOnScreen(context, mouseX, mouseY, delta);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void pgsAddons$notepadKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.keyPressed(input)) {
            cir.setReturnValue(true);
        }
    }
}
