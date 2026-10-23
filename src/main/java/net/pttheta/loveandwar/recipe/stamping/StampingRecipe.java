package net.pttheta.loveandwar.recipe.stamping;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.pttheta.loveandwar.compat.jei.StampingAssemblySubCategory;

public class StampingRecipe extends ProcessingRecipe<RecipeWrapper, StampingRecipeParams> implements IAssemblyRecipe {
   public StampingRecipe(StampingRecipeParams params) {
      super(new StampingRecipeInfo(), params);
   }

   public Ingredient getIngredient() {
      return this.ingredients.isEmpty() ? Ingredient.EMPTY : (Ingredient)this.ingredients.get(0);
   }

   public ItemStack getTemplate() {
      return ((StampingRecipeParams)this.getParams()).getTemplate();
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
      return Component.translatable("createloveandwar.recipe.stamping.sequence");
   }

   public void addRequiredMachines(Set<ItemLike> list) {
   }

   public void addAssemblyIngredients(List<Ingredient> list) {
   }

   public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
      return () -> StampingAssemblySubCategory::new;
   }
}
