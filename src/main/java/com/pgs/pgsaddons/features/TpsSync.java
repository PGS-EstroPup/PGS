package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class TpsSync {
   public static final ServerTickRateTracker TICK_RATE = new ServerTickRateTracker();

   private TpsSync() {
   }

   public static void init() {
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> TICK_RATE.reset());
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TICK_RATE.reset());
   }

   public static float getTargetMillisPerTick(float vanillaTargetMillis) {
      if (!Settings.general.tpsSyncEnabled) return vanillaTargetMillis;

      float tickRate = getServerTickRate();
      if (tickRate <= 0.0F) return vanillaTargetMillis;

      float syncedTargetMillis = getServerMillisPerTick();

      return Math.max(vanillaTargetMillis, syncedTargetMillis);
   }

   public static float getServerTickRate() {
      if (!Settings.general.tpsSyncEnabled) return 20.0F;

      float tickRate = TICK_RATE.getTickRate();
      if (tickRate <= 0.0F) return 20.0F;
      return clamp(tickRate, Settings.general.tpsSyncMinimumTps, Settings.general.tpsSyncMaximumTps);
   }

   public static float getServerMillisPerTick() {
      return 1000.0F / getServerTickRate();
   }

   public static long serverTicksToMillis(float ticks) {
      return Math.max(0L, Math.round(ticks * getServerMillisPerTick()));
   }

   public static long serverAdjustedDelayMillis(long vanillaDelayMillis) {
      if (!Settings.general.tpsSyncEnabled) return vanillaDelayMillis;
      if (vanillaDelayMillis <= 0L) return 0L;
      float vanillaTicks = vanillaDelayMillis / 50.0F;
      return Math.max(vanillaDelayMillis, serverTicksToMillis(vanillaTicks));
   }

   public static float getServerTicksPerClientTick() {
      return 1.0F;
   }

   private static float clamp(float value, float min, float max) {
      float normalizedMin = Math.max(0.1F, Math.min(20.0F, min));
      float normalizedMax = Math.max(normalizedMin, Math.min(20.0F, max));
      return Math.max(normalizedMin, Math.min(normalizedMax, value));
   }
}
