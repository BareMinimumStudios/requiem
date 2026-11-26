package dev.pokesmells.requiem

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import java.util.UUID
import kotlin.math.roundToInt

internal object RequiemRecap {
    private const val MAX_FRAMES = 12
    private const val MAX_EDGE = 128
    private const val SAMPLE_NS = 850_000_000L
    private const val MAX_AGE_NS = 12_000_000_000L

    private class Frame(val image: NativeImage, val time: Long) {
        var texture: ResourceLocation? = null

        fun close(client: Minecraft) {
            val registered = texture
            if (registered == null) image.close() else client.textureManager.release(registered)
        }
    }

    private val frames = ArrayDeque<Frame>(MAX_FRAMES)
    private var connection: ClientPacketListener? = null
    private var level: ClientLevel? = null
    private var transaction: UUID? = null
    private var lastSample = 0L
    private var returning = false

    private fun enabled(): Boolean = RequiemSettings.config.let {
        it.showLastMoments && it.playCinematics && !it.reducedMotion && !it.plainInterface
    }

    fun tick(client: Minecraft) {
        if (connection !== client.connection || !enabled() || returning && client.screen !is BetweenScreen) {
            clear(client)
        }
        connection = client.connection
    }

    fun capture(client: Minecraft) {
        val world = client.level ?: return
        val player = client.player ?: return
        if (connection == null || transaction != null || !enabled() || client.screen != null ||
            client.overlay != null || !player.isAlive || player.isSpectator || !client.options.cameraType.isFirstPerson
        ) return

        if (level !== world) {
            clear(client)
            level = world
        }
        val now = System.nanoTime()
        if (now - lastSample < SAMPLE_NS) return
        lastSample = now
        Screenshot.takeScreenshot(client.mainRenderTarget).use { full ->
            val ratio = minOf(1.0, MAX_EDGE.toDouble() / maxOf(full.width, full.height))
            val image = NativeImage((full.width * ratio).roundToInt().coerceAtLeast(1),
                (full.height * ratio).roundToInt().coerceAtLeast(1), false)
            try {
                full.resizeSubRectTo(0, 0, full.width, full.height, image)
                soften(image)
            } catch (failure: RuntimeException) {
                image.close()
                throw failure
            }
            if (frames.size == MAX_FRAMES) frames.removeFirst().close(client)
            frames.addLast(Frame(image, now))
        }
    }

    fun present(client: Minecraft, view: DeathView) {
        when (view.presentation) {
            DeathPresentation.DECISION -> {
                if (transaction == view.transaction) return
                if (transaction != null || level !== client.level) clear(client)
                val now = System.nanoTime()
                while (frames.isNotEmpty() && now - frames.first().time > MAX_AGE_NS) {
                    frames.removeFirst().close(client)
                }
                transaction = view.transaction
            }
            DeathPresentation.TRANSITION -> {
                if (transaction == view.transaction) returning = true else clear(client)
            }
            DeathPresentation.CLOSE -> if (transaction == view.transaction) clear(client)
        }
    }

    fun render(graphics: GuiGraphics, id: UUID, clock: Float, stillStart: Int) {
        if (transaction != id || frames.isEmpty() || !enabled()) return
        val client = Minecraft.getInstance()
        val duration = minOf(550f, stillStart.toFloat())
        if (clock >= duration || duration < 14f) {
            clear(client)
            return
        }
        val progress = (clock / duration).coerceIn(0f, 1f)
        val cursor = frames.size * recapPosition(progress)
        val position = cursor.toInt().coerceIn(frames.indices)
        val phase = cursor - position
        val rush = smoothStep((progress - 0.63f) / 0.31f)
        val mix = smoothStep(phase / (0.25f - 0.15f * rush))
        val flicker = smoothStep(phase / 0.07f) * (1f - smoothStep((phase - 0.10f) / 0.18f))
        val visibility = smoothStep(progress / 0.05f) *
            (1f - smoothStep((progress - 0.92f) / 0.08f)) * (1f - 0.38f * rush * flicker)
        val depth = (1f - 0.62f * smoothStep(progress)) * (1f + 0.15f * rush * (1f - mix))
        if (position > 0 && mix < 1f) {
            drawMemory(graphics, client, frames[frames.lastIndex - position + 1], depth, visibility * (1f - mix))
        }
        drawMemory(graphics, client, frames[frames.lastIndex - position], depth, visibility * if (position == 0) 1f else mix)
    }

    private fun drawMemory(graphics: GuiGraphics, client: Minecraft, frame: Frame, depth: Float, alpha: Float) {
        if (alpha < 0.01f) return
        val texture = frame.texture ?: client.textureManager.register("requiem_recap", DynamicTexture(frame.image))
            .also { frame.texture = it }
        val scale = depth * minOf(graphics.guiWidth() * 0.72f / frame.image.width,
            graphics.guiHeight() * 0.60f / frame.image.height)
        val width = (frame.image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (frame.image.height * scale).roundToInt().coerceAtLeast(1)
        val x = (graphics.guiWidth() - width) / 2
        val y = (graphics.guiHeight() - height) / 2

        graphics.flush()
        RenderSystem.enableBlend()
        graphics.setColor(0.87f, 0.92f, 1f, alpha * 0.79f)
        try {
            graphics.blit(texture, x, y, width, height, 0f, 0f, frame.image.width, frame.image.height,
                frame.image.width, frame.image.height)
        } finally {
            graphics.setColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }
    }

    private fun soften(image: NativeImage) {
        val xFade = feather(image.width)
        val yFade = feather(image.height)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getPixelRGBA(x, y)
                val red = pixel and 255
                val green = pixel ushr 8 and 255
                val blue = pixel ushr 16 and 255
                val gray = (red * 54 + green * 183 + blue * 19) ushr 8
                val opacity = (255f * xFade[x] * yFade[y]).roundToInt()
                // NativeImage stores RGBA bytes in an ABGR integer.
                image.setPixelRGBA(x, y, (opacity shl 24) or
                    (((blue * 3 + gray) / 4) shl 16) or
                    (((green * 3 + gray) / 4) shl 8) or ((red * 3 + gray) / 4))
            }
        }
    }

    private fun feather(size: Int) = FloatArray(size) { index ->
        smoothStep(minOf(index, size - 1 - index) / (size * 0.18f))
    }

    private fun clear(client: Minecraft) {
        frames.forEach { it.close(client) }
        frames.clear()
        transaction = null
        returning = false
        lastSample = 0L
        level = null
    }
}
