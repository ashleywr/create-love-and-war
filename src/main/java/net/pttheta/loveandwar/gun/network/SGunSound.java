package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.gun.content.SoundDef;

/** Definition and group multipliers; the receiving camera resolves LOD/volume/pitch. */
public record SGunSound(SoundDef sound, Vec3 position, float volumeMultiplier, float pitchMultiplier) implements CustomPacketPayload {
    public static final Type<SGunSound> TYPE = new Type<>(LoveAndWarMod.asResource("gun_sound"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> POSITION = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SGunSound> STREAM_CODEC = StreamCodec.composite(
        SoundDef.STREAM_CODEC, SGunSound::sound, POSITION, SGunSound::position,
        ByteBufCodecs.FLOAT, SGunSound::volumeMultiplier, ByteBufCodecs.FLOAT, SGunSound::pitchMultiplier, SGunSound::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
