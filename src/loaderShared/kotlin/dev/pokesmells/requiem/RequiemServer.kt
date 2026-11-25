package dev.pokesmells.requiem

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.UserBanListEntry
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import java.util.UUID

object RequiemServer {
    private data class Delivery(val id: UUID, val due: Int)
    private val deliveries = mutableMapOf<UUID, Delivery>()
    lateinit var send: (ServerPlayer, DeathView) -> Unit

    @JvmStatic
    fun onDeath(player: ServerPlayer, source: DamageSource) {
        val server = player.server
        val data = RequiemData.get(server)
        val config = RequiemSettings.config
        val attacker = source.entity ?: if (config.creditRecentAttacker) player.killCredit else null
        val cause = when (attacker) {
            is Player -> DeathCause.PLAYER
            is Mob -> DeathCause.MOB
            else -> DeathCause.ENVIRONMENT
        }
        val current = data.record(player.uuid)
        val updated = config.rules().begin(player.uuid, player.gameProfile.name, cause, current)
        if (updated == current) return
        data.update(player.uuid, updated)
        server.overworld().dataStorage.save()
    }

    @JvmStatic
    fun allowVanillaRespawn(player: ServerPlayer): Boolean {
        val data = RequiemData.get(player.server)
        val record = data.record(player.uuid)
        val pending = record.pending ?: return true
        if (pending.respawnMode != RespawnMode.VANILLA || !pending.canReturn) return false
        data.update(player.uuid, record.decide(pending.id, Decision.RETURN))
        return true
    }

    @JvmStatic
    fun isConfined(player: ServerPlayer): Boolean {
        val record = RequiemData.get(player.server).record(player.uuid)
        return record.finalPolicy == FinalPolicy.LIMBO || record.pending != null && player.isAlive
    }

    fun onJoin(player: ServerPlayer) {
        deliveries.remove(player.uuid)
        val data = RequiemData.get(player.server)
        val record = data.record(player.uuid)
        if (record.finalPolicy != null) {
            enforce(player, record.finalPolicy)
        } else if (record.pending == null && !record.introSeen) {
            data.update(player.uuid, record.copy(introSeen = true))
            if (RequiemSettings.config.firstJoinCinematic) {
                send(player, DeathView(UUID.randomUUID(), -1, DeathCause.ENVIRONMENT, false, false, true))
            }
        }
    }

    fun onDisconnect(player: ServerPlayer) {
        deliveries.remove(player.uuid)
    }

    fun tick(player: ServerPlayer) {
        val data = RequiemData.get(player.server)
        val record = data.record(player.uuid)
        if (record.finalPolicy != null && !player.isSpectator) {
            enforce(player, record.finalPolicy)
            return
        }
        val pending = record.pending ?: return
        if (pending.respawnMode == RespawnMode.VANILLA && pending.canReturn && player.isDeadOrDying) return
        val time = player.server.tickCount
        val delivery = deliveries[player.uuid]
        if (delivery?.id != pending.id) {
            if (player.isAlive) player.setGameMode(GameType.SPECTATOR)
            val delay = when (pending.respawnMode) {
                RespawnMode.AUTOMATIC -> pending.automaticDelaySeconds * 20
                RespawnMode.INSTANT, RespawnMode.VANILLA -> 0
                RespawnMode.MANUAL -> -1
            }
            deliveries[player.uuid] = Delivery(pending.id, if (delay >= 0 && pending.canReturn) time + delay else -1)
            send(player, DeathView(pending.id, pending.lives ?: -1, pending.cause, pending.canFinalize, pending.deferredDecision))
            return
        }
        if (delivery.due >= 0 && time >= delivery.due) decide(player, DeathChoice(pending.id, Decision.RETURN))
    }

    fun decide(player: ServerPlayer, choice: DeathChoice) {
        val server = player.server
        val data = RequiemData.get(server)
        val record = data.record(player.uuid)
        val pending = record.pending ?: return
        if (pending.id != choice.transaction) return
        if (choice.decision == Decision.DEFER) {
            if (pending.deferredDecision) {
                server.overworld().dataStorage.save()
                player.connection.disconnect(Component.translatable("requiem_cinematics.deferred"))
            }
            return
        }
        val updated = record.decide(choice.transaction, choice.decision)
        if (updated == record) return
        data.update(player.uuid, updated)
        server.overworld().dataStorage.save()
        deliveries.remove(player.uuid)
        if (updated.finalPolicy == FinalPolicy.BAN) {
            enforce(player, FinalPolicy.BAN)
            return
        }
        if (player.isDeadOrDying) {
            player.connection.handleClientCommand(
                ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN),
            )
        }
        val active = server.playerList.getPlayer(player.uuid) ?: return
        if (updated.finalPolicy != null) {
            enforce(active, updated.finalPolicy)
            send(active, DeathView(pending.id, -1, pending.cause, false, false, true))
        }
        else {
            if (active.isSpectator && !server.isHardcore) active.setGameMode(server.defaultGameType)
            if (pending.respawnMode != RespawnMode.VANILLA) {
                send(active, DeathView(pending.id, -1, pending.cause, false, false, true))
            }
        }
    }

    private fun enforce(player: ServerPlayer, policy: FinalPolicy) {
        if (policy == FinalPolicy.BAN && player.server.isDedicatedServer) {
            val reason = "Final death in Requiem"
            player.server.playerList.bans.add(UserBanListEntry(player.gameProfile, null, "Requiem", null, reason))
            player.connection.disconnect(Component.literal(reason))
            return
        }
        if (player.isDeadOrDying) {
            player.connection.handleClientCommand(
                ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN),
            )
        }
        val active = player.server.playerList.getPlayer(player.uuid) ?: return
        active.setGameMode(GameType.SPECTATOR)
    }

    fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("requiem")
            .then(Commands.literal("release").requires { it.hasPermission(2) }
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("lives", IntegerArgumentType.integer(1)).executes { context ->
                        val player = EntityArgument.getPlayer(context, "player")
                        val data = RequiemData.get(player.server)
                        data.update(player.uuid, PlayerRecord(IntegerArgumentType.getInteger(context, "lives"), introSeen = true))
                        deliveries.remove(player.uuid)
                        if (player.isDeadOrDying) {
                            player.connection.handleClientCommand(
                                ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN),
                            )
                        }
                        player.server.playerList.getPlayer(player.uuid)?.setGameMode(player.server.defaultGameType)
                        1
                    }))))
    }
}


