package dev.pokesmells.requiem

import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class DreamParticles {
    private data class Layer(val scale: Float, val speed: Float, val opacity: Float, val size: Int)
    private data class Mote(val orbit: Float, val distance: Float, val phase: Float, val depth: Float)

    private val layers = listOf(
        Layer(0.82f, 0.0018f, 0.42f, 1),
        Layer(1.05f, 0.0035f, 0.68f, 1),
        Layer(1.30f, 0.0065f, 0.86f, 2),
    )
    private val motes = Random(2871).let { random ->
        List(600) {
            Mote(random.nextFloat() * TAU, 0.12f + random.nextFloat() * 0.86f,
                random.nextFloat() * TAU, random.nextFloat())
        }
    }

    fun draw(
        graphics: GuiGraphics, width: Int, height: Int, time: Float, opacity: Float,
        warp: Float?, reducedMotion: Boolean, count: Int, color: Int, accent: Int,
    ) {
        if (opacity <= 0f) return
        val clock = if (reducedMotion) 0f else time
        val total = count.coerceIn(0, motes.size)
        val centerX = width * 0.5f
        val centerY = height * 0.46f
        val radius = maxOf(width, height) * 0.68f
        for ((layerIndex, layer) in layers.withIndex()) {
            val first = total * layerIndex / layers.size
            val last = total * (layerIndex + 1) / layers.size
            for (index in first until last) {
                val mote = motes[index]
                val glow = opacity * layer.opacity * (0.73f + 0.27f * sin(mote.phase + clock * 0.075f))
                val tint = if (index % 4 == 0) accent else color
                if (warp == null || reducedMotion) {
                    val angle = mote.orbit + clock * layer.speed
                    val current = sin(mote.phase + clock * 0.033f) * 3f
                    val distance = mote.distance * radius * layer.scale
                    val x = (centerX + cos(angle) * distance).toInt()
                    val y = (centerY + sin(angle) * distance * 0.68f + current).toInt()
                    drawMote(graphics, x, y, layer.size, tint, glow, index % 11 == 0)
                } else {
                    drawTrail(graphics, mote, layer, centerX, centerY, radius, warp, tint, glow)
                }
            }
        }
    }

    private fun drawMote(
        graphics: GuiGraphics, x: Int, y: Int, size: Int, tint: Int, opacity: Float, spark: Boolean,
    ) {
        if (size > 1 || spark) {
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, tinted(tint, opacity * 0.12f))
        }
        if (spark) {
            graphics.fill(x - 2, y, x + size + 2, y + 1, tinted(tint, opacity * 0.40f))
            graphics.fill(x, y - 2, x + 1, y + size + 2, tinted(tint, opacity * 0.40f))
        }
        graphics.fill(x, y, x + size, y + size, tinted(tint, opacity))
    }

    private fun drawTrail(
        graphics: GuiGraphics, mote: Mote, layer: Layer, centerX: Float, centerY: Float,
        radius: Float, progress: Float, tint: Int, opacity: Float,
    ) {
        val gather = smoothStep(progress / 0.24f)
        val release = smoothStep((progress - 0.20f) / 0.64f)
        val depth = (mote.depth + release * (0.55f + layer.scale * 0.31f)) % 1f
        val distance = (mote.distance * (1f - gather) + (depth * depth + 0.022f) * gather) * radius * layer.scale
        val length = (1f + depth * depth * release * radius * 0.30f).toInt()
        val angle = mote.orbit + gather * 0.55f + release * (0.15f + layer.speed * 35f)
        val tip = distance.toInt()
        val tail = (tip - length).coerceAtLeast(0)
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(centerX, centerY, 0f)
        pose.mulPose(Axis.ZP.rotation(angle))
        if (layer.size > 1) {
            graphics.fill(tail, -2, tip + 2, 3, tinted(tint, opacity * 0.17f))
        }
        val mid = tail + (tip + 1 - tail) / 2
        graphics.fill(tail, 0, mid, layer.size, tinted(tint, opacity * 0.38f))
        graphics.fill(mid, 0, tip + 1, layer.size, tinted(tint, opacity))
        pose.popPose()
    }
}

internal fun tinted(rgb: Int, opacity: Float): Int =
    ((opacity.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xffffff)

private const val TAU = 6.2831855f
