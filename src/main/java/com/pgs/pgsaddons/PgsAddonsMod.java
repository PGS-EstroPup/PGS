package com.pgs.pgsaddons;

import com.pgs.pgsaddons.features.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import javax.swing.*;

public class PgsAddonsMod implements ClientModInitializer {
   public void onInitializeClient() {
      ClientEvents.register();
      Settings.load();
      TpsSync.init();
      MobEsp.INSTANCE.init();
      StarredMobEsp.init();
      com.pgs.pgsaddons.features.CustomEsp.INSTANCE.init();
      WitherDoorEsp.init();
      KeyHighlight.init();
      DeployablesTracker.init();
      com.pgs.pgsaddons.features.Timer.INSTANCE.init();
      com.pgs.pgsaddons.features.SlotSwap.init();
      com.pgs.pgsaddons.features.EquipmentStatsHud.INSTANCE.init();
      com.pgs.pgsaddons.features.ArrowTypeTracker.INSTANCE.init();
      com.pgs.pgsaddons.features.FarmingTracker.INSTANCE.init();
      com.pgs.pgsaddons.features.AutoFishKiller.INSTANCE.init();
      com.pgs.pgsaddons.features.AutoHarp.INSTANCE.init();
      com.pgs.pgsaddons.features.AutoFarm2.INSTANCE.init();
      com.pgs.pgsaddons.features.DrawNodes.INSTANCE.init();
      MinireenasOverlay.init();
      FishMacroCheck.INSTANCE.start();
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
         Commands.register(dispatcher);
      });
        System.out.println("[pgs_addons] Initialized client mod (PGS 1.0.0).");
   }
}
