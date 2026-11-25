package dev.pokesmells.requiem

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MortalityTest {
    private val player = UUID.randomUUID()

    @Test
    fun unlimitedAndDisabled() {
        val empty = PlayerRecord()
        val unlimited = assertNotNull(MortalityRules().begin(player, "Alex", DeathCause.MOB, empty).pending)
        assertTrue(unlimited.canReturn)
        assertFalse(unlimited.canFinalize)
        assertNull(unlimited.lives)
        assertEquals(empty, MortalityRules(mode = MortalityMode.DISABLED).begin(player, "Alex", DeathCause.MOB, empty))
    }

    @Test
    fun uuidOverrideAndCauseFilter() {
        val rules = MortalityRules(
            chargedCauses = setOf(DeathCause.PLAYER),
            overrides = mapOf(
                "alex" to PlayerOverride(MortalityMode.HARDCORE, 1),
                player.toString() to PlayerOverride(MortalityMode.LIMITED, 2),
            ),
        )
        val excluded = rules.begin(player, "Alex", DeathCause.ENVIRONMENT, PlayerRecord())
        val death = assertNotNull(excluded.pending)
        assertEquals(2, death.lives)
        assertEquals(excluded, excluded.decide(death.id, Decision.DEFER))
        val returned = excluded.decide(death.id, Decision.RETURN)
        val charged = rules.begin(player, "Alex", DeathCause.PLAYER, returned)
        assertEquals(1, charged.remainingLives)
        assertEquals(charged, rules.begin(player, "Alex", DeathCause.PLAYER, charged))
        assertEquals(charged, charged.decide(UUID.randomUUID(), Decision.RETURN))
    }

    @Test
    fun hardcoreRequiresFinalDecision() {
        val rules = MortalityRules(mode = MortalityMode.HARDCORE, finalPolicy = FinalPolicy.LIMBO)
        val deceased = rules.begin(player, "Alex", DeathCause.MOB, PlayerRecord())
        val pending = assertNotNull(deceased.pending)
        assertEquals(deceased, deceased.decide(pending.id, Decision.RETURN))
        val final = deceased.decide(pending.id, Decision.FINAL)
        assertEquals(FinalPolicy.LIMBO, final.finalPolicy)
        assertNull(final.pending)
        assertEquals(final, final.decide(pending.id, Decision.FINAL))
    }
}
