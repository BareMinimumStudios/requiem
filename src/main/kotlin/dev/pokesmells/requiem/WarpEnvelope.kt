package dev.pokesmells.requiem

internal data class WarpAudioFrame(val gain: Float, val pitchProgress: Float)

internal object WarpEnvelope {
    fun prime(progress: Float): WarpAudioFrame {
        val build = smoothStep(progress)
        return WarpAudioFrame(0.22f * build, 0.14f * build)
    }

    fun flight(progress: Float, reveal: Float): WarpAudioFrame {
        val build = smoothStep(progress)
        val world = reveal.coerceIn(0f, 1f)
        val remaining = 1f - world
        return WarpAudioFrame(
            (0.22f + 0.78f * build) * remaining * (0.55f + 0.45f * remaining),
            (0.14f + 0.86f * build) * (1f - 0.27f * world),
        )
    }
}
