package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.pttheta.loveandwar.LoveAndWarMod;

import java.util.UUID;

/**
 * Retires or denies a presentation claim without replaying or rolling back already played visuals.
 */
public record SGunPresentationEnd(
        UUID gun, UUID prediction, int consumed, long ammoRevision, int remainingRounds)
        implements CustomPacketPayload {
    public static final Type<SGunPresentationEnd> TYPE =
            new Type<>(LoveAndWarMod.asResource("gun_presentation_end"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SGunPresentationEnd> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUUID(p.gun());
                        buf.writeUUID(p.prediction());
                        buf.writeVarInt(p.consumed());
                        buf.writeVarLong(p.ammoRevision());
                        buf.writeVarInt(p.remainingRounds());
                    },
                    buf ->
                            new SGunPresentationEnd(
                                    buf.readUUID(),
                                    buf.readUUID(),
                                    buf.readVarInt(),
                                    buf.readVarLong(),
                                    buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
