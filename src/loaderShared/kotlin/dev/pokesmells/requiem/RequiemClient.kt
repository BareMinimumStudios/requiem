package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.DeathScreen
import net.minecraft.client.gui.screens.ReceivingLevelScreen
import net.minecraft.client.multiplayer.ClientPacketListener

object RequiemClient {
    private data class Presentation(
        val connection: ClientPacketListener,
        val payload: DeathView,
        val send: (DeathChoice) -> Unit,
    )

    private data class Handoff(val connection: ClientPacketListener, val screen: BetweenScreen, var ticks: Int = 0)

    private var pending: Presentation? = null
    private var handoff: Handoff? = null

    fun beginReturn(screen: BetweenScreen) {
        val connection = Minecraft.getInstance().connection ?: return
        handoff = Handoff(connection, screen)
    }

    fun renderReturnHandoff(graphics: GuiGraphics): Boolean {
        val current = handoff ?: return false
        if (Minecraft.getInstance().connection !== current.connection) return false
        current.screen.renderHandoff(graphics, current.ticks.toFloat())
        return true
    }

    fun receive(payload: DeathView, send: (DeathChoice) -> Unit) {
        val connection = Minecraft.getInstance().connection ?: return
        pending = Presentation(connection, payload, send)
    }

    fun tick() {
        val minecraft = Minecraft.getInstance()
        advancePresentation(minecraft)
        val frame = (minecraft.screen as? BetweenScreen)?.warpAudioFrame()
            ?: handoff?.let { it.screen.primingAudio(it.ticks.toFloat()) }
        RequiemAudio.updateWarp(frame)
    }

    private fun advancePresentation(minecraft: Minecraft) {
        handoff?.let {
            if (minecraft.connection !== it.connection) {
                handoff = null
            } else if (++it.ticks > 240) {
                handoff = null
                it.screen.cancelReturn()
            } else if (pending == null && (minecraft.screen == null || minecraft.screen is DeathScreen)) {
                minecraft.setScreen(it.screen)
            }
        }
        val next = pending ?: return
        if (minecraft.connection !== next.connection) {
            pending = null
            return
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.screen is ReceivingLevelScreen) return
        pending = null
        val starClock = handoff?.let { it.screen.starClockAtHandoff(it.ticks.toFloat()) } ?: 0f
        handoff = null
        val config = RequiemSettings.config
        if (next.payload.presentation == DeathPresentation.CLOSE) {
            val screen = minecraft.screen
            val transaction = next.payload.transaction
            if (screen is BetweenScreen && screen.isDecisionFor(transaction) ||
                screen is FinalDeathScreen && screen.transaction == transaction
            ) {
                minecraft.setScreen(null)
            }
            return
        }
        if (next.payload.presentation == DeathPresentation.TRANSITION && !config.playCinematics) {
            if (minecraft.screen is BetweenScreen) minecraft.setScreen(null)
            return
        }
        minecraft.setScreen(BetweenScreen(next.payload, next.send, starClock))
    }
}
