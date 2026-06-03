package net.minecraft.client.gui.screen

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component

abstract class Screen(title: Component) : net.minecraft.client.gui.screens.Screen(title) {
    protected val client
        get() = minecraft

    protected val textRenderer
        get() = font

    protected fun <T> addDrawableChild(widget: T): T
        where T : GuiEventListener, T : Renderable, T : NarratableEntry {
        return addRenderableWidget(widget)
    }

    protected fun clearChildren() {
        clearWidgets()
    }

    open fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        extractBackground(context, mouseX, mouseY, delta)
    }

    final override fun extractBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
    }

    open fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
    }

    final override fun extractRenderState(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        render(context, mouseX, mouseY, delta)
    }

    open fun close() {
        super.onClose()
    }

    final override fun onClose() {
        close()
    }

    open fun shouldPause(): Boolean = true

    final override fun isPauseScreen(): Boolean = shouldPause()
}

