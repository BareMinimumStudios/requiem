package dev.pokesmells.requiem

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object FabricRequiemClient : ClientModInitializer {
    override fun onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register { context ->
            EtherealSky.shader = null
            context.register(id("ethereal"), DefaultVertexFormat.POSITION) { EtherealSky.shader = it }
        }
        ClientTickEvents.END_CLIENT_TICK.register { RequiemClient.tick() }
        ClientPlayNetworking.registerGlobalReceiver(DeathView.TYPE) { payload, _ ->
            RequiemClient.receive(payload, ClientPlayNetworking::send)
        }
    }
}




