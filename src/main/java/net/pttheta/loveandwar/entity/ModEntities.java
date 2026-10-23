package net.pttheta.loveandwar.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "createloveandwar");
   public static final DeferredRegister<EntityType<?>> FLANS_ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "flansmod");
   public static final DeferredHolder<EntityType<?>, EntityType<GunProjectile>> BULLET = FLANS_ENTITY_TYPES.register(
      "bullet", () -> Builder.of(GunProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(256).build("flansmod:bullet")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RobotdogEntity>> ROBOT_DOG = ENTITY_TYPES.register(
      "robotdog", () -> Builder.of(RobotdogEntity::new, MobCategory.CREATURE).sized(1.0F, 1.0F).build("robotdog")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ShotProjectile>> SHOT_PROJECTILE = ENTITY_TYPES.register(
      "shot_projectile", () -> Builder.<ShotProjectile>of(ShotProjectile::new, MobCategory.MISC).sized(0.25F, 0.25F).build("shot_projectile")
   );

   public static void register(IEventBus eventBus) {
      ENTITY_TYPES.register(eventBus);
      FLANS_ENTITY_TYPES.register(eventBus);
   }
}
