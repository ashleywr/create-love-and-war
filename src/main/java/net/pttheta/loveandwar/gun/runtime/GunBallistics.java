package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.stat.ModifierContext;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.network.SShotTrail;
import net.pttheta.loveandwar.network.ModMessages;

public final class GunBallistics {
   private static final double HITSCAN_RANGE = 100.0;
   private static final float HEADSHOT_DAMAGE_MULTIPLIER = 2.0F;

   private GunBallistics() {
   }

   public static void fire(ServerPlayer shooter, BulletDef bullet, List<ModifierSource> sources) {
      var id = GunContent.bullets().entrySet().stream().filter(entry -> entry.getValue() == bullet).map(entry -> entry.getKey()).findFirst().orElseThrow();
      fire(shooter, ResourceLocation.fromNamespaceAndPath("createloveandwar", id), bullet, sources, "primary_fire", null, 0);
   }

   public static void fire(ServerPlayer shooter, ResourceLocation bulletId, BulletDef bullet, List<ModifierSource> sources,
                           String group, Entity lockTarget, int progressTicks) {
      ServerLevel level = shooter.serverLevel();
      var context = ModifierContext.ofGroups(List.of(group));
      float spread = StatResolver.resolve("spread", 0.0F, sources, context);
      float vRecoil = StatResolver.resolve("vertical_recoil", 0.0F, sources, context);
      float hRecoil = StatResolver.resolve("horizontal_recoil", 0.0F, sources, context);
      RandomSource random = shooter.getRandom();
      for (var variant : bullet.hitscans()) {
         int shotCount = Math.max(0, (int)Math.ceil(StatResolver.resolve("shot_count", variant.core().shotCount(), sources, context)));
         for (int i = 0; i < shotCount; i++) {
            Vec3 dir = spreadDirection(shooter.getViewVector(1.0F), spread, random);
            fireHitscan(level, shooter, dir, variant, bullet, sources);
         }
      }
      for (var variant : bullet.projectiles()) {
         int shotCount = Math.max(0, (int)Math.ceil(StatResolver.resolve("shot_count", variant.core().shotCount(), sources, context)));
         for (int i = 0; i < shotCount; i++) fireProjectile(level, shooter, spreadDirection(shooter.getViewVector(1), spread, random),
                 variant, bulletId, sources, group, lockTarget, progressTicks);
      }
      applyKick(shooter, vRecoil, hRecoil);
   }

   private static Vec3 spreadDirection(Vec3 look, float spread, RandomSource random) {
      if (spread <= 0.0F) {
         return look;
      }

      float bulletSpread = 1.25F * spread;
      double theta = random.nextFloat() * Math.PI * 2.0;
      double radius = random.nextFloat() * bulletSpread;
      double xComponent = radius * Math.sin(theta);
      double yComponent = radius * Math.cos(theta);
      float yawDegrees = (float)Math.atan(xComponent);
      float pitchDegrees = (float)Math.atan(yComponent);
      return look.yRot((float)Math.toRadians(-yawDegrees)).xRot((float)Math.toRadians(-pitchDegrees));
   }

   private static void fireHitscan(
      ServerLevel level, ServerPlayer shooter, Vec3 dir, BulletDef.ProjectileDef variant, BulletDef bullet, List<ModifierSource> sources
   ) {
      Vec3 eye = shooter.getEyePosition();
      Vec3 look = dir.normalize();
      Vec3 to = eye.add(look.scale(100.0));
      BlockHitResult blockHit = level.clip(new ClipContext(eye, to, Block.COLLIDER, Fluid.NONE, shooter));
      double blockDist = blockHit.getType() != Type.MISS ? eye.distanceTo(blockHit.getLocation()) : 100.0;
      Vec3 clippedTo = eye.add(look.scale(blockDist));
      AABB box = shooter.getBoundingBox().expandTowards(look.scale(blockDist)).inflate(1.0);
      List<Entity> candidates = level.getEntities(shooter, box, e -> canHit(shooter, e));
      List<EntityHitResult> hits = new ArrayList<>();

      for (Entity candidate : candidates) {
         Optional<Vec3> point = candidate.getBoundingBox().inflate(candidate.getPickRadius()).clip(eye, clippedTo);
         point.ifPresent(p -> hits.add(new EntityHitResult(candidate, p)));
      }

      hits.sort(Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(eye)));
      Vec3 trailEnd = !hits.isEmpty() ? hits.get(0).getLocation() : clippedTo;
      ModMessages.sendToNearbyPlayers(new SShotTrail(eye, trailEnd, shooter.getId()), level, eye.x, eye.y, eye.z, 200.0);
      int penetration = Math.max(1, Math.round(variant.core().penetrationPower()));
      int hitEntities = 0;

      for (EntityHitResult hit : hits) {
         if (hitEntities >= penetration) {
            break;
         }

         applyImpacts(level, shooter, variant.core().impacts(), hit, variant.core().splashRadius(), sources);
         hitEntities++;
      }

      if (hitEntities == 0 && blockHit.getType() != Type.MISS) {
         applyImpacts(level, shooter, variant.core().impacts(), blockHit, variant.core().splashRadius(), sources);
      }
   }

   private static boolean canHit(ServerPlayer shooter, Entity e) {
      return e != shooter && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity;
   }

   private static void fireProjectile(
      ServerLevel level, ServerPlayer shooter, Vec3 dir, BulletDef.ProjectileDef variant, ResourceLocation bulletId, List<ModifierSource> sources,
      String group, Entity lockTarget, int progressTicks
   ) {
      GunProjectile projectile = new GunProjectile(ModEntities.BULLET.get(), level);
      projectile.setPos(shooter.getEyePosition());
      projectile.initialize(shooter, bulletId, variant, sources, group, lockTarget, progressTicks);
      projectile.launch(dir);
      level.addFreshEntity(projectile);
   }

   private static boolean isHeadshot(Vec3 hitPoint, LivingEntity target) {
      return hitPoint.y >= target.getEyeY() - 0.25;
   }

   private static float resolveEntityDamage(List<BulletDef.ImpactDef> impacts, List<ModifierSource> sources) {
      for (BulletDef.ImpactDef impact : impacts) {
         if ("ShotEntity".equals(impact.targetType())) {
            for (BulletDef.EffectDef effect : impact.impactEffects()) {
               if ("ApplyDamage".equals(effect.effectType())) {
                  return StatResolver.resolve("impact_damage", 0.0F, StatSources.forImpact(sources, effect.modifiers()), ModifierContext.EMPTY);
               }
            }
         }
      }

      return 0.0F;
   }

   public static void applyImpacts(
      ServerLevel level, ServerPlayer shooter, List<BulletDef.ImpactDef> impacts, HitResult hit, float splashRadius, List<ModifierSource> sources
   ) {
      String targetType = switch (hit.getType()) {
         case ENTITY -> "ShotEntity";
         case BLOCK -> "ShotBlock";
         case MISS -> null;
      };

      for (BulletDef.ImpactDef impact : impacts) {
         if (impact.targetType().equals(targetType) || "ShotPosition".equals(impact.targetType())) {
            for (BulletDef.EffectDef effect : impact.impactEffects()) {
               applyEffect(level, shooter, effect, hit, splashRadius, sources);
            }
         }
      }
   }

   private static void applyEffect(
      ServerLevel level, ServerPlayer shooter, BulletDef.EffectDef effect, HitResult hit, float splashRadius, List<ModifierSource> sources
   ) {
      List<ModifierSource> withEffect = StatSources.forImpact(sources, effect.modifiers());
      switch (effect.effectType()) {
         case "ApplyDamage":
            applyDamage(level, shooter, withEffect, hit, splashRadius);
            break;
         case "Explode":
            applyExplode(level, shooter, withEffect, hit);
            break;
         case "ApplyMobEffect":
            applyMobEffect(withEffect, hit);
      }
   }

   private static void applyDamage(ServerLevel level, ServerPlayer shooter, List<ModifierSource> sources, HitResult hit, float splashRadius) {
      float damage = StatResolver.resolve("impact_damage", 0.0F, sources, ModifierContext.EMPTY);
      float knockback = StatResolver.resolve("knockback", 0.0F, sources, ModifierContext.EMPTY) / 25.0F;
      if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
         float multiplier = isHeadshot(hit.getLocation(), target) ? 2.0F : 1.0F;
         hurt(shooter, target, damage * multiplier, knockback);
      }

      if (splashRadius > 0.0F) {
         Vec3 center = hit.getLocation();

         for (Entity e : level.getEntities(
            shooter, new AABB(center, center).inflate(splashRadius), candidate -> candidate.isAlive() && candidate instanceof LivingEntity
         )) {
            if (!(hit instanceof EntityHitResult eh0 && e == eh0.getEntity())) {
               double dist = e.position().distanceTo(center);
               float falloff = (float)Math.max(0.0, 1.0 - dist / splashRadius);
               if (!(falloff <= 0.0F)) {
                  hurt(shooter, (LivingEntity)e, damage * falloff, knockback * falloff);
               }
            }
         }
      }
   }

   private static void hurt(ServerPlayer shooter, LivingEntity target, float damage, float knockback) {
      if (!(damage <= 0.0F)) {
         DamageSource source = shooter.damageSources().playerAttack(shooter);
         target.hurt(source, damage);
         target.hurtTime = 0;
         target.hurtDuration = 0;
         target.invulnerableTime = 0;
         if (knockback > 0.0F) {
            Vec3 delta = target.position().subtract(shooter.position());
            if (delta.lengthSqr() > 1.0E-4) {
               delta = delta.normalize();
               target.knockback(knockback, -delta.x, -delta.z);
            }
         }
      }
   }

   private static void applyExplode(ServerLevel level, ServerPlayer shooter, List<ModifierSource> sources, HitResult hit) {
      float radius = StatResolver.resolve("explosion_radius", 0.0F, sources, ModifierContext.EMPTY);
      if (!(radius <= 0.0F)) {
         Vec3 pos = hit.getLocation();
         level.explode(shooter, pos.x, pos.y, pos.z, radius, ExplosionInteraction.MOB);
      }
   }

   private static void applyMobEffect(List<ModifierSource> sources, HitResult hit) {
      if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
         String id = StatResolver.resolveOverride("mob_effect_id", "", sources, ModifierContext.EMPTY);
         if (!id.isEmpty()) {
            ResourceLocation rl = id.indexOf(58) >= 0 ? ResourceLocation.parse(id) : ResourceLocation.withDefaultNamespace(id);
            BuiltInRegistries.MOB_EFFECT.getHolder(rl).ifPresent(holder -> {
               int durationTicks = Math.max(1, Math.round(StatResolver.resolve("duration", 100.0F, sources, ModifierContext.EMPTY)));
               target.addEffect(new MobEffectInstance(holder, durationTicks, 0));
            });
         }
      }
   }

   private static void applyKick(ServerPlayer shooter, float vertical, float horizontal) {
      float total = vertical + horizontal;
      if (!(total <= 0.0F)) {
         Vec3 back = shooter.getLookAngle().scale(-total / 2000.0);
         shooter.push(back.x, Math.max(0.0, back.y * 0.2), back.z);
      }
   }
}
