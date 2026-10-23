package net.pttheta.loveandwar.recipe.stamping;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.pttheta.loveandwar.Config;

public class StampingRecipeParams extends ProcessingRecipeParams {
   public static final MapCodec<StampingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(StampingRecipeParams::getIngredient),
            ItemStack.SINGLE_ITEM_CODEC.optionalFieldOf("template", ItemStack.EMPTY).forGetter(StampingRecipeParams::getTemplate),
            ProcessingOutput.CODEC.fieldOf("result").forGetter(StampingRecipeParams::getOutput)
         )
         .apply(instance, StampingRecipeParams::create)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, StampingRecipeParams> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      StampingRecipeParams::getIngredient,
      ItemStack.OPTIONAL_STREAM_CODEC,
      StampingRecipeParams::getTemplate,
      ProcessingOutput.STREAM_CODEC,
      StampingRecipeParams::getOutput,
      StampingRecipeParams::create
   );
   protected ItemStack template = ItemStack.EMPTY;
   protected ProcessingOutput output = ProcessingOutput.EMPTY;

   protected StampingRecipeParams() {
   }

   private static StampingRecipeParams create(Ingredient input, ItemStack template, ProcessingOutput output) {
      StampingRecipeParams params = new StampingRecipeParams();
      params.ingredients.add(input);
      params.results.add(output);
      params.template = template;
      params.output = output;
      params.processingDuration = (Integer)Config.STAMPING_PROCESSING_DURATION.get();
      params.requiredHeat = HeatCondition.NONE;
      return params;
   }

   public Ingredient getIngredient() {
      return this.ingredients.isEmpty() ? Ingredient.EMPTY : (Ingredient)this.ingredients.get(0);
   }

   public ItemStack getTemplate() {
      return this.template;
   }

   public ProcessingOutput getOutput() {
      return this.output;
   }
}
