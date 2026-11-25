package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.RequiemServer
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("CAST_NEVER_SUCCEEDS", "UNUSED_PARAMETER")
@Mixin(ServerGamePacketListenerImpl::class)
abstract class ServerGamePacketListenerMixin {
    @Unique
    private var requiemNextPositionCorrection = 0

    @Inject(method = ["handleClientCommand"], at = [At("HEAD")], cancellable = true)
    private fun requiemGateRespawn(packet: ServerboundClientCommandPacket, callback: CallbackInfo) {
        val player = (this as ServerGamePacketListenerImpl).player
        if (packet.action == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN &&
            player.server.isSameThread && player.isDeadOrDying && !RequiemServer.allowVanillaRespawn(player)
        ) {
            callback.cancel()
        }
    }

    @Inject(method = ["handleMovePlayer"], at = [At("HEAD")], cancellable = true)
    private fun requiemHoldPosition(packet: ServerboundMovePlayerPacket, callback: CallbackInfo) {
        val connection = this as ServerGamePacketListenerImpl
        val player = connection.player
        if (!player.server.isSameThread || !RequiemServer.isConfined(player)) return

        callback.cancel()
        val tick = player.server.tickCount
        if (packet.hasPosition() && tick >= requiemNextPositionCorrection &&
            (packet.getX(player.x) != player.x || packet.getY(player.y) != player.y || packet.getZ(player.z) != player.z)
        ) {
            requiemNextPositionCorrection = tick + 10
            connection.teleport(player.x, player.y, player.z, player.yRot, player.xRot)
        }
    }

    @Inject(method = ["handleTeleportToEntityPacket"], at = [At("HEAD")], cancellable = true)
    private fun requiemHoldSpectator(packet: ServerboundTeleportToEntityPacket, callback: CallbackInfo) {
        val player = (this as ServerGamePacketListenerImpl).player
        if (player.server.isSameThread && RequiemServer.isConfined(player)) callback.cancel()
    }
}
