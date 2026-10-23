package net.pttheta.loveandwar.recipe.petrochemical;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.pttheta.loveandwar.recipe.RecipeRegister;

public class ReactionRecipe extends ProcessingRecipe<RecipeInput, PetrochemRecipeParams> {
   public ReactionRecipe(PetrochemRecipeParams params) {
      super(RecipeRegister.REACTION, params);
   }

   protected int getMaxInputCount() {
      return 0;
   }

   protected int getMaxOutputCount() {
      return 0;
   }

   protected int getMaxFluidInputCount() {
      return 4;
   }

   protected int getMaxFluidOutputCount() {
      return 4;
   }

   protected boolean canRequireHeat() {
      return true;
   }

   protected boolean canSpecifyDuration() {
      return true;
   }

   public boolean matches(RecipeInput input, Level level) {
      return false;
   }
}
