package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class LimboScreen : Screen(Component.translatable("requiem_cinematics.limbo")) {
    private val particles = DreamParticles()
    private val config get() = RequiemSettings.config
    private val reducedMotion get() = config.reducedMotion || !config.playCinematics || config.plainInterface
    private var age = 0

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = true

    override fun init() {
        val buttonWidth = minOf(112, width.coerceAtLeast(1))
        addRenderableWidget(dreamButton((width - buttonWidth) / 2, (height - 34).coerceAtLeast(0),
            buttonWidth, Component.translatable("requiem_cinematics.limbo_menu")) { onClose() })
    }

    override fun onClose() {
        minecraft?.setScreen(PauseScreen(true))
    }

    override fun tick() {
        age++
    }

    @Suppress("DEPRECATION")
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val partial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
        val time = if (reducedMotion) 0f else (age + partial) * 0.55f
        graphics.fillGradient(0, 0, width, height, 0xff080b1b.toInt(), 0xff18243b.toInt())
        if (!config.plainInterface) {
            if (config.etherealSky) EtherealSky.render(graphics, time, 0.88f, 0x70b9b8)
            graphics.drawManaged {
                particles.draw(graphics, width, height, time + 25f, 0.45f, null, reducedMotion,
                    config.starCount.coerceAtMost(180), 0xa2d8d6, 0x809fd6, false)
            }
        }
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 14, config.textColor or 0xff000000.toInt())
        val caption = Component.literal(config.limboText)
        val lines = font.split(caption, (width - 24).coerceIn(1, 270))
        lines.forEachIndexed { index, line ->
            graphics.drawCenteredString(font, line, width / 2, height / 2 + 4 + index * font.lineHeight,
                tinted(config.textColor, 0.73f))
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = Unit
}
