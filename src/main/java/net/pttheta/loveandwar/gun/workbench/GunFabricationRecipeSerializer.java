package net.pttheta.loveandwar.gun.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class GunFabricationRecipeSerializer implements RecipeSerializer<GunFabricationRecipe> {
   public static final MapCodec<GunFabricationRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(GunFabricationRecipe::ingredients),
            ItemStack.CODEC.fieldOf("result").forGetter(GunFabricationRecipe::result),
            Codec.INT.optionalFieldOf("craft_time", 20).forGetter(GunFabricationRecipe::craftTimeTicks),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(GunFabricationRecipe::showNotification)
         )
         .apply(instance, GunFabricationRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, GunFabricationRecipe> STREAM_CODEC = StreamCodec.composite(
      SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunFabricationRecipe::ingredients,
      ItemStack.STREAM_CODEC,
      GunFabricationRecipe::result,
      ByteBufCodecs.VAR_INT,
      GunFabricationRecipe::craftTimeTicks,
      ByteBufCodecs.BOOL,
      GunFabricationRecipe::showNotification,
      GunFabricationRecipe::new
   );

   public MapCodec<GunFabricationRecipe> codec() {
      return CODEC;
   }

   public StreamCodec<RegistryFriendlyByteBuf, GunFabricationRecipe> streamCodec() {
      return STREAM_CODEC;
   }
}
