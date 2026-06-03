package com.pgs.pgsaddons.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class EspRenderLayers {
    private static final DepthStencilState SEE_THROUGH_DEPTH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);

    private static final RenderPipeline LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pgs/esp_lines")
                    .withDepthStencilState(SEE_THROUGH_DEPTH)
                    .build()
    );

    private static final RenderPipeline FILLED_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("pgs/esp_filled")
                    .withCull(false)
                    .withDepthStencilState(SEE_THROUGH_DEPTH)
                    .build()
    );

    public static final RenderType LINE_LIST_ESP = RenderType.create(
            "pgs_esp_lines",
            RenderSetup.builder(LINE_PIPELINE).bufferSize(1536).createRenderSetup()
    );

    public static final RenderType FILLED_ESP = RenderType.create(
            "pgs_esp_filled",
            RenderSetup.builder(FILLED_PIPELINE).bufferSize(1536).createRenderSetup()
    );

    private EspRenderLayers() {
    }
}
