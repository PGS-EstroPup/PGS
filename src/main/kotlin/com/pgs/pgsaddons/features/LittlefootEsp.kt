package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.util.math.Box

object LittlefootEsp {

    private const val WIRE_ALPHA = 1.0f
    private const val FILL_ALPHA = 0.15f

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            render(context)
        }
    }

    private fun render(context: WorldRenderContext) {
        if (!Settings.general.littlefootEspEnabled) return
        if (!LocationUtils.isInMineshaft()) return

        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        if (client.player == null) return

        val r = Settings.colorRed(Settings.general.littlefootEspColor)
        val g = Settings.colorGreen(Settings.general.littlefootEspColor)
        val b = Settings.colorBlue(Settings.general.littlefootEspColor)

        val targets = world.entities.filter(::isLittlefoot)
        if (targets.isEmpty()) return

        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return
        val cameraObject = client.gameRenderer.camera
        val camera = cameraObject.cameraPos
        val tickDelta = client.renderTickCounter.getTickProgress(false)

        matrices.push()
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z)

            val lineBuffer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)
            for (entity in targets) {
                val renderBox = getRenderBox(entity, tickDelta)
                if (Settings.general.littlefootEspTracersEnabled) {
                    EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, renderBox.center, r, g, b, WIRE_ALPHA)
                }
                EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, renderBox, r, g, b, WIRE_ALPHA)
            }

            val fillBuffer = consumers.getBuffer(EspRenderLayers.FILLED_ESP)
            for (entity in targets) {
                EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, getRenderBox(entity, tickDelta), r, g, b, FILL_ALPHA)
            }
        } finally {
            matrices.pop()
        }
    }

    private fun isLittlefoot(entity: Entity): Boolean {
        if (entity.isRemoved || entity is ArmorStandEntity || entity !is LivingEntity) return false
        return entity.displayName?.string?.contains("Littlefoot", ignoreCase = true) == true ||
                entity.name.string.contains("Littlefoot", ignoreCase = true)
    }

    private fun getRenderBox(entity: Entity, tickDelta: Float): Box {
        val pos = entity.getLerpedPos(tickDelta)
        val halfWidth = entity.width / 2.0 + 0.1
        val height = entity.height + 0.1
        return Box(
            pos.x - halfWidth,
            pos.y - 0.05,
            pos.z - halfWidth,
            pos.x + halfWidth,
            pos.y + height,
            pos.z + halfWidth
        )
    }
}
