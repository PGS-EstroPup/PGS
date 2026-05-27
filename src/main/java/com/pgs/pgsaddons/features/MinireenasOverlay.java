package com.pgs.pgsaddons.features;

import com.pgs.pgsaddons.Settings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public final class MinireenasOverlay {
    private static final Path IMAGE_PATH = Path.of("config", "pgs_overlays", "minireenas.png");
    private static final String IMAGE_RESOURCE = "assets/pgs_addons/overlays/minireenas.png";
    private static final Identifier HUD_ID = Identifier.of("pgs_addons", "minireenas_overlay");
    private static final Identifier TEXTURE_ID = Identifier.of("pgs_addons", "overlays/minireenas");
    private static final int SHOW_CHANCE = 500;
    private static final Random RANDOM = new Random();

    private static boolean attemptedLoad = false;
    private static boolean loaded = false;
    private static boolean visible = false;
    private static Screen previousScreen = null;
    private static int imageWidth = 1;
    private static int imageHeight = 1;

    private MinireenasOverlay() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(MinireenasOverlay::onTick);
        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> onRenderHud(context));
    }

    private static void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            visible = false;
            previousScreen = client.currentScreen;
            return;
        }

        if (!Settings.general.minireenasOverlayEnabled) {
            visible = false;
            previousScreen = client.currentScreen;
            return;
        }

        ensureLoaded(client);

        Screen currentScreen = client.currentScreen;
        boolean guiJustClosed = previousScreen != null && currentScreen == null;
        boolean guiJustOpened = previousScreen == null && currentScreen != null;

        if (guiJustOpened) {
            visible = false;
        } else if (guiJustClosed && loaded && RANDOM.nextInt(SHOW_CHANCE) == 0) {
            visible = true;
        }

        previousScreen = currentScreen;
    }

    private static void onRenderHud(DrawContext context) {
        if (!Settings.general.minireenasOverlayEnabled || !visible || !loaded) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE_ID,
                0,
                0,
                0.0f,
                0.0f,
                width,
                height,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight
        );
    }

    private static void ensureLoaded(MinecraftClient client) {
        if (attemptedLoad) return;
        attemptedLoad = true;

        try {
            Files.createDirectories(IMAGE_PATH.getParent());
        } catch (IOException e) {
            System.err.println("[pgs_addons] Failed to create minireenas overlay directory: " + e);
            return;
        }

        boolean configImageExists = Files.exists(IMAGE_PATH);
        if (!configImageExists && MinireenasOverlay.class.getClassLoader().getResource(IMAGE_RESOURCE) == null) {
            System.out.println("[pgs_addons] Minireenas overlay not found at " + IMAGE_PATH.toAbsolutePath());
            return;
        }

        try (InputStream stream = configImageExists
                ? Files.newInputStream(IMAGE_PATH)
                : MinireenasOverlay.class.getClassLoader().getResourceAsStream(IMAGE_RESOURCE)) {
            if (stream == null) return;
            NativeImage image = NativeImage.read(stream);
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "PGS Minireenas overlay", image);
            client.getTextureManager().registerTexture(TEXTURE_ID, texture);
            texture.upload();
            loaded = true;
            System.out.println("[pgs_addons] Loaded Minireenas overlay " + IMAGE_PATH);
        } catch (Exception e) {
            System.err.println("[pgs_addons] Failed to load Minireenas overlay: " + e);
        }
    }
}
