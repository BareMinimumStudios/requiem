package dev.pokesmells.requiem

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

internal object RequiemAudio {
    private var lastHover = 0L
    private var warp: WarpLoop? = null

    fun hover() {
        val config = RequiemSettings.config
        val now = System.nanoTime()
        if (now - lastHover < 110_000_000L) return
        lastHover = now
        play(config.buttonHoverSound, config.buttonHoverVolume)
    }

    fun click() {
        val config = RequiemSettings.config
        play(config.buttonClickSound, config.buttonClickVolume)
    }

    fun play(sound: String, volume: Double = 0.25) {
        val config = RequiemSettings.config
        if (config.muteCinematics || volume <= 0.0) return
        val manager = Minecraft.getInstance().soundManager
        val event = soundEvent(manager, sound) ?: return
        manager.play(SimpleSoundInstance.forUI(event, 1f, volume.coerceIn(0.0, 1.0).toFloat()))
    }

    fun updateWarp(frame: WarpAudioFrame?) {
        val config = RequiemSettings.config
        if (config.muteCinematics || !config.playCinematics) {
            warp?.release()
            warp = null
            return
        }
        val target = (config.warpVolume.coerceIn(0.0, 1.0) * (frame?.gain ?: 0f)).toFloat()
        if (frame == null) {
            warp?.release()
            warp = null
            return
        }
        val start = config.warpStartPitch.coerceIn(0.5, 2.0).toFloat()
        val peak = config.warpPeakPitch.coerceIn(0.5, 2.0).toFloat()
        val pitch = start + (peak - start) * frame.pitchProgress
        if (warp == null || warp?.isStopped == true) {
            if (target <= 0f) return
            val manager = Minecraft.getInstance().soundManager
            val event = soundEvent(manager, config.warpSound) ?: return
            warp = WarpLoop(event, pitch).also(manager::play)
        }
        warp?.aim(target, pitch)
    }

    private fun soundEvent(manager: SoundManager, name: String): SoundEvent? {
        if (name.isBlank()) return null
        val location = ResourceLocation.tryParse(name) ?: return null
        if (manager.getSoundEvent(location) == null) return null
        return SoundEvent.createVariableRangeEvent(location)
    }

    private class WarpLoop(event: SoundEvent, initialPitch: Float) :
        AbstractTickableSoundInstance(event, SoundSource.MASTER, RandomSource.create()) {
        private var targetVolume = 0f
        private var targetPitch = initialPitch
        private var releaseTicks = 0

        init {
            looping = true
            relative = true
            attenuation = SoundInstance.Attenuation.NONE
            volume = 0f
            pitch = initialPitch
        }

        override fun canStartSilent() = true

        fun aim(volume: Float, pitch: Float) {
            targetVolume = volume
            targetPitch = pitch
        }

        fun release() {
            releaseTicks = 6
            targetVolume = 0f
        }

        override fun tick() {
            if (releaseTicks > 0) {
                volume *= (--releaseTicks).toFloat() / (releaseTicks + 1)
                if (releaseTicks == 0) stop()
                return
            }
            volume += (targetVolume - volume) * 0.58f
            pitch += (targetPitch - pitch) * 0.45f
        }
    }
}
