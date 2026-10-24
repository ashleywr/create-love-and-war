package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.pttheta.loveandwar.LoveAndWarMod;

/** One owner-only Shoot callback; the receiving player samples the original yaw distribution. */
public record SGunRecoil(float horizontal,float vertical,long triggerIndex,GunShotReceipt receipt) implements CustomPacketPayload {
    public SGunRecoil(float horizontal,float vertical,long triggerIndex){this(horizontal,vertical,triggerIndex,GunShotReceipt.NONE);}
    public static final Type<SGunRecoil> TYPE=new Type<>(LoveAndWarMod.asResource("gun_recoil"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SGunRecoil> STREAM_CODEC=StreamCodec.of((buf,packet)->{
        buf.writeFloat(packet.horizontal());buf.writeFloat(packet.vertical());buf.writeVarLong(packet.triggerIndex());packet.receipt().write(buf);
    },buf->new SGunRecoil(buf.readFloat(),buf.readFloat(),buf.readVarLong(),GunShotReceipt.read(buf)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
