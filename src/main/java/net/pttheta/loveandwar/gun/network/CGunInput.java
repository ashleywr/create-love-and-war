package net.pttheta.loveandwar.gun.network;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;

public record CGunInput(String inputType, boolean pressed, int inventorySlot, UUID gunId, GunPredictionClaim prediction) implements CustomPacketPayload {
   public CGunInput(String inputType, boolean pressed, int inventorySlot, UUID gunId) { this(inputType,pressed,inventorySlot,gunId,GunPredictionClaim.NONE); }
   public static final Type<CGunInput> TYPE = new Type<>(LoveAndWarMod.asResource("gun_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CGunInput> STREAM_CODEC = StreamCodec.of((buf,input)->{
      buf.writeUtf(input.inputType(),64);buf.writeBoolean(input.pressed());buf.writeVarInt(input.inventorySlot());buf.writeUUID(input.gunId());input.prediction().write(buf);
   },buf->new CGunInput(buf.readUtf(64),buf.readBoolean(),buf.readVarInt(),buf.readUUID(),GunPredictionClaim.read(buf)));

   public static Optional<CGunInput> capture(Player player, InteractionHand hand, String inputType) {
      var id = GunIdentity.id(player.getItemInHand(hand));
      return GunIdentity.INVALID.equals(id) ? Optional.empty() : Optional.of(new CGunInput(inputType, true,
         hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected, id));
   }

   public CGunInput predicted(GunPredictionClaim claim) { return new CGunInput(inputType, pressed, inventorySlot, gunId, claim); }
   public CGunInput released() { return new CGunInput(inputType, false, inventorySlot, gunId, prediction); }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
