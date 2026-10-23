package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record BulletDef(
   ItemSettings itemSettings,
   int roundsPerItem,
   List<BulletDef.ProjectileDef> hitscans,
   List<BulletDef.ProjectileDef> projectiles,
   List<JsonElement> triggers,
   List<ActionGroup> actionGroups
) {
   public static final Codec<BulletDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ItemSettings.CODEC.optionalFieldOf("itemSettings", ItemSettings.DEFAULT).forGetter(BulletDef::itemSettings),
            Codec.INT.optionalFieldOf("roundsPerItem", 1).forGetter(BulletDef::roundsPerItem),
            BulletDef.ProjectileDef.CODEC.listOf().optionalFieldOf("hitscans", List.of()).forGetter(BulletDef::hitscans),
            BulletDef.ProjectileDef.CODEC.listOf().optionalFieldOf("projectiles", List.of()).forGetter(BulletDef::projectiles),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("triggers", List.of()).forGetter(BulletDef::triggers),
            ActionGroup.CODEC.listOf().optionalFieldOf("actionGroups", List.of()).forGetter(BulletDef::actionGroups)
         )
         .apply(instance, BulletDef::new)
   );

   public record EffectDef(String effectType, List<Modifier> modifiers) {
      public static final Codec<BulletDef.EffectDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("effectType", "").forGetter(BulletDef.EffectDef::effectType),
               Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(BulletDef.EffectDef::modifiers)
            )
            .apply(instance, BulletDef.EffectDef::new)
      );
   }

   public record ImpactDef(String targetType, List<BulletDef.EffectDef> impactEffects) {
      public static final Codec<BulletDef.ImpactDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("targetType", "ShotEntity").forGetter(BulletDef.ImpactDef::targetType),
               BulletDef.EffectDef.CODEC.listOf().optionalFieldOf("impactEffects", List.of()).forGetter(BulletDef.ImpactDef::impactEffects)
            )
            .apply(instance, BulletDef.ImpactDef::new)
      );
   }

   public record ParticleTrailDef(String particle, float speed, int fromTick, int toTick, List<String> spawnPoints,
                                  boolean spawnUnderwater, boolean onlySpawnUnderwater) {
      public static final Codec<BulletDef.ParticleTrailDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("particle", "").forGetter(BulletDef.ParticleTrailDef::particle),
               Codec.FLOAT.optionalFieldOf("speed", 0.0F).forGetter(BulletDef.ParticleTrailDef::speed),
               Codec.INT.optionalFieldOf("fromTick", 0).forGetter(BulletDef.ParticleTrailDef::fromTick),
               Codec.INT.optionalFieldOf("toTick", Integer.MAX_VALUE).forGetter(BulletDef.ParticleTrailDef::toTick),
               Codec.STRING.listOf().optionalFieldOf("spawnPoints", List.of()).forGetter(BulletDef.ParticleTrailDef::spawnPoints),
               Codec.BOOL.optionalFieldOf("spawnUnderwater", false).forGetter(BulletDef.ParticleTrailDef::spawnUnderwater),
               Codec.BOOL.optionalFieldOf("onlySpawnUnderwater", false).forGetter(BulletDef.ParticleTrailDef::onlySpawnUnderwater)
            )
            .apply(instance, BulletDef.ParticleTrailDef::new)
      );
   }

   public record ProjectileDef(
      BulletDef.ProjectileDef.Core core,
      BulletDef.ProjectileDef.Motion motion,
      BulletDef.ProjectileDef.Particles particles,
      BulletDef.ProjectileDef.Behavior behavior
   ) {
      public static final Codec<BulletDef.ProjectileDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               RecordCodecBuilder.of(BulletDef.ProjectileDef::core, BulletDef.ProjectileDef.Core.MAP_CODEC),
               RecordCodecBuilder.of(BulletDef.ProjectileDef::motion, BulletDef.ProjectileDef.Motion.MAP_CODEC),
               RecordCodecBuilder.of(BulletDef.ProjectileDef::particles, BulletDef.ProjectileDef.Particles.MAP_CODEC),
               RecordCodecBuilder.of(BulletDef.ProjectileDef::behavior, BulletDef.ProjectileDef.Behavior.MAP_CODEC)
            )
            .apply(instance, BulletDef.ProjectileDef::new)
      );

      public record Behavior(boolean sticky, float fuseTime, String responseToBlock, String responseToEntity, String responseToVehicle) {
         public static final MapCodec<BulletDef.ProjectileDef.Behavior> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                  Codec.BOOL.optionalFieldOf("sticky", false).forGetter(BulletDef.ProjectileDef.Behavior::sticky),
                  Codec.FLOAT.optionalFieldOf("fuseTime", 0.0F).forGetter(BulletDef.ProjectileDef.Behavior::fuseTime),
                  Codec.STRING.optionalFieldOf("responseToBlock", "Bounce").forGetter(BulletDef.ProjectileDef.Behavior::responseToBlock),
                  Codec.STRING.optionalFieldOf("responseToEntity", "Detonate").forGetter(BulletDef.ProjectileDef.Behavior::responseToEntity),
                  Codec.STRING.optionalFieldOf("responseToVehicle", "Detonate").forGetter(BulletDef.ProjectileDef.Behavior::responseToVehicle)
               )
               .apply(instance, BulletDef.ProjectileDef.Behavior::new)
         );
      }

      public record Core(int shotCount, float splashRadius, List<BulletDef.ImpactDef> impacts, float penetrationPower) {
         public static final MapCodec<BulletDef.ProjectileDef.Core> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                  Codec.INT.optionalFieldOf("shotCount", 1).forGetter(BulletDef.ProjectileDef.Core::shotCount),
                  Codec.FLOAT.optionalFieldOf("splashRadius", 0.0F).forGetter(BulletDef.ProjectileDef.Core::splashRadius),
                  BulletDef.ImpactDef.CODEC.listOf().optionalFieldOf("impacts", List.of()).forGetter(BulletDef.ProjectileDef.Core::impacts),
                  Codec.FLOAT.optionalFieldOf("penetrationPower", 0.0F).forGetter(BulletDef.ProjectileDef.Core::penetrationPower)
               )
               .apply(instance, BulletDef.ProjectileDef.Core::new)
         );
      }

      public record Motion(
         String guidanceType, float launchSpeed, float gravityFactor, float turnRate, float dragInAir, float dragInWater, float timeBetweenTrailParticles,
         float acceleration, float maxSpeed, float lockRange, float lockCone, float trackCone, float lockTime,
         float topAttackHeight, float topAttackRange, List<String> targetTypes
      ) {
         public static final MapCodec<BulletDef.ProjectileDef.Motion> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                  Codec.STRING.optionalFieldOf("guidanceType", "none").forGetter(BulletDef.ProjectileDef.Motion::guidanceType),
                  Codec.FLOAT.optionalFieldOf("launchSpeed", 3.0F).forGetter(BulletDef.ProjectileDef.Motion::launchSpeed),
                  Codec.FLOAT.optionalFieldOf("gravityFactor", 1.0F).forGetter(BulletDef.ProjectileDef.Motion::gravityFactor),
                  Codec.FLOAT.optionalFieldOf("turnRate", 0.1F).forGetter(BulletDef.ProjectileDef.Motion::turnRate),
                  Codec.FLOAT.optionalFieldOf("dragInAir", 0.01F).forGetter(BulletDef.ProjectileDef.Motion::dragInAir),
                  Codec.FLOAT.optionalFieldOf("dragInWater", 0.2F).forGetter(BulletDef.ProjectileDef.Motion::dragInWater),
                  Codec.FLOAT.optionalFieldOf("timeBetweenTrailParticles", 0.25F).forGetter(BulletDef.ProjectileDef.Motion::timeBetweenTrailParticles),
                  Codec.FLOAT.optionalFieldOf("acceleration", 0.0F).forGetter(BulletDef.ProjectileDef.Motion::acceleration),
                  Codec.FLOAT.optionalFieldOf("maxSpeed", 3.0F).forGetter(BulletDef.ProjectileDef.Motion::maxSpeed),
                  Codec.FLOAT.optionalFieldOf("lockRange", 128.0F).forGetter(BulletDef.ProjectileDef.Motion::lockRange),
                  Codec.FLOAT.optionalFieldOf("lockCone", 15.0F).forGetter(BulletDef.ProjectileDef.Motion::lockCone),
                  Codec.FLOAT.optionalFieldOf("trackCone", 70.0F).forGetter(BulletDef.ProjectileDef.Motion::trackCone),
                  Codec.FLOAT.optionalFieldOf("lockTime", 20.0F).forGetter(BulletDef.ProjectileDef.Motion::lockTime),
                  Codec.FLOAT.optionalFieldOf("topAttackHeight", 10.0F).forGetter(BulletDef.ProjectileDef.Motion::topAttackHeight),
                  Codec.FLOAT.optionalFieldOf("topAttackRange", 10.0F).forGetter(BulletDef.ProjectileDef.Motion::topAttackRange),
                  Codec.STRING.listOf().optionalFieldOf("targetTypes", List.of()).forGetter(BulletDef.ProjectileDef.Motion::targetTypes)
               )
               .apply(instance, BulletDef.ProjectileDef.Motion::new)
         );
      }

      public record Particles(List<BulletDef.ParticleTrailDef> particleTrails, String airParticles, String waterParticles) {
         public static final MapCodec<BulletDef.ProjectileDef.Particles> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                  BulletDef.ParticleTrailDef.CODEC
                     .listOf()
                     .optionalFieldOf("particleTrails", List.of())
                     .forGetter(BulletDef.ProjectileDef.Particles::particleTrails),
                  Codec.STRING.optionalFieldOf("airParticles", "").forGetter(BulletDef.ProjectileDef.Particles::airParticles),
                  Codec.STRING.optionalFieldOf("waterParticles", "").forGetter(BulletDef.ProjectileDef.Particles::waterParticles)
               )
               .apply(instance, BulletDef.ProjectileDef.Particles::new)
         );
      }
   }
}
