package net.pttheta.loveandwar.gun.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record GunAttachments(List<ItemStack> barrel, List<ItemStack> grip, List<ItemStack> stock, List<ItemStack> scope, List<ItemStack> generic) {
   public static final GunAttachments EMPTY = new GunAttachments(List.of(), List.of(), List.of(), List.of(), List.of());
   public static final StreamCodec<RegistryFriendlyByteBuf, GunAttachments> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunAttachments::barrel,
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunAttachments::grip,
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunAttachments::stock,
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunAttachments::scope,
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunAttachments::generic,
      GunAttachments::new
   );
   public static final Codec<GunAttachments> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ItemStack.CODEC.listOf().optionalFieldOf("barrel", List.of()).forGetter(GunAttachments::barrel),
            ItemStack.CODEC.listOf().optionalFieldOf("grip", List.of()).forGetter(GunAttachments::grip),
            ItemStack.CODEC.listOf().optionalFieldOf("stock", List.of()).forGetter(GunAttachments::stock),
            ItemStack.CODEC.listOf().optionalFieldOf("scope", List.of()).forGetter(GunAttachments::scope),
            ItemStack.CODEC.listOf().optionalFieldOf("generic", List.of()).forGetter(GunAttachments::generic)
         )
         .apply(instance, GunAttachments::new)
   );

   public static String category(String attachmentType) {
      return switch (attachmentType) {
         case "Barrel" -> "barrel";
         case "Grip" -> "grip";
         case "Stock" -> "stock";
         case "Sights" -> "scope";
         default -> "generic";
      };
   }

   public List<ItemStack> forCategory(String category) {
      return switch (category) {
         case "barrel" -> this.barrel;
         case "grip" -> this.grip;
         case "stock" -> this.stock;
         case "scope" -> this.scope;
         default -> this.generic;
      };
   }

   public List<ItemStack> all() {
      List<ItemStack> out = new ArrayList<>(this.barrel.size() + this.grip.size() + this.stock.size() + this.scope.size() + this.generic.size());
      out.addAll(this.barrel);
      out.addAll(this.grip);
      out.addAll(this.stock);
      out.addAll(this.scope);
      out.addAll(this.generic);
      return out;
   }

   public GunAttachments with(String category, List<ItemStack> value) {
      return switch (category) {
         case "barrel" -> new GunAttachments(value, this.grip, this.stock, this.scope, this.generic);
         case "grip" -> new GunAttachments(this.barrel, value, this.stock, this.scope, this.generic);
         case "stock" -> new GunAttachments(this.barrel, this.grip, value, this.scope, this.generic);
         case "scope" -> new GunAttachments(this.barrel, this.grip, this.stock, value, this.generic);
         default -> new GunAttachments(this.barrel, this.grip, this.stock, this.scope, value);
      };
   }
}
