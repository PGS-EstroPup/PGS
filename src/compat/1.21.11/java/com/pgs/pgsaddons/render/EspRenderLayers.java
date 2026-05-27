package com.pgs.pgsaddons.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

/**
 * Minecraft 1.21.11 RenderLayer wrapper.
 */
public final class EspRenderLayers {
    public static final RenderLayer LINE_LIST_ESP = RenderLayer.of(
            "pgs_line_list_esp",
            RenderSetup.builder(EspRenderPipelines.LINE_LIST_ESP)
                    .expectedBufferSize(4194304)
                    .translucent()
                    .build());

    public static final RenderLayer FILLED_ESP = RenderLayer.of(
            "pgs_filled_esp",
            RenderSetup.builder(EspRenderPipelines.FILLED_ESP)
                    .expectedBufferSize(4194304)
                    .translucent()
                    .build());

    private EspRenderLayers() {
    }
}
