package com.pgs.pgsaddons;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

public class Commands {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder) ClientCommands.literal("pgs").executes((context) -> {
         Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new SettingsScreen());
         });
         return 1;
      }));
   }
}
