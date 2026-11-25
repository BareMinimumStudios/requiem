package dev.pokesmells.requiem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarFlightTest {
    @Test
    fun speedRampsThenTravelContinuesAtCruise() {
        assertEquals(0f, StarFlight.speed(0f))
        assertTrue(StarFlight.speed(9f) < StarFlight.speed(18f))
        assertEquals(StarFlight.speed(18f), StarFlight.speed(70f))
        val first = StarFlight.distance(40f) - StarFlight.distance(30f)
        val second = StarFlight.distance(60f) - StarFlight.distance(50f)
        assertEquals(first, second, 0.0001f)
    }

    @Test
    fun aStarMovesTowardTheCameraAndWrapsAtTheFarEdge() {
        val initial = 2f
        assertEquals(initial, StarFlight.depth(initial, 0f), 0.0001f)
        assertTrue(StarFlight.depth(initial, 1f) < StarFlight.depth(initial, 0.5f))
        assertTrue(StarFlight.depth(initial, 1.799f) < StarFlight.NEAR + 0.01f)
        assertTrue(StarFlight.depth(initial, 1.801f) > StarFlight.FAR - 0.01f)
    }
}
