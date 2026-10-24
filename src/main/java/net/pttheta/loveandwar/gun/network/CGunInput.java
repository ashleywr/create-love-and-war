package net.pttheta.loveandwar.gun.network;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;

public record CGunInput(String inputType, boolean pressed, int inventorySlot, UUID gunId) implements CustomPacketPayload {
   public static final Type<CGunInput> TYPE = new Type<>(LoveAndWarMod.asResource("gun_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CGunInput> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CGunInput::inputType, ByteBufCodecs.BOOL, CGunInput::pressed,
      ByteBufCodecs.VAR_INT, CGunInput::inventorySlot, UUIDUtil.STREAM_CODEC, CGunInput::gunId, CGunInput::new
   );

   public static Optional<CGunInput> capture(Player player, InteractionHand hand, String inputType) {
      var id = GunIdentity.id(player.getItemInHand(hand));
      return GunIdentity.INVALID.equals(id) ? Optional.empty() : Optional.of(new CGunInput(inputType, true,
         hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected, id));
   }

   public CGunInput released() { return new CGunInput(inputType, false, inventorySlot, gunId); }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
