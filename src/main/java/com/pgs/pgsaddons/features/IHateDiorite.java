package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import com.pgs.pgsaddons.utils.LocationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IHateDiorite {

    private static final List<Pillar> pillars = new ArrayList<>();
    private static boolean initialized = false;

    private static class Pillar {
        final Set<BlockPos> blocks = new HashSet<>();
        final BlockState replacement;

        Pillar(BlockState replacement) {
            this.replacement = replacement;
        }
    }

    private static void init() {
        if (initialized)
            return;

        // Pillar 1: Lime (46, 169, 41)
        createPillar(new BlockPos(46, 169, 41), Blocks.LIME_STAINED_GLASS.getDefaultState());

        // Pillar 2: Yellow (46, 169, 65)
        createPillar(new BlockPos(46, 169, 65), Blocks.YELLOW_STAINED_GLASS.getDefaultState());

        // Pillar 3: Purple (100, 169, 65)
        createPillar(new BlockPos(100, 169, 65), Blocks.PURPLE_STAINED_GLASS.getDefaultState());

        // Pillar 4: Red (100, 169, 41)
        createPillar(new BlockPos(100, 169, 41), Blocks.RED_STAINED_GLASS.getDefaultState());

        initialized = true;
    }

    private static void createPillar(BlockPos center, BlockState replacement) {
        Pillar pillar = new Pillar(replacement);

        // Generic loop based on Odin's logic:
        // dx from center.x - 3 to center.x + 3
        // dy from center.y to center.y + 37
        // dz from center.z - 3 to center.z + 3

        int startX = center.getX() - 3;
        int endX = center.getX() + 3;

        int startY = center.getY();
        int endY = center.getY() + 37;

        int startZ = center.getZ() - 3;
        int endZ = center.getZ() + 3;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    pillar.blocks.add(new BlockPos(x, y, z));
                }
            }
        }

        pillars.add(pillar);
    }

    public static void onClientTick(MinecraftClient client) {
        if (!Settings.general.iHateDioriteEnabled) {
            return;
        }

        if (client.world == null || client.player == null) {
            return;
        }

        if (!initialized) {
            init();
        }

        if (!LocationUtils.isInDungeon()) {
            return;
        }

        // Ideally we check for F7 P2 here, but isInDungeon is our best proxy for now
        // without more complex dungeon state tracking.

        for (Pillar pillar : pillars) {
            for (BlockPos pos : pillar.blocks) {
                // Optimization: fast check if chunk is loaded
                if (client.world.isChunkLoaded(pos)) {
                    BlockState current = client.world.getBlockState(pos);
                    if (current.isOf(Blocks.DIORITE) || current.isOf(Blocks.POLISHED_DIORITE)) {
                        client.world.setBlockState(pos, pillar.replacement);
                    }
                }
            }
        }
    }
}
