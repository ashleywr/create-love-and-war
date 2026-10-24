package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.pttheta.loveandwar.LoveAndWarMod;

/** One owner-only Shoot callback; the receiving player samples the original yaw distribution. */
public record SGunRecoil(float horizontal,float vertical,long triggerIndex) implements CustomPacketPayload {
    public static final Type<SGunRecoil> TYPE=new Type<>(LoveAndWarMod.asResource("gun_recoil"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SGunRecoil> STREAM_CODEC=StreamCodec.composite(
        ByteBufCodecs.FLOAT,SGunRecoil::horizontal,ByteBufCodecs.FLOAT,SGunRecoil::vertical,ByteBufCodecs.VAR_LONG,SGunRecoil::triggerIndex,SGunRecoil::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
