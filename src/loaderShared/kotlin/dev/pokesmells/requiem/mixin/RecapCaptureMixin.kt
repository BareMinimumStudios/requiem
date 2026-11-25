package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.RequiemRecap
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("UNUSED_PARAMETER")
@Mixin(GameRenderer::class)
abstract class RecapCaptureMixin {
    @Inject(method = ["renderLevel"], at = [At("RETURN")])
    private fun requiemCaptureWorld(deltaTracker: DeltaTracker, callback: CallbackInfo) {
        RequiemRecap.capture(Minecraft.getInstance())
    }
}
