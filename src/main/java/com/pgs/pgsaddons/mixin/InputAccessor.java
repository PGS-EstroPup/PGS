package com.pgs.pgsaddons.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("movementVector")
    void pgsAddons$setMovementVector(Vec2f movementVector);

    @Accessor("playerInput")
    void pgsAddons$setPlayerInput(PlayerInput playerInput);
}
