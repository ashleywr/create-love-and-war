package net.pttheta.loveandwar.recipe.drawing;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class DrawingRecipeSerializer implements RecipeSerializer<DrawingRecipe> {
   public static final MapCodec<DrawingRecipe> CODEC = ProcessingRecipe.codec(DrawingRecipe::new, DrawingRecipeParams.CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, DrawingRecipe> STREAM_CODEC = ProcessingRecipe.streamCodec(
      DrawingRecipe::new, DrawingRecipeParams.STREAM_CODEC
   );

   public MapCodec<DrawingRecipe> codec() {
      return CODEC;
   }

   public StreamCodec<RegistryFriendlyByteBuf, DrawingRecipe> streamCodec() {
      return STREAM_CODEC;
   }
}
