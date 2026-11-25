package dev.pokesmells.requiem

internal fun smoothStep(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal data class CinematicTiming(val duration: Int, val revealDuration: Int) {
    val revealStart: Int get() = duration - revealDuration

    fun reveal(age: Float): Float = smoothStep((age - revealStart) / revealDuration)

    companion object {
        fun forSettings(reducedMotion: Boolean, warpTicks: Int, revealTicks: Int): CinematicTiming {
            if (reducedMotion) return CinematicTiming(12, 12)
            val duration = warpTicks.coerceIn(24, 160)
            return CinematicTiming(duration, revealTicks.coerceIn(8, minOf(22, duration - 12)))
        }
    }
}
