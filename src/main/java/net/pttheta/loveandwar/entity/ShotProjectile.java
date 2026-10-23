package net.pttheta.loveandwar.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ShotProjectile extends AbstractHurtingProjectile {
   private int lifeTime = 80;
   public float damage = 5.0F;
   private int ticker = 0;
   public Entity owner;

   protected ShotProjectile(EntityType<? extends AbstractHurtingProjectile> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.damage = 5.0F;
   }

   protected ShotProjectile(Level pLevel) {
      super((EntityType)ModEntities.SHOT_PROJECTILE.get(), pLevel);
   }

   public ShotProjectile(Level pLevel, LivingEntity pShooter, double pOffsetX, double pOffsetY, double pOffsetZ) {
      super((EntityType)ModEntities.SHOT_PROJECTILE.get(), pShooter, new Vec3(pOffsetX, pOffsetY, pOffsetZ), pLevel);
   }

   public ShotProjectile(Level pLevel, double pX, double pY, double pZ, double pOffsetX, double pOffsetY, double pOffsetZ) {
      super((EntityType)ModEntities.SHOT_PROJECTILE.get(), pX, pY, pZ, new Vec3(pOffsetX, pOffsetY, pOffsetZ), pLevel);
      this.lifeTime = 80;
      Vec3 vec3 = new Vec3(pOffsetX, pOffsetY, pOffsetZ);
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double d0 = vec3.horizontalDistance();
         this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0 / (float) Math.PI));
         this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0 / (float) Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   public boolean isOnFire() {
      return false;
   }

   protected float getInertia() {
      return 1.0F;
   }

   protected ParticleOptions getTrailParticle() {
      return ParticleTypes.CRIT;
   }

   public void tick() {
      super.tick();
      Vec3 vec3 = this.getDeltaMovement();
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double d0 = vec3.horizontalDistance();
         this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0 / (float) Math.PI));
         this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0 / (float) Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }

      this.lifeTime--;
      if (this.lifeTime <= 0) {
         this.discard();
      }

      vec3 = this.getDeltaMovement();
      double d5 = vec3.x;
      double d6 = vec3.y;
      double d1 = vec3.z;
      double d7 = this.getX() + d5;
      double d2 = this.getY() + d6;
      double d3 = this.getZ() + d1;
      double d4 = vec3.horizontalDistance();
      if (this.noPhysics) {
         this.setYRot((float)(Mth.atan2(-d5, -d1) * 180.0 / (float) Math.PI));
      } else {
         this.setYRot((float)(Mth.atan2(d5, d1) * 180.0 / (float) Math.PI));
      }

      this.setXRot((float)(Mth.atan2(d6, d4) * 180.0 / (float) Math.PI));
      this.setXRot(lerpRotation(this.getXRot(), this.getXRot()));
      this.setYRot(lerpRotation(this.getYRot(), this.getYRot()));
      float f = 0.99F;
      float f1 = 0.05F;
   }

   protected void onHitBlock(BlockHitResult pResult) {
      super.onHitBlock(pResult);
      this.discard();
   }

   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level().isClientSide) {
         Entity entity = pResult.getEntity();
         if (entity == this.owner) {
            return;
         }

         if (entity instanceof OwnableEntity && ((OwnableEntity)entity).getOwner() == this.owner) {
            return;
         }

         entity.hurt(this.damageSources().magic(), this.damage);
         this.discard();
      }
   }
}
