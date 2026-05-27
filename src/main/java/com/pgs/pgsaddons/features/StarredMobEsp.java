package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import com.pgs.pgsaddons.render.EspRenderLayers;
import com.pgs.pgsaddons.render.EspRenderer;
import com.pgs.pgsaddons.utils.LocationUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StarredMobEsp {
    private static final float WIRE_ALPHA = 1.0F;
    private static final float FILL_ALPHA = 0.2F;

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(StarredMobEsp::onRenderWorld);
    }

    private static void onRenderWorld(WorldRenderContext context) {
        if (!Settings.general.starredMobEspEnabled)
            return;
        if (!LocationUtils.isInDungeon())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        MatrixStack matrices = context.matrices();
        Camera cameraObject = client.gameRenderer.getCamera();
        Vec3d camera = cameraObject.getCameraPos();
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);

        float r = Settings.colorRed(Settings.general.starredMobEspColor);
        float g = Settings.colorGreen(Settings.general.starredMobEspColor);
        float b = Settings.colorBlue(Settings.general.starredMobEspColor);

        Set<Entity> mobsToHighlight = new HashSet<>();

        // Logic from user: find "star" armor stands and highlight nearby living
        // entities
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity armorStand))
                continue;

            String unformattedName = armorStand.getName().getString();
            if (!unformattedName.contains("✯") || !unformattedName.contains("❤"))
                continue;

            // Search for nearby living entities (not players or armor stands)
            Box searchBox = armorStand.getBoundingBox().stretch(0.0, -2.0, 0.0).expand(1.0, 2.0, 1.0);
            List<Entity> nearby = client.world.getOtherEntities(armorStand, searchBox);

            Entity bestMatch = null;
            double minDist = Double.MAX_VALUE;

            for (Entity e : nearby) {
                if (e instanceof LivingEntity && !(e instanceof ArmorStandEntity)) {
                    double dist = e.distanceTo(armorStand);
                    if (dist < minDist) {
                        minDist = dist;
                        bestMatch = e;
                    }
                }
            }

            if (bestMatch != null) {
                mobsToHighlight.add(bestMatch);
            }
        }

        if (mobsToHighlight.isEmpty())
            return;

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // Wireframe pass
        VertexConsumer lineBuffer = context.consumers().getBuffer(EspRenderLayers.LINE_LIST_ESP);
        for (Entity mob : mobsToHighlight) {
            Box renderBox = getRenderBox(mob, tickDelta);
            if (Settings.general.starredMobEspTracersEnabled) {
                EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, renderBox.getCenter(), r, g, b, WIRE_ALPHA);
            }
            EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, renderBox, r, g, b, WIRE_ALPHA);
        }
        VertexConsumer fillBuffer = context.consumers().getBuffer(EspRenderLayers.FILLED_ESP);
        for (Entity mob : mobsToHighlight) {
            Box renderBox = getRenderBox(mob, tickDelta);
            EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, renderBox, r, g, b, FILL_ALPHA);
        }

        matrices.pop();
    }

    private static Box getRenderBox(Entity mob, float tickDelta) {
        Vec3d lerpedPos = mob.getLerpedPos(tickDelta);
        double halfW = (double) mob.getWidth() / 2.0;
        double h = (double) mob.getHeight();
        return new Box(
                lerpedPos.x - halfW, lerpedPos.y, lerpedPos.z - halfW,
                lerpedPos.x + halfW, lerpedPos.y + h, lerpedPos.z + halfW);
    }
}
