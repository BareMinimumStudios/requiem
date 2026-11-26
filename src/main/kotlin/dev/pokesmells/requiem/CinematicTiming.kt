package dev.pokesmells.requiem

internal fun smoothStep(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal data class CinematicTiming(val duration: Int, val revealDuration: Int) {
    val revealStart: Int get() = duration - revealDuration

    private val ambienceLead: Int get() = ((revealStart - 12) / 3).coerceIn(0, 22)

    fun worldBlend(age: Float): Float {
        val revealBlend = smoothStep(reveal(age))
        if (ambienceLead == 0) return revealBlend
        val lead = smoothStep((age - revealStart + ambienceLead) / ambienceLead)
        return 0.32f * lead + 0.68f * revealBlend
    }

    fun cameraSettle(age: Float): Float {
        val lead = minOf(ambienceLead, 5)
        return smoothStep((age - revealStart + lead) / (revealDuration + lead))
    }

    fun reveal(age: Float): Float = smoothStep((age - revealStart) / revealDuration)

    companion object {
        fun forSettings(reducedMotion: Boolean, warpTicks: Int, revealTicks: Int): CinematicTiming {
            if (reducedMotion) return CinematicTiming(12, 12)
            val duration = warpTicks.coerceIn(24, 720)
            return CinematicTiming(duration, revealTicks.coerceIn(8, minOf(60, duration - 12)))
        }
    }
}
