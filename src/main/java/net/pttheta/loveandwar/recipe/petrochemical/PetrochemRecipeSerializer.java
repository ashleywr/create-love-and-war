package net.pttheta.loveandwar.recipe.petrochemical;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe.Factory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PetrochemRecipeSerializer<R extends ProcessingRecipe<?, PetrochemRecipeParams>> implements RecipeSerializer<R> {
   private final MapCodec<R> codec;
   private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

   public PetrochemRecipeSerializer(Factory<PetrochemRecipeParams, R> factory) {
      this.codec = ProcessingRecipe.codec(factory, PetrochemRecipeParams.CODEC);
      this.streamCodec = ProcessingRecipe.streamCodec(factory, PetrochemRecipeParams.STREAM_CODEC);
   }

   public MapCodec<R> codec() {
      return this.codec;
   }

   public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
      return this.streamCodec;
   }
}
