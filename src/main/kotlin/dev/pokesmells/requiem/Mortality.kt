package dev.pokesmells.requiem

import java.util.Locale
import java.util.UUID

enum class MortalityMode { UNLIMITED, LIMITED, HARDCORE, DISABLED }
enum class DeathCause { PLAYER, MOB, ENVIRONMENT }
enum class FinalPolicy { BAN, SPECTATOR, LIMBO }
enum class RespawnMode { MANUAL, AUTOMATIC, INSTANT, VANILLA }
enum class Decision { RETURN, FINAL, DEFER }

data class PlayerOverride(val mode: MortalityMode, val lives: Int)

data class MortalityRules(
    val mode: MortalityMode = MortalityMode.UNLIMITED,
    val defaultLives: Int = 3,
    val chargedCauses: Set<DeathCause> = DeathCause.entries.toSet(),
    val voluntaryFinalDeath: Boolean = false,
    val deferredDecision: Boolean = true,
    val finalPolicy: FinalPolicy = FinalPolicy.SPECTATOR,
    val respawnMode: RespawnMode = RespawnMode.MANUAL,
    val automaticDelaySeconds: Int = 5,
    val overrides: Map<String, PlayerOverride> = emptyMap(),
) {
    fun begin(
        player: UUID,
        name: String,
        cause: DeathCause,
        previous: PlayerRecord,
        transaction: UUID = UUID.randomUUID(),
        previousGameMode: String? = null,
    ): PlayerRecord {
        if (previous.pending != null || previous.finalPolicy != null) return previous
        val override = overrides[player.toString()] ?: overrides[name.lowercase(Locale.ROOT)]
        val effectiveMode = override?.mode ?: mode
        if (effectiveMode == MortalityMode.DISABLED) return previous

        val remaining = when (effectiveMode) {
            MortalityMode.LIMITED -> previous.remainingLives ?: (override?.lives ?: defaultLives).coerceAtLeast(1)
            MortalityMode.HARDCORE -> 1
            else -> null
        }?.let { if (cause in chargedCauses) (it - 1).coerceAtLeast(0) else it }
        return previous.copy(
            remainingLives = if (effectiveMode == MortalityMode.LIMITED) remaining else previous.remainingLives,
            pending = PendingDeath(
                transaction, cause, remaining, voluntaryFinalDeath, deferredDecision, finalPolicy,
                respawnMode, automaticDelaySeconds.coerceIn(0, 600), previousGameMode,
            ),
        )
    }
}

data class PendingDeath(
    val id: UUID,
    val cause: DeathCause,
    val lives: Int?,
    val voluntaryFinalDeath: Boolean,
    val deferredDecision: Boolean,
    val policy: FinalPolicy,
    val respawnMode: RespawnMode,
    val automaticDelaySeconds: Int,
    val previousGameMode: String? = null,
) {
    val canReturn get() = lives != 0
    val canFinalize get() = !canReturn || voluntaryFinalDeath
}

data class PlayerRecord(
    val remainingLives: Int? = null,
    val pending: PendingDeath? = null,
    val finalPolicy: FinalPolicy? = null,
    val introSeen: Boolean = false,
) {
    fun decide(id: UUID, choice: Decision): PlayerRecord {
        val death = pending ?: return this
        if (id != death.id) return this
        return when (choice) {
            Decision.RETURN -> if (death.canReturn) copy(pending = null) else this
            Decision.FINAL -> if (death.canFinalize) copy(pending = null, finalPolicy = death.policy) else this
            Decision.DEFER -> this
        }
    }
}
