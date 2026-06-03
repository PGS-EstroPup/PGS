package com.pgs.pgsaddons;

import com.pgs.pgsaddons.features.ArrowTypeTracker;
import com.pgs.pgsaddons.features.ChestHighlight;
import com.pgs.pgsaddons.features.DeployablesTracker;
import com.pgs.pgsaddons.features.EquipmentStatsHud;
import com.pgs.pgsaddons.features.FarmingTracker;
import com.pgs.pgsaddons.features.SlotSwap;
import com.pgs.pgsaddons.features.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private DraggedHud draggedHud = null;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private enum DraggedHud {
        DEPLOYABLES,
        SLOT_SWAP,
        EQUIPMENT_STATS,
        POWDER_CHEST,
        PEST_TRACKER,
        ARROW_TYPE,
        TIMER
    }

    public HudEditorScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);
        context.centeredText(Minecraft.getInstance().font, Component.literal("§bDrag the trackers to move them."), width / 2, 20, 0xFFFFFF);
        context.centeredText(Minecraft.getInstance().font, Component.literal("§7Release mouse to save. Press ESC to go back."), width / 2, 35, 0xAAAAAA);

        drawMockups(context);
        updateDrag(mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawMockups(GuiGraphicsExtractor context) {
        DeployablesTracker.drawHud(context, true);
        SlotSwap.INSTANCE.drawHud(context, true);
        EquipmentStatsHud.INSTANCE.drawHud(context, true);
        ChestHighlight.INSTANCE.drawHud(context, true);
        FarmingTracker.INSTANCE.drawHud(context, true);
        ArrowTypeTracker.INSTANCE.drawHud(context, true);
        Timer.INSTANCE.drawHud(context, true);
    }

    private void updateDrag(int mouseX, int mouseY) {
        if (draggedHud == null) return;

        int newX = Math.max(0, Math.min(width - 50, (int) (mouseX - dragOffsetX)));
        int newY = Math.max(0, Math.min(height - 20, (int) (mouseY - dragOffsetY)));

        switch (draggedHud) {
            case DEPLOYABLES -> {
                Settings.general.deployablesOverlayX = newX;
                Settings.general.deployablesOverlayY = newY;
            }
            case SLOT_SWAP -> {
                Settings.general.slotSwapHudX = newX;
                Settings.general.slotSwapHudY = newY;
            }
            case EQUIPMENT_STATS -> {
                Settings.general.equipmentStatsHudX = newX;
                Settings.general.equipmentStatsHudY = newY;
            }
            case POWDER_CHEST -> {
                Settings.general.powderChestHudX = newX;
                Settings.general.powderChestHudY = newY;
            }
            case PEST_TRACKER -> {
                Settings.general.pestTimersX = newX;
                Settings.general.pestTimersY = newY;
            }
            case ARROW_TYPE -> {
                Settings.general.arrowTypeTrackerX = newX;
                Settings.general.arrowTypeTrackerY = newY;
            }
            case TIMER -> {
                Settings.general.timerHudX = newX;
                Settings.general.timerHudY = newY;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();

            if (tryDrag(mouseX, mouseY, Settings.general.deployablesOverlayX, Settings.general.deployablesOverlayY, DeployablesTracker.mockupWidth(), DeployablesTracker.mockupHeight(), DraggedHud.DEPLOYABLES)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.slotSwapHudX, Settings.general.slotSwapHudY, SlotSwap.INSTANCE.mockupWidth(), 50, DraggedHud.SLOT_SWAP)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.equipmentStatsHudX, Settings.general.equipmentStatsHudY, 122, 98, DraggedHud.EQUIPMENT_STATS)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.powderChestHudX, Settings.general.powderChestHudY, ChestHighlight.INSTANCE.mockupWidth(), 24, DraggedHud.POWDER_CHEST)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.pestTimersX, Settings.general.pestTimersY, 170, 92, DraggedHud.PEST_TRACKER)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.arrowTypeTrackerX, Settings.general.arrowTypeTrackerY, ArrowTypeTracker.INSTANCE.mockupWidth(), 38, DraggedHud.ARROW_TYPE)) return true;
            if (tryDrag(mouseX, mouseY, Settings.general.timerHudX, Settings.general.timerHudY, Timer.INSTANCE.mockupWidth(), 24, DraggedHud.TIMER)) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean tryDrag(double mouseX, double mouseY, int x, int y, int width, int height, DraggedHud hud) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;
        draggedHud = hud;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0 && draggedHud != null) {
            draggedHud = null;
            Settings.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        Settings.save();
        minecraft.setScreen(parent);
    }
}
