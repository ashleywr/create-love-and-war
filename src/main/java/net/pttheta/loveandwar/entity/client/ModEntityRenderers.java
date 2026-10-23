package net.pttheta.loveandwar.entity.client;

import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.pttheta.loveandwar.entity.ModEntities;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public class ModEntityRenderers {
   @SubscribeEvent
   static void registerLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(ModModelLayers.ROBOT_DOG_LAYER, RobotdogModel::createBodyLayer);
   }

   @SubscribeEvent
   static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.ROBOT_DOG.get(), RobotdogRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SHOT_PROJECTILE.get(), ShotRenderer::new);
   }
}
