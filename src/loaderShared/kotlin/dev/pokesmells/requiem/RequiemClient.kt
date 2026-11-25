package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ReceivingLevelScreen
import net.minecraft.client.multiplayer.ClientPacketListener

object RequiemClient {
    private data class Presentation(
        val connection: ClientPacketListener,
        val payload: DeathView,
        val send: (DeathChoice) -> Unit,
    )

    private var pending: Presentation? = null

    fun receive(payload: DeathView, send: (DeathChoice) -> Unit) {
        val connection = Minecraft.getInstance().connection ?: return
        pending = Presentation(connection, payload, send)
    }

    fun tick() {
        val next = pending ?: return
        val minecraft = Minecraft.getInstance()
        if (minecraft.connection !== next.connection) {
            pending = null
            return
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.screen is ReceivingLevelScreen) return
        pending = null
        val config = RequiemSettings.config
        if (next.payload.intro && !config.playCinematics) {
            if (minecraft.screen is BetweenScreen) minecraft.setScreen(null)
            return
        }
        minecraft.setScreen(BetweenScreen(next.payload, next.send))
    }
}
