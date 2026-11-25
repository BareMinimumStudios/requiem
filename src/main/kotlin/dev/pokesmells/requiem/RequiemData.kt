package dev.pokesmells.requiem

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class RequiemData : SavedData() {
    private val records = mutableMapOf<UUID, PlayerRecord>()

    fun record(player: UUID): PlayerRecord = records[player] ?: PlayerRecord()

    fun update(player: UUID, record: PlayerRecord) {
        if (record == record(player)) return
        records[player] = record
        setDirty()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val players = CompoundTag()
        for ((uuid, record) in records) {
            val entry = CompoundTag()
            record.remainingLives?.let { entry.putInt("lives", it) }
            record.finalPolicy?.let { entry.putString("final", it.name) }
            entry.putBoolean("introSeen", record.introSeen)
            record.pending?.let { pending ->
                val death = CompoundTag()
                death.putUUID("id", pending.id)
                death.putString("cause", pending.cause.name)
                pending.lives?.let { death.putInt("lives", it) }
                death.putBoolean("voluntary", pending.voluntaryFinalDeath)
                death.putBoolean("defer", pending.deferredDecision)
                death.putString("policy", pending.policy.name)
                death.putString("respawn", pending.respawnMode.name)
                death.putInt("delay", pending.automaticDelaySeconds)
                entry.put("pending", death)
            }
            players.put(uuid.toString(), entry)
        }
        tag.put("players", players)
        return tag
    }

    companion object {
        private val factory = Factory(::RequiemData, ::load, null)

        fun get(server: MinecraftServer): RequiemData =
            server.overworld().dataStorage.computeIfAbsent(factory, "requiem_cinematics")

        @Suppress("UNUSED_PARAMETER")
        private fun load(tag: CompoundTag, registries: HolderLookup.Provider): RequiemData {
            val result = RequiemData()
            val players = tag.getCompound("players")
            for (key in players.allKeys) {
                val uuid = UUID.fromString(key)
                val entry = players.getCompound(key)
                val pending = if (entry.contains("pending", Tag.TAG_COMPOUND.toInt())) {
                    val death = entry.getCompound("pending")
                    PendingDeath(
                        death.getUUID("id"), DeathCause.valueOf(death.getString("cause")),
                        death.lives(), death.getBoolean("voluntary"), death.getBoolean("defer"),
                        FinalPolicy.valueOf(death.getString("policy")), RespawnMode.valueOf(death.getString("respawn")),
                        death.getInt("delay"),
                    )
                } else null
                result.records[uuid] = PlayerRecord(
                    entry.lives(), pending, entry.getString("final").takeIf(String::isNotEmpty)?.let(FinalPolicy::valueOf),
                    entry.getBoolean("introSeen"),
                )
            }
            return result
        }

        private fun CompoundTag.lives(): Int? = getInt("lives").takeIf { contains("lives", Tag.TAG_INT.toInt()) }
    }
}
