package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.RequiemClient
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ReceivingLevelScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("UNUSED_PARAMETER")
@Mixin(ReceivingLevelScreen::class)
abstract class ReceivingLevelScreenMixin {
    @Inject(method = ["render"], at = [At("HEAD")], cancellable = true)
    private fun requiemRenderHandoff(
        graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float, callback: CallbackInfo,
    ) {
        if (RequiemClient.renderReturnHandoff(graphics)) callback.cancel()
    }
}
