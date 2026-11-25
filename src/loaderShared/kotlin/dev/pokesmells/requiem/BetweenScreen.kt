package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class BetweenScreen(
    private val view: DeathView,
    private val send: (DeathChoice) -> Unit,
) : Screen(label("screen")) {
    private val particles = DreamParticles()
    private val config get() = RequiemSettings.config
    private var age = 0
    private var skip: Button? = null
    private val reducedMotion = !config.playCinematics || config.reducedMotion || config.plainInterface
    private val timing = CinematicTiming.forSettings(reducedMotion, config.warpTicks, config.revealTicks)

    private fun sceneClock(partialTick: Float): Float = (age - 1).coerceAtLeast(0) + partialTick.coerceIn(0f, 1f)

    internal fun cameraTilt(partialTick: Float): Float =
        if (!view.intro || reducedMotion || !config.cameraSettle) 0f
        else -4f * (1f - timing.reveal(sceneClock(partialTick)))

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = view.intro

    override fun init() {
        if (age == 0 && !config.muteCinematics && config.playCinematics) {
            val sound = if (view.intro) config.returnSound else config.deathSound
            ResourceLocation.tryParse(sound)?.let { key ->
                BuiltInRegistries.SOUND_EVENT.getOptional(key).ifPresent {
                    Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(it, 1f))
                }
            }
        }
        if (view.intro) {
            skip = addRenderableWidget(Button.builder(label("skip")) { onClose() }
                .bounds(width - 82, 10, 72, 20).build())
            return
        }
        var top = height / 2 + 20
        fun button(text: String, action: () -> Unit) {
            val buttonWidth = minOf(200, width - 20)
            val x = (width - buttonWidth) / 2
            val message = Component.literal(text)
            val button = if (config.plainInterface) {
                Button.builder(message) { action() }.bounds(x, top, buttonWidth, 20).build()
            } else {
                DreamButton(x, top, buttonWidth, message, config.accentColor, action)
            }
            addRenderableWidget(button)
            top += 24
        }
        if (view.lives != 0) button(config.returnText) { choose(Decision.RETURN) }
        if (view.canFinalize) button(config.finalText) {
            minecraft?.setScreen(ConfirmScreen({ confirmed ->
                minecraft?.setScreen(this)
                if (confirmed) choose(Decision.FINAL)
            }, label("confirm_title"), label("confirm_body")))
        }
        if (view.canDefer) button(config.deferText) { choose(Decision.DEFER) }
    }

    private fun choose(choice: Decision) {
        children().filterIsInstance<Button>().forEach { it.active = false }
        send(DeathChoice(view.transaction, choice))
    }

    override fun tick() {
        age++
        if (view.intro && age > timing.duration) onClose()
        if (minecraft?.level == null) onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val clock = sceneClock(partialTick)
        val timing = timing
        val reveal = if (view.intro) timing.reveal(clock) else 0f
        val emergence = if (view.intro || reducedMotion) 1f else smoothStep(clock / 14f)
        val darkness = if (view.intro) 1f - reveal else emergence
        val warp = if (view.intro && !reducedMotion) (clock / timing.revealStart).coerceIn(0f, 1f) else null
        val veil = if (warp != null) smoothStep((warp - 0.72f) / 0.28f) else 0f
        val textColor = 0xff000000.toInt() or (config.textColor and 0xffffff)

        graphics.fillGradient(0, 0, width, height, tinted(0x070814, darkness), tinted(config.skyColor, darkness))
        if (!config.plainInterface) {
            particles.draw(graphics, width, height, clock, darkness * (1f - veil), warp, reducedMotion,
                config.starCount, config.starColor, config.accentColor)
        }
        if (view.intro && !reducedMotion) {
            graphics.fill(0, 0, width, height, tinted(0x03040b, veil * darkness))
        }
        skip?.setAlpha(darkness)
        if (!view.intro) {
            val scale = config.titleScale.coerceIn(0.75, 3.0).toFloat()
            val pose = graphics.pose()
            pose.pushPose()
            pose.translate(width / 2f, height / 2f - 70, 0f)
            pose.scale(scale, scale, 1f)
            graphics.drawCenteredString(font, config.title, 0, 0, textColor)
            pose.popPose()
            graphics.drawWordWrap(font, Component.literal(config.subtitle), width / 8, height / 2 - 42,
                width * 3 / 4, textColor)
            val lives = if (view.lives < 0) label("unlimited") else label("lives", view.lives)
            graphics.drawCenteredString(font, lives, width / 2, height / 2 - 12, textColor)
            val cause = label("cause.${view.cause.name.lowercase(java.util.Locale.ROOT)}")
            graphics.drawCenteredString(font, cause, width / 2, height / 2 + 1, textColor)
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = Unit
}

private class DreamButton(
    x: Int, y: Int, width: Int, message: Component, private val accent: Int, action: () -> Unit,
) : Button(x, y, width, 20, message, { action() }, DEFAULT_NARRATION) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val focused = active && isHoveredOrFocused
        val border = (if (focused) 0xff000000.toInt() else 0x80000000.toInt()) or (accent and 0xffffff)
        val base = if (focused) 0xe0303e67.toInt() else 0xe019203b.toInt()
        graphics.fill(x, y, x + width, y + height, border)
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, base, 0xe012172e.toInt())
        val font = Minecraft.getInstance().font
        renderScrollingString(graphics, font, 6, if (active) 0xffeff3ff.toInt() else 0xffa3acc8.toInt())
    }
}

private fun label(key: String, vararg args: Any) = Component.translatable("requiem_cinematics.$key", *args)
