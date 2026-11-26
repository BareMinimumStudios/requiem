package dev.pokesmells.requiem

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f
import net.minecraft.client.gui.GuiGraphics
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
        graphics: GuiGraphics, width: Int, height: Int, skyClock: Float,
        opacity: Float, flightTicks: Float?, reducedMotion: Boolean, count: Int,
        color: Int, accent: Int, colorful: Boolean, pointerX: Float? = null, pointerY: Float = 0f,
    ) {
        val anchorCount = count.coerceIn(0, 600)
        if (opacity <= 0f || anchorCount == 0) return
        val travel = flightTicks?.let(StarFlight::distance) ?: 0f
        val speed = flightTicks?.let(StarFlight::speed) ?: 0f
        val stretch = flightTicks?.let(StarFlight::stretch) ?: 0f
        val limit = if (stretch > 0f) anchorCount * 3 else anchorCount
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f
        val vertices = graphics.bufferSource().getBuffer(RenderType.gui())
        val pose = graphics.pose().last().pose()

        for (index in 0 until limit) {
            val star = stars[index]
            val layer = layers[index % layers.size]
            val anchored = index < anchorCount
            val initialDepth = if (anchored) StarFlight.anchorDepth(index, anchorCount, star.phase)
                else StarFlight.initialDepth(index - anchorCount, anchorCount * 2, star.phase)
            val depth = StarFlight.depth(initialDepth, travel)
            val projection = if (anchored) StarFlight.anchorProjection(initialDepth, travel) else 2.65f / depth
            val x = star.x * halfWidth * projection
            val y = star.y * halfHeight * projection
            val exposure = speed * (1.9f + star.phase * 2.5f)
            val tailRatio = depth / (depth + exposure)
            if (kotlin.math.abs(x * tailRatio) > halfWidth + 6f ||
                kotlin.math.abs(y * tailRatio) > halfHeight + 6f
            ) continue

            val wave = if (reducedMotion) 0.78f else
                (1f + sin(star.phase * TAU + skyClock * star.rate)) * 0.5f
            val flare = smoothStep(wave) * wave
            val stillGlow = 0.30f + 0.70f * flare
            val flightGlow = 0.60f + star.phase * 0.40f
            val brightness = if (anchored) {
                val entrance = if (reducedMotion) 1f else smoothStep((skyClock - star.phase * 15f) / 13f)
                val visibility = 1f + (StarFlight.visibility(depth) - 1f) * stretch
                opacity * layer.brightness * entrance * visibility * (stillGlow + (flightGlow - stillGlow) * stretch)
            } else {
                opacity * layer.brightness * StarFlight.visibility(depth) * flightGlow * stretch
            }
            if (brightness < 0.008f) continue

            val flightTint = if (index % 4 == 0) accent else color
            val restTint = if (colorful && index % 17 < 3) {
                when (index % 17) {
                    0 -> 0x72EBCD
                    1 -> 0xFFD9A1
                    else -> 0xDCB3FF
                }
            } else flightTint
            val tint = if (anchored) mixRgb(restTint, flightTint, stretch) else flightTint
            var headX = halfWidth + x
            var headY = halfHeight + y
            if (pointerX != null) {
                val dx = headX - pointerX
                val dy = headY - pointerY
                val radius = minOf(width, height) * 0.20f
                val distance = hypot(dx, dy)
                if (distance in 0.01f..<radius) {
                    val push = 8f * smoothStep(1f - distance / radius) * distance / (distance + 12f)
                    headX += dx * push / distance
                    headY += dy * push / distance
                }
            }
            if (anchored && stretch < 1f) {
                val rays = if (index % 8 == 0 && !reducedMotion) smoothStep((flare - 0.38f) / 0.62f) else 0f
                drawStar(graphics, headX.toInt(), headY.toInt(), layer.size, tint,
                    brightness * (1f - stretch), rays)
            }
            if (stretch > 0f) {
                val radius = hypot(x, y).coerceAtLeast(0.001f)
                val trailOpacity = if (anchored) brightness * stretch else brightness
                val tailX = halfWidth + x * tailRatio
                val tailY = halfHeight + y * tailRatio
                val acrossX = -y / radius
                val acrossY = x / radius
                if (layer.size > 1) {
                    drawTrail(vertices, pose, headX, headY, tailX, tailY, acrossX, acrossY,
                        1.48f, tint, trailOpacity * 0.22f)
                }
                drawTrail(vertices, pose, headX, headY, tailX, tailY, acrossX, acrossY,
                    layer.size * 0.48f, mixRgb(tint, 0xF2F8FF, 0.24f), trailOpacity)
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
        vertices: VertexConsumer, pose: Matrix4f, x: Float, y: Float, tailX: Float, tailY: Float,
        acrossX: Float, acrossY: Float, width: Float, tint: Int, opacity: Float,
    ) {
        val edgeX = acrossX * width
        val edgeY = acrossY * width
        val tip = tinted(tint, 0f)
        val head = tinted(tint, opacity)
        vertices.addVertex(pose, tailX + edgeX * 0.14f, tailY + edgeY * 0.14f, 0f).setColor(tip)
        vertices.addVertex(pose, x + edgeX, y + edgeY, 0f).setColor(head)
        vertices.addVertex(pose, x - edgeX, y - edgeY, 0f).setColor(head)
        vertices.addVertex(pose, tailX - edgeX * 0.14f, tailY - edgeY * 0.14f, 0f).setColor(tip)
    }
}

internal fun tinted(rgb: Int, opacity: Float): Int =
    ((opacity.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xffffff)

private const val TAU = 6.2831855f
