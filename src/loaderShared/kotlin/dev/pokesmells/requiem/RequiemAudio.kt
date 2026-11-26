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
    private var warp: WarpPlayback? = null
    private var warpTailTicks = 0
    private var playedBundledCue = false
    var worldGain = 1f
        private set

    fun isWorldSound(source: SoundSource) = when (source) {
        SoundSource.AMBIENT, SoundSource.WEATHER, SoundSource.BLOCKS, SoundSource.NEUTRAL,
        SoundSource.HOSTILE, SoundSource.PLAYERS, SoundSource.RECORDS -> true
        else -> false
    }

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

    fun play(sound: String, volume: Double = 0.25, pitch: Float = 1f) {
        val config = RequiemSettings.config
        if (config.muteCinematics || volume <= 0.0) return
        val manager = Minecraft.getInstance().soundManager
        val event = soundEvent(manager, sound) ?: return
        manager.play(SimpleSoundInstance.forUI(event, pitch, volume.coerceIn(0.0, 1.0).toFloat()))
    }

    fun updateWarp(frame: WarpAudioFrame?, flightStarted: Boolean) {
        val config = RequiemSettings.config
        val blend = if (config.crossfadeWorldAudio && !config.muteCinematics && config.playCinematics) {
            frame?.worldBlend ?: 1f
        } else {
            1f
        }
        // Keep live channels open even at the quietest setting.
        val floor = config.warpWorldVolume.coerceIn(0.01, 1.0).toFloat()
        val worldTarget = floor + (1f - floor) * blend
        worldGain += (worldTarget - worldGain) * 0.45f
        if (kotlin.math.abs(worldTarget - worldGain) < 0.001f) worldGain = worldTarget

        val audioEnabled = !config.muteCinematics && config.playCinematics
        if (frame == null || !audioEnabled) {
            val connected = Minecraft.getInstance().connection != null
            if (!audioEnabled || !connected) warp?.release(6, shorten = true)
            else warp?.release(if (warpTailTicks > 0) warpTailTicks else 6)
            warpTailTicks = 0
            if (frame == null) playedBundledCue = false
            if (warp?.isStopped == true) warp = null
            return
        }
        warpTailTicks = frame.tailTicks
        val target = (config.warpVolume.coerceIn(0.0, 1.0) * frame.gain).toFloat()
        val start = config.warpStartPitch.coerceIn(0.5, 2.0).toFloat()
        val peak = config.warpPeakPitch.coerceIn(0.5, 2.0).toFloat()
        val bundled = config.warpSound == "$MOD_ID:warp"
        val pitch = if (bundled) 1f else start + (peak - start) * frame.pitchProgress
        if (warp == null || warp?.isStopped == true || warp?.isReleasing == true) {
            if (target <= 0f || bundled && (!flightStarted || playedBundledCue)) return
            val manager = Minecraft.getInstance().soundManager
            val event = soundEvent(manager, config.warpSound) ?: return
            warp?.release(6, shorten = true)
            warp = WarpPlayback(event, pitch, repeat = !bundled).also(manager::play)
            if (bundled) playedBundledCue = true
        }
        warp?.aim(target, pitch)
    }

    private fun soundEvent(manager: SoundManager, name: String): SoundEvent? {
        if (name.isBlank()) return null
        val location = ResourceLocation.tryParse(name) ?: return null
        if (manager.getSoundEvent(location) == null) return null
        return SoundEvent.createVariableRangeEvent(location)
    }

    private class WarpPlayback(event: SoundEvent, initialPitch: Float, repeat: Boolean) :
        AbstractTickableSoundInstance(event, SoundSource.MASTER, RandomSource.create()) {
        private var targetVolume = 0f
        private var targetPitch = initialPitch
        private var releaseTicks = 0
        private var releaseDuration = 0
        private var releaseVolume = 0f
        val isReleasing get() = releaseTicks > 0

        init {
            looping = repeat
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

        fun release(ticks: Int, shorten: Boolean = false) {
            if (isStopped || (isReleasing && (!shorten || releaseTicks <= ticks))) return
            releaseTicks = ticks
            releaseDuration = ticks
            releaseVolume = volume
        }

        override fun tick() {
            if (releaseTicks > 0) {
                val remaining = (--releaseTicks).toFloat() / releaseDuration
                volume = releaseVolume * remaining * remaining
                if (releaseTicks == 0) stop()
                return
            }
            volume += (targetVolume - volume) * 0.58f
            pitch += (targetPitch - pitch) * 0.45f
        }
    }
}
