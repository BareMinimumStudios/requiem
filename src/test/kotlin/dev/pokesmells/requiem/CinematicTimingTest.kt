package dev.pokesmells.requiem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CinematicTimingTest {
    @Test
    fun shortSettingsRetainTimeForTheWarpAndReducedMotionOnlyFades() {
        assertEquals(CinematicTiming(580, 52), CinematicTiming.forSettings(false, 580, 52))
        assertEquals(CinematicTiming(102, 60), CinematicTiming.forSettings(false, 102, 100))
        assertEquals(CinematicTiming(24, 12), CinematicTiming.forSettings(false, 24, 22))
        assertEquals(CinematicTiming(12, 12), CinematicTiming.forSettings(true, 48, 18))
    }

    @Test
    fun revealStaysDarkUntilTheWarpEndsAndSettlesAtFullVisibility() {
        val timing = CinematicTiming(duration = 48, revealDuration = 18)
        assertEquals(0f, timing.reveal(29f))
        assertEquals(0f, timing.reveal(30f))
        assertEquals(0.5f, timing.reveal(39f))
        assertEquals(1f, timing.reveal(48f))
        assertEquals(1f, timing.reveal(70f))
        assertTrue(timing.reveal(31f) < 1f / 18f)
        assertTrue(timing.reveal(47f) > 17f / 18f)
    }

    @Test
    fun starsStopAndWorldAudioLeadsTheReveal() {
        val timing = CinematicTiming.forSettings(false, 580, 52)
        val still = timing.stillStart.toFloat()
        val reveal = timing.revealStart.toFloat()
        assertEquals(0f, timing.starMotion(still))
        assertEquals(timing.starClock(still), timing.starClock(reveal + 12f))
        assertEquals(0f, timing.reveal(reveal))
        assertEquals(0.32f, timing.worldBlend(reveal))
        assertEquals(1f, timing.worldBlend(580f))
        assertEquals(1f, timing.cameraSettle(580f))
        assertEquals(0f, CinematicTiming.forSettings(true, 580, 52).worldBlend(0f))
    }
}
