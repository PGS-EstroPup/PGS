package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import com.pgs.pgsaddons.utils.DeployableList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeployablesTracker {

    private static final int SECONDS_BEFORE_EXPIRATION = 10;
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("\\u00A7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMER_PATTERN = Pattern.compile("(?:(\\d+)m\\s*)?(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Map<String, ActiveDeployable> activeDeployables = new LinkedHashMap<>();
    private static int tickCounter = 0;

    private record DeployableInfo(String name, String lookupName, int range, int countdownSeconds, boolean flare) {
    }

    private static class ActiveDeployable {
        final String name;
        final int range;
        final boolean flare;
        Vec3d center;
        int remainingSeconds;
        Long lastAlertAt = null;
        boolean seenThisScan = false;
        boolean sawTimerThisScan = false;

        ActiveDeployable(DeployableInfo info, Vec3d center) {
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
        HudRenderCallback.EVENT.register(DeployablesTracker::onRenderHud);
        UseBlockCallback.EVENT.register(DeployablesTracker::onUseBlock);
        UseItemCallback.EVENT.register(DeployablesTracker::onUseItem);
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

    private static ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hitResult) {
        if (!world.isClient() || !Settings.general.deployablesTrackerEnabled) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        String itemName = stripFormatting(stack.getName());
        DeployableInfo flare = matchFlare(itemName);
        if (flare == null) return ActionResult.PASS;

        Vec3d center = Vec3d.ofCenter(hitResult.getBlockPos());
        addOrRefresh(flare, center, flare.countdownSeconds());
        return ActionResult.PASS;
    }

    private static ActionResult onUseItem(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand) {
        if (!world.isClient() || !Settings.general.deployablesTrackerEnabled) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        String itemName = stripFormatting(stack.getName());
        DeployableInfo flare = matchFlare(itemName);
        if (flare == null) return ActionResult.PASS;

        addOrRefresh(flare, Vec3d.ofCenter(player.getBlockPos()), flare.countdownSeconds());
        return ActionResult.PASS;
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null)
            return;

        tickCounter++;
        if (tickCounter < 20)
            return;
        tickCounter = 0;

        if (!Settings.general.deployablesTrackerEnabled)
            return;

        trackDeployablesStatus(client);
    }

    private static void trackDeployablesStatus(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        Box searchBox = new Box(px - 512, py - 256, pz - 512, px + 512, py + 256, pz + 512);
        List<ArmorStandEntity> armorStands = client.world.getEntitiesByType(
                TypeFilter.instanceOf(ArmorStandEntity.class),
                searchBox,
                e -> true
        );

        for (ActiveDeployable deployable : activeDeployables.values()) {
            deployable.seenThisScan = false;
            deployable.sawTimerThisScan = false;
        }

        for (ArmorStandEntity stand : armorStands) {
            String name = stripFormatting(stand.getCustomName());
            DeployableInfo info = matchNamedDeployable(name);
            if (info == null) continue;

            Integer seconds = parseTimerSeconds(name);
            addOrRefresh(info, entityPos(stand), seconds);
        }

        updateCountdownsAndRanges(client);
    }

    private static void addOrRefresh(DeployableInfo info, Vec3d center, Integer seconds) {
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

    private static void updateCountdownsAndRanges(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        activeDeployables.entrySet().removeIf(entry -> {
            ActiveDeployable deployable = entry.getValue();

            if (deployable.range > 0 && deployable.center != null && entityPos(player).distanceTo(deployable.center) > deployable.range) {
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

    private static Vec3d entityPos(net.minecraft.entity.Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    private static void resetAll() {
        activeDeployables.clear();
    }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds <= 0)
            return "";
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }

    private static String stripFormatting(Text text) {
        if (text == null) return null;
        return FORMATTING_PATTERN.matcher(text.getString()).replaceAll("");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void playAlert(MinecraftClient client, ActiveDeployable deployable) {
        client.inGameHud.setTitle(Text.literal(deployable.name + " expires soon!").formatted(Formatting.BOLD));
        client.inGameHud.setTitleTicks(2, 30, 10);
        client.player.sendMessage(Text.literal("[PGS] " + deployable.name + " expires soon.").formatted(Formatting.GOLD), false);
        deployable.lastAlertAt = System.currentTimeMillis();
    }

    private static void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!Settings.general.deployablesTrackerEnabled) return;
        drawHud(context, false);
    }

    public static void drawHud(DrawContext context, boolean mockup) {
        int x = Settings.general.deployablesOverlayX;
        int y = Settings.general.deployablesOverlayY;
        int spacing = 12;
        int count = 0;

        if (mockup) {
            context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("[HUD Editor]").formatted(Formatting.BOLD), x, y + (count * spacing), 0xFF888888, true);
            count++;
            context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("Alert: 02m 58s").formatted(Formatting.BOLD), x, y + (count * spacing), 0xFFFFFFFF, true);
            count++;
            context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("Overflux: 56s").formatted(Formatting.BOLD), x, y + (count * spacing), 0xFFFFFFFF, true);
            return;
        }

        Collection<ActiveDeployable> deployables = activeDeployables.values();
        for (ActiveDeployable deployable : deployables) {
            String remainingTime = formatTime(deployable.remainingSeconds);
            if (remainingTime.isEmpty() || remainingTime.equals("00s")) continue;

            int color = deployable.remainingSeconds <= SECONDS_BEFORE_EXPIRATION ? 0xFFFF5555 : 0xFFFFFFFF;
            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(deployable.name + ": " + remainingTime).formatted(Formatting.BOLD),
                    x,
                    y + (count * spacing),
                    color,
                    true
            );
            count++;
        }
    }
}
