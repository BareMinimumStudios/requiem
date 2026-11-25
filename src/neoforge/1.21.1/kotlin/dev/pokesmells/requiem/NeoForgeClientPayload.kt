package dev.pokesmells.requiem

import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

object NeoForgeClientPayload {
    fun register() {
        NeoForge.EVENT_BUS.addListener { _: ClientTickEvent.Post -> RequiemClient.tick() }
    }
    fun receive(payload: DeathView, context: IPayloadContext) {
        RequiemClient.receive(payload) { PacketDistributor.sendToServer(it) }
    }
}



