package com.pgs.pgsaddons.features

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.Options
import net.minecraft.client.MouseHandler
import com.mojang.blaze3d.platform.Window
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.client.multiplayer.PlayerInfo

val Minecraft.textRenderer: Font
    get() = font

val Minecraft.crosshairTarget
    get() = hitResult

val Minecraft.mouse: MouseHandler
    get() = mouseHandler

val MouseHandler.x: Double
    get() = xpos()

val MouseHandler.y: Double
    get() = ypos()

class RenderTickCounterCompat(private val minecraft: Minecraft) {
    fun getTickProgress(ignoreFreeze: Boolean): Float = minecraft.deltaTracker.getGameTimeDeltaPartialTick(ignoreFreeze)
}

val Minecraft.renderTickCounter: RenderTickCounterCompat
    get() = RenderTickCounterCompat(this)

val Window.scaledWidth: Int
    get() = guiScaledWidth

val Window.scaledHeight: Int
    get() = guiScaledHeight

fun Gui.setTitleTicks(fadeIn: Int, stay: Int, fadeOut: Int) {
}

fun DrawContext.drawItem(stack: ItemStack, x: Int, y: Int) {
    item(stack, x, y)
}

fun net.minecraft.client.KeyMapping.set(pressed: Boolean) {
    setDown(pressed)
}

val KeyMapping.isPressed: Boolean
    get() = isDown

val Options.forwardKey: KeyMapping
    get() = keyUp

val Options.backKey: KeyMapping
    get() = keyDown

val Options.leftKey: KeyMapping
    get() = keyLeft

val Options.rightKey: KeyMapping
    get() = keyRight

val Options.sneakKey: KeyMapping
    get() = keyShift

var Inventory.selectedSlot: Int
    get() = selectedSlot
    set(value) {
        setSelectedSlot(value)
    }

val Player.mainHandStack: ItemStack
    get() = mainHandItem

val ItemStack.name: Component
    get() = hoverName

fun Player.getStackInHand(hand: InteractionHand): ItemStack = getItemInHand(hand)

fun LocalPlayer.swingHand(hand: InteractionHand) {
    swing(hand)
}

fun LocalPlayer.swingHand(hand: InteractionHand, fromServer: Boolean) {
    swing(hand)
}

fun LocalPlayer.closeHandledScreen() {
    closeContainer()
}

fun LocalPlayer.sendMessage(message: Component, overlay: Boolean = false) {
    if (overlay) sendOverlayMessage(message) else sendSystemMessage(message)
}

fun ClientPacketListener.sendChatCommand(command: String) {
    sendCommand(command.removePrefix("/"))
}

fun MultiPlayerGameMode.interactItem(player: Player, hand: InteractionHand) {
    useItem(player, hand)
}

fun MultiPlayerGameMode.clickSlot(
    syncId: Int,
    slot: Int,
    button: Int,
    actionType: ContainerInput,
    player: Player
) {
    if (slot >= 0 && slot >= player.containerMenu.slots.size) return
    handleContainerInput(syncId, slot, button, actionType, player)
}

val <T : net.minecraft.world.inventory.AbstractContainerMenu> AbstractContainerScreen<T>.screenHandler: T
    get() = menu

val net.minecraft.world.inventory.AbstractContainerMenu.syncId: Int
    get() = containerId

val Slot.id: Int
    get() = index

val Slot.stack: ItemStack
    get() = item

val Entity.eyePos: Vec3
    get() = eyePosition

val Entity.boundingBox: AABB
    get() = getBoundingBox()

fun Entity.squaredDistanceTo(other: Entity): Double = distanceToSqr(other)

fun Entity.getLerpedPos(tickDelta: Float): Vec3 = getPosition(tickDelta)

val Entity.width: Float
    get() = bbWidth

val Entity.height: Float
    get() = bbHeight

var Entity.yaw: Float
    get() = yRot
    set(value) {
        yRot = value
    }

var Entity.pitch: Float
    get() = xRot
    set(value) {
        xRot = value
    }

var Entity.headYaw: Float
    get() = yHeadRot
    set(value) {
        setYHeadRot(value)
    }

var Entity.bodyYaw: Float
    get() = getYRot()
    set(value) {
        setYBodyRot(value)
    }

val Entity.blockPos
    get() = blockPosition()

val net.minecraft.client.Camera.cameraPos: Vec3
    get() = position()

fun PoseStack.push() = pushPose()

fun PoseStack.pop() = popPose()

fun PoseStack.peek(): PoseStack.Pose = last()

fun AABB.stretch(x: Double, y: Double, z: Double): AABB = expandTowards(x, y, z)

fun AABB.offset(x: Double, y: Double, z: Double): AABB = move(x, y, z)

fun AABB.expand(x: Double, y: Double, z: Double): AABB = inflate(x, y, z)

fun LevelRenderContext.matrices(): PoseStack? = poseStack()

fun LevelRenderContext.consumers(): net.minecraft.client.renderer.MultiBufferSource.BufferSource? = bufferSource()

fun LevelRenderContext.camera(): net.minecraft.client.Camera = Minecraft.getInstance().gameRenderer.mainCamera

val Entity.handSwinging: Boolean
    get() = (this as? net.minecraft.world.entity.LivingEntity)?.swinging == true

fun BlockPos.down(): BlockPos = below()

fun BlockPos.getSquaredDistance(pos: Vec3): Double = distToCenterSqr(pos)

fun BlockPos.getSquaredDistance(x: Double, y: Double, z: Double): Double = distToCenterSqr(x, y, z)

val Level.isClient: Boolean
    get() = isClientSide

fun Level.isChunkLoaded(pos: BlockPos): Boolean = hasChunkAt(pos)

fun Level.setBlockState(pos: BlockPos, state: BlockState) {
    setBlock(pos, state, 3)
}

fun Level.setBlockState(pos: BlockPos, state: BlockState, flags: Int) {
    setBlock(pos, state, flags)
}

val ClientPacketListener.playerList: Collection<PlayerInfo>
    get() = listedOnlinePlayers

