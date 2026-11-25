package dev.pokesmells.requiem

import kotlin.math.floor

internal object StarFlight {
    const val NEAR = 0.20f
    const val FAR = 4.80f
    private const val RAMP_TICKS = 18f
    private const val SPEED = 0.19f

    fun speed(ticks: Float): Float = SPEED * smoothStep(ticks / RAMP_TICKS)

    fun distance(ticks: Float): Float {
        val u = (ticks / RAMP_TICKS).coerceIn(0f, 1f)
        val u2 = u * u
        val rampDistance = RAMP_TICKS * u2 * u * (1f - 0.5f * u)
        return SPEED * (rampDistance + (ticks - RAMP_TICKS).coerceAtLeast(0f))
    }

    fun depth(initial: Float, distance: Float): Float {
        val span = FAR - NEAR
        val offset = initial - NEAR - distance
        return NEAR + offset - floor(offset / span) * span
    }
}
