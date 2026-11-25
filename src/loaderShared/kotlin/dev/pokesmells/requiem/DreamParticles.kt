package dev.pokesmells.requiem

import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

internal class DreamParticles {
    private data class Layer(val depth: Float, val brightness: Float, val size: Int)
    private data class Star(val x: Float, val y: Float, val phase: Float, val rate: Float, val depthOffset: Float)

    private val layers = listOf(Layer(4.1f, 0.63f, 1), Layer(2.9f, 0.82f, 1), Layer(1.6f, 0.98f, 2))
    private val stars = Random(2871).let { random ->
        List(1800) {
            Star((random.nextFloat() - 0.5f) * 2.15f, (random.nextFloat() - 0.5f) * 2.15f,
                random.nextFloat(), 0.08f + random.nextFloat() * 0.13f, random.nextFloat() - 0.5f)
        }
    }

    fun draw(
        graphics: GuiGraphics, width: Int, height: Int, skyClock: Float, entranceClock: Float,
        opacity: Float, flightTicks: Float?, reducedMotion: Boolean, count: Int,
        color: Int, accent: Int, colorful: Boolean,
    ) {
        val total = count.coerceIn(0, 600)
        if (opacity <= 0f || total == 0) return
        val travel = flightTicks?.let(StarFlight::distance) ?: 0f
        val speed = flightTicks?.let(StarFlight::speed) ?: 0f
        val warpBlend = smoothStep(speed / 0.19f)
        val limit = if (warpBlend > 0f) total * 3 else total
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f

        for ((layerIndex, layer) in layers.withIndex()) {
            for (index in layerIndex until limit step layers.size) {
                val star = stars[index]
                val wave = if (reducedMotion) 0.78f else (1f + sin(star.phase * TAU + skyClock * star.rate)) * 0.5f
                val flare = wave * wave
                val weight = if (index < total) 1f else warpBlend
                val entrance = if (reducedMotion || flightTicks != null) 1f
                    else smoothStep((entranceClock - star.phase * 15f) / 13f)
                val colorAtRest = if (colorful && index % 17 < 3) {
                    when (index % 17) {
                        0 -> 0x72EBCD
                        1 -> 0xFFD9A1
                        else -> 0xDCB3FF
                    }
                } else if (index % 4 == 0) accent else color
                val tint = mixRgb(colorAtRest, if (index % 4 == 0) accent else color, warpBlend)
                val brightness = opacity * weight * entrance * layer.brightness *
                    (0.30f + 0.70f * (flare + (1f - flare) * warpBlend * 0.78f))
                val initialDepth = layer.depth + star.depthOffset
                val depth = StarFlight.depth(initialDepth, travel)
                val ratio = initialDepth / depth
                val x = star.x * halfWidth * ratio
                val y = star.y * halfHeight * ratio
                val farFade = if (initialDepth - travel < StarFlight.NEAR) smoothStep((StarFlight.FAR - depth) / 0.70f) else 1f
                val glow = brightness * farFade
                if (glow < 0.009f) continue

                if (speed < 0.012f) {
                    if (kotlin.math.abs(x) > halfWidth + 5f || kotlin.math.abs(y) > halfHeight + 5f) continue
                    val rays = if (index % 8 == 0 && !reducedMotion) smoothStep((flare - 0.38f) / 0.62f) else 0f
                    drawStar(graphics, (centerX + x).toInt(), (centerY + y).toInt(), layer.size, tint, glow, rays)
                } else {
                    val exposure = speed * (1.5f + star.phase * 2.1f)
                    val trailingRatio = depth / (depth + exposure)
                    if (kotlin.math.abs(x * trailingRatio) > halfWidth + 4f ||
                        kotlin.math.abs(y * trailingRatio) > halfHeight + 4f
                    ) continue
                    val length = hypot(x, y) * (1f - trailingRatio)
                    drawTrail(graphics, centerX + x, centerY + y, atan2(y, x), length, layer.size, tint, glow)
                }
            }
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

    private fun mixRgb(from: Int, to: Int, fraction: Float): Int {
        val red = channel(from, to, 16, fraction)
        val green = channel(from, to, 8, fraction)
        val blue = channel(from, to, 0, fraction)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun channel(from: Int, to: Int, shift: Int, fraction: Float): Int {
        val start = (from shr shift) and 255
        val end = (to shr shift) and 255
        return (start + (end - start) * fraction).toInt()
    }
}

internal fun tinted(rgb: Int, opacity: Float): Int =
    ((opacity.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xffffff)

private const val TAU = 6.2831855f
