package net.pttheta.loveandwar.blocks.stamping;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.pttheta.loveandwar.blocks.LAWPartialModels;

public class StampingPressRenderer extends KineticBlockEntityRenderer<StampingPressBlockEntity> {
   public StampingPressRenderer(Context context) {
      super(context);
   }

   public boolean shouldRenderOffScreen(StampingPressBlockEntity be) {
      return true;
   }

   protected void renderSafe(StampingPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      ItemStack itemStack = be.getRenderStack();
      ms.pushPose();
      ms.translate(0.5F, -be.getStampingBehaviour().getRenderedHeadOffset(partialTicks) * be.getStampingBehaviour().mode.headOffset, 0.5F);
      ms.scale(0.5F, 0.5F, 0.5F);
      ms.mulPose(Axis.XP.rotationDegrees(270.0F));
      itemRenderer.renderStatic(
         itemStack, ItemDisplayContext.FIXED, this.getLightLevel(be.getLevel(), be.getBlockPos()), OverlayTexture.NO_OVERLAY, ms, buffer, be.getLevel(), 1
      );
      ms.popPose();
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState blockState = be.getBlockState();
         StampingBehaviour drawingBehaviour = be.getStampingBehaviour();
         float renderedHeadOffset = drawingBehaviour.getRenderedHeadOffset(partialTicks) * drawingBehaviour.mode.headOffset;
         SuperByteBuffer headRender = CachedBuffers.partialFacing(
            LAWPartialModels.STAMPING_PRESS_HEAD, blockState, (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
         );
         ((SuperByteBuffer)headRender.translate(0.0F, -renderedHeadOffset, 0.0F)).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   protected BlockState getRenderedBlockState(StampingPressBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }

   private int getLightLevel(Level level, BlockPos pos) {
      int bLight = level.getBrightness(LightLayer.BLOCK, pos);
      int sLight = level.getBrightness(LightLayer.SKY, pos);
      return LightTexture.pack(bLight, sLight);
   }
}
