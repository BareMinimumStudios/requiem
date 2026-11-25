package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

internal fun dreamButton(
    x: Int, y: Int, width: Int, message: Component, action: () -> Unit,
): Button {
    val config = RequiemSettings.config
    return DreamButton(x, y, width, message, config.accentColor, config.plainInterface,
        config.reducedMotion || !config.playCinematics, action)
}

private class DreamButton(
    x: Int, y: Int, width: Int, message: Component, private val accent: Int,
    private val plain: Boolean, private val reducedMotion: Boolean, action: () -> Unit,
) : Button(x, y, width, 20, message, {
    RequiemAudio.click()
    action()
}, DEFAULT_NARRATION) {
    private var highlight = 0f
    private var lastFrame = 0L
    private var wasHovered = false

    override fun playDownSound(soundManager: SoundManager) = Unit

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val hovered = active && isHovered
        if (hovered && !wasHovered) RequiemAudio.hover()
        wasHovered = hovered
        if (plain) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick)
            return
        }
        val target = if (active && isHoveredOrFocused) 1f else 0f
        val now = System.nanoTime()
        val elapsed = ((now - lastFrame) / 1_000_000f).coerceIn(0f, 50f)
        lastFrame = now
        highlight = if (reducedMotion || !active) target else
            highlight + (target - highlight).coerceIn(-elapsed / 155f, elapsed / 110f)
        val eased = smoothStep(highlight)
        val keyboardFocus = active && isFocused
        val border = tinted(accent, alpha * (0.46f + 0.49f * eased))
        val top = tinted(mixRgb(0x19203b, 0x354772, eased), alpha * 0.92f)
        val bottom = tinted(mixRgb(0x12172e, 0x202d4b, eased), alpha * 0.94f)
        graphics.fill(x, y, x + width, y + height, border)
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, top, bottom)
        if (eased > 0.03f) {
            val center = x + width / 2
            val span = ((width - 12) * eased * 0.5f).roundToInt()
            graphics.fill(center - span, y + height - 2, center + span, y + height - 1,
                tinted(accent, alpha * eased * 0.72f))
        }
        if (keyboardFocus) {
            val mark = tinted(0xE0ECFF, alpha * 0.95f)
            graphics.fill(x, y, x + 4, y + 1, mark)
            graphics.fill(x, y, x + 1, y + 4, mark)
            graphics.fill(x + width - 4, y + height - 1, x + width, y + height, mark)
            graphics.fill(x + width - 1, y + height - 4, x + width, y + height, mark)
        }
        val foreground = if (active) 0xEFF3FF else 0xA3ACC8
        renderScrollingString(graphics, Minecraft.getInstance().font, 6, tinted(foreground, alpha))
    }
}

internal fun mixRgb(from: Int, to: Int, amount: Float): Int {
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 255
        val b = (to shr shift) and 255
        return (a + (b - a) * amount).roundToInt()
    }
    return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
