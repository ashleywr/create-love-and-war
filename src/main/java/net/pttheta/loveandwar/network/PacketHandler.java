package net.pttheta.loveandwar.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

public class PacketHandler {
   public static void register() {
   }

   public static <T extends CustomPacketPayload> void sendToServer(T message) {
      PacketDistributor.sendToServer(message, new CustomPacketPayload[0]);
   }

   public static <T extends CustomPacketPayload> void sendToPlayer(T message, ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, message, new CustomPacketPayload[0]);
   }

   public static <T extends CustomPacketPayload> void sendToNearbyPlayers(T message, LevelChunk chunk) {
      if (chunk.getLevel() instanceof ServerLevel serverLevel) {
         ChunkPos pos = chunk.getPos();
         PacketDistributor.sendToPlayersTrackingChunk(serverLevel, pos, message, new CustomPacketPayload[0]);
      }
   }

   public static <T extends CustomPacketPayload> void sendToNearbyPlayers(T message, ServerLevel level, double x, double y, double z, double radius) {
      PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, message, new CustomPacketPayload[0]);
   }

   public static <T extends CustomPacketPayload> void sendToAllClients(T message) {
      PacketDistributor.sendToAllPlayers(message, new CustomPacketPayload[0]);
   }
}
