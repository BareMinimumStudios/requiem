package dev.pokesmells.requiem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarpEnvelopeTest {
    @Test
    fun warpBuildsContinuouslyFromTheWaitingToneAndFadesIntoTheReveal() {
        assertEquals(0f, WarpEnvelope.prime(0f).gain)
        assertEquals(WarpEnvelope.prime(1f), WarpEnvelope.flight(0f, 0f))
        val peak = WarpEnvelope.flight(1f, 0f)
        assertTrue(peak.gain > WarpEnvelope.flight(0.5f, 0f).gain)
        assertTrue(peak.pitchProgress > WarpEnvelope.flight(0.5f, 0f).pitchProgress)
        assertTrue(WarpEnvelope.flight(1f, 0.5f).gain < peak.gain)
        assertEquals(0f, WarpEnvelope.flight(1f, 1f).gain)
    }

    @Test
    fun worldAudioDucksDuringTheWaitAndReturnsWithTheReveal() {
        val waiting = (0..10).map { WarpEnvelope.prime(it / 10f).worldBlend }
        assertEquals(1f, waiting.first())
        assertEquals(0f, waiting.last())
        assertTrue(waiting.zipWithNext().all { (before, after) -> before > after })

        val reveal = (0..10).map { WarpEnvelope.flight(1f, it / 10f).worldBlend }
        assertEquals(0f, reveal.first())
        assertEquals(1f, reveal.last())
        assertTrue(reveal.zipWithNext().all { (before, after) -> before < after })
    }
}
