package com.pgs.pgsaddons.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility to detect the current Skyblock island from the tab list.
 * Hypixel shows "Area: Garden", "Area: Hub", etc. in tab list entries.
 */
public class LocationUtils {
    private static final long LOCATION_CACHE_NANOS = 500_000_000L;
    private static String cachedLocationInfo = "";
    private static long lastLocationUpdateNanos = 0L;

    /**
     * Returns true if the player is currently on the Garden island.
     * Checks tab list entries for "Area: Garden".
     */
    public static boolean isInGarden() {
        return hasLocationText("Garden");
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

    /**
     * Returns true if the player is currently in the Crystal Hollows.
     * Checks tab list entries and scoreboard lines for the text.
     */
    public static boolean isInCrystalHollows() {
        return hasLocationText("Crystal Hollows");
    }

    /**
     * Returns true if the player is currently in a Catacombs dungeon.
     * Checks tab list entries and scoreboard lines for dungeon-related text.
     */
    public static boolean isInDungeon() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
            return false;

        if (client.world.getScoreboard() != null) {
            ScoreboardObjective objective = client.world.getScoreboard()
                    .getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                String title = stripFormatting(objective.getDisplayName().getString());
                if (title.contains("Catacombs") || title.contains("Dungeon")) {
                    return true;
                }
            }
        }

        try {
            for (String line : getScoreboardLines()) {
                String cleanLine = stripFormatting(line);
                if (cleanLine.contains("The Catacombs") || cleanLine.contains("Dungeon")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        String locInfo = getTabLocationInfo();
        return locInfo.contains("Catacombs") || locInfo.contains("Dungeon");
    }

    /**
     * Reads the lines from the sidebar scoreboard.
     */
    public static List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
            return lines;

        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null)
            return lines;

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null)
            return lines;

        Collection<ScoreHolder> holders = scoreboard.getKnownScoreHolders();
        List<ScoreInfo> scoreInfos = new ArrayList<>();

        for (ScoreHolder holder : holders) {
            if (holder.getNameForScoreboard().startsWith("#"))
                continue;

            ReadableScoreboardScore scoreObj = scoreboard.getScore(holder, objective);
            int scoreVal = scoreObj.getScore();

            scoreInfos.add(new ScoreInfo(holder, scoreVal));
        }

        scoreInfos.sort((s1, s2) -> Integer.compare(s2.score, s1.score));

        if (scoreInfos.size() > 15) {
            scoreInfos = scoreInfos.subList(0, 15);
        }

        for (ScoreInfo info : scoreInfos) {
            Team team = scoreboard.getScoreHolderTeam(info.holder.getNameForScoreboard());
            Text text = Team.decorateName(team, Text.literal(info.holder.getNameForScoreboard()));
            lines.add(text.getString());
        }

        return lines;
    }

    private static class ScoreInfo {
        final ScoreHolder holder;
        final int score;

        ScoreInfo(ScoreHolder holder, int score) {
            this.holder = holder;
            this.score = score;
        }
    }

    /**
     * Reads the "Area: ..." or "Dungeon: ..." text from the tab list.
     * Returns the area string, or empty if not found.
     */
    private static String getTabLocationInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null)
            return "";

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        Collection<PlayerListEntry> entries = handler.getPlayerList();

        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null)
                continue;

            String text = stripFormatting(displayName.getString());
            if (text.startsWith("Area: ") || text.contains("Area: ") ||
                    text.startsWith("Dungeon: ") || text.contains("Dungeon: ")) {
                return text;
            }
        }
        return "";
    }

    private static boolean hasLocationText(String... needles) {
        return containsAny(getCurrentLocationInfo(), needles);
    }

    public static void resetLocationCache() {
        cachedLocationInfo = "";
        lastLocationUpdateNanos = 0L;
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
        String tabLocation = stripFormatting(getTabLocationInfo());
        if (!tabLocation.isEmpty()) {
            return tabLocation;
        }

        try {
            for (String line : getScoreboardLines()) {
                String cleanLine = stripFormatting(line);
                if (cleanLine.contains("Crystal Hollows") ||
                        cleanLine.contains("Dwarven Mines") ||
                        cleanLine.contains("Mineshaft") ||
                        cleanLine.contains("The End") ||
                        cleanLine.contains("Garden")) {
                    return cleanLine;
                }
            }
        } catch (Exception e) {
            // Ignore errors
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
        return stripFormatting(text)
                .toLowerCase()
                .replace(" ", "")
                .replace("_", "");
    }

    private static String stripFormatting(String text) {
        if (text == null)
            return "";
        return text.replaceAll("\\u00A7[0-9A-FK-ORa-fk-or]", "").replace("Â", "");
    }
}
