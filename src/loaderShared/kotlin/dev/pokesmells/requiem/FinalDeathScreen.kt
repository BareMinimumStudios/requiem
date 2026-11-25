package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.network.chat.Component
import java.util.UUID
import org.lwjgl.glfw.GLFW

internal class FinalDeathScreen(
    val transaction: UUID,
    private val between: BetweenScreen,
    private val onChoice: (Boolean, Float) -> Unit,
) : ConfirmScreen(
    {},
    Component.translatable("requiem_cinematics.confirm_title"),
    Component.translatable("requiem_cinematics.confirm_body"),
    Component.translatable("requiem_cinematics.confirm_accept"),
    Component.translatable("requiem_cinematics.confirm_cancel"),
) {
    private val startClock = between.starClock()
    private var age = 0

    override fun tick() {
        super.tick()
        age++
    }

    override fun onClose() {
        onChoice(false, startClock + age)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers)
        onClose()
        return true
    }

    override fun addButtons(y: Int) {
        val buttonWidth = minOf(150, (width - 32) / 2).coerceAtLeast(1)
        val center = width / 2
        addExitButton(dreamButton(center - buttonWidth - 4, y, buttonWidth, yesButton) { onChoice(true, startClock + age) })
        val back = dreamButton(center + 4, y, buttonWidth, noButton) { onClose() }
        addExitButton(back)
        setInitialFocus(back)
    }

    override fun isPauseScreen() = false

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val config = RequiemSettings.config
        if (config.plainInterface) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick)
        } else {
            val partial = minecraft?.timer?.getGameTimeDeltaPartialTick(false) ?: partialTick
            between.renderStarField(graphics, startClock + (age - 1).coerceAtLeast(0) + partial)
        }
    }
}
