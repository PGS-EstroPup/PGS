package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import com.pgs.pgsaddons.utils.LocationUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.sqrt

object LotusWormholeDetector {
    private const val DETECTION_RADIUS = 7.0
    private const val CIRCLE_SEGMENTS = 96
    private const val CIRCLE_LINE_WIDTH = 5.0f
    private const val PARTICLE_PAIR_TICKS = 20
    private const val PARTICLE_SAMPLE_TICKS = 30
    private const val MIN_CIRCLE_SAMPLES = 3
    private const val MAX_CIRCLE_SAMPLES = 120
    private const val MIN_CIRCLE_RADIUS = 1.5
    private const val MAX_CIRCLE_RADIUS = 5.5
    private const val WIRE_ALPHA = 1.0f
    private const val WATER_SCAN_RANGE = 16

    private val wormholes = listOf(
        BlockPos(13, 72, 28),
        BlockPos(78, 59, -19),
        BlockPos(83, 88, -4),
        BlockPos(98, 78, -5),
        BlockPos(25, 64, 3),
        BlockPos(16, 64, -14),
        BlockPos(54, 63, -20),
        BlockPos(67, 59, -6),
        BlockPos(37, 62, 7),
        BlockPos(27, 63, -11),
        BlockPos(52, 62, 5),
        BlockPos(73, 60, 7),
        BlockPos(13, 63, -5),
        BlockPos(41, 63, 17),
        BlockPos(37, 63, -20),
        BlockPos(17, 63, 3),
        BlockPos(62, 63, -30),
        BlockPos(43, 62, -12),
        BlockPos(92, 78, -4),
        BlockPos(67, 64, 20),
        BlockPos(25, 64, 20),
        BlockPos(36, 64, -37)
    )

    private val activeWormholes = linkedMapOf<BlockPos, ActiveWormhole>()
    private val detections = linkedMapOf<BlockPos, WormholeDetection>()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            try {
                onClientTick()
            } catch (e: RuntimeException) {
                clearDetections()
                System.err.println("[pgs_addons] Cleared Lotus wormhole detections after tick failure: $e")
            }
        }
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            try {
                render(context)
            } catch (e: RuntimeException) {
                clearDetections()
                System.err.println("[pgs_addons] Cleared Lotus wormhole detections after render failure: $e")
            }
        }
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            onGameMessage(message.string)
        }
    }

    fun onParticlePacket(packet: ClientboundLevelParticlesPacket) {
        try {
            handleParticlePacket(packet)
        } catch (e: RuntimeException) {
            clearDetections()
            System.err.println("[pgs_addons] Cleared Lotus wormhole detections after particle failure: $e")
        }
    }

    private fun handleParticlePacket(packet: ClientboundLevelParticlesPacket) {
        if (!Settings.general.lotusWormholeDetectorEnabled || !LocationUtils.isInLotusAtoll()) return
        if (packet.particle.type != ParticleTypes.PORTAL && packet.particle.type != ParticleTypes.ENCHANT) return

        val detected = nearestWormhole(packet.x, packet.y, packet.z) ?: return
        val detection = detections.getOrPut(detected) { WormholeDetection() }
        detection.addSample(packet.x, packet.z, packet.particle.type == ParticleTypes.PORTAL)

        if (!detection.hasParticlePair() || detection.sampleCount() < MIN_CIRCLE_SAMPLES) return
        val circle = detection.estimatedCircle() ?: return

        activeWormholes[detected] = ActiveWormhole(circle.centerX, circle.centerZ, circle.radius)
    }

    private fun clearDetections() {
        activeWormholes.clear()
        detections.clear()
    }

    private fun onGameMessage(rawMessage: String) {
        val message = rawMessage.replace(Regex("\\u00A7."), "").trim()
        if (message.equals("Your wormhole closed up...", ignoreCase = true) ||
            message.contains("Your wormhole closed up", ignoreCase = true)) {
            clearDetections()
        }
    }

    private fun onClientTick() {
        if (!Settings.general.lotusWormholeDetectorEnabled || !LocationUtils.isInLotusAtoll()) {
            clearDetections()
            return
        }

        tickDownDetections()
    }

    private fun tickDownDetections() {
        val iterator = detections.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.tickDown()
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
    }

    private fun nearestWormhole(x: Double, y: Double, z: Double): BlockPos? {
        val maxDistanceSquared = DETECTION_RADIUS * DETECTION_RADIUS
        return wormholes
            .asSequence()
            .map { it to distanceSquaredToBlockCenter(it, x, y, z) }
            .filter { it.second <= maxDistanceSquared }
            .minByOrNull { it.second }
            ?.first
    }

    private fun distanceSquaredToBlockCenter(pos: BlockPos, x: Double, y: Double, z: Double): Double {
        val dx = pos.x + 0.5 - x
        val dy = pos.y + 0.5 - y
        val dz = pos.z + 0.5 - z
        return dx * dx + dy * dy + dz * dz
    }

    private fun render(context: WorldRenderContext) {
        if (!Settings.general.lotusWormholeDetectorEnabled || activeWormholes.isEmpty()) return
        if (!LocationUtils.isInLotusAtoll()) return

        val client = MinecraftClient.getInstance()
        val camera = client.gameRenderer.mainCamera.cameraPos
        val cameraObject = client.gameRenderer.mainCamera
        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return

        matrices.push()
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z)
            val buffer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)
            for ((pos, active) in activeWormholes) {
                val detection = detections[pos]
                if (detection != null && detection.sampleCount() >= MIN_CIRCLE_SAMPLES) {
                    val circle = detection.estimatedCircle()
                    if (circle != null) {
                        active.centerX = circle.centerX
                        active.centerZ = circle.centerZ
                        active.radius = circle.radius
                    }
                }

                val renderBlock = BlockPos(floor(active.centerX).toInt(), pos.y, floor(active.centerZ).toInt())
                val circleY = nearestWaterTopY(renderBlock)
                drawCircle(matrices.peek(), buffer, active.centerX, circleY, active.centerZ, active.radius)
                if (Settings.general.lotusWormholeTracersEnabled) {
                    drawTracer(matrices.peek(), buffer, cameraObject, active.centerX, pos.y + 0.5, active.centerZ)
                }
            }
        } finally {
            matrices.pop()
        }
    }

    private fun nearestWaterTopY(centerBlock: BlockPos): Double {
        val world = MinecraftClient.getInstance().level ?: return centerBlock.y + 0.05
        var bestY: Int? = null
        var bestDistance = Int.MAX_VALUE

        for (offset in -WATER_SCAN_RANGE..WATER_SCAN_RANGE) {
            val y = centerBlock.y + offset
            val pos = BlockPos(centerBlock.x, y, centerBlock.z)
            if (!world.isChunkLoaded(pos)) continue
            if (world.getBlockState(pos).block != Blocks.WATER) continue

            val distance = kotlin.math.abs(offset)
            if (distance < bestDistance) {
                bestY = y
                bestDistance = distance
            }
        }

        return (bestY ?: centerBlock.y) + 1.05
    }

    private fun drawCircle(
        pose: com.mojang.blaze3d.vertex.PoseStack.Pose,
        buffer: com.mojang.blaze3d.vertex.VertexConsumer,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double
    ) {
        val color = Settings.general.lotusWormholeColor
        val r = Settings.colorRed(color)
        val g = Settings.colorGreen(color)
        val b = Settings.colorBlue(color)

        for (i in 0 until CIRCLE_SEGMENTS) {
            val angleA = Math.PI * 2.0 * i / CIRCLE_SEGMENTS
            val angleB = Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS
            val from = Vec3d(centerX + kotlin.math.cos(angleA) * radius, centerY, centerZ + kotlin.math.sin(angleA) * radius)
            val to = Vec3d(centerX + kotlin.math.cos(angleB) * radius, centerY, centerZ + kotlin.math.sin(angleB) * radius)
            EspRenderer.drawLine(pose, buffer, from, to, CIRCLE_LINE_WIDTH, r, g, b, WIRE_ALPHA)
        }
    }

    private fun drawTracer(
        pose: com.mojang.blaze3d.vertex.PoseStack.Pose,
        buffer: com.mojang.blaze3d.vertex.VertexConsumer,
        camera: net.minecraft.client.Camera,
        centerX: Double,
        centerY: Double,
        centerZ: Double
    ) {
        val color = Settings.general.lotusWormholeColor
        EspRenderer.drawTracer(
            pose,
            buffer,
            camera,
            Vec3d(centerX, centerY, centerZ),
            Settings.colorRed(color),
            Settings.colorGreen(color),
            Settings.colorBlue(color),
            WIRE_ALPHA
        )
    }

    private class WormholeDetection {
        private val samples = mutableListOf<ParticleSample?>()
        private var portalTicks = 0
        private var enchantTicks = 0
        private var sampleSumX = 0.0
        private var sampleSumZ = 0.0

        @Synchronized
        fun addSample(x: Double, z: Double, portal: Boolean) {
            addCenterSample(ParticleSample(x, z, PARTICLE_SAMPLE_TICKS))
            while (samples.size > MAX_CIRCLE_SAMPLES) {
                removeFirstCenterSample()
            }

            if (portal) {
                portalTicks = PARTICLE_PAIR_TICKS
            } else {
                enchantTicks = PARTICLE_PAIR_TICKS
            }
        }

        @Synchronized
        fun hasParticlePair(): Boolean = portalTicks > 0 && enchantTicks > 0

        @Synchronized
        fun sampleCount(): Int = samples.count { it != null }

        @Synchronized
        fun centerX(): Double = sampleSumX / sampleCount().coerceAtLeast(1)

        @Synchronized
        fun centerZ(): Double = sampleSumZ / sampleCount().coerceAtLeast(1)

        @Synchronized
        fun estimatedCircle(): Circle? {
            val sampleSnapshot = samples.filterNotNull()
            if (sampleSnapshot.size < 3) return null

            val p1 = sampleSnapshot.first()
            val p2 = sampleSnapshot.maxBy { distanceSquared(p1, it) }
            val p3 = sampleSnapshot.maxBy { triangleAreaSquared(p1, p2, it) }
            if (triangleAreaSquared(p1, p2, p3) < 0.0001) return null

            return circleFromThreePoints(p1, p2, p3)?.takeIf {
                it.radius in MIN_CIRCLE_RADIUS..MAX_CIRCLE_RADIUS
            }
        }

        @Synchronized
        fun tickDown() {
            portalTicks = (portalTicks - 1).coerceAtLeast(0)
            enchantTicks = (enchantTicks - 1).coerceAtLeast(0)

            for (index in samples.lastIndex downTo 0) {
                val sample = samples[index]
                if (sample == null) {
                    samples.removeAt(index)
                    continue
                }
                sample.ticks--
                if (sample.ticks <= 0) removeCenterSampleAt(index)
            }
        }

        @Synchronized
        fun isEmpty(): Boolean = portalTicks <= 0 && enchantTicks <= 0 && samples.isEmpty()

        private fun addCenterSample(sample: ParticleSample) {
            samples.add(sample)
            sampleSumX += sample.x
            sampleSumZ += sample.z
        }

        private fun removeFirstCenterSample(): ParticleSample? {
            val sample = samples.removeAt(0)
            if (sample == null) return null
            sampleSumX -= sample.x
            sampleSumZ -= sample.z
            return sample
        }

        private fun removeCenterSampleAt(index: Int): ParticleSample? {
            val sample = samples.removeAt(index)
            if (sample == null) return null
            sampleSumX -= sample.x
            sampleSumZ -= sample.z
            return sample
        }
    }

    private data class ParticleSample(val x: Double, val z: Double, var ticks: Int)

    private data class Circle(val centerX: Double, val centerZ: Double, val radius: Double)

    private data class ActiveWormhole(
        var centerX: Double,
        var centerZ: Double,
        var radius: Double
    )

    private fun distanceSquared(a: ParticleSample, b: ParticleSample): Double {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return dx * dx + dz * dz
    }

    private fun triangleAreaSquared(a: ParticleSample, b: ParticleSample, c: ParticleSample): Double {
        val cross = (b.x - a.x) * (c.z - a.z) - (b.z - a.z) * (c.x - a.x)
        return cross * cross
    }

    private fun circleFromThreePoints(a: ParticleSample, b: ParticleSample, c: ParticleSample): Circle? {
        val d = 2.0 * (a.x * (b.z - c.z) + b.x * (c.z - a.z) + c.x * (a.z - b.z))
        if (kotlin.math.abs(d) < 0.0001) return null

        val a2 = a.x * a.x + a.z * a.z
        val b2 = b.x * b.x + b.z * b.z
        val c2 = c.x * c.x + c.z * c.z
        val centerX = (a2 * (b.z - c.z) + b2 * (c.z - a.z) + c2 * (a.z - b.z)) / d
        val centerZ = (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d
        val radius = sqrt((centerX - a.x) * (centerX - a.x) + (centerZ - a.z) * (centerZ - a.z))
        return Circle(centerX, centerZ, radius)
    }
}
