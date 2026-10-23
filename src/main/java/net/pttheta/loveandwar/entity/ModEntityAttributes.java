package net.pttheta.loveandwar.entity;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;

@EventBusSubscriber(modid = "createloveandwar")
public class ModEntityAttributes {
   @SubscribeEvent
   static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
      event.put((EntityType)ModEntities.ROBOT_DOG.get(), RobotdogEntity.createAttributes().build());
   }
}
