package com.pgs.pgsaddons.render;

import net.minecraft.client.render.RenderLayer;

/**
 * Minecraft 1.21.10 RenderLayer wrapper. 1.21.11 replaced this older
 * MultiPhaseParameters factory with RenderSetup.
 */
public final class EspRenderLayers {
    public static final RenderLayer LINE_LIST_ESP = RenderLayer.of(
            "pgs_line_list_esp",
            4194304,
            EspRenderPipelines.LINE_LIST_ESP,
            RenderLayer.MultiPhaseParameters.builder().build(RenderLayer.OutlineMode.NONE));

    public static final RenderLayer FILLED_ESP = RenderLayer.of(
            "pgs_filled_esp",
            4194304,
            EspRenderPipelines.FILLED_ESP,
            RenderLayer.MultiPhaseParameters.builder().build(RenderLayer.OutlineMode.NONE));

    private EspRenderLayers() {
    }
}
