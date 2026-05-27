package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.AutoFarm2;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void pgsAddons$applyAutoFarm2Movement(CallbackInfo ci) {
        AutoFarm2.INSTANCE.applyMovementInputOverride((Input) (Object) this);
    }
}
