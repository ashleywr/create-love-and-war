package net.pttheta.loveandwar.recipe.drawing;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.pttheta.loveandwar.compat.jei.DrawingAssemblySubCategory;

public class DrawingRecipe extends ProcessingRecipe<RecipeWrapper, DrawingRecipeParams> implements IAssemblyRecipe {
   public DrawingRecipe(DrawingRecipeParams params) {
      super(new DrawingRecipeInfo(), params);
   }

   public Ingredient getIngredient() {
      return this.ingredients.isEmpty() ? Ingredient.EMPTY : (Ingredient)this.ingredients.get(0);
   }

   public boolean matches(RecipeWrapper inv, Level worldIn) {
      return !inv.isEmpty() && this.getIngredient().test(inv.getItem(0));
   }

   protected int getMaxInputCount() {
      return 1;
   }

   protected boolean canSpecifyDuration() {
      return true;
   }

   protected int getMaxOutputCount() {
      return 32;
   }

   public boolean isSpecial() {
      return true;
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width * height > 0;
   }

   public Component getDescriptionForAssembly() {
      return Component.translatable("createloveandwar.recipe.drawing.sequence");
   }

   public void addRequiredMachines(Set<ItemLike> list) {
   }

   public void addAssemblyIngredients(List<Ingredient> list) {
   }

   public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
      return () -> DrawingAssemblySubCategory::new;
   }
}
