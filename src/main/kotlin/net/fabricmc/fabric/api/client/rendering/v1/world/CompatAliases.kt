package net.fabricmc.fabric.api.client.rendering.v1.world

typealias WorldRenderContext = net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext

object WorldRenderEvents {
    val AFTER_ENTITIES = net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN
    val END = net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.END_MAIN
}
