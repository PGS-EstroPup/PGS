package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import com.pgs.pgsaddons.utils.DeployableList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeployablesTracker {
    private static final int HEADER_HEIGHT = 20;
    private static final int PADDING = 6;
    private static final int SECONDS_BEFORE_EXPIRATION = 10;
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("\u00A7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMER_PATTERN = Pattern.compile("(?:(\\d+)m\\s*)?(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Map<String, ActiveDeployable> activeDeployables = new LinkedHashMap<>();
    private static int tickCounter = 0;

    private record DeployableInfo(String name, String lookupName, int range, int countdownSeconds, boolean flare) {
    }

    private static class ActiveDeployable {
        final String name;
        final int range;
        final boolean flare;
        Vec3 center;
        int remainingSeconds;
        Long lastAlertAt = null;
        boolean seenThisScan = false;
        boolean sawTimerThisScan = false;

        ActiveDeployable(DeployableInfo info, Vec3 center) {
            this.name = info.name();
            this.range = info.range();
            this.flare = info.flare();
            this.center = center;
            this.remainingSeconds = info.countdownSeconds();
        }
    }

    private static final List<DeployableInfo> DEPLOYABLES = List.of(
            info("Radiant", false),
            info("Manaflux", false),
            info("Overflux", false),
            info("Plasmaflux", false),
            info("Warning", true),
            info("Alert", true),
            info("S.O.S", "SOS", true),
            info("Umbrella", false),
            info("Lantern", false),
            info("Will-o'-wisp", false),
            info("Black Hole", false),
            info("Totem of Corruption", false)
    );

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(DeployablesTracker::onTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("pgs_addons", "deployables_tracker"),
                (context, tickCounter) -> {
                    if (Settings.general.deployablesTrackerEnabled) {
                        drawHud(context, false);
                    }
                }
        );
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() || !Settings.general.deployablesTrackerEnabled) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            DeployableInfo deployable = matchHeldDeployable(stripFormatting(stack.getHoverName()));
            if (deployable == null) return InteractionResult.PASS;

            addOrRefresh(deployable, Vec3.atCenterOf(hitResult.getBlockPos()), deployable.countdownSeconds());
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide() || !Settings.general.deployablesTrackerEnabled) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            DeployableInfo deployable = matchHeldDeployable(stripFormatting(stack.getHoverName()));
            if (deployable == null) return InteractionResult.PASS;

            addOrRefresh(deployable, Vec3.atCenterOf(player.blockPosition()), deployable.countdownSeconds());
            return InteractionResult.PASS;
        });
    }

    private static DeployableInfo info(String displayName, boolean flare) {
        return info(displayName, displayName, flare);
    }

    private static DeployableInfo info(String displayName, String lookupName, boolean flare) {
        int[][] values = DeployableList.INSTANCE.getDeployableRange(lookupName);
        int range = 0;
        int countdown = 0;
        if (values != null && values.length > 0 && values[0].length >= 2) {
            range = values[0][0];
            countdown = values[0][1];
        }
        return new DeployableInfo(displayName, lookupName, range, countdown, flare);
    }

    private static void onTick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (Settings.general.deployablesTrackerEnabled) {
            trackDeployablesStatus(client);
        }
    }

    private static void trackDeployablesStatus(Minecraft client) {
        if (client.player == null || client.level == null) return;

        for (ActiveDeployable deployable : activeDeployables.values()) {
            deployable.seenThisScan = false;
            deployable.sawTimerThisScan = false;
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;
            if (stand.distanceToSqr(client.player) > 512.0 * 512.0) continue;

            DeployableInfo info = matchNamedDeployable(stripFormatting(stand.getCustomName()));
            if (info == null) continue;

            Integer seconds = parseTimerSeconds(stripFormatting(stand.getCustomName()));
            addOrRefresh(info, stand.position(), seconds);
        }

        updateCountdownsAndRanges(client);
    }

    private static void addOrRefresh(DeployableInfo info, Vec3 center, Integer seconds) {
        ActiveDeployable active = activeDeployables.get(info.name());
        if (active == null) {
            active = new ActiveDeployable(info, center);
            activeDeployables.put(info.name(), active);
        }

        active.center = center;
        active.seenThisScan = true;
        active.sawTimerThisScan = seconds != null;
        if (seconds != null) {
            active.remainingSeconds = seconds;
        }
    }

    private static void updateCountdownsAndRanges(Minecraft client) {
        if (client.player == null) return;

        activeDeployables.entrySet().removeIf(entry -> {
            ActiveDeployable deployable = entry.getValue();

            if (deployable.range > 0 && deployable.center != null && client.player.position().distanceTo(deployable.center) > deployable.range) {
                return true;
            }

            if (deployable.flare || !deployable.seenThisScan || !deployable.sawTimerThisScan) {
                deployable.remainingSeconds--;
            }

            if (deployable.remainingSeconds <= 0) {
                return true;
            }

            if (deployable.remainingSeconds == SECONDS_BEFORE_EXPIRATION &&
                    (deployable.lastAlertAt == null || System.currentTimeMillis() - deployable.lastAlertAt >= 1000)) {
                playAlert(client, deployable);
            }

            return false;
        });
    }

    private static DeployableInfo matchFlare(String text) {
        if (text == null || !normalize(text).contains("flare")) return null;
        for (DeployableInfo info : DEPLOYABLES) {
            if (info.flare() && normalizedTextMatches(text, info)) {
                return info;
            }
        }
        return null;
    }

    private static DeployableInfo matchHeldDeployable(String text) {
        if (text == null) return null;
        DeployableInfo flare = matchFlare(text);
        if (flare != null) return flare;

        for (DeployableInfo info : DEPLOYABLES) {
            if (!info.flare() && normalizedTextMatches(text, info)) {
                return info;
            }
        }
        return null;
    }

    private static DeployableInfo matchNamedDeployable(String text) {
        if (text == null) return null;
        String normalizedText = normalize(text);
        for (DeployableInfo info : DEPLOYABLES) {
            if (!info.flare() && normalizedTextMatches(normalizedText, info)) {
                return info;
            }
        }
        return null;
    }

    private static boolean normalizedTextMatches(String text, DeployableInfo info) {
        String normalizedText = normalize(text);
        return normalizedText.contains(normalize(info.name())) || normalizedText.contains(normalize(info.lookupName()));
    }

    private static Integer parseTimerSeconds(String text) {
        if (text == null) return null;
        Matcher matcher = TIMER_PATTERN.matcher(text);
        if (!matcher.find()) return null;

        int minutes = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return (minutes * 60) + seconds;
    }

    private static void resetAll() {
        activeDeployables.clear();
    }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "";
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return minutes > 0 ? String.format("%02dm %02ds", minutes, seconds) : String.format("%02ds", seconds);
    }

    private static String stripFormatting(Component text) {
        if (text == null) return null;
        return FORMATTING_PATTERN.matcher(text.getString()).replaceAll("");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void playAlert(Minecraft client, ActiveDeployable deployable) {
        client.gui.setTitle(Component.literal(deployable.name + " expires soon!").withStyle(ChatFormatting.BOLD));
        client.player.sendSystemMessage(Component.literal("[PGS] " + deployable.name + " expires soon.").withStyle(ChatFormatting.GOLD));
        deployable.lastAlertAt = System.currentTimeMillis();
    }

    public static void drawHud(GuiGraphicsExtractor context, boolean mockup) {
        int x = Settings.general.deployablesOverlayX;
        int y = Settings.general.deployablesOverlayY;
        int spacing = 12;
        int count = 0;

        Collection<ActiveDeployable> deployables = activeDeployables.values();
        if (!mockup && deployables.isEmpty()) return;
        List<String> displayLines = mockup ? List.of("Alert: 02m 58s", "Overflux: 56s") : deployables.stream()
                .map(deployable -> deployable.name + ": " + formatTime(deployable.remainingSeconds))
                .filter(line -> !line.endsWith(": ") && !line.endsWith(": 00s"))
                .toList();
        if (!mockup && displayLines.isEmpty()) return;

        int lineCount = displayLines.size();
        int height = HEADER_HEIGHT + PADDING + lineCount * spacing + PADDING;
        int bodyY = drawPanel(context, x, y, hudWidth(displayLines), height, "Deployables");

        if (mockup) {
            drawLine(context, "Alert: 02m 58s", x + PADDING, bodyY, 0xFFFFFFFF);
            drawLine(context, "Overflux: 56s", x + PADDING, bodyY + spacing, 0xFFFFFFFF);
            return;
        }

        for (ActiveDeployable deployable : deployables) {
            String remainingTime = formatTime(deployable.remainingSeconds);
            if (remainingTime.isEmpty() || remainingTime.equals("00s")) continue;

            int color = deployable.remainingSeconds <= SECONDS_BEFORE_EXPIRATION ? 0xFFFF5555 : 0xFFFFFFFF;
            drawLine(context, deployable.name + ": " + remainingTime, x + PADDING, bodyY + count * spacing, color);
            count++;
        }
    }

    public static int mockupWidth() {
        return hudWidth(List.of("Alert: 02m 58s", "Overflux: 56s"));
    }

    public static int mockupHeight() {
        int lines = 2;
        return HEADER_HEIGHT + PADDING + lines * 12 + PADDING;
    }

    private static int hudWidth(Collection<String> lines) {
        int width = Minecraft.getInstance().font.width(Component.literal("Deployables"));
        for (String line : lines) {
            width = Math.max(width, Minecraft.getInstance().font.width(Component.literal(line)));
        }
        return width + PADDING * 2;
    }

    private static int drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height, String title) {
        int accent = 0xFF000000 | (Settings.general.menuColor & 0xFFFFFF);

        context.fill(x, y, x + width, y + height, 0xDD080808);
        context.fill(x, y, x + width, y + HEADER_HEIGHT, 0xEE151515);
        drawBorder(context, x, y, x + width, y + height, accent);
        drawLine(context, title, x + PADDING, y + 6, 0xFFFFFFFF);
        return y + HEADER_HEIGHT + 5;
    }

    private static void drawLine(GuiGraphicsExtractor context, String text, int x, int y, int color) {
        context.text(Minecraft.getInstance().font, Component.literal(text), x, y, color, true);
    }

    private static void drawBorder(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int color) {
        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }
}
