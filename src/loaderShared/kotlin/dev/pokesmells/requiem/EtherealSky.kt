package dev.pokesmells.requiem

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.ShaderInstance

internal object EtherealSky {
    var shader: ShaderInstance? = null

    fun render(
        graphics: GuiGraphics, clock: Float, opacity: Float, color: Int,
        pointerX: Float = 0.5f, pointerY: Float = 0.5f,
    ) {
        val program = shader ?: return
        if (opacity <= 0f) return
        val width = graphics.guiWidth().toFloat()
        val height = graphics.guiHeight().toFloat()
        graphics.flush()
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        try {
            RenderSystem.setShader { program }
            program.safeGetUniform("Canvas").set(width, height)
            program.safeGetUniform("Pointer").set(pointerX, pointerY)
            program.safeGetUniform("Clock").set(clock / 20f)
            program.safeGetUniform("Opacity").set(opacity)
            program.safeGetUniform("Accent").set(
                (color shr 16 and 255) / 255f, (color shr 8 and 255) / 255f, (color and 255) / 255f,
            )
            val pose = graphics.pose().last().pose()
            val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            buffer.addVertex(pose, 0f, height, 0f)
            buffer.addVertex(pose, width, height, 0f)
            buffer.addVertex(pose, width, 0f, 0f)
            buffer.addVertex(pose, 0f, 0f, 0f)
            BufferUploader.drawWithShader(buffer.buildOrThrow())
        } finally {
            RenderSystem.disableBlend()
            RenderSystem.enableDepthTest()
        }
    }
}
