package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class LimboScreen : Screen(Component.translatable("requiem_cinematics.limbo")) {
    private val particles = DreamParticles()
    private val config get() = RequiemSettings.config
    private val reducedMotion get() = config.reducedMotion || !config.playCinematics || config.plainInterface
    private var age = 0
    private var wakeStart: Int? = null
    private val wakeTicks get() = if (reducedMotion) 12 else config.limboWakeTicks.coerceIn(12, 100)

    internal fun beginWake() {
        if (wakeStart != null) return
        wakeStart = age
        children().filterIsInstance<Button>().forEach {
            it.visible = false
            it.active = false
        }
    }

    internal fun wakeAudioFrame(): WarpAudioFrame? {
        val start = wakeStart ?: return null
        val progress = smoothStep((age - start).toFloat() / wakeTicks)
        return WarpAudioFrame(0f, 0f, 0.13f + 0.87f * progress)
    }

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = true

    override fun init() {
        if (wakeStart != null) return
        val buttonWidth = minOf(112, width.coerceAtLeast(1))
        addRenderableWidget(dreamButton((width - buttonWidth) / 2, (height - 34).coerceAtLeast(0),
            buttonWidth, Component.translatable("requiem_cinematics.limbo_menu")) { onClose() })
    }

    override fun onClose() {
        minecraft?.setScreen(if (wakeStart == null) PauseScreen(true) else null)
    }

    override fun tick() {
        age++
        val start = wakeStart ?: return
        if (age > start + wakeTicks) onClose()
    }

    @Suppress("DEPRECATION")
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val partial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
        val clock = (age - 1).coerceAtLeast(0) + partial.coerceIn(0f, 1f)
        val time = if (reducedMotion) 0f else clock * 0.55f
        val waking = wakeStart?.let { (clock - it).coerceAtLeast(0f) }
        val lead = if (reducedMotion) 0 else 7
        val reveal = waking?.let { smoothStep((it - lead) / (wakeTicks - lead)) } ?: 0f
        val mist = 1f - reveal
        val stars = 1f - smoothStep((reveal - 0.12f) / 0.88f)
        graphics.fillGradient(0, 0, width, height, tinted(0x080b1b, mist), tinted(0x18243b, mist))
        if (!config.plainInterface) {
            if (config.etherealSky) EtherealSky.render(graphics, time, 0.88f * mist, 0x70b9b8)
            graphics.drawManaged {
                particles.draw(graphics, width, height, time + 25f, 0.45f * stars, null, reducedMotion,
                    config.starCount.coerceAtMost(180), 0xa2d8d6, 0x809fd6, false)
            }
        }
        val textOpacity = waking?.let { 1f - smoothStep(it / 12f) } ?: 1f
        if (textOpacity > 0.05f) renderCaption(graphics, textOpacity)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun renderCaption(graphics: GuiGraphics, opacity: Float) {
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 14, tinted(config.textColor, opacity))
        val caption = Component.literal(config.limboText)
        val lines = font.split(caption, (width - 24).coerceIn(1, 270))
        lines.forEachIndexed { index, line ->
            graphics.drawCenteredString(font, line, width / 2, height / 2 + 4 + index * font.lineHeight,
                tinted(config.textColor, 0.73f * opacity))
        }
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = Unit
}
