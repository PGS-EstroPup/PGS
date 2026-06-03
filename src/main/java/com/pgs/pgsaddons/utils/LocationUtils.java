package com.pgs.pgsaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public final class LocationUtils {
    private static final long LOCATION_CACHE_NANOS = 500_000_000L;
    private static String cachedLocationInfo = "";
    private static long lastLocationUpdateNanos = 0L;

    private LocationUtils() {
    }

    public static void resetLocationCache() {
        cachedLocationInfo = "";
        lastLocationUpdateNanos = 0L;
    }

    public static boolean isInGarden() {
        return hasLocationText("Garden", "Plot", "Pest", "Pests", "Composter", "Visitors");
    }

    public static boolean isInCrystalHollows() {
        return hasLocationText("Crystal Hollows");
    }

    public static boolean isInMineshaft() {
        return hasLocationText("Mineshaft");
    }

    public static boolean isInDwarvenMines() {
        return hasLocationText("Dwarven Mines", "DwarvenMines");
    }

    public static boolean isInTheEnd() {
        return hasLocationText("The End", "TheEnd");
    }

    public static boolean isInLotusAtoll() {
        return hasLocationText("Lotus Atoll", "LotusAtoll");
    }

    public static boolean isInDungeon() {
        String info = getCurrentLocationInfo();
        return containsAny(info, "Catacombs", "Dungeon", "The Catacombs");
    }

    public static List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return lines;

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) return lines;

        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return lines;

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            Component display = entry.display();
            Component ownerName = entry.ownerName();
            String line = display != null ? display.getString() : ownerName != null ? ownerName.getString() : entry.owner();
            lines.add(stripFormatting(line));
        }

        return lines;
    }

    private static boolean hasLocationText(String... needles) {
        return containsAny(getCurrentLocationInfo(), needles);
    }

    private static String getCurrentLocationInfo() {
        long now = System.nanoTime();
        if (now - lastLocationUpdateNanos < LOCATION_CACHE_NANOS) {
            return cachedLocationInfo;
        }

        lastLocationUpdateNanos = now;
        cachedLocationInfo = readCurrentLocationInfo();
        return cachedLocationInfo;
    }

    private static String readCurrentLocationInfo() {
        StringBuilder info = new StringBuilder();

        String tabLocation = getTabLocationInfo();
        if (!tabLocation.isEmpty()) {
            info.append(tabLocation).append('\n');
        }

        for (String line : getScoreboardLines()) {
            info.append(stripFormatting(line)).append('\n');
        }

        return info.toString();
    }

    private static String getTabLocationInfo() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection == null) return "";

        for (PlayerInfo entry : connection.getListedOnlinePlayers()) {
            Component displayName = entry.getTabListDisplayName();
            if (displayName == null) continue;

            String text = stripFormatting(displayName.getString());
            if (text.startsWith("Area: ") || text.contains("Area: ") ||
                    text.startsWith("Dungeon: ") || text.contains("Dungeon: ")) {
                return text;
            }
        }

        return "";
    }

    private static boolean containsAny(String haystack, String... needles) {
        String normalizedHaystack = normalizeLocationText(haystack);
        for (String needle : needles) {
            if (normalizedHaystack.contains(normalizeLocationText(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeLocationText(String text) {
        return stripFormatting(text).toLowerCase().replace(" ", "").replace("_", "");
    }

    private static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "").replace("Ã‚", "");
    }
}
