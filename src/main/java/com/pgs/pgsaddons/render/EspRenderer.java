package com.pgs.pgsaddons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class EspRenderer {
    private static final int[][] BOX_EDGES = {
            {0, 0, 0, 1, 0, 0},
            {1, 0, 0, 1, 0, 1},
            {1, 0, 1, 0, 0, 1},
            {0, 0, 1, 0, 0, 0},
            {0, 1, 0, 1, 1, 0},
            {1, 1, 0, 1, 1, 1},
            {1, 1, 1, 0, 1, 1},
            {0, 1, 1, 0, 1, 0},
            {0, 0, 0, 0, 1, 0},
            {1, 0, 0, 1, 1, 0},
            {1, 0, 1, 1, 1, 1},
            {0, 0, 1, 0, 1, 1}
    };

    public static void drawWireframeBox(PoseStack.Pose pose, VertexConsumer buffer, AABB aabb, float r, float g,
            float b, float a) {
        int color = color(r, g, b, a);
        float x0 = (float) aabb.minX;
        float x1 = (float) aabb.maxX;
        float y0 = (float) aabb.minY;
        float y1 = (float) aabb.maxY;
        float z0 = (float) aabb.minZ;
        float z1 = (float) aabb.maxZ;

        for (int[] edge : BOX_EDGES) {
            drawLine(
                    pose,
                    buffer,
                    edge[0] == 0 ? x0 : x1,
                    edge[1] == 0 ? y0 : y1,
                    edge[2] == 0 ? z0 : z1,
                    edge[3] == 0 ? x0 : x1,
                    edge[4] == 0 ? y0 : y1,
                    edge[5] == 0 ? z0 : z1,
                    2.0F,
                    color
            );
        }
    }

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, float r, float g,
            float b, float a) {
        drawLine(pose, buffer, (float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y,
                (float) to.z, 2.0F, color(r, g, b, a));
    }

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, float width, float r,
            float g, float b, float a) {
        drawLine(pose, buffer, (float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y,
                (float) to.z, width, color(r, g, b, a));
    }

    public static void drawTracer(PoseStack.Pose pose, VertexConsumer buffer, Camera camera, Vec3 target, float r,
            float g, float b, float a) {
        Vec3 from = crosshairWorldPoint(camera);
        drawLine(pose, buffer, from, target, r, g, b, a);
    }

    private static Vec3 crosshairWorldPoint(Camera camera) {
        Vec3 position = camera.position();
        Vector3fc forward = camera.forwardVector();
        return position.add(forward.x() * 0.5, forward.y() * 0.5, forward.z() * 0.5);
    }

    public static void drawFilledBox(PoseStack.Pose pose, VertexConsumer buffer, AABB aabb, float r, float g, float b,
            float a) {
        int color = color(r, g, b, a);
        float x1 = (float) aabb.minX;
        float x2 = (float) aabb.maxX;
        float y1 = (float) aabb.minY;
        float y2 = (float) aabb.maxY;
        float z1 = (float) aabb.minZ;
        float z2 = (float) aabb.maxZ;

        vertex(buffer, pose, x1, y1, z1, color);
        vertex(buffer, pose, x1, y1, z2, color);
        vertex(buffer, pose, x1, y2, z2, color);
        vertex(buffer, pose, x1, y2, z1, color);

        vertex(buffer, pose, x2, y1, z2, color);
        vertex(buffer, pose, x2, y1, z1, color);
        vertex(buffer, pose, x2, y2, z1, color);
        vertex(buffer, pose, x2, y2, z2, color);

        vertex(buffer, pose, x1, y1, z1, color);
        vertex(buffer, pose, x1, y2, z1, color);
        vertex(buffer, pose, x2, y2, z1, color);
        vertex(buffer, pose, x2, y1, z1, color);

        vertex(buffer, pose, x2, y1, z2, color);
        vertex(buffer, pose, x2, y2, z2, color);
        vertex(buffer, pose, x1, y2, z2, color);
        vertex(buffer, pose, x1, y1, z2, color);

        vertex(buffer, pose, x1, y1, z1, color);
        vertex(buffer, pose, x2, y1, z1, color);
        vertex(buffer, pose, x2, y1, z2, color);
        vertex(buffer, pose, x1, y1, z2, color);

        vertex(buffer, pose, x1, y2, z2, color);
        vertex(buffer, pose, x2, y2, z2, color);
        vertex(buffer, pose, x2, y2, z1, color);
        vertex(buffer, pose, x1, y2, z1, color);
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, float x1, float y1, float z1, float x2,
            float y2, float z2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = length > 0.0F ? dx / length : 0.0F;
        float ny = length > 0.0F ? dy / length : 0.0F;
        float nz = length > 0.0F ? dz / length : 0.0F;

        buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
        buffer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color) {
        buffer.addVertex(pose, x, y, z).setColor(color);
    }

    private static int color(float r, float g, float b, float a) {
        int alpha = clamp255(a);
        int red = clamp255(r);
        int green = clamp255(g);
        int blue = clamp255(b);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int clamp255(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    private EspRenderer() {
    }
}
