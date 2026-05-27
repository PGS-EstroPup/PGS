package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import com.pgs.pgsaddons.utils.LocationUtils;
import com.pgs.pgsaddons.render.EspRenderLayers;
import com.pgs.pgsaddons.render.EspRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Door ESP: scans for wither doors (coal blocks) and blood doors (red
 * terracotta)
 * in dungeons using a grid-based approach.
 * Draws merged ESP boxes around each door visible through walls.
 */
public class WitherDoorEsp {

    // Wither door: cyan
    private static final float WITHER_R = 0.0f;
    private static final float WITHER_G = 1.0f;
    private static final float WITHER_B = 1.0f;

    // Blood door: red
    private static final float BLOOD_R = 1.0f;
    private static final float BLOOD_G = 0.0f;
    private static final float BLOOD_B = 0.0f;

    private static final float WIRE_ALPHA = 1.0f;
    private static final float FILL_ALPHA = 0.18f;

    private static final int SCAN_INTERVAL = 10;

    // How long doors stay visible (milliseconds)
    private static final long DOOR_LIFETIME_MS = 2000; // 2 seconds persistence

    private static int tickCounter = 0;
    private static final List<TimedBox> witherDoorBoxes = new ArrayList<>();
    private static final List<TimedBox> bloodDoorBoxes = new ArrayList<>();
    private static ClientWorld lastWorld = null;

    // Dungeon Grid Constants
    private static final int GRID_START_X = -200;
    private static final int GRID_START_Z = -200;
    private static final int GRID_SIZE = 11;
    private static final int HALF_ROOM = 15;
    private static final int GRID_STEP = 16;

    /** A box with a creation timestamp. */
    private static class TimedBox {
        final Box box;
        long lastSeen;

        TimedBox(Box box) {
            this.box = box;
            this.lastSeen = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastSeen > DOOR_LIFETIME_MS;
        }
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(WitherDoorEsp::onRenderWorld);
    }

    private static void resetDoors() {
        witherDoorBoxes.clear();
        bloodDoorBoxes.clear();
    }

    private static void removeExpired(List<TimedBox> list) {
        list.removeIf(TimedBox::isExpired);
    }

    private static void scanDoors(World world) {
        // Iterate through the 11x11 grid
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                boolean xOdd = (x % 2 != 0);
                boolean zOdd = (z % 2 != 0);

                // XOR: We want exactly one of them to be odd (Door), not both (Pillar) or
                // neither (Room).
                if (xOdd ^ zOdd) {
                    // Calculate center coordinates
                    int rx = GRID_START_X + HALF_ROOM + x * GRID_STEP;
                    int rz = GRID_START_Z + HALF_ROOM + z * GRID_STEP;

                    // Scan Block at (rx, 69, rz)
                    BlockPos pos = new BlockPos(rx, 69, rz);
                    if (!world.isChunkLoaded(pos))
                        continue;

                    BlockStateCheck result = checkDoorBlock(world, pos);
                    if (result != BlockStateCheck.NONE) {
                        // Found a door! Determine orientation and size.
                        double doorWidth = 3.0; // Adjusted from 4.0
                        double doorThickness = 1.0;
                        double minX, minZ, maxX, maxZ;
                        double cx = rx + 0.5;
                        double cz = rz + 0.5;

                        if (xOdd) { // X is Odd -> Connection along X -> Door plane is Z-aligned (blocks X passage)
                            minX = cx - doorThickness / 2.0;
                            maxX = cx + doorThickness / 2.0;
                            minZ = cz - doorWidth / 2.0;
                            maxZ = cz + doorWidth / 2.0;
                        } else { // zOdd -> Connection along Z -> Door plane is X-aligned (blocks Z passage)
                            minX = cx - doorWidth / 2.0;
                            maxX = cx + doorWidth / 2.0;
                            minZ = cz - doorThickness / 2.0;
                            maxZ = cz + doorThickness / 2.0;
                        }

                        // Fixed height for stability
                        double minY = 69.0;
                        double maxY = 73.0; // Standard door height

                        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);

                        List<TimedBox> targetList = (result == BlockStateCheck.WITHER) ? witherDoorBoxes
                                : bloodDoorBoxes;
                        updateOrAddBox(targetList, box);
                    }
                }
            }
        }
    }

    private enum BlockStateCheck {
        NONE, WITHER, BLOOD
    }

    private static BlockStateCheck checkDoorBlock(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.COAL_BLOCK)
            return BlockStateCheck.WITHER;
        // Check for Red Terracotta (Stained Clay) or Red Stained Glass (if converted)
        // Using string check or block check.
        // 1.21 uses Red Terracotta.
        if (block == Blocks.RED_TERRACOTTA || block == Blocks.RED_STAINED_GLASS)
            return BlockStateCheck.BLOOD;
        return BlockStateCheck.NONE;
    }

    private static void updateOrAddBox(List<TimedBox> list, Box outputBox) {
        for (TimedBox tb : list) {
            // If roughly same location, update timestamp
            if (tb.box.intersects(outputBox) || tb.box.getCenter().distanceTo(outputBox.getCenter()) < 1.0) {
                tb.lastSeen = System.currentTimeMillis();
                return;
            }
        }
        list.add(new TimedBox(outputBox));
    }

    private static void onRenderWorld(WorldRenderContext context) {
        boolean witherEnabled = Settings.general.witherDoorEspEnabled;
        boolean bloodEnabled = Settings.general.bloodDoorEspEnabled;
        if (!witherEnabled && !bloodEnabled)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return;

        // Only run in Dungeons
        if (!LocationUtils.isInDungeon()) {
            resetDoors();
            return;
        }

        // Detect world change and reset
        if (client.world != lastWorld) {
            lastWorld = client.world;
            resetDoors();
        }

        // Periodic scan
        tickCounter++;
        if (tickCounter >= SCAN_INTERVAL) {
            tickCounter = 0;
            scanDoors(client.world);
            removeExpired(witherDoorBoxes);
            removeExpired(bloodDoorBoxes);
        }

        if (witherDoorBoxes.isEmpty() && bloodDoorBoxes.isEmpty())
            return;

        MatrixStack matrices = context.matrices();
        Camera cameraObject = client.gameRenderer.getCamera();
        Vec3d camera = cameraObject.getCameraPos();

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // Draw lines first
        VertexConsumer lineBuffer = context.consumers().getBuffer(EspRenderLayers.LINE_LIST_ESP);

        if (witherEnabled) {
            for (TimedBox tb : witherDoorBoxes) {
                if (Settings.general.witherDoorEspTracersEnabled) {
                    EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, tb.box.getCenter(), WITHER_R, WITHER_G,
                            WITHER_B, WIRE_ALPHA);
                }
                EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, tb.box, WITHER_R, WITHER_G, WITHER_B,
                        WIRE_ALPHA);
            }
        }
        if (bloodEnabled) {
            for (TimedBox tb : bloodDoorBoxes) {
                if (Settings.general.bloodDoorEspTracersEnabled) {
                    EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, tb.box.getCenter(), BLOOD_R, BLOOD_G,
                            BLOOD_B, WIRE_ALPHA);
                }
                EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, tb.box, BLOOD_R, BLOOD_G, BLOOD_B,
                        WIRE_ALPHA);
            }
        }

        // Draw filled boxes second
        VertexConsumer fillBuffer = context.consumers().getBuffer(EspRenderLayers.FILLED_ESP);

        if (witherEnabled) {
            for (TimedBox tb : witherDoorBoxes) {
                EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, tb.box, WITHER_R, WITHER_G, WITHER_B,
                        FILL_ALPHA);
            }
        }
        if (bloodEnabled) {
            for (TimedBox tb : bloodDoorBoxes) {
                EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, tb.box, BLOOD_R, BLOOD_G, BLOOD_B, FILL_ALPHA);
            }
        }

        matrices.pop();
    }
}
