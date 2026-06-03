package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.mixin.InputAccessor
import com.pgs.pgsaddons.mixin.KeyBindingAccessor as KeyMappingAccessor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.input.Input
import net.minecraft.client.option.KeyBinding as KeyMapping
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.util.PlayerInput
import net.minecraft.util.math.Vec2f
import net.minecraft.world.InteractionResult

object FrozenBlazeFishing {
    private val mc = MinecraftClient.getInstance()

    private var armedAfterCast = false
    private var active = false
    private var originX = 0.0
    private var originZ = 0.0
    private var originYaw = 0f
    private var originPitch = 0f
    private var sideX = 0.0
    private var sideZ = 0.0
    private var targetOffset = 0.0
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var moveDirection = 0
    private var nextTargetAtMillis = 0L
    private var ignoreInputUntilMillis = 0L

    fun init() {
        UseItemCallback.EVENT.register { player, world, hand ->
            if (!world.isClientSide || !Settings.general.frozenBlazeFishingEnabled) {
                return@register InteractionResult.PASS
            }

            val stack = player.getItemInHand(hand)
            if (stack.item == Items.FISHING_ROD) {
                armedAfterCast = true
                ignoreInputUntilMillis = System.currentTimeMillis() + 700L
            }
            InteractionResult.PASS
        }

        ClientTickEvents.END_CLIENT_TICK.register { tick(it) }
    }

    fun onUserInput() {
        if (!active && !armedAfterCast) return
        if (System.currentTimeMillis() < ignoreInputUntilMillis) return
        stop()
    }

    fun applyMovementInputOverride(input: Input): Boolean {
        if (!active || !Settings.general.frozenBlazeFishingEnabled || mc.screen != null) return false

        val shouldMove = moveDirection != 0
        val left = moveDirection < 0
        val right = moveDirection > 0
        val movementX = when {
            left -> 1f
            right -> -1f
            else -> 0f
        }

        (input as InputAccessor).`pgsAddons$setMovementVector`(Vec2f(movementX, 0f))
        (input as InputAccessor).`pgsAddons$setPlayerInput`(PlayerInput(false, false, left, right, false, true, false))
        return shouldMove
    }

    private fun tick(client: MinecraftClient) {
        val player = client.player
        if (!Settings.general.frozenBlazeFishingEnabled || client.screen != null || player == null) {
            stop()
            return
        }

        if (armedAfterCast && player.fishing != null) {
            start(player)
            armedAfterCast = false
        }

        if (!active) return

        if (player.fishing == null || player.mainHandItem.item != Items.FISHING_ROD) {
            stop()
            return
        }

        holdCrouch(true)
        updateTargetIfNeeded()
        updateMovement(player)
        updateLook(player)
    }

    private fun start(player: PlayerEntity) {
        active = true
        originX = player.x
        originZ = player.z
        originYaw = player.yaw
        originPitch = player.pitch
        val radians = Math.toRadians((originYaw + 90.0).toDouble())
        sideX = -sin(radians)
        sideZ = cos(radians)
        targetOffset = 0.0
        targetYaw = originYaw
        targetPitch = originPitch
        moveDirection = 0
        ignoreInputUntilMillis = System.currentTimeMillis() + 500L
        pickNewTarget()
        holdCrouch(true)
    }

    private fun stop() {
        if (!active && !armedAfterCast) return
        armedAfterCast = false
        active = false
        moveDirection = 0
        holdCrouch(false)
    }

    private fun updateTargetIfNeeded() {
        if (System.currentTimeMillis() >= nextTargetAtMillis) {
            pickNewTarget()
        }
    }

    private fun pickNewTarget() {
        targetOffset = Random.nextDouble(-0.85, 0.85)
        targetYaw = originYaw + Random.nextDouble(-9.0, 9.0).toFloat()
        targetPitch = originPitch
        nextTargetAtMillis = System.currentTimeMillis() + Random.nextLong(2600L, 5200L)
    }

    private fun updateMovement(player: PlayerEntity) {
        val currentOffset = ((player.x - originX) * sideX) + ((player.z - originZ) * sideZ)
        moveDirection = when {
            currentOffset > 0.95 -> -1
            currentOffset < -0.95 -> 1
            abs(targetOffset - currentOffset) < 0.08 -> 0
            targetOffset > currentOffset -> 1
            else -> -1
        }
    }

    private fun updateLook(player: PlayerEntity) {
        val nextYaw = approachAngle(player.yaw, targetYaw, 0.09f)
        val nextPitch = approach(player.pitch, targetPitch, 0.04f).coerceIn(originPitch - 3f, originPitch + 3f).coerceIn(-89f, 89f)

        player.yaw = nextYaw
        player.pitch = nextPitch
        player.headYaw = nextYaw
    }

    private fun approach(current: Float, target: Float, step: Float): Float {
        val diff = target - current
        return when {
            abs(diff) <= step -> target
            diff > 0 -> current + step
            else -> current - step
        }
    }

    private fun approachAngle(current: Float, target: Float, step: Float): Float {
        var diff = (target - current) % 360f
        if (diff >= 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return when {
            abs(diff) <= step -> target
            diff > 0 -> current + step
            else -> current - step
        }
    }

    private fun holdCrouch(pressed: Boolean) {
        try {
            KeyMapping.set((mc.options.keyShift as KeyMappingAccessor).`pgsAddons$getBoundKey`(), pressed)
        } catch (_: Exception) {
        }
    }
}
