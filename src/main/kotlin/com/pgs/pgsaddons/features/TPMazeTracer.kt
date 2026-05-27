package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs
import kotlin.math.atan2

object TPMazeTracer {
    private val tpPads = CopyOnWriteArraySet<BlockPos>()
    private val visited = CopyOnWriteArraySet<BlockPos>()
    private var best: BlockPos? = null

    private const val WIRE_ALPHA = 0.9f

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register(::render)
    }

    fun onPlayerPositionLook(packet: PlayerPositionLookS2CPacket) {
        if (!Settings.general.tpMazeTracerEnabled || !LocationUtils.isInDungeon()) return

        val pos = packet.change().position()
        if (!looksLikeTpMazeTeleport(pos)) return

        refreshPads(pos)
        if (tpPads.isEmpty()) return

        val playerBox = MinecraftClient.getInstance().player?.boundingBox?.expand(1.0, 0.0, 1.0)
        val teleportBox = Box(pos.x - 1.0, pos.y - 0.5, pos.z - 1.0, pos.x + 1.0, pos.y + 0.5, pos.z + 1.0)
        visited.addAll(tpPads.filter { teleportBox.intersects(Box(it)) || playerBox?.intersects(Box(it)) == true })

        val candidates = tpPads.filter { it !in visited }
        if (candidates.isEmpty()) {
            best = null
            return
        }

        val yaw = packet.change().yaw()
        val pitch = packet.change().pitch()
        val visibleCandidates = candidates.filter { isLookRayIntersectingPad(pos, yaw, pitch, it) }

        best = visibleCandidates.minByOrNull { it.getSquaredDistance(pos) }
            ?: candidates.minByOrNull { yawDistance(pos, yaw, it) }
    }

    fun reset() {
        tpPads.clear()
        visited.clear()
        best = null
    }

    private fun render(context: WorldRenderContext) {
        if (!Settings.general.tpMazeTracerEnabled || !LocationUtils.isInDungeon()) return
        val target = best ?: return

        val client = MinecraftClient.getInstance()
        val cameraObject = client.gameRenderer.camera
        val camera = cameraObject.cameraPos
        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return
        val lineBuffer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)

        matrices.push()
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z)
            val box = Box(target).expand(0.04, 0.04, 0.04)
            EspRenderer.drawTracer(matrices.peek(), lineBuffer, cameraObject, box.center.add(0.0, 0.35, 0.0), 0.0f, 1.0f, 0.25f, WIRE_ALPHA)
            EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, box, 0.0f, 1.0f, 0.25f, WIRE_ALPHA)
        } finally {
            matrices.pop()
        }
    }

    private fun refreshPads(center: Vec3d) {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val centerPos = BlockPos.ofFloored(center)

        tpPads.removeIf { world.getBlockState(it).block != Blocks.END_PORTAL_FRAME || it.getSquaredDistance(center) > 2500.0 }

        for (x in -35..35) {
            for (y in -4..4) {
                for (z in -35..35) {
                    val pos = centerPos.add(x, y, z)
                    if (world.getBlockState(pos).block == Blocks.END_PORTAL_FRAME) {
                        tpPads.add(pos.toImmutable())
                    }
                }
            }
        }
    }

    private fun looksLikeTpMazeTeleport(pos: Vec3d): Boolean {
        return abs(pos.y - 69.5) < 0.01 &&
            abs(pos.x * 2.0 - (pos.x * 2.0).toInt()) < 0.001 &&
            abs(pos.z * 2.0 - (pos.z * 2.0).toInt()) < 0.001
    }

    private fun isLookRayIntersectingPad(origin: Vec3d, yaw: Float, pitch: Float, pad: BlockPos): Boolean {
        val direction = Vec3d.fromPolar(pitch, yaw).normalize()
        val box = Box(
            pad.x - 0.75,
            pad.y.toDouble(),
            pad.z - 0.75,
            pad.x + 1.75,
            pad.y + 4.0,
            pad.z + 1.75
        )
        return box.raycast(origin, origin.add(direction.multiply(32.0))).isPresent
    }

    private fun yawDistance(origin: Vec3d, yaw: Float, pad: BlockPos): Float {
        val targetYaw = (atan2(pad.z + 0.5 - origin.z, pad.x + 0.5 - origin.x) * 180.0 / Math.PI).toFloat() - 90f
        return abs(wrapDegrees(targetYaw) - wrapDegrees(yaw))
    }

    private fun wrapDegrees(value: Float): Float {
        var wrapped = value % 360.0f
        if (wrapped >= 180.0f) wrapped -= 360.0f
        if (wrapped < -180.0f) wrapped += 360.0f
        return wrapped
    }
}
