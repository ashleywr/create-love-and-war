package net.pttheta.loveandwar.blocks.drawing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.pttheta.loveandwar.blocks.LAWPartialModels;

public class DrawingPressRenderer extends KineticBlockEntityRenderer<DrawingPressBlockEntity> {
   public DrawingPressRenderer(Context context) {
      super(context);
   }

   public boolean shouldRenderOffScreen(DrawingPressBlockEntity be) {
      return true;
   }

   protected void renderSafe(DrawingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState blockState = be.getBlockState();
         DrawingBehaviour drawingBehaviour = be.getDrawingBehaviour();
         float renderedHeadOffset = drawingBehaviour.getRenderedHeadOffset(partialTicks) * drawingBehaviour.mode.headOffset;
         SuperByteBuffer headRender = CachedBuffers.partialFacing(
            LAWPartialModels.DRAWING_PRESS_HEAD, blockState, (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
         );
         ((SuperByteBuffer)headRender.translate(0.0F, -renderedHeadOffset, 0.0F)).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   protected BlockState getRenderedBlockState(DrawingPressBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
