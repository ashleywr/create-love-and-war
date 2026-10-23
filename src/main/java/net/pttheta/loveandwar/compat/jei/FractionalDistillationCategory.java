package net.pttheta.loveandwar.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.Info;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.pttheta.loveandwar.compat.jei.animations.AnimatedFractionalColumn;
import net.pttheta.loveandwar.recipe.petrochemical.FractionalDistillationRecipe;

public class FractionalDistillationCategory extends CreateRecipeCategory<FractionalDistillationRecipe> {
   private final AnimatedFractionalColumn fractionatingColumn = new AnimatedFractionalColumn();
   private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();
   public int shift = 60;
   int height = 0;

   public FractionalDistillationCategory(Info<FractionalDistillationRecipe> info) {
      super(info);
   }

   public void setRecipe(IRecipeLayoutBuilder builder, FractionalDistillationRecipe recipe, IFocusGroup focuses) {
      if (!recipe.getFluidIngredients().isEmpty()) {
         SizedFluidIngredient fluidIngredient = (SizedFluidIngredient)recipe.getFluidIngredients().get(0);
         addFluidSlot(builder, 17, 145 - this.shift, fluidIngredient);
         int i = 1;
         int size = recipe.getRollableResults().size() + recipe.getFluidResults().size();

         for (FluidStack fluidResult : recipe.getFluidResults()) {
            int yPosition = -23 * i + 150 - this.shift;
            addFluidSlot(builder, 130, yPosition, fluidResult);
            i++;
         }

         HeatCondition requiredHeat = recipe.getRequiredHeat();
         if (!requiredHeat.testBlazeBurner(HeatLevel.NONE)) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 171 - this.shift).addItemStack(AllBlocks.BLAZE_BURNER.asStack());
         }

         if (!requiredHeat.testBlazeBurner(HeatLevel.KINDLED)) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 153, 171 - this.shift).addItemStack(AllItems.BLAZE_CAKE.asStack());
         }
      }
   }

   public void draw(FractionalDistillationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      HeatCondition requiredHeat = recipe.getRequiredHeat();
      boolean noHeat = requiredHeat == HeatCondition.NONE;
      AllGuiTextures.JEI_ARROW.render(graphics, 40, 150 - this.shift);
      AllGuiTextures shadow = noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
      shadow.render(graphics, 81, 153 + (noHeat ? 10 : 30) - this.shift);
      this.fractionatingColumn.draw(graphics, 91, 142 - this.shift, recipe.getFluidResults().size());
      if (!noHeat) {
         this.heater.withHeat(requiredHeat.visualizeAsBlazeBurner()).draw(graphics, 91, 150 - this.shift);
      }

      AllGuiTextures heatBar = noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR;
      heatBar.render(graphics, 4, 170);
      graphics.drawString(
         Minecraft.getInstance().font,
         CreateLang.translateDirect(requiredHeat.getTranslationKey(), new Object[0]),
         9,
         176 - this.shift,
         requiredHeat.getColor(),
         false
      );
   }
}
