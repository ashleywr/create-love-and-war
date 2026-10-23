package net.pttheta.loveandwar.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.pttheta.loveandwar.LoveAndWarMod;

public record SSendMessage(Component toSay) implements CustomPacketPayload {
   public static final Type<SSendMessage> TYPE = new Type(LoveAndWarMod.asResource("send_message"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SSendMessage> STREAM_CODEC = ComponentSerialization.STREAM_CODEC
      .map(SSendMessage::new, SSendMessage::toSay);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
