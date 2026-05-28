package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.NotepadOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParentElement.class)
public interface ParentElementMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void pgsAddons$notepadMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.mouseClicked(click, doubled)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void pgsAddons$notepadMouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.mouseDragged(click, offsetX, offsetY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void pgsAddons$notepadMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.mouseReleased(click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void pgsAddons$notepadCharTyped(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        if (NotepadOverlay.INSTANCE.charTyped(input)) {
            cir.setReturnValue(true);
        }
    }
}
