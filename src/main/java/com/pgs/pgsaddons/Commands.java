package com.pgs.pgsaddons;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public class Commands {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder) ClientCommandManager.literal("pgs").executes((context) -> {
         MinecraftClient.getInstance().send(() -> {
            MinecraftClient.getInstance().setScreen(new SettingsScreen());
         });
         return 1;
      }));
   }
}
