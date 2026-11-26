package dev.pokesmells.requiem

internal fun smoothStep(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal data class CinematicTiming(val duration: Int, val revealDuration: Int) {
    val revealStart: Int get() = duration - revealDuration

    private val stillTicks: Int get() = ((revealStart - 12) / 3).coerceIn(0, 22)
    private val brakeTicks: Int get() = ((revealStart - stillTicks - 12) / 2).coerceIn(0, 24)
    val stillStart: Int get() = revealStart - stillTicks
    private val brakeStart: Int get() = stillStart - brakeTicks

    fun starClock(age: Float): Float {
        if (brakeTicks == 0 || age <= brakeStart) return age
        val t = ((age - brakeStart) / brakeTicks).coerceIn(0f, 1f)
        return brakeStart + brakeTicks * (t - t * t * t * (1f - 0.5f * t))
    }

    fun starMotion(age: Float): Float =
        if (brakeTicks == 0) 1f else 1f - smoothStep((age - brakeStart) / brakeTicks)

    fun worldBlend(age: Float): Float {
        val revealBlend = smoothStep(reveal(age))
        if (stillTicks == 0) return revealBlend
        val lead = smoothStep((age - stillStart) / stillTicks)
        return 0.32f * lead + 0.68f * revealBlend
    }

    fun cameraSettle(age: Float): Float {
        val lead = minOf(stillTicks, 5)
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
