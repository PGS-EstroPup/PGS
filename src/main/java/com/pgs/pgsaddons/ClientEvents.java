package com.pgs.pgsaddons;

import com.pgs.pgsaddons.features.*;
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
      LittlefootEsp.INSTANCE.init();
      TPMazeTracer.INSTANCE.init();
      LotusWormholeDetector.INSTANCE.init();
   }
}
