package net.pttheta.loveandwar.compat.jei.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.blocks.LAWPartialModels;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnBlock;

public class AnimatedFractionalColumn extends AnimatedKinetics {
   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      this.draw(graphics, xOffset, yOffset, 3);
   }

   public void draw(GuiGraphics graphics, int xOffset, int yOffset, int height) {
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(xOffset, yOffset, 201.0F);
      matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
      matrixStack.mulPose(Axis.YP.rotationDegrees(22.5F));
      int scale = 23;
      this.blockElement(LAWBlocks.DISTILLATION_COLUMN.getDefaultState()).atLocal(0.0, 1.0, 0.0).scale(scale).render(graphics);

      for (int i = 0; i < height - 1; i++) {
         this.blockElement(
               (BlockState)((BlockState)LAWBlocks.DISTILLATION_COLUMN.getDefaultState().setValue(DistillationColumnBlock.BOTTOM, false))
                  .setValue(DistillationColumnBlock.TOP, false)
            )
            .atLocal(0.0, -i, 0.0)
            .scale(scale)
            .render(graphics);
      }

      this.blockElement((BlockState)LAWBlocks.DISTILLATION_COLUMN.getDefaultState().setValue(DistillationColumnBlock.BOTTOM, false))
         .atLocal(0.0, -height + 1, 0.0)
         .scale(scale)
         .render(graphics);
      this.blockElement(LAWPartialModels.DISTILLATION_GAUGE).atLocal(1.0, 1.0, 0.125).rotate(0.0, -90.0, 0.0).scale(scale).render(graphics);
      this.blockElement(LAWPartialModels.DISTILLATION_GAUGE_DIAL)
         .atLocal(0.625, 0.65, 1.125)
         .scale(scale)
         .rotate(0.0, -90.0, getCurrentAngle() / 4.0F - 90.0F)
         .render(graphics);
      this.blockElement(LAWPartialModels.DISTILLATION_GAUGE).atLocal(0.875, 1.0, 1.0).rotate(0.0, 180.0, 0.0).scale(scale).render(graphics);
      this.blockElement(LAWPartialModels.DISTILLATION_GAUGE_DIAL)
         .atLocal(-0.125, 0.65, 0.625)
         .scale(scale)
         .rotate(-getCurrentAngle() / 4.0F + 90.0F, 180.0, 0.0)
         .render(graphics);
      matrixStack.popPose();
   }
}
