package com.pgs.pgsaddons;

import com.pgs.pgsaddons.features.*;
import com.pgs.pgsaddons.utils.LocationUtils;
import com.pgs.pgsaddons.utils.MiningUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientEvents {
   public static void register() {
      ClientTickEvents.END_CLIENT_TICK.register((client) -> {
         AutoFishDefault.INSTANCE.onClientTick(client);
         IHateDiorite.onClientTick(client);
         NoTerminatorSwing.onClientTick(client);
         AutoFishKiller.INSTANCE.onClientTick(client);
      });
      
      ZeroTickHardstone.INSTANCE.registerEvents();
      ChestHighlight.INSTANCE.registerEvents();
      PinglessMiningDwarven.INSTANCE.registeredEvents();
      PinglessMiningAir.INSTANCE.registeredEvents();
      PinglessMiningMineshaft.INSTANCE.registeredEvents();
      ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
         LocationUtils.resetLocationCache();
         MiningUtils.INSTANCE.clearCachedMiningSpeed();
         TpsSync.TICK_RATE.reset();
         PinglessMiningDwarven.INSTANCE.reset();
         PinglessMiningAir.INSTANCE.reset();
         PinglessMiningMineshaft.INSTANCE.reset();
      });
      LittlefootEsp.INSTANCE.init();
      TPMazeTracer.INSTANCE.init();
   }
}
