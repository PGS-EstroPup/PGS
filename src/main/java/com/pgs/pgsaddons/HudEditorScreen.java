package com.pgs.pgsaddons;

import com.pgs.pgsaddons.features.DeployablesTracker;
import com.pgs.pgsaddons.features.EquipmentStatsHud;
import com.pgs.pgsaddons.features.ChestHighlight;
import com.pgs.pgsaddons.features.ArrowTypeTracker;
import com.pgs.pgsaddons.features.FarmingTracker;
import com.pgs.pgsaddons.features.SlotSwap;
import com.pgs.pgsaddons.features.Timer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

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
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a dark translucent background
        context.fill(0, 0, this.width, this.height, 0x44000000);

        // Draw instructions
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§bDrag the trackers to move them."), this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Press any key or click Done to save."), this.width / 2, 35, 0xAAAAAA);

        // Draw the trackers at their current position
        DeployablesTracker.drawHud(context, true);
        SlotSwap.INSTANCE.drawHud(context, true);
        EquipmentStatsHud.INSTANCE.drawHud(context, true);
        ChestHighlight.INSTANCE.drawHud(context, true);
        FarmingTracker.drawHud(context, true);
        ArrowTypeTracker.INSTANCE.drawHud(context, true);
        Timer.INSTANCE.drawHud(context, true);

        // If dragging, update the position
        if (draggedHud != null) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            
            // Boundary checks
            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX > this.width - 50) newX = this.width - 50;
            if (newY > this.height - 20) newY = this.height - 20;

            if (draggedHud == DraggedHud.DEPLOYABLES) {
                Settings.general.deployablesOverlayX = newX;
                Settings.general.deployablesOverlayY = newY;
            } else if (draggedHud == DraggedHud.SLOT_SWAP) {
                Settings.general.slotSwapHudX = newX;
                Settings.general.slotSwapHudY = newY;
            } else if (draggedHud == DraggedHud.EQUIPMENT_STATS) {
                Settings.general.equipmentStatsHudX = newX;
                Settings.general.equipmentStatsHudY = newY;
            } else if (draggedHud == DraggedHud.POWDER_CHEST) {
                Settings.general.powderChestHudX = newX;
                Settings.general.powderChestHudY = newY;
            } else if (draggedHud == DraggedHud.PEST_TRACKER) {
                Settings.general.pestTimersX = newX;
                Settings.general.pestTimersY = newY;
            } else if (draggedHud == DraggedHud.ARROW_TYPE) {
                Settings.general.arrowTypeTrackerX = newX;
                Settings.general.arrowTypeTrackerY = newY;
            } else if (draggedHud == DraggedHud.TIMER) {
                Settings.general.timerHudX = newX;
                Settings.general.timerHudY = newY;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) { // Left click
            double mX = click.x();
            double mY = click.y();

            // Check Deployables Tracker
            int depX = Settings.general.deployablesOverlayX;
            int depY = Settings.general.deployablesOverlayY;
            if (mX >= depX && mX <= depX + 120 && mY >= depY && mY <= depY + 36) {
                draggedHud = DraggedHud.DEPLOYABLES;
                dragOffsetX = mX - depX;
                dragOffsetY = mY - depY;
                return true;
            }

            // Check Slot Swap Tracker
            int swpX = Settings.general.slotSwapHudX;
            int swpY = Settings.general.slotSwapHudY;
            if (mX >= swpX && mX <= swpX + 120 && mY >= swpY && mY <= swpY + 36) {
                draggedHud = DraggedHud.SLOT_SWAP;
                dragOffsetX = mX - swpX;
                dragOffsetY = mY - swpY;
                return true;
            }

            // Check Equipment Stats Tracker
            int eqpX = Settings.general.equipmentStatsHudX;
            int eqpY = Settings.general.equipmentStatsHudY;
            if (mX >= eqpX && mX <= eqpX + 120 && mY >= eqpY && mY <= eqpY + 76) {
                draggedHud = DraggedHud.EQUIPMENT_STATS;
                dragOffsetX = mX - eqpX;
                dragOffsetY = mY - eqpY;
                return true;
            }

            // Check Powder Chest HUD
            int pchX = Settings.general.powderChestHudX;
            int pchY = Settings.general.powderChestHudY;
            if (mX >= pchX && mX <= pchX + 140 && mY >= pchY && mY <= pchY + 14) {
                draggedHud = DraggedHud.POWDER_CHEST;
                dragOffsetX = mX - pchX;
                dragOffsetY = mY - pchY;
                return true;
            }

            // Check Pest Tracker HUD
            int pstX = Settings.general.pestTimersX;
            int pstY = Settings.general.pestTimersY;
            if (mX >= pstX && mX <= pstX + 160 && mY >= pstY && mY <= pstY + 60) {
                draggedHud = DraggedHud.PEST_TRACKER;
                dragOffsetX = mX - pstX;
                dragOffsetY = mY - pstY;
                return true;
            }

            // Check Arrow Type Tracker
            int arrX = Settings.general.arrowTypeTrackerX;
            int arrY = Settings.general.arrowTypeTrackerY;
            if (mX >= arrX && mX <= arrX + 170 && mY >= arrY && mY <= arrY + 14) {
                draggedHud = DraggedHud.ARROW_TYPE;
                dragOffsetX = mX - arrX;
                dragOffsetY = mY - arrY;
                return true;
            }

            // Check Timer HUD
            int tmrX = Settings.general.timerHudX;
            int tmrY = Settings.general.timerHudY;
            if (mX >= tmrX && mX <= tmrX + 120 && mY >= tmrY && mY <= tmrY + 14) {
                draggedHud = DraggedHud.TIMER;
                dragOffsetX = mX - tmrX;
                dragOffsetY = mY - tmrY;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            if (draggedHud != null) {
                draggedHud = null;
                Settings.save();
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
        Settings.save();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
