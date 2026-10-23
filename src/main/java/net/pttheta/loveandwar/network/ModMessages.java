package net.pttheta.loveandwar.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1.0");
      registrar.playToServer(SSendMessage.TYPE, SSendMessage.STREAM_CODEC, ModMessages::handleSSendMessage);
   }

   private static void handleSSendMessage(SSendMessage payload, IPayloadContext context) {
      context.enqueueWork(() -> context.player().sendSystemMessage(payload.toSay()));
   }

   public static <T extends CustomPacketPayload> void sendToServer(T message) {
      PacketDistributor.sendToServer(message, new CustomPacketPayload[0]);
   }

   public static <T extends CustomPacketPayload> void sendToPlayer(T message, ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, message, new CustomPacketPayload[0]);
   }

   public static <T extends CustomPacketPayload> void sendToNearbyPlayers(T message, ServerLevel level, double x, double y, double z, double radius) {
      PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, message, new CustomPacketPayload[0]);
   }
}
