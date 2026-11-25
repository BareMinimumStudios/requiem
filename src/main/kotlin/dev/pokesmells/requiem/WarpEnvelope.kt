package dev.pokesmells.requiem

internal data class WarpAudioFrame(
    val gain: Float,
    val pitchProgress: Float,
    val worldBlend: Float,
    val tailTicks: Int = 0,
)

internal object WarpEnvelope {
    fun prime(progress: Float): WarpAudioFrame {
        val build = smoothStep(progress)
        return WarpAudioFrame(0.22f * build, 0.14f * build, 1f - build)
    }

    fun flight(progress: Float, reveal: Float, stretch: Float, tailTicks: Int = 0): WarpAudioFrame {
        val build = 0.72f * stretch + 0.28f * smoothStep(progress)
        val world = reveal.coerceIn(0f, 1f)
        val remaining = 1f - world
        val tail = if (tailTicks > 0) 0.22f * world else 0f
        return WarpAudioFrame(
            (0.22f + 0.78f * build) * (remaining * (0.55f + 0.45f * remaining) + tail),
            (0.14f + 0.86f * build) * (1f - 0.27f * world),
            smoothStep(world),
            if (world >= 1f) tailTicks else 0,
        )
    }
}
