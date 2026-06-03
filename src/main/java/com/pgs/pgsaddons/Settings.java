package com.pgs.pgsaddons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Settings {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final Path CONFIG_PATH = Path.of("config", "pgs_addons.json");
   public static final Settings.Config general = new Settings.Config();

   public static void save() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         saveCurrentAutoFarm2Profile();
         String json = GSON.toJson(general);
         Files.writeString(CONFIG_PATH, json);
      } catch (IOException var1) {
         System.err.println("[pgs_addons] Failed to save config: " + var1);
      }
   }

   public static void load() {
      try {
         if (Files.exists(CONFIG_PATH)) {
            String json = Files.readString(CONFIG_PATH);
            Settings.Config loaded = GSON.fromJson(json, Settings.Config.class);
            if (loaded != null) {
               general.autofish = loaded.autofish;
               general.autofishRange = loaded.autofishRange;
               general.autofishJumpBeforeCatch = loaded.autofishJumpBeforeCatch;
               general.frozenBlazeFishingEnabled = json.contains("\"frozenBlazeFishingEnabled\"") ? loaded.frozenBlazeFishingEnabled : general.frozenBlazeFishingEnabled;
               general.lotusWormholeDetectorEnabled = json.contains("\"lotusWormholeDetectorEnabled\"") ? loaded.lotusWormholeDetectorEnabled : true;
               general.lotusWormholeTracersEnabled = json.contains("\"lotusWormholeTracersEnabled\"") ? loaded.lotusWormholeTracersEnabled : true;
               general.lotusWormholeColor = json.contains("\"lotusWormholeColor\"") ? loaded.lotusWormholeColor : general.lotusWormholeColor;
               general.iHateDioriteEnabled = loaded.iHateDioriteEnabled;
               general.pestEspEnabled = json.contains("\"pestEspEnabled\"") ? loaded.pestEspEnabled : true;
               general.mobEspColorIndex = clamp(loaded.mobEspColorIndex, 0, 7);
               general.pestEspTracersEnabled = json.contains("\"pestEspTracersEnabled\"") ? loaded.pestEspTracersEnabled : true;
               general.pestEspColor = json.contains("\"pestEspColor\"") ? loaded.pestEspColor : colorFromIndex(general.mobEspColorIndex);
               general.pestTimersEnabled = json.contains("\"pestTimersEnabled\"") ? loaded.pestTimersEnabled : true;
               general.pestTimersX = json.contains("\"pestTimersX\"") ? loaded.pestTimersX : general.pestTimersX;
               general.pestTimersY = json.contains("\"pestTimersY\"") ? loaded.pestTimersY : general.pestTimersY;
               general.witherDoorEspEnabled = loaded.witherDoorEspEnabled;
               general.witherDoorEspTracersEnabled = json.contains("\"witherDoorEspTracersEnabled\"") ? loaded.witherDoorEspTracersEnabled : true;
               general.bloodDoorEspEnabled = loaded.bloodDoorEspEnabled;
               general.bloodDoorEspTracersEnabled = json.contains("\"bloodDoorEspTracersEnabled\"") ? loaded.bloodDoorEspTracersEnabled : true;
               general.keyHighlightEnabled = loaded.keyHighlightEnabled;
               general.keyHighlightTracersEnabled = json.contains("\"keyHighlightTracersEnabled\"") ? loaded.keyHighlightTracersEnabled : true;
               general.noTerminatorSwingEnabled = loaded.noTerminatorSwingEnabled;
               general.showOwnNametag = loaded.showOwnNametag;
               general.stopSwimmingEnabled = json.contains("\"stopSwimmingEnabled\"") ? loaded.stopSwimmingEnabled : general.stopSwimmingEnabled;
               general.menuColor = json.contains("\"menuColor\"") ? loaded.menuColor : general.menuColor;
               general.starredMobEspEnabled = loaded.starredMobEspEnabled;
               general.starredMobEspTracersEnabled = json.contains("\"starredMobEspTracersEnabled\"") ? loaded.starredMobEspTracersEnabled : true;
               general.starredMobEspColorIndex = loaded.starredMobEspColorIndex;
               general.starredMobEspColor = json.contains("\"starredMobEspColor\"") ? loaded.starredMobEspColor : colorFromIndex(general.starredMobEspColorIndex);
               general.deployablesTrackerEnabled = loaded.deployablesTrackerEnabled;
               general.alertOnTotemExpiresSoon = loaded.alertOnTotemExpiresSoon;
               general.alertOnBlackHoleExpiresSoon = loaded.alertOnBlackHoleExpiresSoon;
               general.alertOnFlareExpiresSoon = loaded.alertOnFlareExpiresSoon;
               general.remainingTimeTotem = loaded.remainingTimeTotem;
               general.remainingTimeBlackHole = loaded.remainingTimeBlackHole;
               general.remainingTimeFlare = loaded.remainingTimeFlare;
               general.deployablesOverlayX = loaded.deployablesOverlayX;
               general.deployablesOverlayY = loaded.deployablesOverlayY;
               general.autofishWithKillerEnabled = loaded.autofishWithKillerEnabled;
               general.autofishRodSlot = loaded.autofishRodSlot;
               general.killingItemSlot = loaded.killingItemSlot;
               general.killingSwingCount = loaded.killingSwingCount;
               general.killingSwingCount = loaded.killingSwingCount;
               general.slotSwapRecordMode = loaded.slotSwapRecordMode;
               if (loaded.savedSwapSlots != null) {
                  general.savedSwapSlots = loaded.savedSwapSlots;
               }
               general.slotSwapEnabled = loaded.slotSwapEnabled;
               general.macroCheckEnabled = loaded.macroCheckEnabled;
               if (loaded.macroCheckAlertText != null) {
                  general.macroCheckAlertText = loaded.macroCheckAlertText;
               }
               if (loaded.notepadText != null) {
                  general.notepadText = loaded.notepadText;
               }
               general.notepadRenderMode = json.contains("\"notepadRenderMode\"") ? clamp(loaded.notepadRenderMode, 0, 2) : general.notepadRenderMode;
               general.notepadX = json.contains("\"notepadX\"") ? loaded.notepadX : general.notepadX;
               general.notepadY = json.contains("\"notepadY\"") ? loaded.notepadY : general.notepadY;
               general.notepadWidth = json.contains("\"notepadWidth\"") ? loaded.notepadWidth : general.notepadWidth;
               general.notepadHeight = json.contains("\"notepadHeight\"") ? loaded.notepadHeight : general.notepadHeight;
               general.nodeRenderMode = clamp(loaded.nodeRenderMode, 0, 2);
               general.zeroTickHardstoneEnabled = loaded.zeroTickHardstoneEnabled;
               general.ChestHighlightEnabled = loaded.ChestHighlightEnabled;
               general.chestHighlightTracersEnabled = json.contains("\"chestHighlightTracersEnabled\"") ? loaded.chestHighlightTracersEnabled : true;
               general.powderChestHudEnabled = loaded.powderChestHudEnabled;
               general.powderChestHudX = loaded.powderChestHudX;
               general.powderChestHudY = loaded.powderChestHudY;
               general.autoHarpEnabled = loaded.autoHarpEnabled;
               general.autoHarpCooldown = loaded.autoHarpCooldown;
               if (loaded.timerDuration != null) {
                  general.timerDuration = loaded.timerDuration;
               }
               if (loaded.timerCommand != null) {
                  general.timerCommand = loaded.timerCommand;
               }
               general.timerHudX = loaded.timerHudX;
               general.timerHudY = loaded.timerHudY;
               general.slotSwapHudEnabled = loaded.slotSwapHudEnabled;
               general.slotSwapHudX = loaded.slotSwapHudX;
               general.slotSwapHudY = loaded.slotSwapHudY;
               general.equipmentStatsHudEnabled = loaded.equipmentStatsHudEnabled;
               general.equipmentStatsHudX = loaded.equipmentStatsHudX;
               general.equipmentStatsHudY = loaded.equipmentStatsHudY;
               general.arrowTypeTrackerEnabled = loaded.arrowTypeTrackerEnabled;
               general.tpMazeTracerEnabled = loaded.tpMazeTracerEnabled;
               general.arrowTypeTrackerX = loaded.arrowTypeTrackerX;
               general.arrowTypeTrackerY = loaded.arrowTypeTrackerY;
               general.pinglessMiningEnabled = loaded.pinglessMiningEnabled;
               general.pinglessMiningDebugEnabled = loaded.pinglessMiningDebugEnabled;
               general.tpsSyncEnabled = json.contains("\"tpsSyncEnabled\"") ? loaded.tpsSyncEnabled : true;
               general.miningTickOverride = clamp(loaded.miningTickOverride, 0, 2);
               general.tpsSyncMinimumTps = clamp(loaded.tpsSyncMinimumTps, 0.1F, 20.0F);
               general.tpsSyncMaximumTps = clamp(loaded.tpsSyncMaximumTps, general.tpsSyncMinimumTps, 20.0F);
               general.littlefootEspEnabled = loaded.littlefootEspEnabled;
               general.littlefootEspTracersEnabled = json.contains("\"littlefootEspTracersEnabled\"") ? loaded.littlefootEspTracersEnabled : true;
               general.littlefootEspColorIndex = loaded.littlefootEspColorIndex;
               general.littlefootEspColor = json.contains("\"littlefootEspColor\"") ? loaded.littlefootEspColor : colorFromIndex(general.littlefootEspColorIndex);
               if (loaded.extraOreSpeed != null) {
                  general.extraOreSpeed = loaded.extraOreSpeed;
               }
               if (loaded.extraBlockSpeed != null) {
                  general.extraBlockSpeed = loaded.extraBlockSpeed;
               }
               if (loaded.extraGemstoneSpeed != null) {
                  general.extraGemstoneSpeed = loaded.extraGemstoneSpeed;
               }
               if (loaded.extraDwarvenMetalSpeed != null) {
                  general.extraDwarvenMetalSpeed = loaded.extraDwarvenMetalSpeed;
               }
               general.minireenasOverlayEnabled = json.contains("\"minireenasOverlayEnabled\"") ? loaded.minireenasOverlayEnabled : true;
               general.customEspEnabled = loaded.customEspEnabled;
               general.customEspTracersEnabled = json.contains("\"customEspTracersEnabled\"") ? loaded.customEspTracersEnabled : true;
               general.customEspColorIndex = loaded.customEspColorIndex;
               general.customEspColor = json.contains("\"customEspColor\"") ? loaded.customEspColor : colorFromIndex(general.customEspColorIndex);
               if (loaded.customEspNames != null) {
                  general.customEspNames = loaded.customEspNames;
               }
               general.autoSellEnabled = loaded.autoSellEnabled;
               if (loaded.autoSellNames != null) {
                  general.autoSellNames = loaded.autoSellNames;
               }
               general.autoFarm2Enabled = loaded.autoFarm2Enabled;
               general.attackDestroyToggleMode = loaded.attackDestroyToggleMode;
               general.autoFarm2HoeSlot = clamp(loaded.autoFarm2HoeSlot, 1, 9);
               general.autoFarm2MousematSlot = clamp(loaded.autoFarm2MousematSlot, 1, 9);
               general.autoFarm2RodSlot = clamp(loaded.autoFarm2RodSlot, 1, 9);
               general.autoFarm2VacuumSlot = clamp(loaded.autoFarm2VacuumSlot, 1, 9);
               general.autoFarm2SpraySlot = clamp(loaded.autoFarm2SpraySlot, 1, 9);
               general.autoFarm2PestSpawnOffsetSeconds = Math.max(0, loaded.autoFarm2PestSpawnOffsetSeconds);
               if (loaded.autoFarm2PlotName != null) {
                  general.autoFarm2PlotName = loaded.autoFarm2PlotName;
               }
               if (loaded.autoFarm2ArmorSlot1 != null) {
                  general.autoFarm2ArmorSlot1 = loaded.autoFarm2ArmorSlot1;
               }
               if (loaded.autoFarm2ArmorSlot2 != null) {
                  general.autoFarm2ArmorSlot2 = loaded.autoFarm2ArmorSlot2;
               }
               if (loaded.autoFarm2ArmorSlot3 != null) {
                  general.autoFarm2ArmorSlot3 = loaded.autoFarm2ArmorSlot3;
               }
               if (loaded.autoFarm2Cycle1 != null) {
                  general.autoFarm2Cycle1 = loaded.autoFarm2Cycle1;
               }
               if (loaded.autoFarm2Cycle2 != null) {
                  general.autoFarm2Cycle2 = loaded.autoFarm2Cycle2;
               }
               if (loaded.autoFarm2Cycle3 != null) {
                  general.autoFarm2Cycle3 = loaded.autoFarm2Cycle3;
               }
               if (loaded.autoFarm2Profiles != null) {
                  general.autoFarm2Profiles = loaded.autoFarm2Profiles;
               }
               if (loaded.autoFarm2ActiveProfile != null) {
                  general.autoFarm2ActiveProfile = loaded.autoFarm2ActiveProfile;
               }
               if (loaded.nodeActiveProfile != null) {
                  general.nodeActiveProfile = loaded.nodeActiveProfile;
               }
               ensureAutoFarm2Profile();
               applyAutoFarm2Profile(general.autoFarm2ActiveProfile);
            }
         } else {
            save();
         }
      } catch (Exception var2) {
         System.err.println("[pgs_addons] Failed to load config: " + var2);
      }
   }

   public static class Config {
      public boolean autofish = true;
      public int autofishRange = 30;
      public boolean autofishJumpBeforeCatch = false;
      public boolean frozenBlazeFishingEnabled = false;
      public boolean lotusWormholeDetectorEnabled = true;
      public boolean lotusWormholeTracersEnabled = true;
      public int lotusWormholeColor = 0x55FFFF;
      public boolean iHateDioriteEnabled = true;
      public boolean pestEspEnabled = true;
      public int mobEspColorIndex = 0;
      public boolean pestEspTracersEnabled = true;
      public int pestEspColor = Settings.colorFromIndex(0);
      public boolean pestTimersEnabled = true;
      public int pestTimersX = 10;
      public int pestTimersY = 200;
      public boolean witherDoorEspEnabled = true;
      public boolean witherDoorEspTracersEnabled = true;
      public boolean bloodDoorEspEnabled = true;
      public boolean bloodDoorEspTracersEnabled = true;
      public boolean keyHighlightEnabled = true;
      public boolean keyHighlightTracersEnabled = true;
      public boolean noTerminatorSwingEnabled = true;
      public boolean showOwnNametag = false;
      public boolean stopSwimmingEnabled = false;
      public int menuColor = 0x555555;
      public boolean starredMobEspEnabled = true;
      public boolean starredMobEspTracersEnabled = true;
      public int starredMobEspColorIndex = 0;
      public int starredMobEspColor = Settings.colorFromIndex(0);
      public boolean deployablesTrackerEnabled = true;
      public boolean alertOnTotemExpiresSoon = true;
      public boolean alertOnBlackHoleExpiresSoon = true;
      public boolean slotSwapEnabled = false;
      public boolean slotSwapRecordMode = false;
      public List<Integer> savedSwapSlots = new ArrayList<>();
      public boolean alertOnFlareExpiresSoon = true;
      public boolean remainingTimeTotem = true;
      public boolean remainingTimeBlackHole = true;
      public boolean remainingTimeFlare = true;
      public int deployablesOverlayX = 10;
      public int deployablesOverlayY = 10;
      public boolean autofishWithKillerEnabled = false;
      public int autofishRodSlot = 0; // 1-9 (interactor) vs 0-8 (internal)
      public int killingItemSlot = 1; // 1-9
      public int killingSwingCount = 1; // 1-5
      public boolean macroCheckEnabled = false;
      public String macroCheckAlertText = "MACRO CHECK!";
      public int notepadRenderMode = 0;
      public String notepadText = "";
      public int notepadX = 20;
      public int notepadY = 20;
      public int notepadWidth = 180;
      public int notepadHeight = 120;
      public int nodeRenderMode = 0;
      public boolean zeroTickHardstoneEnabled = false;
      public boolean ChestHighlightEnabled = true;
      public boolean chestHighlightTracersEnabled = true;
      public boolean powderChestHudEnabled = true;
      public int powderChestHudX = 10;
      public int powderChestHudY = 160;
      public boolean autoHarpEnabled = false;
      public int autoHarpCooldown = 0;
      public String timerDuration = "2m 30s";
      public String timerCommand = "";
      public int timerHudX = 10;
      public int timerHudY = 220;
      public boolean slotSwapHudEnabled = false;
      public int slotSwapHudX = 10;
      public int slotSwapHudY = 100;
      public boolean equipmentStatsHudEnabled = false;
      public int equipmentStatsHudX = 10;
      public int equipmentStatsHudY = 140;
      public boolean arrowTypeTrackerEnabled = false;
      public boolean tpMazeTracerEnabled = false;
      public int arrowTypeTrackerX = 10;
      public int arrowTypeTrackerY = 180;
      public boolean minireenasOverlayEnabled = true;
      public boolean pinglessMiningEnabled = false;
      public boolean pinglessMiningDebugEnabled = false;
      public boolean tpsSyncEnabled = true;
      public int miningTickOverride = 1;
      public float tpsSyncMinimumTps = 1.0F;
      public float tpsSyncMaximumTps = 20.0F;
      public String extraOreSpeed = "";
      public String extraBlockSpeed = "";
      @SerializedName(value = "extraGemstoneSpeed", alternate = {"professionalGemstoneSpeed"})
      public String extraGemstoneSpeed = "";
      @SerializedName(value = "extraDwarvenMetalSpeed", alternate = {"strongArmDwarvenMetalSpeed"})
      public String extraDwarvenMetalSpeed = "";
      public boolean littlefootEspEnabled = false;
      public boolean littlefootEspTracersEnabled = true;
      public int littlefootEspColorIndex = 0;
      public int littlefootEspColor = Settings.colorFromIndex(0);
      public boolean customEspEnabled = false;
      public boolean customEspTracersEnabled = true;
      public int customEspColorIndex = 0;
      public int customEspColor = Settings.colorFromIndex(0);
      public String customEspNames = "";
      public boolean autoSellEnabled = false;
      public String autoSellNames = "";
      public boolean autoFarm2Enabled = false;
      @SerializedName(value = "attackDestroyToggleMode", alternate = {"autoFarm2ToggleAttackMode"})
      public boolean attackDestroyToggleMode = false;
      public int autoFarm2HoeSlot = 1;
      public int autoFarm2MousematSlot = 2;
      public int autoFarm2RodSlot = 3;
      public int autoFarm2VacuumSlot = 4;
      public int autoFarm2SpraySlot = 5;
      public int autoFarm2PestSpawnOffsetSeconds = 0;
      public String autoFarm2ActiveProfile = "Default";
      public String nodeActiveProfile = "Default";
      public Map<String, AutoFarm2Profile> autoFarm2Profiles = new LinkedHashMap<>();
      public String autoFarm2PlotName = "";
      public String autoFarm2ArmorSlot1 = "";
      public String autoFarm2ArmorSlot2 = "";
      public String autoFarm2ArmorSlot3 = "";
      public List<String> autoFarm2Cycle1 = new ArrayList<>(List.of("START_FARM", "START_MOVEMENT"));
      public List<String> autoFarm2Cycle2 = new ArrayList<>(List.of("STOP_FARM", "SLOT_SWAP", "INTERACT_MOUSEMAT", "TPTOPLOT", "HOLD_HOE", "START_FARM", "START_MOVEMENT"));
      public List<String> autoFarm2Cycle3 = new ArrayList<>(List.of("STOP_MOVEMENT", "STOP_FARM", "INTERACT_VACUUM_UNTIL_0_PESTS", "WARP_SPAWN", "HOLD_HOE", "REPEAT"));

   }

   public static class AutoFarm2Profile {
      public boolean toggleAttackMode = false;
      public int hoeSlot = 1;
      public int mousematSlot = 2;
      public int rodSlot = 3;
      public int vacuumSlot = 4;
      public int spraySlot = 5;
      public int pestSpawnOffsetSeconds = 0;
      public String plotName = "";
      public String armorSlot1 = "";
      public String armorSlot2 = "";
      public String armorSlot3 = "";
      public List<String> cycle1 = new ArrayList<>(List.of("START_FARM", "START_MOVEMENT"));
      public List<String> cycle2 = new ArrayList<>(List.of("STOP_FARM", "SLOT_SWAP", "INTERACT_MOUSEMAT", "TPTOPLOT", "HOLD_HOE", "START_FARM", "START_MOVEMENT"));
      public List<String> cycle3 = new ArrayList<>(List.of("STOP_MOVEMENT", "STOP_FARM", "INTERACT_VACUUM_UNTIL_0_PESTS", "WARP_SPAWN", "HOLD_HOE", "REPEAT"));
   }

   public static void switchAutoFarm2Profile(String profileName) {
      saveCurrentAutoFarm2Profile();
      general.autoFarm2ActiveProfile = normalizeProfileName(profileName);
      ensureAutoFarm2Profile();
      applyAutoFarm2Profile(general.autoFarm2ActiveProfile);
      save();
   }

   public static boolean renameAutoFarm2Profile(String profileName) {
      String oldName = normalizeProfileName(general.autoFarm2ActiveProfile);
      String newName = normalizeProfileName(profileName);
      if (oldName.equals(newName)) return true;
      if (general.autoFarm2Profiles.containsKey(newName)) return false;

      saveCurrentAutoFarm2Profile();
      AutoFarm2Profile profile = general.autoFarm2Profiles.remove(oldName);
      if (profile == null) {
         profile = snapshotAutoFarm2Profile();
      }
      general.autoFarm2Profiles.put(newName, profile);
      general.autoFarm2ActiveProfile = newName;
      applyAutoFarm2Profile(newName);
      save();
      return true;
   }

   public static void saveCurrentAutoFarm2Profile() {
      general.autoFarm2Profiles.put(normalizeProfileName(general.autoFarm2ActiveProfile), snapshotAutoFarm2Profile());
   }

   private static void ensureAutoFarm2Profile() {
      String profileName = normalizeProfileName(general.autoFarm2ActiveProfile);
      general.autoFarm2ActiveProfile = profileName;
      if (!general.autoFarm2Profiles.containsKey(profileName)) {
         general.autoFarm2Profiles.put(profileName, snapshotAutoFarm2Profile());
      }
   }

   private static AutoFarm2Profile snapshotAutoFarm2Profile() {
      AutoFarm2Profile profile = new AutoFarm2Profile();
      profile.hoeSlot = general.autoFarm2HoeSlot;
      profile.mousematSlot = general.autoFarm2MousematSlot;
      profile.rodSlot = general.autoFarm2RodSlot;
      profile.vacuumSlot = general.autoFarm2VacuumSlot;
      profile.spraySlot = general.autoFarm2SpraySlot;
      profile.pestSpawnOffsetSeconds = general.autoFarm2PestSpawnOffsetSeconds;
      profile.plotName = general.autoFarm2PlotName;
      profile.armorSlot1 = general.autoFarm2ArmorSlot1;
      profile.armorSlot2 = general.autoFarm2ArmorSlot2;
      profile.armorSlot3 = general.autoFarm2ArmorSlot3;
      profile.cycle1 = new ArrayList<>(general.autoFarm2Cycle1);
      profile.cycle2 = new ArrayList<>(general.autoFarm2Cycle2);
      profile.cycle3 = new ArrayList<>(general.autoFarm2Cycle3);
      return profile;
   }

   private static void applyAutoFarm2Profile(String profileName) {
      AutoFarm2Profile profile = general.autoFarm2Profiles.get(normalizeProfileName(profileName));
      if (profile == null) return;
      general.autoFarm2HoeSlot = clamp(profile.hoeSlot, 1, 9);
      general.autoFarm2MousematSlot = clamp(profile.mousematSlot, 1, 9);
      general.autoFarm2RodSlot = clamp(profile.rodSlot, 1, 9);
      general.autoFarm2VacuumSlot = clamp(profile.vacuumSlot, 1, 9);
      general.autoFarm2SpraySlot = clamp(profile.spraySlot, 1, 9);
      general.autoFarm2PestSpawnOffsetSeconds = Math.max(0, profile.pestSpawnOffsetSeconds);
      general.autoFarm2PlotName = profile.plotName != null ? profile.plotName : "";
      general.autoFarm2ArmorSlot1 = profile.armorSlot1 != null ? profile.armorSlot1 : "";
      general.autoFarm2ArmorSlot2 = profile.armorSlot2 != null ? profile.armorSlot2 : "";
      general.autoFarm2ArmorSlot3 = profile.armorSlot3 != null ? profile.armorSlot3 : "";
      general.autoFarm2Cycle1 = profile.cycle1 != null ? new ArrayList<>(profile.cycle1) : new ArrayList<>(List.of("START_FARM", "START_MOVEMENT"));
      general.autoFarm2Cycle2 = profile.cycle2 != null ? new ArrayList<>(profile.cycle2) : new ArrayList<>(List.of("STOP_FARM", "SLOT_SWAP", "INTERACT_MOUSEMAT", "TPTOPLOT", "HOLD_HOE", "START_FARM", "START_MOVEMENT"));
      general.autoFarm2Cycle3 = profile.cycle3 != null ? new ArrayList<>(profile.cycle3) : new ArrayList<>(List.of("STOP_MOVEMENT", "STOP_FARM", "INTERACT_VACUUM_UNTIL_0_PESTS", "WARP_SPAWN", "HOLD_HOE", "REPEAT"));
   }

   private static String normalizeProfileName(String profileName) {
      if (profileName == null || profileName.isBlank()) return "Default";
      return profileName.trim();
   }

   public static int colorFromIndex(int index) {
      float[][] colors = {
              {1.0f, 0.4f, 0.7f},
              {1.0f, 0.25f, 0.25f},
              {0.25f, 1.0f, 0.25f},
              {0.25f, 0.55f, 1.0f},
              {1.0f, 0.9f, 0.2f},
              {0.2f, 1.0f, 1.0f},
              {1.0f, 0.6f, 0.1f},
              {1.0f, 1.0f, 1.0f},
              {0.65f, 0.25f, 1.0f},
              {0.0f, 0.0f, 0.0f},
              {0.55f, 0.55f, 0.55f},
              {0.6f, 1.0f, 0.1f},
              {0.0f, 0.15f, 0.8f},
              {1.0f, 0.0f, 1.0f}
      };
      int clamped = clamp(index, 0, colors.length - 1);
      int r = clamp(Math.round(colors[clamped][0] * 255.0F), 0, 255);
      int g = clamp(Math.round(colors[clamped][1] * 255.0F), 0, 255);
      int b = clamp(Math.round(colors[clamped][2] * 255.0F), 0, 255);
      return (r << 16) | (g << 8) | b;
   }

   public static float colorRed(int rgb) {
      return ((rgb >> 16) & 255) / 255.0F;
   }

   public static float colorGreen(int rgb) {
      return ((rgb >> 8) & 255) / 255.0F;
   }

   public static float colorBlue(int rgb) {
      return (rgb & 255) / 255.0F;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
