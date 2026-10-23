package net.pttheta.loveandwar.compat.jei;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Info;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;
import net.pttheta.loveandwar.compat.jei.animations.AnimatedStampingPress;
import net.pttheta.loveandwar.recipe.stamping.StampingRecipe;

public class StampingCategory extends LAWRecipeCategory<StampingRecipe> {
   private final AnimatedStampingPress press = new AnimatedStampingPress(false);

   public StampingCategory(Info<StampingRecipe> info) {
      super(info);
   }

   public void setRecipe(IRecipeLayoutBuilder builder, StampingRecipe recipe, IFocusGroup focuses) {
      builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients((Ingredient)recipe.getIngredients().get(0));
      builder.addSlot(RecipeIngredientRole.CATALYST, 27, 21).setBackground(getRenderedSlot(), -1, -1).addItemStack(recipe.getTemplate());
      List<ProcessingOutput> results = recipe.getRollableResults();
      int i = 0;

      for (ProcessingOutput output : results) {
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 131 + 19 * i, 50)
               .setBackground(getRenderedSlot(output), -1, -1)
               .addItemStack(output.getStack()))
            .addRichTooltipCallback(addStochasticTooltip(output));
         i++;
      }
   }

   public void draw(StampingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      AllGuiTextures.JEI_SHADOW.render(graphics, 61, 41);
      AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 54);
      this.press.draw(graphics, this.getBackground().getWidth() / 2 - 17, 22);
   }
}
