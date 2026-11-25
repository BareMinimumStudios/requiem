package dev.pokesmells.requiem

import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.random.Random

class BetweenScreen(
    private val view: DeathView,
    private val send: (DeathChoice) -> Unit,
) : Screen(label("screen")) {
    private data class Star(val x: Float, val y: Float, val phase: Double)
    private val stars = Random(2871).let { random ->
        List(600) { Star(random.nextFloat(), random.nextFloat(), random.nextDouble() * 6.283) }
    }
    private val config get() = RequiemSettings.config
    private var age = 0
    private val reducedMotion get() = !config.playCinematics || config.reducedMotion || config.plainInterface
    private val duration get() = if (reducedMotion) 12 else config.warpTicks.coerceIn(24, 160)

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
            addRenderableWidget(Button.builder(label("skip")) { onClose() }
                .bounds(width / 2 - 50, height - 30, 100, 20).build())
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
        if (view.intro && age >= duration) onClose()
        if (minecraft?.level == null) onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val clock = age + partialTick
        val sky = 0xff000000.toInt() or (config.skyColor and 0xffffff)
        val textColor = 0xff000000.toInt() or (config.textColor and 0xffffff)
        val opacity = if (view.intro) {
            ((duration - clock) / (if (reducedMotion) 12f else 15f)).coerceIn(0f, 1f)
        } else 1f
        val alpha = (opacity * 255).toInt() shl 24
        graphics.fillGradient(0, 0, width, height, alpha or 0x070814, alpha or (sky and 0xffffff))
        if (!config.plainInterface) {
            val centerX = width * 0.5f
            val centerY = height * 0.5f
            val warpProgress = (clock / duration).coerceIn(0f, 1f)
            val warping = view.intro && !reducedMotion
            val count = config.starCount.coerceIn(0, stars.size)
            for (index in 0 until count) {
                val star = stars[index]
                val twinkle = if (reducedMotion) 0.75 else 0.65 + sin(star.phase + clock / 28) * 0.25
                val color = ((opacity * twinkle * 255).toInt() shl 24) or (config.starColor and 0xffffff)
                val drift = if (reducedMotion) 0.0 else cos(star.phase + clock / 75) * 3
                val rawX = star.x * width + drift.toFloat()
                val rawY = star.y * height
                if (warping) {
                    val angle = atan2(rawY - centerY, rawX - centerX)
                    val initialDepth = (star.phase / (Math.PI * 2)).toFloat()
                    val depth = (initialDepth + warpProgress * (0.7f + star.y * 0.5f)) % 1f
                    val distance = depth * depth * width * 0.65f
                    val length = (3 + depth * depth * warpProgress * width * 0.14f).toInt()
                    val pose = graphics.pose()
                    pose.pushPose()
                    pose.translate(centerX, centerY, 0f)
                    pose.mulPose(Axis.ZP.rotation(angle))
                    val tip = distance.toInt()
                    graphics.fill((tip - length).coerceAtLeast(0), 0, tip + 1, 1, color)
                    pose.popPose()
                } else {
                    val x = rawX.toInt()
                    val y = rawY.toInt()
                    graphics.fill(x, y, x + 1, y + 1, color)
                }
            }
        }
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

