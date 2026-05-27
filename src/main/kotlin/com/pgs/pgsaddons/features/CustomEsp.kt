package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import kotlin.math.abs

object CustomEsp {
    private const val WIRE_ALPHA = 1.0f
    private const val FILL_ALPHA = 0.15f

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            render(context)
        }
    }

    private fun render(context: WorldRenderContext) {
        if (!Settings.general.customEspEnabled) return

        val targets = parseTargets(Settings.general.customEspNames)
        if (targets.isEmpty()) return

        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        if (client.player == null) return

        val r = Settings.colorRed(Settings.general.customEspColor)
        val g = Settings.colorGreen(Settings.general.customEspColor)
        val b = Settings.colorBlue(Settings.general.customEspColor)

        val matches = world.entities
            .mapNotNull { entity ->
                if (matchesTarget(entity, targets)) resolveRenderTarget(entity, world.entities) else null
            }
            .distinct()
        if (matches.isEmpty()) return

        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return
        val cameraObject = client.gameRenderer.camera
        val camera = cameraObject.cameraPos
        val tickDelta = client.renderTickCounter.getTickProgress(false)

        matrices.push()
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z)

            val lineBuffer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)
            for (entity in matches) {
                val renderBox = getRenderBox(entity, tickDelta)
                if (Settings.general.customEspTracersEnabled) {
                    EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, renderBox.center, r, g, b, WIRE_ALPHA)
                }
                EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, renderBox, r, g, b, WIRE_ALPHA)
            }

            val fillBuffer = consumers.getBuffer(EspRenderLayers.FILLED_ESP)
            for (entity in matches) {
                EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, getRenderBox(entity, tickDelta), r, g, b, FILL_ALPHA)
            }
        } finally {
            matrices.pop()
        }
    }

    private fun parseTargets(input: String): List<String> {
        return input
            .trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }

    private fun matchesTarget(entity: Entity, targets: List<String>): Boolean {
        val clientPlayer = MinecraftClient.getInstance().player
        if (entity.isRemoved || entity == clientPlayer) return false

        val names = listOf(
            entity.name.string,
            entity.displayName?.string.orEmpty(),
            entity.type.toString()
        ).map { it.lowercase() }

        return targets.any { target -> names.any { it.contains(target) } }
    }

    private fun resolveRenderTarget(entity: Entity, entities: Iterable<Entity>): Entity? {
        if (entity !is ArmorStandEntity) return entity

        val clientPlayer = MinecraftClient.getInstance().player
        val searchBox = entity.boundingBox.stretch(0.0, -3.0, 0.0).expand(1.25, 0.5, 1.25)
        return entities
            .asSequence()
            .filter { candidate ->
                candidate !== entity &&
                    candidate != clientPlayer &&
                    !candidate.isRemoved &&
                    candidate !is ArmorStandEntity &&
                    candidate.boundingBox.intersects(searchBox)
            }
            .minByOrNull { candidate ->
                val center = candidate.boundingBox.center
                val dx = center.x - entity.x
                val dz = center.z - entity.z
                dx * dx + dz * dz + abs(center.y - entity.y) * 0.25
            }
    }

    private fun getRenderBox(entity: Entity, tickDelta: Float): Box {
        val pos = entity.getLerpedPos(tickDelta)
        val offsetX = pos.x - entity.x
        val offsetY = pos.y - entity.y
        val offsetZ = pos.z - entity.z

        if (entity is ArmorStandEntity) {
            return Box(
                pos.x - 0.4,
                pos.y,
                pos.z - 0.4,
                pos.x + 0.4,
                pos.y + 2.0,
                pos.z + 0.4
            )
        }

        return entity.boundingBox
            .offset(offsetX, offsetY, offsetZ)
            .expand(0.1, 0.05, 0.1)
    }
}
