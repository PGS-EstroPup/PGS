package com.pgs.pgsaddons.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;

/**
 * Custom render pipelines for ESP rendering (through-wall visibility).
 * Ported from Odin's CustomRenderPipelines to Java.
 */
public final class EspRenderPipelines {

        /** Lines with NO depth test — visible through walls */
        public static final RenderPipeline LINE_LIST_ESP = RenderPipelines.register(
                        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                                        .withLocation("pipeline/lines")
                                        .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL,
                                                        VertexFormat.DrawMode.LINES)
                                        .withCull(false)
                                        .withBlend(BlendFunction.TRANSLUCENT)
                                        .withDepthWrite(false)
                                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                        .build());

        /** Filled triangles with NO depth test — visible through walls */
        public static final RenderPipeline FILLED_ESP = RenderPipelines.register(
                        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                                        .withLocation("pipeline/debug_filled_box")
                                        .withCull(false)
                                        .withVertexFormat(VertexFormats.POSITION_COLOR,
                                                        VertexFormat.DrawMode.TRIANGLE_STRIP)
                                        .withDepthWrite(false)
                                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                        .withBlend(BlendFunction.TRANSLUCENT)
                                        .build());

        private EspRenderPipelines() {
        }
}
