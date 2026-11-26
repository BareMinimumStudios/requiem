package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class SpectatorFadeScreen(
    private val between: BetweenScreen,
    private val startClock: Float,
) : Screen(Component.translatable("requiem_cinematics.screen")) {
    private val config get() = RequiemSettings.config
    private val holdTicks = if (config.reducedMotion) 0 else 18
    private val fadeTicks = if (config.reducedMotion) 12 else config.spectatorRevealTicks.coerceIn(12, 100)
    private var age = 0

    override fun isPauseScreen() = false

    override fun tick() {
        if (age == 0) RequiemAudio.play(config.finalDeathSound, 0.22, 0.72f)
        age++
        if (age > holdTicks + fadeTicks || minecraft?.player?.isSpectator != true) onClose()
    }

    private fun reveal(clock: Float): Float = smoothStep((clock - holdTicks) / fadeTicks)

    internal fun audioFrame() = WarpAudioFrame(0f, 0f, reveal(age.toFloat()))

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val partial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
        val clock = (age - 1).coerceAtLeast(0) + partial.coerceIn(0f, 1f)
        between.renderStarField(graphics, startClock + clock * 0.72f, reveal(clock))
        if (holdTicks > 0 && config.finalDeathThought.isNotBlank()) {
            val alpha = smoothStep(clock / 7f) * (1f - smoothStep((clock - holdTicks) / 16f))
            if (alpha > 0.03f) {
                val line = Component.literal(config.finalDeathThought)
                val lines = font.split(line, (width - 24).coerceIn(1, 270))
                val top = (height - lines.size * font.lineHeight) / 2
                lines.forEachIndexed { index, text ->
                    graphics.drawCenteredString(font, text, width / 2, top + index * font.lineHeight,
                        tinted(config.textColor, alpha * 0.86f))
                }
            }
        }
    }
}
