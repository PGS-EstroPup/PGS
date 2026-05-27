package com.pgs.pgsaddons.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws wireframe and filled boxes for ESP.
 * Ported from Odin's PrimitiveRenderer to Java.
 */
public final class EspRenderer {

    // Edge pairs: each pair of indices defines one line of the wireframe.
    // 8 corners × 12 edges = 24 indices
    private static final int[] EDGES = {
            0, 1, 1, 5, 5, 4, 4, 0, // bottom face
            3, 2, 2, 6, 6, 7, 7, 3, // top face
            0, 3, 1, 2, 5, 6, 4, 7 // vertical edges
    };

    /**
     * Draws a wireframe box into the given vertex consumer.
     */
    public static void drawWireframeBox(MatrixStack.Entry pose, VertexConsumer buffer, Box aabb, float r, float g,
            float b, float a) {
        float x0 = (float) aabb.minX;
        float y0 = (float) aabb.minY;
        float z0 = (float) aabb.minZ;
        float x1 = (float) aabb.maxX;
        float y1 = (float) aabb.maxY;
        float z1 = (float) aabb.maxZ;

        float[] corners = {
                x0, y0, z0, // 0
                x1, y0, z0, // 1
                x1, y1, z0, // 2
                x0, y1, z0, // 3
                x0, y0, z1, // 4
                x1, y0, z1, // 5
                x1, y1, z1, // 6
                x0, y1, z1 // 7
        };

        for (int i = 0; i < EDGES.length; i += 2) {
            int i0 = EDGES[i] * 3;
            int i1 = EDGES[i + 1] * 3;

            float cx0 = corners[i0], cy0 = corners[i0 + 1], cz0 = corners[i0 + 2];
            float cx1 = corners[i1], cy1 = corners[i1 + 1], cz1 = corners[i1 + 2];

            float dx = cx1 - cx0;
            float dy = cy1 - cy0;
            float dz = cz1 - cz0;

            buffer.vertex(pose, cx0, cy0, cz0).color(r, g, b, a).normal(pose, dx, dy, dz);
            buffer.vertex(pose, cx1, cy1, cz1).color(r, g, b, a).normal(pose, dx, dy, dz);
        }
    }

    /**
     * Draws a single line into the given vertex consumer.
     */
    public static void drawLine(MatrixStack.Entry pose, VertexConsumer buffer, Vec3d from, Vec3d to, float r, float g,
            float b, float a) {
        float x0 = (float) from.x;
        float y0 = (float) from.y;
        float z0 = (float) from.z;
        float x1 = (float) to.x;
        float y1 = (float) to.y;
        float z1 = (float) to.z;
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;

        buffer.vertex(pose, x0, y0, z0).color(r, g, b, a).normal(pose, dx, dy, dz);
        buffer.vertex(pose, x1, y1, z1).color(r, g, b, a).normal(pose, dx, dy, dz);
    }

    public static void drawTracer(MatrixStack.Entry pose, VertexConsumer buffer, Camera camera, Vec3d target, float r,
            float g, float b, float a) {
        Vec3d cursor = camera.getCameraPos()
                .add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(0.25));
        drawLine(pose, buffer, target, cursor, r, g, b, a);
    }

    /**
     * Draws a filled box using triangle strip into the given vertex consumer.
     */
    public static void drawFilledBox(MatrixStack.Entry pose, VertexConsumer buffer, Box aabb, float r, float g, float b,
            float a) {
        Matrix4f matrix = pose.getPositionMatrix();
        float minX = (float) aabb.minX, minY = (float) aabb.minY, minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX, maxY = (float) aabb.maxY, maxZ = (float) aabb.maxZ;

        // Triangle strip vertices (same order as Odin's addChainedFilledBoxVertices)
        vertex(buffer, matrix, r, g, b, a, minX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, minY, maxZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, minX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, minZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, maxZ);
        vertex(buffer, matrix, r, g, b, a, maxX, maxY, maxZ);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float r, float g, float b, float a, float x,
            float y, float z) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
    }

    private EspRenderer() {
    }
}
