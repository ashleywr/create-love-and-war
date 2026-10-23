package net.pttheta.loveandwar.compat.jei;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Info;
import net.minecraft.world.item.crafting.Recipe;

public abstract class LAWRecipeCategory<T extends Recipe<?>> extends CreateRecipeCategory<T> {
   public LAWRecipeCategory(Info<T> info) {
      super(info);
   }
}
