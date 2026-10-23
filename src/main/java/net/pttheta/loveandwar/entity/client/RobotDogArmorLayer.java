package net.pttheta.loveandwar.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;

public class RobotDogArmorLayer extends RenderLayer<RobotdogEntity, RobotdogModel<RobotdogEntity>> {
   private static final ResourceLocation NONE = null;
   private static final ResourceLocation ARMOUR_DEFAULT = LoveAndWarMod.asResource("textures/entity/robotdog_armor.png");
   private static final ResourceLocation ARMOUR_VALKYRIE = LoveAndWarMod.asResource("textures/entity/robotdog_armor_valk.png");
   private static final ResourceLocation ARMOUR_BUBBLEGUM = LoveAndWarMod.asResource("textures/entity/robotdog_armor_bubblegum.png");

   public RobotDogArmorLayer(RenderLayerParent<RobotdogEntity, RobotdogModel<RobotdogEntity>> pRenderer) {
      super(pRenderer);
   }

   public ResourceLocation getArmour(RobotdogEntity e) {
      if (e.hasArmor()) {
         if (e.getPaintjob() == 16) {
            return ARMOUR_VALKYRIE;
         } else {
            return e.getPaintjob() == 17 ? ARMOUR_BUBBLEGUM : ARMOUR_DEFAULT;
         }
      } else {
         return NONE;
      }
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      RobotdogEntity pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTick,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      ResourceLocation resourcelocation = this.getArmour(pLivingEntity);
      if (resourcelocation != null && !pLivingEntity.isInvisible()) {
         VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entityTranslucent(resourcelocation));
         ((RobotdogModel)this.getParentModel())
            .renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F));
      }
   }
}
