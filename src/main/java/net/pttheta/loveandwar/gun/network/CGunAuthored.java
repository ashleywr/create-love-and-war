package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.pttheta.loveandwar.LoveAndWarMod;

import java.util.UUID;

/**
 * Ordered owner-authored group triggers, due-action steps and final retirement; never hit/damage
 * data.
 */
public record CGunAuthored(UUID gun, UUID activation, int tick, long ordinal, Kind kind)
        implements CustomPacketPayload {
    public enum Kind {
        TRIGGER,
        STEP,
        FINISH,
        CANCEL
    }

    public static final Type<CGunAuthored> TYPE =
            new Type<>(LoveAndWarMod.asResource("gun_authored"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CGunAuthored> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeUUID(p.gun());
                        buf.writeUUID(p.activation());
                        buf.writeVarInt(p.tick());
                        buf.writeVarLong(p.ordinal());
                        buf.writeEnum(p.kind());
                    },
                    buf ->
                            new CGunAuthored(
                                    buf.readUUID(),
                                    buf.readUUID(),
                                    buf.readVarInt(),
                                    buf.readVarLong(),
                                    buf.readEnum(Kind.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
