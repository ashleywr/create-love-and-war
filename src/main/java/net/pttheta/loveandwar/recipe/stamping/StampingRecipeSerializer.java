package net.pttheta.loveandwar.recipe.stamping;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class StampingRecipeSerializer implements RecipeSerializer<StampingRecipe> {
   public static final MapCodec<StampingRecipe> CODEC = ProcessingRecipe.codec(StampingRecipe::new, StampingRecipeParams.CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, StampingRecipe> STREAM_CODEC = ProcessingRecipe.streamCodec(
      StampingRecipe::new, StampingRecipeParams.STREAM_CODEC
   );

   public MapCodec<StampingRecipe> codec() {
      return CODEC;
   }

   public StreamCodec<RegistryFriendlyByteBuf, StampingRecipe> streamCodec() {
      return STREAM_CODEC;
   }
}
