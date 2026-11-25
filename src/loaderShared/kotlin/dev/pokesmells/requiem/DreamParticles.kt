package dev.pokesmells.requiem

import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

internal class DreamParticles {
    private data class Layer(val brightness: Float, val size: Int)
    private data class Star(val x: Float, val y: Float, val phase: Float, val rate: Float)

    private val layers = listOf(Layer(0.63f, 1), Layer(0.82f, 1), Layer(0.98f, 2))
    private val stars = Random(2871).let { random ->
        List(1800) {
            Star((random.nextFloat() - 0.5f) * 2.15f, (random.nextFloat() - 0.5f) * 2.15f,
                random.nextFloat(), 0.08f + random.nextFloat() * 0.13f)
        }
    }

    fun draw(
        graphics: GuiGraphics, width: Int, height: Int, skyClock: Float, entranceClock: Float,
        opacity: Float, flightTicks: Float?, reducedMotion: Boolean, count: Int,
        color: Int, accent: Int, colorful: Boolean,
    ) {
        val countAtRest = count.coerceIn(0, 600)
        if (opacity <= 0f || countAtRest == 0) return
        val blend = flightTicks?.let { smoothStep(it / 15f) } ?: 0f
        if (blend < 1f) {
            drawSky(graphics, width, height, skyClock, entranceClock, opacity * (1f - blend), reducedMotion,
                countAtRest, color, accent, colorful)
        }
        if (flightTicks != null && blend > 0f) {
            drawFlight(graphics, width, height, flightTicks, opacity * blend, countAtRest * 3, color, accent)
        }
    }

    private fun drawSky(
        graphics: GuiGraphics, width: Int, height: Int, clock: Float, entranceClock: Float, opacity: Float,
        reducedMotion: Boolean, count: Int, color: Int, accent: Int, colorful: Boolean,
    ) {
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        for (index in 0 until count) {
            val star = stars[index]
            val layer = layers[index % layers.size]
            val wave = if (reducedMotion) 0.78f else (1f + sin(star.phase * TAU + clock * star.rate)) * 0.5f
            val flare = wave * wave
            val entrance = if (reducedMotion) 1f else smoothStep((entranceClock - star.phase * 15f) / 13f)
            val tint = if (colorful && index % 17 < 3) {
                when (index % 17) {
                    0 -> 0x72EBCD
                    1 -> 0xFFD9A1
                    else -> 0xDCB3FF
                }
            } else if (index % 4 == 0) accent else color
            val brightness = opacity * entrance * layer.brightness * (0.30f + 0.70f * flare)
            val rays = if (index % 8 == 0 && !reducedMotion) smoothStep((flare - 0.38f) / 0.62f) else 0f
            drawStar(graphics, (centerX * (1f + star.x)).toInt(), (centerY * (1f + star.y)).toInt(),
                layer.size, tint, brightness, rays)
        }
    }

    private fun drawFlight(
        graphics: GuiGraphics, width: Int, height: Int, clock: Float, opacity: Float,
        count: Int, color: Int, accent: Int,
    ) {
        val travel = StarFlight.distance(clock)
        val speed = StarFlight.speed(clock)
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f
        for (index in 0 until count) {
            val star = stars[index]
            val layer = layers[index % layers.size]
            val depth = StarFlight.depth(StarFlight.initialDepth(index, count, star.phase), travel)
            val exposure = 0.025f + speed * (1.9f + star.phase * 2.5f)
            val projection = 2.65f / depth
            val x = star.x * halfWidth * projection
            val y = star.y * halfHeight * projection
            val tailRatio = depth / (depth + exposure)
            if (kotlin.math.abs(x * tailRatio) > halfWidth + 3f ||
                kotlin.math.abs(y * tailRatio) > halfHeight + 3f
            ) continue

            val brightness = opacity * StarFlight.visibility(depth) * (0.60f + star.phase * 0.40f) * layer.brightness
            if (brightness < 0.008f) continue
            val tint = if (index % 4 == 0) accent else color
            val length = hypot(x, y) * (1f - tailRatio)
            drawTrail(graphics, halfWidth + x, halfHeight + y, atan2(y, x), length, layer.size, tint, brightness)
        }
    }

    private fun drawStar(
        graphics: GuiGraphics, x: Int, y: Int, size: Int, tint: Int, opacity: Float, flare: Float,
    ) {
        if (size > 1 || flare > 0f) {
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, tinted(tint, opacity * 0.15f))
        }
        if (flare > 0f) {
            val reach = 2 + (flare * 4f).toInt()
            val ray = tinted(tint, opacity * flare * 0.74f)
            graphics.fill(x - reach, y, x + size + reach, y + 1, ray)
            graphics.fill(x, y - reach, x + 1, y + size + reach, ray)
        }
        graphics.fill(x, y, x + size, y + size, tinted(tint, opacity))
        if (flare > 0.4f) graphics.fill(x, y, x + 1, y + 1, tinted(0xF6F9FF, opacity * flare))
    }

    private fun drawTrail(
        graphics: GuiGraphics, x: Float, y: Float, angle: Float, length: Float, size: Int, tint: Int, opacity: Float,
    ) {
        val tail = -length.toInt().coerceAtLeast(1)
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(x, y, 0f)
        pose.mulPose(Axis.ZP.rotation(angle))
        if (size > 1) graphics.fill(tail, -1, size + 2, size + 1, tinted(tint, opacity * 0.27f))
        graphics.fill(tail, 0, tail / 2, 1, tinted(tint, opacity * 0.25f))
        graphics.fill(tail / 2, 0, tail / 5, 1, tinted(tint, opacity * 0.60f))
        graphics.fill(tail / 5, 0, size, size, tinted(tint, opacity))
        graphics.fill(0, 0, 1, 1, tinted(0xF2F8FF, opacity * 0.82f))
        pose.popPose()
    }
}

internal fun tinted(rgb: Int, opacity: Float): Int =
    ((opacity.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xffffff)

private const val TAU = 6.2831855f
