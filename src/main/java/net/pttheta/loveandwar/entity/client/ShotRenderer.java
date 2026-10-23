package net.pttheta.loveandwar.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.entity.ShotProjectile;

public class ShotRenderer<T extends ShotProjectile> extends EntityRenderer<T> {
   public ShotRenderer(Context pContext) {
      super(pContext);
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
      pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot())));
      float f9 = 0.0F;
      if (f9 > 0.0F) {
         float f10 = -Mth.sin(f9 * 3.0F) * f9;
         pPoseStack.mulPose(Axis.ZP.rotationDegrees(f10));
      }

      pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
      pPoseStack.scale(0.05625F, 0.05625F, 0.05625F);
      pPoseStack.translate(-4.0F, 0.0F, 0.0F);
      VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entityTranslucentEmissive(this.getTextureLocation(pEntity)));
      Pose posestack$pose = pPoseStack.last();
      this.vertex(posestack$pose, vertexconsumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, pPackedLight);
      this.vertex(posestack$pose, vertexconsumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, pPackedLight);

      for (int j = 0; j < 4; j++) {
         pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         this.vertex(posestack$pose, vertexconsumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, pPackedLight);
         this.vertex(posestack$pose, vertexconsumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, pPackedLight);
         this.vertex(posestack$pose, vertexconsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, pPackedLight);
         this.vertex(posestack$pose, vertexconsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, pPackedLight);
      }

      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   public void vertex(
      Pose pPose, VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV, int pNormalX, int pNormalZ, int pNormalY, int pPackedLight
   ) {
      pConsumer.addVertex(pPose, pX, pY, pZ)
         .setColor(-1)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(pPackedLight)
         .setNormal(pPose, pNormalX, pNormalZ, pNormalY);
   }

   public ResourceLocation getTextureLocation(T pEntity) {
      return LoveAndWarMod.asResource("textures/entity/shot.png");
   }
}
