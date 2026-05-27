package com.pgs.pgsaddons.features

import com.pgs.pgsaddons.Settings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.sound.SoundEvents

object FishMacroCheck {
    private var idleTicks = 0
    private var soundsRemaining = 0
    private var soundDelay = 0

    // Store the last known state for idle detection
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var lastYaw: Float = 0f
    private var lastPitch: Float = 0f

    // Mouse tracking
    private var lastMouseX: Double = 0.0
    private var lastMouseY: Double = 0.0

    // Macro Check state
    private var catchCount = 0
    private var baselineSaved = false
    private var baselineX: Float = 0f
    private var baselineY: Float = 0f
    private var baselineZ: Float = 0f
    private var baselineYaw: Float = 0f
    private var baselinePitch: Float = 0f

    fun onCatch() {
        if (Settings.general.macroCheckEnabled) {
            catchCount++
            // After the 5th catch, we refresh the baseline to the latest "stable" position
            if (catchCount > 5) {
                baselineSaved = false
            }
        }
    }

    fun start() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            val player = client.player ?: return@EndTick

            val isAutoFishEnabled = Settings.general.autofish || Settings.general.autofishWithKillerEnabled
            val isMacroCheckEnabled = Settings.general.macroCheckEnabled

            // Handle mouse movement as manual input
            val currentMouseX = client.mouse.x
            val currentMouseY = client.mouse.y
            val mouseMoved = currentMouseX != lastMouseX || currentMouseY != lastMouseY
            lastMouseX = currentMouseX
            lastMouseY = currentMouseY

            // Manual input check (ignoring mod-triggered hand swings)
            val isManualInput = mouseMoved || 
                                client.options.forwardKey.isPressed || client.options.backKey.isPressed || 
                                client.options.leftKey.isPressed || client.options.rightKey.isPressed ||
                                client.options.jumpKey.isPressed || client.options.sneakKey.isPressed ||
                                client.options.useKey.isPressed || client.options.attackKey.isPressed

            // --- 1. IDLE DETECTION (Standard Alert) ---
            if (isAutoFishEnabled && isMacroCheckEnabled && catchCount >= 5) {
                val movedSinceLastTick = player.x.toFloat() != lastX || player.y.toFloat() != lastY || player.z.toFloat() != lastZ ||
                        player.yaw != lastYaw || player.pitch != lastPitch
                
                // Standard idle reset: either moved, pushed keys, or mod interacted
                if (movedSinceLastTick || mouseMoved || player.handSwinging || client.options.useKey.isPressed) {
                    idleTicks = 0
                } else {
                    idleTicks++
                }

                if (idleTicks >= 300) {
                    if (soundsRemaining <= 0) {
                        soundsRemaining = 10
                        soundDelay = 0
                        client.inGameHud.setTitle(net.minecraft.text.Text.literal("§c" + Settings.general.macroCheckAlertText))
                        client.inGameHud.setTitleTicks(10, 60, 20)
                    }
                    idleTicks = 0
                }

                lastX = player.x.toFloat()
                lastY = player.y.toFloat()
                lastZ = player.z.toFloat()
                lastYaw = player.yaw
                lastPitch = player.pitch
            } else {
                idleTicks = 0
            }

            // --- 2. MACRO CHECK (Movement Check) ---
            if (isMacroCheckEnabled) {
                if (catchCount >= 5) {
                    if (!baselineSaved) {
                        baselineX = player.x.toFloat()
                        baselineY = player.y.toFloat()
                        baselineZ = player.z.toFloat()
                        baselineYaw = player.yaw
                        baselinePitch = player.pitch
                        baselineSaved = true
                    }

                    val movedFromBaseline = player.x.toFloat() != baselineX || player.y.toFloat() != baselineY || player.z.toFloat() != baselineZ ||
                            player.yaw != baselineYaw || player.pitch != baselinePitch

                    if (movedFromBaseline) {
                        if (isManualInput) {
                            // Intentional movement: Update baseline and stop sounds
                            baselineX = player.x.toFloat()
                            baselineY = player.y.toFloat()
                            baselineZ = player.z.toFloat()
                            baselineYaw = player.yaw
                            baselinePitch = player.pitch
                            soundsRemaining = 0
                        } else {
                            // SERVER-SIDE MOVEMENT DETECTED (No keys pressed)
                            if (soundsRemaining < 20) {
                                soundsRemaining = 20
                                soundDelay = 0
                                client.inGameHud.setTitle(net.minecraft.text.Text.literal("§c" + Settings.general.macroCheckAlertText))
                                client.inGameHud.setTitleTicks(10, 60, 20)
                            }
                        }
                    }
                }
            } else {
                catchCount = 0
                baselineSaved = false
            }

            // --- 3. GLOBAL RESET ---
            // If the player is actively pressing keys, stop all alerts and restart the catch counter
            if (isManualInput) {
                soundsRemaining = 0
                catchCount = 0
                baselineSaved = false
            }

            // If neither feature is active, definitely stop sounds and reset counter
            if (!isAutoFishEnabled && !isMacroCheckEnabled) {
                soundsRemaining = 0
                catchCount = 0
                baselineSaved = false
            }


            // Sequential sound logic
            if (soundsRemaining > 0) {
                if (soundDelay <= 0) {
                    player.playSound(SoundEvents.BLOCK_ANVIL_PLACE, 10.0f, 1.0f)
                    
                    // Display the alert title when sound plays
                    client.inGameHud.setTitle(net.minecraft.text.Text.literal("§c" + Settings.general.macroCheckAlertText))
                    client.inGameHud.setTitleTicks(0, 45, 5)
                    
                    soundsRemaining--
                    soundDelay = 40
                } else {
                    soundDelay--
                }
            }
        })
    }
}