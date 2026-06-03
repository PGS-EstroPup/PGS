package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.mixin.InputAccessor
import com.pgs.pgsaddons.mixin.KeyBindingAccessor as KeyMappingAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.Input
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.option.KeyBinding as KeyMapping
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.SilverfishEntity
import net.minecraft.entity.passive.BatEntity
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.PlayerInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec2f
import java.util.concurrent.ThreadLocalRandom
import org.lwjgl.glfw.GLFW
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

enum class AutoFarmAction(val id: String, val label: String) {
    START_FARM("START_FARM", "⛏ Start Farm"),
    STOP_FARM("STOP_FARM", "⛏ Stop Farm"),
    STOP_ACTION("STOP_ACTION", "⏹ Stop Action"),
    INTERACT_MOUSEMAT("INTERACT_MOUSEMAT", "🖱 Use Mousemat"),
    INTERACT_ROD("INTERACT_ROD", "🎣 Use Rod"),
    AUTO_SPRAY("AUTO_SPRAY", "Auto Spray"),
    INTERACT_VACUUM_UNTIL_0_PESTS("INTERACT_VACUUM_UNTIL_0_PESTS", "🐜 Tracking Vacuum"),
    HOLD_VACUUM_5S("HOLD_VACUUM_5S", "🐜 Vacuum Interact"),
    HOLD_HOE("HOLD_HOE", "🌿 Hold Hoe"),
    SET_SPAWN("SET_SPAWN", "🏠 Set Spawn"),
    WARP_SPAWN("WARP_SPAWN", "🏠 Warp Spawn"),
    TPTOPLOT("TPTOPLOT", "🏠 TP To Plot"),
    ARMOR_SLOT_1("ARMOR_SLOT_1", "🛡 Armor Slot 1"),
    ARMOR_SLOT_2("ARMOR_SLOT_2", "🛡 Armor Slot 2"),
    ARMOR_SLOT_3("ARMOR_SLOT_3", "🛡 Armor Slot 3"),
    SLOT_SWAP("SLOT_SWAP", "🔀 Slot Swap"),
    REPEAT("REPEAT", "∞ Start Over"),
    START_MOVEMENT("START_MOVEMENT", "▶ Start Movement"),
    STOP_MOVEMENT("STOP_MOVEMENT", "⏹ Stop Movement"),
    AUTO_SELL("AUTO_SELL", "♲ Auto Sell");

    companion object {
        fun fromId(id: String): AutoFarmAction? = entries.firstOrNull { it.id == id }
    }
}

object AutoFarm2 {
    private val mc = MinecraftClient.getInstance()
    lateinit var toggleKey: KeyBinding

    private enum class Cycle { C1, C2, C3 }

    private var enabled = false
    private var cycle = Cycle.C1
    private var actionIndex = 0
    private var waitTicks = 0f
    private var waitingForSlotSwap = false
    private var waitingForAutoSell = false
    private var waitingForWardrobe = false
    private var pendingWardrobeSlot = ""
    private var pendingWardrobeClickSlot: Int? = null
    private var pendingWardrobeSyncId: Int? = null
    private var wardrobeCommandDelayTicks = 0f
    private var wardrobeClickDelayTicks = 0f
    private var movementActive = false
    private var attackHeld = false
    private var transientAttackTicks = 0f
    private var mousematActive = false
    private var mousematHoldTicks = 0f
    private var mousematClicksRemaining = 0
    private var mousematClickDown = false
    private var vacuumActive = false
    private var vacuumInteractTicks = 0f
    private var timedVacuumActive = false
    private var timedVacuumTicks = 0f
    private var lastMovementVertical = NodeVerticalDirection.NONE
    private var lastMovementHorizontal = NodeHorizontalDirection.NONE
    private var lastAppliedMovementNodePos: BlockPos? = null
    private var lastAppliedActionNodePos: BlockPos? = null
    private var pendingMovementNodePos: BlockPos? = null
    private var pendingMovementVertical = NodeVerticalDirection.NONE
    private var pendingMovementHorizontal = NodeHorizontalDirection.NONE
    private var pendingMovementTicks = 0f
    private var pestCount = 0
    private var cooldownReady = false
    private val completedActions = mutableMapOf<Cycle, Int>()

    private const val MIN_NODE_TICK_OFFSET = 3
    private const val MAX_NODE_TICK_OFFSET = 6
    private const val DIAGONAL_MOVEMENT_SCALE = 0.70710677f

    fun init() {
        enabled = Settings.general.autoFarm2Enabled
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("PGS Toggle Auto Farm 2.0", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, net.minecraft.client.KeyMapping.Category.MISC)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleKey.consumeClick()) toggle()
            tick(client)
        }

        ClientReceiveMessageEvents.GAME.register { message, _ -> onMessage(message.string) }

        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (waitingForWardrobe && screen is HandledScreen<*>) {
                val slot = pendingWardrobeSlot.toIntOrNull()?.plus(35)
                if (slot != null) {
                    pendingWardrobeClickSlot = slot
                    pendingWardrobeSyncId = screen.menu.syncId
                    wardrobeClickDelayTicks = randomDelayTicks(5)
                }
            }
        }
    }

    fun toggle() {
        if (enabled) {
            pause()
        } else {
            enabled = true
            Settings.general.autoFarm2Enabled = true
            resumeCycle()
            message("Started ${cycle.name}")
        }
        Settings.save()
    }

    fun isRunning(): Boolean = enabled

    private fun pause() {
        enabled = false
        Settings.general.autoFarm2Enabled = false
        waitTicks = 0f
        setAttackHeld(false)
        setUseKeyPressed(false)
        resetMousematClick()
        movementActive = false
        vacuumActive = false
        timedVacuumActive = false
        timedVacuumTicks = 0f
        waitingForSlotSwap = false
        waitingForAutoSell = false
        waitingForWardrobe = false
        resetNodeDelay()
        resetWardrobeClick()
        message("Stopped")
        Settings.save()
    }

    fun stop() {
        pause()
        resetProgress()
    }

    fun resetToCycle1() {
        waitingForSlotSwap = false
        waitingForAutoSell = false
        waitingForWardrobe = false
        resetWardrobeClick()
        resetNodeDelay()
        resetMousematClick()
        vacuumActive = false
        timedVacuumActive = false
        timedVacuumTicks = 0f
        movementActive = false
        resetProgress()
        message("Reset to C1")
    }

    fun startCycle2FromPestCooldown(): Boolean {
        if (!enabled || cycle != Cycle.C1) return false
        waitingForSlotSwap = false
        waitingForAutoSell = false
        vacuumActive = false
        timedVacuumActive = false
        timedVacuumTicks = 0f
        waitingForWardrobe = false
        resetNodeDelay()
        resetWardrobeClick()
        waitTicks = 0f
        cooldownReady = true
        clearCycleProgress(Cycle.C2)
        startCycle(Cycle.C2)
        return true
    }

    fun updatePestAliveCount(alive: Int) {
        pestCount = alive.coerceAtLeast(0)
        if (vacuumActive && pestCount <= 0) {
            finishVacuum()
        }
    }

    fun applyMovementInputOverride(input: Input) {
        if (!enabled) return
        if (mc.screen != null) {
            (input as InputAccessor).`pgsAddons$setMovementVector`(Vec2f(0f, 0f))
            (input as InputAccessor).`pgsAddons$setPlayerInput`(PlayerInput(false, false, false, false, false, false, false))
            return
        }
        if (!movementActive) return
        val forward = lastMovementVertical == NodeVerticalDirection.FORWARD
        val backward = lastMovementVertical == NodeVerticalDirection.BACKWARD
        val left = lastMovementHorizontal == NodeHorizontalDirection.LEFT
        val right = lastMovementHorizontal == NodeHorizontalDirection.RIGHT
        val movementX = when {
            left && !right -> 1f
            right && !left -> -1f
            else -> 0f
        }
        val movementY = when {
            forward && !backward -> 1f
            backward && !forward -> -1f
            else -> 0f
        }
        val diagonalScale = if (movementX != 0f && movementY != 0f) DIAGONAL_MOVEMENT_SCALE else 1f
        (input as InputAccessor).`pgsAddons$setMovementVector`(Vec2f(movementX * diagonalScale, movementY * diagonalScale))
        (input as InputAccessor).`pgsAddons$setPlayerInput`(PlayerInput(forward, backward, left, right, false, false, false))
    }

    private fun tick(client: MinecraftClient) {
        if (!enabled || client.player == null) return

        if (client.screen != null) {
            releaseWorldInputs()
            if (waitingForWardrobe) {
                handleWardrobeTick(client)
            }
            return
        }

        applyActiveFlowStates()
        if (movementActive) updateLastNodeDirection()
        if (waitTicks > 0f) {
            waitTicks -= TpsSync.getServerTicksPerClientTick()
            return
        }
        if (waitingForWardrobe) {
            handleWardrobeTick(client)
            return
        }
        if (mousematActive) {
            runMousematTick(client)
            return
        }
        if (timedVacuumActive) {
            runTimedVacuumTick(client)
            return
        }
        if (waitingForSlotSwap) return
        if (waitingForAutoSell) return
        if (vacuumActive) {
            runVacuumTick(client)
            return
        }

        val actions = currentActions()
        if (actionIndex !in actions.indices) {
            finishCycle()
            return
        }

        execute(actions[actionIndex], client)
    }

    private fun execute(action: AutoFarmAction, client: MinecraftClient) {
        when (action) {
            AutoFarmAction.START_FARM -> {
                startFarmAttack()
                next()
            }
            AutoFarmAction.STOP_FARM -> {
                stopFarmAttack()
                next()
            }
            AutoFarmAction.STOP_ACTION -> stop()
            AutoFarmAction.INTERACT_MOUSEMAT -> useMousematSlot(Settings.general.autoFarm2MousematSlot)
            AutoFarmAction.INTERACT_ROD -> interactSlot(Settings.general.autoFarm2RodSlot)
            AutoFarmAction.AUTO_SPRAY -> {
                if (FarmingTracker.isSprayNone()) {
                    interactSlot(Settings.general.autoFarm2SpraySlot)
                } else {
                    next()
                }
            }
            AutoFarmAction.INTERACT_VACUUM_UNTIL_0_PESTS -> {
                selectSlot(Settings.general.autoFarm2VacuumSlot)
                vacuumActive = true
                vacuumInteractTicks = 0f
            }
            AutoFarmAction.HOLD_VACUUM_5S -> {
                selectSlot(Settings.general.autoFarm2VacuumSlot)
                timedVacuumActive = true
                timedVacuumTicks = ThreadLocalRandom.current().nextInt(50, 71).toFloat()
            }
            AutoFarmAction.HOLD_HOE -> {
                selectSlot(Settings.general.autoFarm2HoeSlot)
                next()
            }
            AutoFarmAction.SET_SPAWN -> {
                client?.player?.connection?.sendChatCommand("setspawn")
                pauseThenNext(10)
            }
            AutoFarmAction.WARP_SPAWN -> {
                client?.player?.connection?.sendChatCommand("warp garden")
                pauseThenNext(40)
            }
            AutoFarmAction.TPTOPLOT -> {
                val plot = Settings.general.autoFarm2PlotName.trim()
                if (plot.isNotEmpty()) client?.player?.connection?.sendChatCommand("tptoplot $plot")
                pauseThenNext(40)
            }
            AutoFarmAction.ARMOR_SLOT_1 -> runWardrobe(Settings.general.autoFarm2ArmorSlot1)
            AutoFarmAction.ARMOR_SLOT_2 -> runWardrobe(Settings.general.autoFarm2ArmorSlot2)
            AutoFarmAction.ARMOR_SLOT_3 -> runWardrobe(Settings.general.autoFarm2ArmorSlot3)
            AutoFarmAction.SLOT_SWAP -> {
                waitingForSlotSwap = true
                SlotSwap.triggerSwap(onComplete = {
                    waitingForSlotSwap = false
                    next()
                })
            }
            AutoFarmAction.AUTO_SELL -> {
                waitingForAutoSell = true
                AutoSell.execute(onComplete = {
                    waitingForAutoSell = false
                    next()
                })
            }
            AutoFarmAction.REPEAT -> {
                cooldownReady = false
                resetProgress()
                startCycle(Cycle.C1)
            }
            AutoFarmAction.START_MOVEMENT -> {
                resetNodeDelay()
                updateLastNodeDirection()
                movementActive = true
                next()
            }
            AutoFarmAction.STOP_MOVEMENT -> {
                movementActive = false
                resetNodeDelay()
                next()
            }
        }
    }

    private fun interactSlot(slot: Int) {
        selectSlot(slot)
        val player = mc.player ?: return
        mc.gameMode?.interactItem(player, Hand.MAIN_HAND)
        player.swingHand(Hand.MAIN_HAND, true)
        pauseThenNext(8)
    }

    private fun useMousematSlot(slot: Int) {
        selectSlot(slot)
        mousematActive = true
        mousematHoldTicks = randomDelayTicks(7)
        mousematClicksRemaining = 3
        mousematClickDown = false
        setAttackKeyPressed(true)
    }

    private fun runMousematTick(client: MinecraftClient) {
        if (client.player == null) {
            resetMousematClick()
            next()
            return
        }

        if (mousematHoldTicks > 0f) {
            setAttackKeyPressed(true)
            mousematHoldTicks -= TpsSync.getServerTicksPerClientTick()
            if (mousematHoldTicks <= 0f) {
                setAttackKeyPressed(false)
            }
            return
        }

        if (mousematClicksRemaining <= 0) {
            resetMousematClick()
            pauseThenNext(4)
            return
        }

        if (mousematClickDown) {
            setAttackKeyPressed(false)
            mousematClickDown = false
            mousematClicksRemaining--
        } else {
            net.minecraft.client.KeyMapping.set((mc.options.keyAttack as KeyMappingAccessor).`pgsAddons$getBoundKey`(), true)
            mousematClickDown = true
        }
    }

    private fun resetMousematClick() {
        if (mousematActive || mousematClickDown) {
            setAttackKeyPressed(false)
        }
        mousematActive = false
        mousematHoldTicks = 0f
        mousematClicksRemaining = 0
        mousematClickDown = false
    }

    private fun runVacuumTick(client: MinecraftClient) {
        if (pestCount <= 0) {
            finishVacuum()
            return
        }
        vacuumInteractTicks -= TpsSync.getServerTicksPerClientTick()
        if (vacuumInteractTicks <= 0f) {
            val player = client.player ?: return
            if (lookAtNearestPest(client)) {
                client.gameMode?.interactItem(player, Hand.MAIN_HAND)
                player.swingHand(Hand.MAIN_HAND, true)
                vacuumInteractTicks = ThreadLocalRandom.current().nextInt(7, 13).toFloat()
            } else {
                vacuumInteractTicks = 1f
            }
        }
    }

    private fun runTimedVacuumTick(client: MinecraftClient) {
        if (client.player == null) {
            finishTimedVacuum(false)
            return
        }

        setUseKeyPressed(true)
        timedVacuumTicks -= TpsSync.getServerTicksPerClientTick()
        if (timedVacuumTicks <= 0f) {
            finishTimedVacuum()
        }
    }

    private fun finishTimedVacuum(nextAction: Boolean = true) {
        if (!timedVacuumActive) return
        timedVacuumActive = false
        timedVacuumTicks = 0f
        setUseKeyPressed(false)
        if (nextAction) {
            next()
        }
    }

    private fun finishVacuum() {
        if (!vacuumActive) return
        vacuumActive = false
        startCooldown()
        next()
    }

    private fun lookAtNearestPest(client: MinecraftClient): Boolean {
        val player = client.player ?: return false
        val world = client.level ?: return false
        val target = world.entitiesForRendering()
            .asSequence()
            .filter { it !== player && !it.isRemoved && isVacuumPest(it) }
            .minByOrNull { it.squaredDistanceTo(player) }
            ?: return false

        val eye = player.eyePos
        val targetPos = target.boundingBox.center
        val dx = targetPos.x - eye.x
        val dy = targetPos.y - eye.y
        val dz = targetPos.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f
        val pitch = (-Math.toDegrees(atan2(dy, horizontal)).toFloat()).coerceIn(-90f, 90f)
        val nextYaw = approachAngle(player.yaw, yaw, ThreadLocalRandom.current().nextDouble(9.0, 15.0).toFloat())
        val nextPitch = approachAngle(player.pitch, pitch, ThreadLocalRandom.current().nextDouble(7.0, 12.0).toFloat()).coerceIn(-90f, 90f)

        player.yaw = nextYaw
        player.pitch = nextPitch
        player.headYaw = nextYaw
        player.bodyYaw = nextYaw
        return abs(angleDelta(nextYaw, yaw)) <= 8f && abs(nextPitch - pitch) <= 8f
    }

    private fun isVacuumPest(entity: Entity): Boolean {
        return entity is SilverfishEntity || entity is BatEntity
    }

    private fun runWardrobe(slotText: String) {
        val slot = slotText.trim()
        if (slot.toIntOrNull() == null) {
            next()
            return
        }
        pendingWardrobeSlot = slot
        waitingForWardrobe = true
        pendingWardrobeClickSlot = null
        pendingWardrobeSyncId = null
        wardrobeCommandDelayTicks = randomDelayTicks(5)
        wardrobeClickDelayTicks = 0f
        mc?.player?.connection?.sendChatCommand("wardrobe")
    }

    private fun handleWardrobeTick(client: MinecraftClient) {
        val clickSlot = pendingWardrobeClickSlot ?: return
        val syncId = pendingWardrobeSyncId ?: return

        if (client.screen !is HandledScreen<*>) {
            resetWardrobeClick()
            next()
            return
        }

        if (wardrobeCommandDelayTicks > 0f) {
            wardrobeCommandDelayTicks -= TpsSync.getServerTicksPerClientTick()
            return
        }

        if (wardrobeClickDelayTicks > 0f) {
            wardrobeClickDelayTicks -= TpsSync.getServerTicksPerClientTick()
            return
        }

        val player = client.player ?: return
        client.gameMode?.clickSlot(syncId, clickSlot, 0, SlotActionType.PICKUP, player)
        player.closeContainer()
        resetWardrobeClick()
        pauseThenNext(5)
    }

    private fun resetWardrobeClick() {
        waitingForWardrobe = false
        pendingWardrobeSlot = ""
        pendingWardrobeClickSlot = null
        pendingWardrobeSyncId = null
        wardrobeCommandDelayTicks = 0f
        wardrobeClickDelayTicks = 0f
    }

    private fun finishCycle() {
        val nextCycle = nextCycleWhenReady()
        if (nextCycle != null) {
            clearCycleProgress(nextCycle)
            startCycle(nextCycle)
            return
        }

        if (shouldWaitForNextCycle()) {
            waitTicks = max(waitTicks, randomDelayTicks(5))
            return
        }
        stop()
        message("Finished all available sections")
    }

    private fun startCycle(nextCycle: Cycle?): Boolean {
        if (nextCycle == null) return false
        val previousCycle = cycle
        cycle = nextCycle
        actionIndex = completedActions[nextCycle]?.coerceAtMost(currentActions().size) ?: 0
        resetNodeDelay()
        if (previousCycle != nextCycle) {
            message("Switched to ${nextCycle.name}")
        }
        return true
    }

    private fun resumeCycle() {
        val actions = currentActions()
        actionIndex = completedActions[cycle]?.coerceAtMost(actions.size) ?: actionIndex.coerceIn(0, actions.size)
        if ((cycle == Cycle.C1 || cycle == Cycle.C2) && actionIndex >= actions.size) {
            startDefaultFarmingState()
        }
    }

    private fun startDefaultFarmingState() {
        startFarmAttack()
        resetNodeDelay()
        updateLastNodeDirection()
        movementActive = true
    }

    private fun nextCycleWhenReady(): Cycle? {
        return when (cycle) {
            Cycle.C1 -> Cycle.C2.takeIf { shouldRunCycle2() }
            Cycle.C2 -> Cycle.C3.takeIf { shouldRunCycle3() }
            Cycle.C3 -> null
        }
    }

    private fun shouldRunCycle2(): Boolean {
        return cooldownReady
    }

    private fun shouldRunCycle3(): Boolean {
        return pestCount > 0
    }

    private fun hasActiveFlowState(): Boolean {
        return attackHeld || movementActive || vacuumActive || timedVacuumActive || mousematActive
    }

    private fun shouldWaitForNextCycle(): Boolean {
        return cycle == Cycle.C1 || cycle == Cycle.C2 || hasActiveFlowState()
    }

    private fun currentActions(): List<AutoFarmAction> {
        val ids = when (cycle) {
            Cycle.C1 -> Settings.general.autoFarm2Cycle1
            Cycle.C2 -> Settings.general.autoFarm2Cycle2
            Cycle.C3 -> Settings.general.autoFarm2Cycle3
        }
        return ids.mapNotNull { AutoFarmAction.fromId(it) }
    }

    private fun updateLastNodeDirection() {
        val player = mc.player ?: return
        val currentNode = currentPathNode()
        handleActionNode(currentNode)
        val node = currentNode?.takeIf { it.isMovementNode } ?: return
        val offset = randomNodeTickOffset()

        if (lastAppliedMovementNodePos != node.pos) {
            if (offset <= 0) {
                applyMovementNode(node.pos, node.vertical, node.horizontal)
                return
            }

            if (pendingMovementNodePos != node.pos) {
                pendingMovementNodePos = node.pos
                pendingMovementVertical = node.vertical
                pendingMovementHorizontal = node.horizontal
                pendingMovementTicks = offset.toFloat()
                return
            }
        }

        val pendingPos = pendingMovementNodePos ?: return
        pendingMovementTicks -= TpsSync.getServerTicksPerClientTick()
        if (pendingMovementTicks <= 0f) {
            applyMovementNode(pendingPos, pendingMovementVertical, pendingMovementHorizontal)
        }
    }

    private fun currentPathNode(): PathNode? {
        val player = mc.player ?: return null
        val exact = (NodeManager.nodeAt(player.blockPos.down()) ?: NodeManager.nodeAt(player.blockPos))
            ?.takeIf { it.isMovementNode || it.isActionNode }
        if (exact != null) return exact

        val px = player.x
        val py = player.blockPos.y
        val pz = player.z
        return NodeManager.nodes
            .asSequence()
            .filter { it.isMovementNode || it.isActionNode }
            .filter { it.pos.y == py || it.pos.y == py - 1 }
            .map { node ->
                val dx = node.pos.x + 0.5 - px
                val dz = node.pos.z + 0.5 - pz
                node to (dx * dx + dz * dz)
            }
            .filter { it.second <= 0.9 * 0.9 }
            .minByOrNull { it.second }
            ?.first
    }

    private fun handleActionNode(node: PathNode?) {
        if (node == null || !node.isActionNode) {
            lastAppliedActionNodePos = null
            return
        }
        if (lastAppliedActionNodePos == node.pos) return

        when (node.type) {
            NodeType.TP_TO_PLOT -> {
                val plot = node.plotName.trim().ifEmpty { Settings.general.nodeTpPlotName.trim() }
                if (plot.isNotEmpty()) {
                    mc.player?.connection?.sendChatCommand("tptoplot $plot")
                    waitTicks = max(waitTicks, 40f)
                    lastAppliedMovementNodePos = null
                    lastAppliedActionNodePos = node.pos
                }
            }
            else -> {}
        }
    }

    private fun applyMovementNode(pos: BlockPos, vertical: NodeVerticalDirection, horizontal: NodeHorizontalDirection) {
        lastMovementVertical = vertical
        lastMovementHorizontal = horizontal
        lastAppliedMovementNodePos = pos
        pendingMovementNodePos = null
        pendingMovementVertical = NodeVerticalDirection.NONE
        pendingMovementHorizontal = NodeHorizontalDirection.NONE
        pendingMovementTicks = 0f
    }

    private fun resetNodeDelay() {
        lastAppliedMovementNodePos = null
        lastAppliedActionNodePos = null
        pendingMovementNodePos = null
        pendingMovementVertical = NodeVerticalDirection.NONE
        pendingMovementHorizontal = NodeHorizontalDirection.NONE
        pendingMovementTicks = 0f
    }

    private fun next() {
        actionIndex++
        markCurrentActionDone()
        waitTicks = max(waitTicks, randomDelayTicks(1))
    }

    private fun pauseThenNext(ticks: Int) {
        actionIndex++
        markCurrentActionDone()
        waitTicks = randomDelayTicks(ticks)
    }

    private fun randomDelayTicks(baseTicks: Int): Float {
        val minTicks = max(1, baseTicks - 2)
        val maxTicks = max(minTicks, baseTicks + 2)
        return ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1).toFloat()
    }

    private fun randomNodeTickOffset(): Int {
        return ThreadLocalRandom.current().nextInt(MIN_NODE_TICK_OFFSET, MAX_NODE_TICK_OFFSET + 1)
    }

    private fun markCurrentActionDone() {
        completedActions[cycle] = max(completedActions[cycle] ?: 0, actionIndex)
    }

    private fun clearCycleProgress(targetCycle: Cycle) {
        completedActions.remove(targetCycle)
    }

    private fun resetProgress() {
        cycle = Cycle.C1
        actionIndex = 0
        completedActions.clear()
    }

    private fun selectSlot(slot: Int) {
        mc.player?.inventory?.selectedSlot = (slot - 1).coerceIn(0, 8)
    }

    private fun setAttackHeld(held: Boolean) {
        attackHeld = held
        if (held) transientAttackTicks = 0f
        setAttackKeyPressed(held)
    }

    private fun startFarmAttack() {
        setAttackHeld(true)
    }

    private fun stopFarmAttack() {
        setAttackHeld(false)
    }

    private fun pulseAttackKey() {
        attackHeld = false
        transientAttackTicks = 2f
        setAttackKeyPressed(true)
    }

    private fun applyActiveFlowStates() {
        if (attackHeld) {
            setAttackKeyPressed(true)
            return
        }

        if (transientAttackTicks > 0f) {
            transientAttackTicks -= TpsSync.getServerTicksPerClientTick()
            if (transientAttackTicks <= 0f) {
                setAttackKeyPressed(false)
            } else {
                setAttackKeyPressed(true)
            }
        }
    }

    private fun setAttackKeyPressed(pressed: Boolean) {
        net.minecraft.client.KeyMapping.set((mc.options.keyAttack as KeyMappingAccessor).`pgsAddons$getBoundKey`(), pressed)
    }

    private fun setUseKeyPressed(pressed: Boolean) {
        net.minecraft.client.KeyMapping.set((mc.options.keyUse as KeyMappingAccessor).`pgsAddons$getBoundKey`(), pressed)
    }

    private fun releaseWorldInputs() {
        setAttackKeyPressed(false)
        setUseKeyPressed(false)
    }

    private fun onMessage(raw: String) {
        val msg = raw.replace(Regex("\\u00A7."), "").trim()
        Regex("Alive\\s*[:：]\\s*(\\d+)", RegexOption.IGNORE_CASE).find(msg)?.let {
            pestCount = it.groupValues[1].toIntOrNull() ?: pestCount
        }
        Regex("(?:YUCK!\\s*)?(?:(\\d+)\\s*)?Pests?\\s+have\\s+spawned\\s+in\\s+Plot\\b.*", RegexOption.IGNORE_CASE).find(msg)?.let {
            pestCount = it.groupValues.getOrNull(1)?.toIntOrNull() ?: pestCount.coerceAtLeast(1)
            startCycle3FromPestSpawn()
        }
        if (msg.contains("Cooldown", true) && msg.contains("Ready", true)) {
            cooldownReady = true
        }
    }

    private fun startCooldown() {
        cooldownReady = false
    }

    private fun startCycle3FromPestSpawn() {
        if (!enabled || cycle == Cycle.C3) return
        waitingForSlotSwap = false
        vacuumActive = false
        timedVacuumActive = false
        timedVacuumTicks = 0f
        waitingForWardrobe = false
        resetNodeDelay()
        resetWardrobeClick()
        waitTicks = 0f
        clearCycleProgress(Cycle.C3)
        startCycle(Cycle.C3)
    }

    private fun approachAngle(current: Float, target: Float, maxStep: Float): Float {
        val delta = angleDelta(current, target)
        if (abs(delta) <= maxStep) return target
        return current + delta.coerceIn(-maxStep, maxStep)
    }

    private fun angleDelta(current: Float, target: Float): Float {
        var delta = (target - current) % 360f
        if (delta >= 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun message(text: String) {
        mc.player?.sendSystemMessage(Text.literal("§b[AutoFarm 2.0] §7$text"))
    }
}
