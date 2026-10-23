package net.pttheta.loveandwar.recipe.stamping;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.pttheta.loveandwar.recipe.ModRecipes;

public class StampingRecipeInfo implements IRecipeTypeInfo {
   public ResourceLocation getId() {
      return ModRecipes.STAMPING_ID;
   }

   public <T extends RecipeSerializer<?>> T getSerializer() {
      return (T)ModRecipes.STAMPING.get();
   }

   public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
      return (RecipeType<R>)ModRecipes.STAMPING_TYPE.get();
   }
}
