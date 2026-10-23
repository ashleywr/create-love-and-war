package net.pttheta.loveandwar.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public class FuelItem extends Item {
   private final int burnTime;

   public FuelItem(Properties properties, int burnTime) {
      super(properties);
      this.burnTime = burnTime;
   }

   public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
      return this.burnTime;
   }
}
