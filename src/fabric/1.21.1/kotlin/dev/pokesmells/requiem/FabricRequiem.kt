package dev.pokesmells.requiem

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object FabricRequiem : ModInitializer {
    override fun onInitialize() {
        RequiemSettings.config
        PayloadTypeRegistry.playS2C().register(DeathView.TYPE, DeathView.CODEC)
        PayloadTypeRegistry.playC2S().register(DeathChoice.TYPE, DeathChoice.CODEC)
        RequiemServer.send = ServerPlayNetworking::send
        ServerPlayNetworking.registerGlobalReceiver(DeathChoice.TYPE) { message, context ->
            RequiemServer.decide(context.player(), message)
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> RequiemServer.onJoin(handler.player) }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> RequiemServer.onDisconnect(handler.player) }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.playerList.players.toList().forEach(RequiemServer::tick)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> RequiemServer.registerCommands(dispatcher) }
    }
}
