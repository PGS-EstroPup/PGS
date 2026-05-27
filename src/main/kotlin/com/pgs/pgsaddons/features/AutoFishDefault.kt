package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import kotlin.random.Random
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object AutoFishDefault {

    private var Stage: Int = 0
    private var nextActionAtMillis: Long = 0L
    private var aSTriggered: Boolean = false
    private var guiPausedAtMillis: Long? = null


    fun onClientTick(client: MinecraftClient) {
        val player = client.player ?: return
        val world = client.world ?: return

        if (Settings.general.autofish && player.mainHandStack.item == Items.FISHING_ROD) {
            val now = System.currentTimeMillis()
            if (pauseForGui(client, now)) return

            if (!aSTriggered) {
                val pos = Vec3d(player.x, player.y, player.z)
                val range = Settings.general.autofishRange.toDouble()
                val rangeBox = Box.of(pos, range * 2, range * 2, range * 2)

                val found =
                        world
                                .getEntitiesByClass(ArmorStandEntity::class.java, rangeBox) { stand
                                    ->
                                    stand?.hasCustomName() == true &&
                                            stand.name.string.contains("!!!")
                                }
                                .isNotEmpty()

                if (found) {
                    aSTriggered = true
                    Stage = 1
                    nextActionAtMillis = now + TpsSync.serverTicksToMillis((3 + Random.nextInt(4)).toFloat())
                }
            }
            if (Stage == 1 && now >= nextActionAtMillis) {
                interact(player, client)
                Stage = 2
                nextActionAtMillis = now + TpsSync.serverTicksToMillis((4 + Random.nextInt(4)).toFloat())
            } else if (Stage == 2 && now >= nextActionAtMillis) {
                interact(player, client)
                Stage = 0
                aSTriggered = false
                FishMacroCheck.onCatch()
            }
        }
    }

    private fun pauseForGui(client: MinecraftClient, now: Long): Boolean {
        if (client.currentScreen != null) {
            if (guiPausedAtMillis == null) guiPausedAtMillis = now
            return true
        }

        guiPausedAtMillis?.let { pausedAt ->
            if (nextActionAtMillis > 0L) {
                nextActionAtMillis += now - pausedAt
            }
            guiPausedAtMillis = null
        }
        return false
    }

    private fun interact(player: ClientPlayerEntity, client: MinecraftClient) {
        try {
            client.interactionManager?.interactItem(player, Hand.MAIN_HAND)
            player.swingHand(Hand.MAIN_HAND, true)
        } catch (e: Exception) {}
    }
}
