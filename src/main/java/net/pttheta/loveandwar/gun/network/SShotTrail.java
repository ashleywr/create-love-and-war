package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.LoveAndWarMod;

public record SShotTrail(Vec3 origin, Vec3 end, int shooterId) implements CustomPacketPayload {
   public static final Type<SShotTrail> TYPE = new Type(LoveAndWarMod.asResource("shot_trail"));
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3 = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SShotTrail> STREAM_CODEC = StreamCodec.composite(
      VEC3, SShotTrail::origin, VEC3, SShotTrail::end, ByteBufCodecs.VAR_INT, SShotTrail::shooterId, SShotTrail::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
