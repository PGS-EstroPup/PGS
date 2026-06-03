package com.pgs.pgsaddons.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface InputAccessor {
    @Accessor("moveVector")
    void pgsAddons$setMovementVector(Vec2 movementVector);

    @Accessor("keyPresses")
    void pgsAddons$setPlayerInput(Input playerInput);
}
