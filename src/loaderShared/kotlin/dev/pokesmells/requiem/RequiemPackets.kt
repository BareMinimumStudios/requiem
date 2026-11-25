package dev.pokesmells.requiem

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.UUID

data class DeathView(
    val transaction: UUID,
    val lives: Int,
    val cause: DeathCause,
    val canFinalize: Boolean,
    val canDefer: Boolean,
    val intro: Boolean = false,
) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<DeathView>(id("death_view"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, DeathView> = StreamCodec.of(
            { buf, view ->
                buf.writeUUID(view.transaction)
                buf.writeVarInt(view.lives)
                buf.writeEnum(view.cause)
                buf.writeBoolean(view.canFinalize)
                buf.writeBoolean(view.canDefer)
                buf.writeBoolean(view.intro)
            },
            { buf ->
                DeathView(buf.readUUID(), buf.readVarInt(), buf.readEnum(DeathCause::class.java),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
            },
        )
    }
}

data class DeathChoice(val transaction: UUID, val decision: Decision) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<DeathChoice>(id("death_choice"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, DeathChoice> = StreamCodec.of(
            { buf, choice -> buf.writeUUID(choice.transaction); buf.writeEnum(choice.decision) },
            { buf -> DeathChoice(buf.readUUID(), buf.readEnum(Decision::class.java)) },
        )
    }
}
