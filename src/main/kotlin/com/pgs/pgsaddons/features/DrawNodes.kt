package com.pgs.pgsaddons.features

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pgs.pgsaddons.Settings
import com.pgs.pgsaddons.render.EspRenderLayers
import com.pgs.pgsaddons.render.EspRenderer
import com.pgs.pgsaddons.utils.PgsButtonWidget
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.drawCenteredTextWithShadow
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.nio.file.Path

// ─────────────────────────────────────────────────────────────────────────────
// Node type  –  controls color
// ─────────────────────────────────────────────────────────────────────────────

enum class NodeType(
    val label: String,
    val r: Float,
    val g: Float,
    val b: Float,
) {
    UNSET  ("Unset",   0.2f, 1.0f, 0.9f),  // cyan  – default before type is assigned
    FORWARD("Forward", 1.0f, 1.0f, 1.0f),  // white
    BACKWARD("Backward", 1.0f, 0.7f, 0.2f), // orange
    LEFT   ("Left",    1.0f, 0.2f, 0.2f),  // red
    RIGHT  ("Right",   0.2f, 0.4f, 1.0f),  // blue
    GARDEN ("Garden",  0.2f, 1.0f, 0.3f),  // green
    TP_TO_PLOT("TP To Plot", 0.9f, 0.8f, 0.2f), // yellow
}

enum class NodeVerticalDirection(val label: String) {
    NONE("None"),
    FORWARD("Forward"),
    BACKWARD("Backward")
}

enum class NodeHorizontalDirection(val label: String) {
    NONE("None"),
    LEFT("Left"),
    RIGHT("Right")
}

// ─────────────────────────────────────────────────────────────────────────────
// Node data  –  serialization-friendly (Gson)
// ─────────────────────────────────────────────────────────────────────────────

/** A placed node on top of a block. */
data class PathNode(
    val pos: BlockPos,
    var type: NodeType = NodeType.UNSET,
    var vertical: NodeVerticalDirection = NodeVerticalDirection.NONE,
    var horizontal: NodeHorizontalDirection = NodeHorizontalDirection.NONE,
    var plotName: String = ""
) {
    val isMovementNode: Boolean
        get() = vertical != NodeVerticalDirection.NONE || horizontal != NodeHorizontalDirection.NONE
    val isActionNode: Boolean
        get() = type == NodeType.TP_TO_PLOT

    fun displayLabel(): String {
        if (type == NodeType.GARDEN || type == NodeType.TP_TO_PLOT) return type.label
        if (!isMovementNode) return NodeType.UNSET.label
        return listOfNotNull(
            horizontal.takeIf { it != NodeHorizontalDirection.NONE }?.label,
            vertical.takeIf { it != NodeVerticalDirection.NONE }?.label
        ).joinToString(" + ")
    }

    fun renderColor(): FloatArray {
        if (type == NodeType.GARDEN || type == NodeType.TP_TO_PLOT) return floatArrayOf(type.r, type.g, type.b)
        if (!isMovementNode) return floatArrayOf(NodeType.UNSET.r, NodeType.UNSET.g, NodeType.UNSET.b)

        val colors = mutableListOf<FloatArray>()
        when (horizontal) {
            NodeHorizontalDirection.LEFT -> colors.add(floatArrayOf(NodeType.LEFT.r, NodeType.LEFT.g, NodeType.LEFT.b))
            NodeHorizontalDirection.RIGHT -> colors.add(floatArrayOf(NodeType.RIGHT.r, NodeType.RIGHT.g, NodeType.RIGHT.b))
            NodeHorizontalDirection.NONE -> {}
        }
        when (vertical) {
            NodeVerticalDirection.FORWARD -> colors.add(floatArrayOf(NodeType.FORWARD.r, NodeType.FORWARD.g, NodeType.FORWARD.b))
            NodeVerticalDirection.BACKWARD -> colors.add(floatArrayOf(NodeType.BACKWARD.r, NodeType.BACKWARD.g, NodeType.BACKWARD.b))
            NodeVerticalDirection.NONE -> {}
        }

        return floatArrayOf(
            colors.map { it[0] }.average().toFloat(),
            colors.map { it[1] }.average().toFloat(),
            colors.map { it[2] }.average().toFloat()
        )
    }
}

/** Flat DTO used for JSON serialization (no BlockPos dependency in Gson). */
private data class NodeDto(
    val x: Int,
    val y: Int,
    val z: Int,
    val type: String? = null,
    val vertical: String? = null,
    val horizontal: String? = null,
    val plotName: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Node Manager  –  state + save / load
// ─────────────────────────────────────────────────────────────────────────────

object NodeManager {

    val nodes = mutableListOf<PathNode>()
    var placementActive = false

    private val GSON = GsonBuilder().setPrettyPrinting().create()
    private val LEGACY_SAVE_PATH: Path = Path.of("config", "pgs_nodes.json")
    private val PROFILE_DIR: Path = Path.of("config", "pgs_node_profiles")
    private val LIST_TYPE = object : TypeToken<List<NodeDto>>() {}.type

    fun toggle() {
        placementActive = !placementActive
        val mc = MinecraftClient.getInstance()
        val status = if (placementActive) "§aON" else "§cOFF"
        mc.player?.sendSystemMessage(Text.literal("§b[DrawNodes] §7Node Placement $status"))
    }

    /** Returns the node at [pos] if one exists, null otherwise. */
    fun nodeAt(pos: BlockPos): PathNode? = nodes.find { it.pos == pos }

    fun addNode(pos: BlockPos): PathNode {
        val node = PathNode(pos)
        nodes.add(node)
        save()
        return node
    }

    fun removeNode(node: PathNode) {
        nodes.remove(node)
        save()
    }

    fun clearAll() {
        nodes.clear()
        save()
    }

    fun switchProfile(profileName: String) {
        save()
        Settings.general.nodeActiveProfile = normalizeProfileName(profileName)
        Settings.save()
        nodes.clear()
        load()
    }

    fun renameProfile(profileName: String): Boolean {
        val oldName = normalizeProfileName(Settings.general.nodeActiveProfile)
        val newName = normalizeProfileName(profileName)
        if (oldName == newName) return true

        return try {
            save()
            Files.createDirectories(PROFILE_DIR)
            val oldPath = PROFILE_DIR.resolve("${safeProfileName(oldName)}.json")
            val newPath = PROFILE_DIR.resolve("${safeProfileName(newName)}.json")
            if (Files.exists(newPath)) return false
            if (Files.exists(oldPath)) {
                Files.move(oldPath, newPath)
            }
            Settings.general.nodeActiveProfile = newName
            Settings.save()
            true
        } catch (e: Exception) {
            System.err.println("[pgs_addons] Failed to rename node profile: $e")
            false
        }
    }

    fun profileNames(): List<String> {
        return try {
            Files.createDirectories(PROFILE_DIR)
            Files.list(PROFILE_DIR).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                    .map { it.fileName.toString().removeSuffix(".json") }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save() {
        try {
            Files.createDirectories(PROFILE_DIR)
            val dtos = nodes.map {
                NodeDto(
                    it.pos.x,
                    it.pos.y,
                    it.pos.z,
                    it.type.name,
                    it.vertical.name,
                    it.horizontal.name,
                    it.plotName
                )
            }
            Files.writeString(savePath(), GSON.toJson(dtos))
        } catch (e: Exception) {
            System.err.println("[pgs_addons] Failed to save nodes: $e")
        }
    }

    fun load() {
        try {
            Files.createDirectories(PROFILE_DIR)
            val path = savePath()
            if (!Files.exists(path)) {
                if (Settings.general.nodeActiveProfile == "Default" && Files.exists(LEGACY_SAVE_PATH)) {
                    loadFromPath(LEGACY_SAVE_PATH)
                    save()
                }
                return
            }
            loadFromPath(path)
            println("[pgs_addons] Loaded ${nodes.size} node(s) for node profile ${Settings.general.nodeActiveProfile}.")
        } catch (e: Exception) {
            System.err.println("[pgs_addons] Failed to load nodes: $e")
        }
    }

    private fun loadFromPath(path: Path) {
        val json = Files.readString(path)
            val dtos: List<NodeDto> = GSON.fromJson(json, LIST_TYPE) ?: return
            nodes.clear()
            for (dto in dtos) {
                val type = runCatching { NodeType.valueOf(dto.type ?: NodeType.UNSET.name) }.getOrDefault(NodeType.UNSET)
                var vertical = runCatching { NodeVerticalDirection.valueOf(dto.vertical ?: NodeVerticalDirection.NONE.name) }.getOrDefault(NodeVerticalDirection.NONE)
                var horizontal = runCatching { NodeHorizontalDirection.valueOf(dto.horizontal ?: NodeHorizontalDirection.NONE.name) }.getOrDefault(NodeHorizontalDirection.NONE)

                if (dto.vertical == null && dto.horizontal == null) {
                    when (type) {
                        NodeType.FORWARD -> vertical = NodeVerticalDirection.FORWARD
                        NodeType.BACKWARD -> vertical = NodeVerticalDirection.BACKWARD
                        NodeType.LEFT -> horizontal = NodeHorizontalDirection.LEFT
                        NodeType.RIGHT -> horizontal = NodeHorizontalDirection.RIGHT
                        else -> {}
                    }
                }

                val storedType = when (type) {
                    NodeType.GARDEN, NodeType.TP_TO_PLOT -> type
                    else -> NodeType.UNSET
                }
                nodes.add(PathNode(BlockPos(dto.x, dto.y, dto.z), storedType, vertical, horizontal, dto.plotName ?: ""))
            }
    }

    private fun savePath(): Path {
        return PROFILE_DIR.resolve("${safeProfileName(Settings.general.nodeActiveProfile)}.json")
    }

    private fun normalizeProfileName(profileName: String): String {
        return profileName.trim().ifEmpty { "Default" }
    }

    private fun safeProfileName(profileName: String): String {
        return normalizeProfileName(profileName).replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Node placement handler  –  keybind + right-click intercept
// ─────────────────────────────────────────────────────────────────────────────

object DrawNodes {

    lateinit var toggleKey: KeyBinding

    fun init() {
        // Load saved nodes immediately
        NodeManager.load()

        // Register the toggle keybind (unbound by default – assign in Controls)
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "PGS Toggle Node Placement",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                net.minecraft.client.KeyMapping.Category.MISC
            )
        )

        // Poll the keybind every tick
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleKey.consumeClick()) {
                if (com.pgs.pgsaddons.utils.LocationUtils.isInGarden()) {
                    NodeManager.toggle()
                } else {
                    MinecraftClient.getInstance().player?.sendMessage(
                        net.minecraft.text.Text.literal("§c[Nodes] Only usable in the Garden."), true
                    )
                }
            }
            handleClientNodePlacement(client)
        }

        // Intercept right-clicks on blocks
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (!NodeManager.placementActive) return@register ActionResult.PASS
            if (!com.pgs.pgsaddons.utils.LocationUtils.isInGarden()) return@register ActionResult.PASS
            if (world.isClient && player != null) {
                val clickedBlock = hitResult.blockPos
                openOrAddNode(clickedBlock)
                return@register ActionResult.SUCCESS
            }
            ActionResult.PASS
        }

        // Render all nodes each frame
        WorldRenderEvents.AFTER_ENTITIES.register(DrawNodes::onRenderWorld)
    }

    private fun handleClientNodePlacement(client: MinecraftClient) {
        if (!NodeManager.placementActive || client.screen != null) return
        if (!com.pgs.pgsaddons.utils.LocationUtils.isInGarden()) return

        while (client.options.keyUse.consumeClick()) {
            val hit = client.hitResult
            if (hit !is BlockHitResult || hit.type != net.minecraft.world.phys.HitResult.Type.BLOCK) continue
            openOrAddNode(hit.blockPos)
        }
    }

    private fun openOrAddNode(pos: BlockPos) {
        val mc = MinecraftClient.getInstance()
        val existing = NodeManager.nodeAt(pos)
        if (existing != null) {
            mc.execute { mc.setScreen(NodeMenuScreen(existing)) }
        } else {
            NodeManager.addNode(pos)
        }
    }

    private fun onRenderWorld(context: WorldRenderContext) {
        if (NodeManager.nodes.isEmpty()) return
        if (!com.pgs.pgsaddons.utils.LocationUtils.isInGarden()) return
        if (Settings.general.nodeRenderMode == 2) return

        val matrices = context.matrices() ?: return
        val consumers = context.consumers() ?: return
        val camera = MinecraftClient.getInstance().gameRenderer.mainCamera.cameraPos
        val nodesToRender = NodeManager.nodes.filter { shouldRenderNode(it, camera.x, camera.y, camera.z) }
        if (nodesToRender.isEmpty()) return

        matrices.push()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        // Wireframe pass — must be fully completed before opening the fill buffer
        val lineBuffer: VertexConsumer = consumers.getBuffer(EspRenderLayers.LINE_LIST_ESP)
        for (node in nodesToRender) {
            val box = slabBox(node.pos)
            val c = node.renderColor()
            EspRenderer.drawWireframeBox(matrices.peek(), lineBuffer, box, c[0], c[1], c[2], 1.0f)
        }

        // Fill pass — get fill buffer only after wireframe is done
        val fillBuffer: VertexConsumer = consumers.getBuffer(EspRenderLayers.FILLED_ESP)
        for (node in nodesToRender) {
            val box = slabBox(node.pos)
            val c = node.renderColor()
            EspRenderer.drawFilledBox(matrices.peek(), fillBuffer, box, c[0], c[1], c[2], 0.25f)
        }

        matrices.pop()
    }

    private fun shouldRenderNode(node: PathNode, cameraX: Double, cameraY: Double, cameraZ: Double): Boolean {
        if (Settings.general.nodeRenderMode == 0) return true

        val dx = node.pos.x + 0.5 - cameraX
        val dy = node.pos.y + 0.5 - cameraY
        val dz = node.pos.z + 0.5 - cameraZ
        return dx * dx + dy * dy + dz * dz <= 30.0 * 30.0
    }

    private const val SLAB_HEIGHT = 1.0 / 8.0

    private fun slabBox(pos: BlockPos): Box {
        val x = pos.x.toDouble()
        val y = pos.y.toDouble() + 1.0
        val z = pos.z.toDouble()
        return Box(x, y, z, x + 1.0, y + SLAB_HEIGHT, z + 1.0)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Node Menu Screen
// ─────────────────────────────────────────────────────────────────────────────

class NodeMenuScreen(private val node: PathNode) : Screen(Text.literal("Node Options")) {

    private val panelW = 260
    private val panelH = 460
    private val btnW   = 200
    private val btnH   = 24
    private val btnGap = 8

    // Tracks whether the "Delete All" button is in confirm state
    private var deleteAllConfirming = false
    private var deleteAllButton: PgsButtonWidget? = null
    private var lastHandledClickMs = 0L

    override fun init() {
        val px = (width  - panelW) / 2
        val py = (height - panelH) / 2

        val contentStartX = px + (panelW - btnW) / 2
        var currentY = py + 55

        fun verticalButton(direction: NodeVerticalDirection) {
            addDrawableChild(
                PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("${if (node.vertical == direction) "§a" else ""}${direction.label}")) {
                    node.type = NodeType.UNSET
                    node.vertical = if (node.vertical == direction) NodeVerticalDirection.NONE else direction
                    NodeManager.save()
                }
            )
        }

        fun horizontalButton(direction: NodeHorizontalDirection) {
            addDrawableChild(
                PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("${if (node.horizontal == direction) "§a" else ""}${direction.label}")) {
                    node.type = NodeType.UNSET
                    node.horizontal = if (node.horizontal == direction) NodeHorizontalDirection.NONE else direction
                    NodeManager.save()
                }
            )
        }

        verticalButton(NodeVerticalDirection.FORWARD); currentY += btnH + btnGap
        verticalButton(NodeVerticalDirection.BACKWARD); currentY += btnH + btnGap
        horizontalButton(NodeHorizontalDirection.LEFT); currentY += btnH + btnGap
        horizontalButton(NodeHorizontalDirection.RIGHT); currentY += btnH + btnGap
        addDrawableChild(
            PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("Clear Direction")) {
                node.type = NodeType.UNSET
                node.vertical = NodeVerticalDirection.NONE
                node.horizontal = NodeHorizontalDirection.NONE
                NodeManager.save()
            }
        )
        currentY += btnH + btnGap

        val plotInput = TextFieldWidget(textRenderer, contentStartX, currentY, btnW, btnH, Text.literal("Plot Name"))
        plotInput.text = node.plotName
        plotInput.setPlaceholder(Text.literal("Plot name"))
        plotInput.setChangedListener {
            node.plotName = it
            NodeManager.save()
        }
        addDrawableChild(plotInput)
        currentY += btnH + btnGap

        addDrawableChild(
            PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal(NodeType.TP_TO_PLOT.label)) {
                node.type = NodeType.TP_TO_PLOT
                node.vertical = NodeVerticalDirection.NONE
                node.horizontal = NodeHorizontalDirection.NONE
                NodeManager.save()
            }
        )
        currentY += btnH + btnGap + 4

        addDrawableChild(
            PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal(NodeType.GARDEN.label)) {
                node.type = NodeType.GARDEN
                node.vertical = NodeVerticalDirection.NONE
                node.horizontal = NodeHorizontalDirection.NONE
                NodeManager.save()
            }
        )
        currentY += btnH + btnGap + 4

        // Delete single node
        addDrawableChild(
            PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("§cDelete Node")) {
                NodeManager.removeNode(node)
                close()
            }
        )
        currentY += btnH + btnGap

        // Delete all — double-confirm
        val dab = PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("§4Delete All Nodes")) {
            if (deleteAllConfirming) {
                NodeManager.clearAll()
                close()
            } else {
                deleteAllConfirming = true
                deleteAllButton?.setMessage(Text.literal("§c§lAre you sure?"))
            }
        }
        addDrawableChild(dab)
        deleteAllButton = dab
        currentY += btnH + btnGap

        // Close
        addDrawableChild(
            PgsButtonWidget(contentStartX, currentY, btnW, btnH, Text.literal("Close")) {
                close()
            }
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val px = (width  - panelW) / 2
        val py = (height - panelH) / 2

        // Panel background
        context.fill(px, py, px + panelW, py + panelH, 0xCC111111.toInt())

        // Border — colored to match the current node type
        val c = node.renderColor()
        val borderColor = (0xFF000000 or
                ((c[0] * 255).toInt().coerceIn(0, 255).toLong() shl 16) or
                ((c[1] * 255).toInt().coerceIn(0, 255).toLong() shl 8) or
                (c[2] * 255).toInt().coerceIn(0, 255).toLong()).toInt()
        context.fill(px,              py,              px + panelW,     py + 1,          borderColor)
        context.fill(px,              py + panelH - 1, px + panelW,     py + panelH,     borderColor)
        context.fill(px,              py,              px + 1,          py + panelH,     borderColor)
        context.fill(px + panelW - 1, py,              px + panelW,     py + panelH,     borderColor)

        // Title
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("§b§lNode Options"),
            width / 2,
            py + 14,
            0xFFFFFFFF.toInt()
        )

        // Sub-title: coords + current type
        val pos = node.pos
        val tags = mutableListOf<String>()
        if (false) tags.add("")
        if (false) tags.add("")
        val tagStr = if (tags.isNotEmpty()) " " + tags.joinToString(", ") else ""
        
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("§7${pos.x}, ${pos.y}, ${pos.z}  §8[${node.displayLabel()}]$tagStr"),
            width / 2,
            py + 30,
            0xFFAAAAAA.toInt()
        )

        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastHandledClickMs < 150L) return true

        val handled = super.mouseClicked(click, doubled)
        if (handled) lastHandledClickMs = now
        return handled
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0x66000000.toInt())
    }

    override fun shouldPause(): Boolean = false
}









