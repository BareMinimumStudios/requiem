package dev.pokesmells.requiem

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.network.chat.Component
import java.util.UUID

internal class FinalDeathScreen(
    val transaction: UUID,
    onChoice: (Boolean) -> Unit,
) : ConfirmScreen(
    { onChoice(it) },
    Component.translatable("requiem_cinematics.confirm_title"),
    Component.translatable("requiem_cinematics.confirm_body"),
    Component.translatable("requiem_cinematics.confirm_accept"),
    Component.translatable("requiem_cinematics.confirm_cancel"),
) {
    override fun addButtons(y: Int) {
        val buttonWidth = minOf(150, (width - 32) / 2).coerceAtLeast(1)
        val center = width / 2
        addExitButton(dreamButton(center - buttonWidth - 4, y, buttonWidth, yesButton) { callback.accept(true) })
        val back = dreamButton(center + 4, y, buttonWidth, noButton) { callback.accept(false) }
        addExitButton(back)
        setInitialFocus(back)
    }

    override fun isPauseScreen() = false

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val config = RequiemSettings.config
        if (config.plainInterface) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick)
        } else {
            graphics.fillGradient(0, 0, width, height, 0xff03040c.toInt(), tinted(config.skyColor, 1f))
        }
    }
}
