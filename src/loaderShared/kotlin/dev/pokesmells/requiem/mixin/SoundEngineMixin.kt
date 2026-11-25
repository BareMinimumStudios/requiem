package dev.pokesmells.requiem.mixin

import dev.pokesmells.requiem.RequiemAudio
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.ChannelAccess
import net.minecraft.client.sounds.SoundEngine
import net.minecraft.sounds.SoundSource
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.gen.Invoker
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Suppress("UNUSED_PARAMETER")
@Mixin(SoundEngine::class)
abstract class SoundEngineMixin {
    @Shadow @Final
    private lateinit var instanceToChannel: Map<SoundInstance, ChannelAccess.ChannelHandle>

    @Unique
    private var requiemAppliedWorldGain = 1f

    @Invoker("calculateVolume")
    protected abstract fun requiemCalculateVolume(sound: SoundInstance): Float

    @Inject(method = ["calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"], at = [At("RETURN")], cancellable = true)
    private fun requiemMixWorldSound(volume: Float, source: SoundSource, callback: CallbackInfoReturnable<Float>) {
        if (RequiemAudio.isWorldSound(source)) {
            callback.returnValue = callback.returnValue * RequiemAudio.worldGain
        }
    }

    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun requiemRefreshWorldSounds(paused: Boolean, callback: CallbackInfo) {
        val gain = RequiemAudio.worldGain
        if (gain == requiemAppliedWorldGain) return
        requiemAppliedWorldGain = gain
        instanceToChannel.forEach { (sound, handle) ->
            if (RequiemAudio.isWorldSound(sound.source)) {
                val volume = requiemCalculateVolume(sound)
                handle.execute { it.setVolume(volume) }
            }
        }
    }
}
