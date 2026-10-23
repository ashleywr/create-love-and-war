package net.pttheta.loveandwar.recipe.petrochemical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.foundation.codec.CreateCodecs;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class PetrochemRecipeParams extends ProcessingRecipeParams {
   public static final MapCodec<PetrochemRecipeParams> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            CreateCodecs.SIZED_FLUID_INGREDIENT.listOf().fieldOf("ingredients").forGetter(PetrochemRecipeParams::getFluidIngredientList),
            FluidStack.CODEC.listOf().fieldOf("results").forGetter(PetrochemRecipeParams::getFluidResultList),
            Codec.INT.optionalFieldOf("processing_time", 0).forGetter(rec$ -> rec$.processingDuration()),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(rec$ -> rec$.requiredHeat())
         )
         .apply(instance, PetrochemRecipeParams::create)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PetrochemRecipeParams> STREAM_CODEC = streamCodec(PetrochemRecipeParams::new);

   protected PetrochemRecipeParams() {
   }

   private static PetrochemRecipeParams create(List<SizedFluidIngredient> ingredients, List<FluidStack> results, int duration, HeatCondition heat) {
      PetrochemRecipeParams params = new PetrochemRecipeParams();
      params.fluidIngredients.addAll(ingredients);
      params.fluidResults.addAll(results);
      params.processingDuration = duration;
      params.requiredHeat = heat;
      return params;
   }

   public List<SizedFluidIngredient> getFluidIngredientList() {
      return this.fluidIngredients;
   }

   public List<FluidStack> getFluidResultList() {
      return this.fluidResults;
   }
}
