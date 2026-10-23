package net.pttheta.loveandwar.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Builder;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.item.ModItems;
import net.pttheta.loveandwar.recipe.ModRecipes;
import net.pttheta.loveandwar.recipe.RecipeRegister;
import net.pttheta.loveandwar.recipe.drawing.DrawingRecipe;
import net.pttheta.loveandwar.recipe.petrochemical.FractionalDistillationRecipe;
import net.pttheta.loveandwar.recipe.stamping.StampingRecipe;

@JeiPlugin
public class LAWCompatJEI implements IModPlugin {
   private static final ResourceLocation ID = LoveAndWarMod.asResource("jei_plugin");
   public IIngredientManager ingredientManager;
   final List<CreateRecipeCategory<?>> ALL = new ArrayList<>();

   public ResourceLocation getPluginUid() {
      return ID;
   }

   public void registerCategories(IRecipeCategoryRegistration registration) {
      this.ALL.clear();
      this.ALL
         .add(
            this.builder(DrawingRecipe.class)
               .addTypedRecipes(ModRecipes.DRAWING_TYPE::get)
               .catalyst(LAWBlocks.DRAWING_PRESS::get)
               .itemIcon((ItemLike)LAWBlocks.DRAWING_PRESS.get())
               .emptyBackground(177, 53)
               .build("drawing", DrawingCategory::new)
         );
      this.ALL
         .add(
            this.builder(StampingRecipe.class)
               .addTypedRecipes(ModRecipes.STAMPING_TYPE::get)
               .catalyst(LAWBlocks.STAMPING_PRESS::get)
               .itemIcon((ItemLike)LAWBlocks.STAMPING_PRESS.get())
               .emptyBackground(177, 53)
               .build("stamping", StampingCategory::new)
         );
      this.ALL
         .add(
            this.builder(FractionalDistillationRecipe.class)
               .addTypedRecipes(RecipeRegister.FRACTIONAL_DISTILLATION)
               .catalyst(AllBlocks.FLUID_TANK::get)
               .catalyst(ModItems.THERMOSTAT::get)
               .doubleItemIcon((ItemLike)AllBlocks.FLUID_TANK.get(), (ItemLike)ModItems.THERMOSTAT.get())
               .emptyBackground(177, 200)
               .build("fractional_distillation", FractionalDistillationCategory::new)
         );
      this.ALL.forEach(category -> registration.addRecipeCategories(new IRecipeCategory[]{category}));
   }

   public void registerRecipes(IRecipeRegistration registration) {
      this.ingredientManager = registration.getIngredientManager();
      this.ALL.forEach(c -> c.registerRecipes(registration));
   }

   private <T extends Recipe<? extends RecipeInput>> LAWCompatJEI.CategoryBuilder<T> builder(Class<? extends T> recipeClass) {
      return new LAWCompatJEI.CategoryBuilder<>(recipeClass);
   }

   private static class CategoryBuilder<T extends Recipe<? extends RecipeInput>> extends Builder<T> {
      CategoryBuilder(Class<? extends T> recipeClass) {
         super(recipeClass);
      }
   }
}
