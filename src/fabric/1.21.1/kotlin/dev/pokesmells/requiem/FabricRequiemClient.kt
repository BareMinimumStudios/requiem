package dev.pokesmells.requiem

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object FabricRequiemClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { RequiemClient.tick() }
        ClientPlayNetworking.registerGlobalReceiver(DeathView.TYPE) { payload, _ ->
            RequiemClient.receive(payload, ClientPlayNetworking::send)
        }
    }
}




