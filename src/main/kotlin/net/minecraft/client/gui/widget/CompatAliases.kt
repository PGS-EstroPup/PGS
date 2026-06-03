package net.minecraft.client.gui.widget

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

typealias ClickableWidget = net.minecraft.client.gui.components.AbstractWidget
typealias SliderWidget = net.minecraft.client.gui.components.AbstractSliderButton

class TextFieldWidget(
    font: Font,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component
) : net.minecraft.client.gui.components.EditBox(font, x, y, width, height, message) {
    var text: String
        get() = value
        set(value) {
            setValue(value)
        }

    fun setChangedListener(listener: (String) -> Unit) {
        setResponder(listener)
    }

    fun setPlaceholder(message: Component) {
        setHint(message)
    }

    fun setTextPredicate(predicate: (String) -> Boolean) {
        // Minecraft 26 removed EditBox's public text predicate hook. Keep the
        // old call sites source-compatible; their responders still save values.
    }
}
