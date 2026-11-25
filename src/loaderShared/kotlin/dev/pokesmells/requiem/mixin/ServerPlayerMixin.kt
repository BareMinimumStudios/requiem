package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.RequiemServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("CAST_NEVER_SUCCEEDS", "UNUSED_PARAMETER")
@Mixin(ServerPlayer::class)
abstract class ServerPlayerMixin {
    @Inject(method = ["die"], at = [At("TAIL")])
    private fun requiemRecordDeath(source: DamageSource, callback: CallbackInfo) {
        val player = this as ServerPlayer
        if (player.isDeadOrDying) RequiemServer.onDeath(player, source)
    }
}
