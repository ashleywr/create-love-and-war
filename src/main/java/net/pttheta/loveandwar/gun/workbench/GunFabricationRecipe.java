package net.pttheta.loveandwar.gun.workbench;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.pttheta.loveandwar.gun.runtime.CraftedParts;
import net.pttheta.loveandwar.item.ModDataComponents;

public record GunFabricationRecipe(List<SizedIngredient> ingredients, ItemStack result, int craftTimeTicks, boolean showNotification) implements Recipe<RecipeInput> {
   public GunFabricationRecipe {
      ingredients = List.copyOf(ingredients);
      result = result.copy();
   }

   @Override
   public ItemStack result() {
      return this.result.copy();
   }
   public boolean matches(RecipeInput input, Level level) {
      return this.planConsumption(input).isPresent();
   }

   public ItemStack assemble(RecipeInput input, Provider registries) {
      ItemStack assembled = this.result.copy();
      assembled.set(ModDataComponents.CRAFTED_PARTS, CraftedParts.from(input));
      return assembled;
   }

   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   public ItemStack getResultItem(Provider registries) {
      return this.result.copy();
   }

   public RecipeSerializer<?> getSerializer() {
      return (RecipeSerializer<?>)ModWorkbenchRecipes.GUN_FABRICATION_SERIALIZER.get();
   }

   public RecipeType<?> getType() {
      return (RecipeType<?>)ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get();
   }

   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> out = NonNullList.create();

      for (SizedIngredient sized : this.ingredients) {
         out.add(sized.ingredient());
      }

      return out;
   }

   public Optional<int[]> planConsumption(RecipeInput input) {
      if (this.ingredients.isEmpty() || input.size() < this.ingredients.size()) return Optional.empty();
      int[] toConsume = new int[input.size()];
      // GunFabricationRecipe in the reference matches each ingredient to its own slot.
      for (int i = 0; i < this.ingredients.size(); i++) {
         if (!this.ingredients.get(i).test(input.getItem(i))) return Optional.empty();
         toConsume[i] = this.ingredients.get(i).count();
      }
      return Optional.of(toConsume);
   }
}
