package dev.pokesmells.requiem

import net.minecraft.server.level.ServerPlayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@Mod(MOD_ID)
class NeoForgeRequiem(bus: IEventBus) {
    init {
        if (FMLEnvironment.dist == Dist.CLIENT) NeoForgeClientPayload.register(bus)
        bus.addListener { event: FMLCommonSetupEvent -> event.enqueueWork { RequiemSettings.config; Unit } }
        bus.addListener(::registerPayloads)
        RequiemServer.send = PacketDistributor::sendToPlayer
        NeoForge.EVENT_BUS.addListener { event: PlayerEvent.PlayerLoggedInEvent ->
            (event.entity as? ServerPlayer)?.let(RequiemServer::onJoin)
        }
        NeoForge.EVENT_BUS.addListener { event: PlayerEvent.PlayerLoggedOutEvent ->
            (event.entity as? ServerPlayer)?.let(RequiemServer::onDisconnect)
        }
        NeoForge.EVENT_BUS.addListener { event: ServerTickEvent.Post ->
            event.server.playerList.players.toList().forEach(RequiemServer::tick)
        }
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent ->
            RequiemServer.registerCommands(event.dispatcher)
        }
    }

    private fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToServer(DeathChoice.TYPE, DeathChoice.CODEC) { message, context ->
            (context.player() as? ServerPlayer)?.let { RequiemServer.decide(it, message) }
        }
        registrar.playToClient(DeathView.TYPE, DeathView.CODEC, NeoForgeClientPayload::receive)
    }
}


