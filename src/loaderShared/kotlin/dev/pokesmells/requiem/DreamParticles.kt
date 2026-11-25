package dev.pokesmells.requiem

import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class DreamParticles {
    private data class Layer(val radius: Float, val drift: Float, val brightness: Float, val size: Int)
    private data class Star(val angle: Float, val radius: Float, val phase: Float, val speed: Float)

    private val layers = listOf(
        Layer(0.79f, 0.0014f, 0.55f, 1),
        Layer(1.04f, 0.0031f, 0.75f, 1),
        Layer(1.29f, 0.0062f, 0.97f, 2),
    )
    private val stars = Random(2871).let { random ->
        List(600) {
            Star(random.nextFloat() * TAU, 0.08f + random.nextFloat() * 0.90f,
                random.nextFloat(), 0.5f + random.nextFloat() * 0.5f)
        }
    }

    fun draw(
        graphics: GuiGraphics, width: Int, height: Int, time: Float, opacity: Float,
        warp: Float?, reducedMotion: Boolean, count: Int, color: Int, accent: Int, colorful: Boolean,
    ) {
        val total = count.coerceIn(0, stars.size)
        if (opacity <= 0f || total == 0) return
        val clock = if (reducedMotion) 0f else time
        val centerX = width * 0.5f
        val centerY = height * 0.46f
        val radius = maxOf(width, height) * 0.69f
        val gather = if (warp == null) 0f else smoothStep(warp / 0.39f)
        val acceleration = if (warp == null) 0f else smoothStep((warp - 0.30f) / 0.70f)
        val warpDensity = if (warp == null) 0f else smoothStep((warp - 0.33f) / 0.32f)
        val limit = if (warpDensity > 0f) minOf(stars.size, total * 2) else total
        if (warp != null && !reducedMotion) {
            val glow = smoothStep(warp / 0.36f) * (1f - smoothStep((warp - 0.48f) / 0.52f))
            drawGatherGlow(graphics, centerX.toInt(), centerY.toInt(), accent, glow * opacity)
        }

        for ((layerIndex, layer) in layers.withIndex()) {
            for (index in layerIndex until limit step layers.size) {
                val star = stars[index]
                val wave = (1f + sin(star.phase * TAU + clock * (0.11f + star.speed * 0.12f))) * 0.5f
                val sparkle = wave * wave * wave
                val weight = if (index < total) 1f else warpDensity
                val entrance = if (reducedMotion || warp != null) 1f
                    else smoothStep((clock - star.phase * 15f) / 9f)
                val tint = if (warp != null) {
                    if (index % 4 == 0) accent else color
                } else if (colorful && index % 13 < 3) {
                    when (index % 13) {
                        0 -> 0x7EE8CB
                        1 -> 0xF3CDA2
                        else -> 0xD9BBF6
                    }
                } else if (index % 4 == 0) accent else color
                val brightness = opacity * weight * entrance * layer.brightness * (0.27f + 0.73f * sparkle)
                val drift = clock * layer.drift * (1f - gather)
                val angle = star.angle + drift + gather * (0.27f + layerIndex * 0.12f)
                val orbit = star.radius * radius * layer.radius
                val distance = orbit * (1f - gather * 0.78f) *
                    (1f + acceleration * acceleration * (4f + 3f * star.speed))
                val x = centerX + cos(angle) * distance
                val y = centerY + sin(angle) * distance * (0.72f + acceleration * 0.28f)
                if (warp == null || reducedMotion || acceleration < 0.025f) {
                    val lit = if (reducedMotion) 0.70f * opacity * layer.brightness else brightness
                    val flare = if (index % 7 == 0 && !reducedMotion) smoothStep((sparkle - 0.53f) / 0.47f) else 0f
                    drawStar(graphics, x.toInt(), y.toInt(), layer.size, tint, lit, flare)
                } else {
                    val length = (2f + acceleration * acceleration * distance * (0.07f + star.speed * 0.17f)).coerceAtMost(radius * 0.18f)
                    drawTrail(graphics, x, y, angle, length, layer.size, tint, brightness)
                }
            }
        }
    }

    private fun drawStar(
        graphics: GuiGraphics, x: Int, y: Int, size: Int, tint: Int, opacity: Float, flare: Float,
    ) {
        if (size > 1 || flare > 0f) {
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, tinted(tint, opacity * 0.13f))
        }
        if (flare > 0f) {
            val spread = 2 + (flare * 3f).toInt()
            val ray = tinted(tint, opacity * flare * 0.57f)
            graphics.fill(x - spread, y, x + size + spread, y + 1, ray)
            graphics.fill(x, y - spread, x + 1, y + size + spread, ray)
        }
        graphics.fill(x, y, x + size, y + size, tinted(tint, opacity))
    }

    private fun drawTrail(
        graphics: GuiGraphics, x: Float, y: Float, angle: Float, length: Float, size: Int, tint: Int, opacity: Float,
    ) {
        val tail = -length.toInt()
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(x, y, 0f)
        pose.mulPose(Axis.ZP.rotation(angle))
        if (size > 1) graphics.fill(tail, -1, size + 2, size + 1, tinted(tint, opacity * 0.18f))
        graphics.fill(tail, 0, tail / 2, 1, tinted(tint, opacity * 0.22f))
        graphics.fill(tail / 2, 0, tail / 5, 1, tinted(tint, opacity * 0.53f))
        graphics.fill(tail / 5, 0, size, size, tinted(tint, opacity))
        graphics.fill(0, 0, 1, 1, tinted(0xF5F6FF, opacity * 0.80f))
        pose.popPose()
    }

    private fun drawGatherGlow(graphics: GuiGraphics, x: Int, y: Int, tint: Int, intensity: Float) {
        for (step in 1..5) {
            val radius = step * 5
            for (offset in -radius..radius step 2) {
                val halfWidth = radius - kotlin.math.abs(offset)
                graphics.fill(x - halfWidth, y + offset, x + halfWidth + 1, y + offset + 2,
                    tinted(tint, intensity * 0.018f))
            }
        }
    }
}

internal fun tinted(rgb: Int, opacity: Float): Int =
    ((opacity.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xffffff)

private const val TAU = 6.2831855f
