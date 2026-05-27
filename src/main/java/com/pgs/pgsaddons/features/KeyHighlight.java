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
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Key Highlight: Highlights Wither Keys and Blood Keys in dungeons.
 * Based on OdinClient's KeyHighlight.
 */
public class KeyHighlight {

    // Wither Key Color: Black/Dark Gray
    private static final float WITHER_R = 0.1f;
    private static final float WITHER_G = 0.1f;
    private static final float WITHER_B = 0.1f;

    // Blood Key Color: Red
    private static final float BLOOD_R = 1.0f;
    private static final float BLOOD_G = 0.0f;
    private static final float BLOOD_B = 0.0f;

    private static final float WIRE_ALPHA = 1.0f;
    private static final float FILL_ALPHA = 0.2f;

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(KeyHighlight::onRenderWorld);
    }

    private static void onRenderWorld(WorldRenderContext context) {
        if (!Settings.general.keyHighlightEnabled)
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

        List<Entity> witherKeys = new ArrayList<>();
        List<Entity> bloodKeys = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity) {
                String name = entity.getDisplayName().getString();
                // Check for key names. Ideally use formatted check if possible, or strict
                // string match.
                // Odin logic checks for "Wither Key" or "Blood Key".
                if (name.contains("Wither Key")) {
                    witherKeys.add(entity);
                } else if (name.contains("Blood Key")) {
                    bloodKeys.add(entity);
                }
            }
        }

        if (witherKeys.isEmpty() && bloodKeys.isEmpty())
            return;

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // Draw wireframes
        VertexConsumer lineBuffer = context.consumers().getBuffer(EspRenderLayers.LINE_LIST_ESP); // Through walls

        for (Entity key : witherKeys) {
            Box renderBox = getRenderBox(key, tickDelta);
            if (Settings.general.keyHighlightTracersEnabled) {
                EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, renderBox.getCenter(), WITHER_R, WITHER_G, WITHER_B, WIRE_ALPHA);
            }
            drawBox(matrices, lineBuffer, renderBox, WITHER_R, WITHER_G, WITHER_B, true);
        }
        for (Entity key : bloodKeys) {
            Box renderBox = getRenderBox(key, tickDelta);
            if (Settings.general.keyHighlightTracersEnabled) {
                EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, renderBox.getCenter(), BLOOD_R, BLOOD_G, BLOOD_B, WIRE_ALPHA);
            }
            drawBox(matrices, lineBuffer, renderBox, BLOOD_R, BLOOD_G, BLOOD_B, true);
        }

        // Draw filled boxes
        VertexConsumer fillBuffer = context.consumers().getBuffer(EspRenderLayers.FILLED_ESP); // Through walls

        for (Entity key : witherKeys) {
            drawBox(matrices, fillBuffer, getRenderBox(key, tickDelta), WITHER_R, WITHER_G, WITHER_B, false);
        }
        for (Entity key : bloodKeys) {
            drawBox(matrices, fillBuffer, getRenderBox(key, tickDelta), BLOOD_R, BLOOD_G, BLOOD_B, false);
        }

        matrices.pop();
    }

    private static Box getRenderBox(Entity entity, float tickDelta) {
        Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
        // ArmorStand is the key itself usually (text + head).
        // Odin draws a small box around it.
        // Let's draw a box around the entity.
        double halfW = 0.4; // Fixed size for keys usually looks better than entity width
        double h = 0.8;

        // Offset slightly up to center on the head/item
        return new Box(
                lerpedPos.x - halfW, lerpedPos.y + 1.0, lerpedPos.z - halfW,
                lerpedPos.x + halfW, lerpedPos.y + 1.0 + h, lerpedPos.z + halfW);
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer buffer, Box renderBox, float r,
            float g, float b, boolean wireframe) {
        if (wireframe) {
            EspRenderer.drawWireframeBox(matrices.peek(), buffer, renderBox, r, g, b, WIRE_ALPHA);
        } else {
            EspRenderer.drawFilledBox(matrices.peek(), buffer, renderBox, r, g, b, FILL_ALPHA);
        }
    }
}
