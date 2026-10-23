package net.pttheta.loveandwar.recipe.drawing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.pttheta.loveandwar.Config;

public class DrawingRecipeParams extends ProcessingRecipeParams {
   public static final MapCodec<DrawingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(DrawingRecipeParams::getIngredient),
            ProcessingOutput.CODEC.fieldOf("result").forGetter(DrawingRecipeParams::getOutput)
         )
         .apply(instance, DrawingRecipeParams::create)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DrawingRecipeParams> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      DrawingRecipeParams::getIngredient,
      ProcessingOutput.STREAM_CODEC,
      DrawingRecipeParams::getOutput,
      DrawingRecipeParams::create
   );
   protected ProcessingOutput output = ProcessingOutput.EMPTY;

   protected DrawingRecipeParams() {
   }

   private static DrawingRecipeParams create(Ingredient input, ProcessingOutput output) {
      DrawingRecipeParams params = new DrawingRecipeParams();
      params.ingredients.add(input);
      params.results.add(output);
      params.output = output;
      params.processingDuration = (Integer)Config.DRAWING_PROCESSING_DURATION.get();
      params.requiredHeat = HeatCondition.NONE;
      return params;
   }

   public Ingredient getIngredient() {
      return this.ingredients.isEmpty() ? Ingredient.EMPTY : (Ingredient)this.ingredients.get(0);
   }

   public ProcessingOutput getOutput() {
      return this.output;
   }
}
