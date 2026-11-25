package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class SpectatorFadeScreen(
    private val between: BetweenScreen,
    private val startClock: Float,
) : Screen(Component.translatable("requiem_cinematics.screen")) {
    private val config get() = RequiemSettings.config
    private val holdTicks = if (config.reducedMotion) 0 else 6
    private val fadeTicks = if (config.reducedMotion) 12 else config.spectatorRevealTicks.coerceIn(12, 100)
    private var age = 0

    override fun isPauseScreen() = false

    override fun tick() {
        age++
        if (age > holdTicks + fadeTicks || minecraft?.player?.isSpectator != true) onClose()
    }

    private fun reveal(clock: Float): Float = smoothStep((clock - holdTicks) / fadeTicks)

    internal fun audioFrame() = WarpAudioFrame(0f, 0f, reveal(age.toFloat()))

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val partial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
        val clock = (age - 1).coerceAtLeast(0) + partial.coerceIn(0f, 1f)
        between.renderStarField(graphics, startClock + clock, reveal(clock))
    }
}
