package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.pttheta.loveandwar.LoveAndWarMod;

public record CGunInput(String inputType, boolean pressed) implements CustomPacketPayload {
   public static final Type<CGunInput> TYPE = new Type(LoveAndWarMod.asResource("gun_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CGunInput> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CGunInput::inputType, ByteBufCodecs.BOOL, CGunInput::pressed, CGunInput::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
