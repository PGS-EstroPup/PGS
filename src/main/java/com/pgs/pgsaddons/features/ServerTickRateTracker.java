package com.pgs.pgsaddons.features;

import java.util.Arrays;

public final class ServerTickRateTracker {
   private static final int SAMPLE_SIZE = 20;

   private final float[] samples = new float[SAMPLE_SIZE];
   private int nextIndex;
   private long lastTimeUpdateMillis = -1L;
   private long joinedAtMillis = System.currentTimeMillis();

   public void reset() {
      Arrays.fill(samples, 0.0F);
      nextIndex = 0;
      joinedAtMillis = System.currentTimeMillis();
      lastTimeUpdateMillis = -1L;
   }

   public void onWorldTimeUpdate() {
      long now = System.currentTimeMillis();

      if (lastTimeUpdateMillis > 0L) {
         float elapsedSeconds = (now - lastTimeUpdateMillis) / 1000.0F;
         if (elapsedSeconds > 0.0F) {
            samples[nextIndex] = clamp(20.0F / elapsedSeconds, 0.0F, 20.0F);
            nextIndex = (nextIndex + 1) % samples.length;
         }
      }

      lastTimeUpdateMillis = now;
   }

   public float getTickRate() {
      if (System.currentTimeMillis() - joinedAtMillis < 4000L) return 20.0F;

      int count = 0;
      float sum = 0.0F;

      for (float sample : samples) {
         if (sample > 0.0F) {
            sum += sample;
            count++;
         }
      }

      return count == 0 ? 20.0F : sum / count;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
