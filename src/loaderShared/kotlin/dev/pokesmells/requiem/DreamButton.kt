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
        val center = x + width / 2
        val midY = y + height / 2
        val halfSpan = (width * 0.36f).roundToInt()
        val haze = mixRgb(0x6076AA, accent, eased)
        for (row in -8..8) {
            val shape = 1f - (row * row) / 81f
            val span = (halfSpan * kotlin.math.sqrt(shape)).roundToInt()
            graphics.fill(center - span, midY + row, center + span, midY + row + 1,
                tinted(haze, alpha * shape * (0.08f - 0.04f * eased)))
        }
        if (eased > 0.03f) {
            val inset = 3
            val top = tinted(haze, alpha * eased * 0.17f)
            val bottom = tinted(haze, alpha * eased * 0.08f)
            graphics.fillGradient(x + inset, y + 2, x + width - inset, y + height - 2, top, bottom)
            val span = ((width - 12) * eased * 0.20f).roundToInt()
            graphics.fill(center - span, y + height - 2, center + span, y + height - 1,
                tinted(accent, alpha * eased * 0.72f))
        }
        if (keyboardFocus) {
            val mark = tinted(0xE0ECFF, alpha * 0.95f)
            for (side in 0..1) {
                val edge = if (side == 0) x + 6 else x + width - 6
                graphics.fill(edge - 1, midY - 2, edge + 2, midY + 3, mark)
                graphics.fill(edge - 2, midY - 1, edge + 3, midY + 2, mark)
            }
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
