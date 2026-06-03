package com.pgs.pgsaddons.mixin;

import com.pgs.pgsaddons.features.AutoFarm2;
import com.pgs.pgsaddons.features.FrozenBlazeFishing;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class AutoFarmInputMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void pgsAddons$applyAutoFarm2Movement(CallbackInfo ci) {
        AutoFarm2.INSTANCE.applyMovementInputOverride((ClientInput) (Object) this);
        FrozenBlazeFishing.INSTANCE.applyMovementInputOverride((ClientInput) (Object) this);
    }
}
