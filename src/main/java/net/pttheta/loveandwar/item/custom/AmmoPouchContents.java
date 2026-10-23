package net.pttheta.loveandwar.item.custom;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record AmmoPouchContents(List<ItemStack> items) {
   public static final AmmoPouchContents EMPTY = new AmmoPouchContents(List.of());
   public static final Codec<AmmoPouchContents> CODEC = ItemStack.CODEC.listOf().xmap(AmmoPouchContents::new, AmmoPouchContents::items);
   public static final StreamCodec<RegistryFriendlyByteBuf, AmmoPouchContents> STREAM_CODEC = ItemStack.LIST_STREAM_CODEC
      .map(AmmoPouchContents::new, AmmoPouchContents::items);

   public AmmoPouchContents {
      items = List.copyOf(items);
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }
}
