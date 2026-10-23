package net.pttheta.loveandwar.gun.workbench;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record GunFabricationRecipe(List<SizedIngredient> ingredients, Item result, int craftTimeTicks, boolean showNotification) implements Recipe<RecipeInput> {
   public boolean matches(RecipeInput input, Level level) {
      return this.planConsumption(input).isPresent();
   }

   public ItemStack assemble(RecipeInput input, Provider registries) {
      return new ItemStack(this.result);
   }

   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   public ItemStack getResultItem(Provider registries) {
      return new ItemStack(this.result);
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
      int size = input.size();
      int[] remaining = new int[size];

      for (int i = 0; i < size; i++) {
         remaining[i] = input.getItem(i).getCount();
      }

      int[] toConsume = new int[size];

      for (SizedIngredient ingredient : this.ingredients) {
         int need = ingredient.count();

         for (int i = 0; i < size && need > 0; i++) {
            if (remaining[i] > 0) {
               ItemStack stack = input.getItem(i);
               if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                  int take = Math.min(need, remaining[i]);
                  remaining[i] -= take;
                  toConsume[i] += take;
                  need -= take;
               }
            }
         }

         if (need > 0) {
            return Optional.empty();
         }
      }

      return Optional.of(toConsume);
   }
}
