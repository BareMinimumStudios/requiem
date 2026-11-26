package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.BetweenScreen
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.BlockGetter
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("CAST_NEVER_SUCCEEDS", "UNUSED_PARAMETER")
@Mixin(Camera::class)
abstract class CameraMixin {
    @Shadow
    protected abstract fun setRotation(yaw: Float, pitch: Float)

    @Shadow
    protected abstract fun setPosition(x: Double, y: Double, z: Double)

    @Inject(method = ["setup"], at = [At("TAIL")])
    private fun requiemSettleCamera(
        level: BlockGetter, entity: Entity, detached: Boolean, mirrored: Boolean, partialTick: Float,
        callback: CallbackInfo,
    ) {
        val minecraft = Minecraft.getInstance()
        if (detached || entity !== minecraft.player || !entity.isAlive) return
        val screen = minecraft.screen as? BetweenScreen ?: return
        val tilt = screen.cameraTilt(partialTick)
        val lift = screen.cameraLift(partialTick)
        if (tilt == 0f && lift == 0f) return
        val camera = this as Camera
        setRotation(camera.yRot, (camera.xRot + tilt).coerceIn(-90f, 90f))
        val position = camera.position.add(0.0, lift.toDouble(), 0.0)
        val block = BlockPos.containing(position)
        if (lift > 0f && level.getBlockState(block).getCollisionShape(level, block).isEmpty) {
            setPosition(position.x, position.y, position.z)
        }
    }
}
