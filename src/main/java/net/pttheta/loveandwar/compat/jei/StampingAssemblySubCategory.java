package net.pttheta.loveandwar.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.client.gui.GuiGraphics;

public class StampingAssemblySubCategory extends SequencedAssemblySubCategory {
   public StampingAssemblySubCategory() {
      super(20);
   }

   public void draw(SequencedRecipe<?> sequencedRecipe, GuiGraphics gg, double mouseX, double mouseY, int index) {
      PoseStack ms = gg.pose();
      ms.pushPose();
      ms.translate(0.0F, 51.5F, 0.0F);
      ms.scale(0.6F, 0.6F, 0.6F);
      ms.popPose();
   }
}
