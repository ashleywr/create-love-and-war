package net.pttheta.loveandwar.gun.workbench;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

public record PartsInput(IItemHandler handler) implements RecipeInput {
   public ItemStack getItem(int slot) {
      return this.handler.getStackInSlot(slot);
   }

   public int size() {
      return this.handler.getSlots();
   }
}
