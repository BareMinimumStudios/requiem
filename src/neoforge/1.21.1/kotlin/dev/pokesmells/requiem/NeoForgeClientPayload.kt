package dev.pokesmells.requiem

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

object NeoForgeClientPayload {
    fun register(bus: IEventBus) {
        bus.addListener { event: RegisterShadersEvent ->
            EtherealSky.shader = null
            event.registerShader(ShaderInstance(event.resourceProvider, id("ethereal"), DefaultVertexFormat.POSITION)) {
                EtherealSky.shader = it
            }
        }
        NeoForge.EVENT_BUS.addListener { _: ClientTickEvent.Post -> RequiemClient.tick() }
    }
    fun receive(payload: DeathView, context: IPayloadContext) {
        RequiemClient.receive(payload) { PacketDistributor.sendToServer(it) }
    }
}



