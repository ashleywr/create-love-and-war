package net.pttheta.loveandwar.entity.ai;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.pttheta.loveandwar.entity.ShotProjectile;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;
import net.pttheta.loveandwar.sound.ModSounds;

public class RobotDogAttackGoal extends MeleeAttackGoal {
   private final RobotdogEntity entity;
   private int attackDelay = 4;
   private int ticksUntilNextAttack = 12;
   private boolean shouldCountTillNextAttack = false;
   private long lastCanUseCheck;
   private Path path;
   private final double speedModifier;

   public RobotDogAttackGoal(PathfinderMob pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
      super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
      this.entity = (RobotdogEntity)pMob;
      this.speedModifier = pSpeedModifier;
   }

   public boolean canUse() {
      long i = this.mob.level().getGameTime();
      if (i - this.lastCanUseCheck < 20L) {
         return false;
      } else {
         this.lastCanUseCheck = i;
         LivingEntity livingentity = this.mob.getTarget();
         if (livingentity == null) {
            return false;
         } else if (!livingentity.isAlive()) {
            return false;
         } else {
            this.path = this.mob.getNavigation().createPath(livingentity, this.entity.hasTurret() ? 10 : 0);
            if (this.path != null) {
               return true;
            } else if (this.entity.hasTurret()) {
               this.path = null;
               return true;
            } else {
               return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            }
         }
      }
   }

   public void start() {
      super.start();
      this.mob.getNavigation().moveTo(this.path, this.speedModifier);
      this.mob.setAggressive(true);
      this.ticksUntilNextAttack = 0;
      this.attackDelay = 4;
      this.ticksUntilNextAttack = 12;
   }

   protected void checkAndPerformAttack(LivingEntity pEnemy) {
      double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy.getX(), pEnemy.getY(), pEnemy.getZ());
      if (this.entity.hasTurret() && this.entity.hasTurretAmmo()) {
         this.projectileAttack(pEnemy, (float)pDistToEnemySqr);
      } else if (this.isEnemyWithinAttackDistance(pEnemy, pDistToEnemySqr)) {
         this.shouldCountTillNextAttack = true;
         if (this.isTimeToStartAttackAnimation()) {
            this.entity.setAttacking(true);
         }

         if (this.isTimeToAttack()) {
            this.mob.getLookControl().setLookAt(pEnemy.getX(), pEnemy.getEyeY(), pEnemy.getZ());
            this.performAttack(pEnemy);
         }
      } else {
         this.resetAttackCooldown();
         this.shouldCountTillNextAttack = false;
         this.entity.setAttacking(false);
         this.entity.attackAnimationTimeout = 0;
      }
   }

   public void projectileAttack(LivingEntity pTarget, float pDistanceFactor) {
      if (this.entity.projectileAttackTick <= 0) {
         this.performRangedAttack(pTarget, pDistanceFactor);
         this.entity.projectileAttackTick = 10;
      }
   }

   public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
      double d4 = pTarget.getX() - this.entity.getX();
      double d5 = pTarget.getY(0.5) - this.entity.getY(0.5);
      double d6 = pTarget.getZ() - this.entity.getZ();
      ShotProjectile smallfireball = new ShotProjectile(this.entity.level(), this.entity, 0.0, 0.0, 0.0);
      smallfireball.setPos(smallfireball.getX(), this.entity.getY(1.0) + 0.25, smallfireball.getZ());
      smallfireball.shoot(d4, d5, d6, 5.0F, 0.0F);
      smallfireball.owner = this.entity.getOwner();
      this.entity.level().addFreshEntity(smallfireball);
      this.playSound((SoundEvent)ModSounds.GUNSHOT.get(), 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
      this.entity.ConsumeAmmo();
   }

   public RandomSource getRandom() {
      return this.entity.getRandom();
   }

   public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
      this.entity
         .level()
         .playSound((Player)null, this.entity.getX(), this.entity.getY(), this.entity.getZ(), pSound, this.entity.getSoundSource(), pVolume, pPitch);
   }

   private boolean isEnemyWithinAttackDistance(LivingEntity pEnemy, double pDistToEnemySqr) {
      return pDistToEnemySqr <= this.getAttackReachSqr(pEnemy);
   }

   protected void resetAttackCooldown() {
      this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackDelay * 2);
   }

   protected boolean isTimeToAttack() {
      return this.ticksUntilNextAttack <= 0;
   }

   protected boolean isTimeToStartAttackAnimation() {
      return this.ticksUntilNextAttack <= this.attackDelay;
   }

   protected int getTicksUntilNextAttack() {
      return this.ticksUntilNextAttack;
   }

   protected void performAttack(LivingEntity pEnemy) {
      this.resetAttackCooldown();
      this.mob.swing(InteractionHand.MAIN_HAND);
      this.mob.doHurtTarget(pEnemy);
   }

   public void tick() {
      super.tick();
      if (this.shouldCountTillNextAttack) {
         this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
      }
   }

   public void stop() {
      this.entity.setAttacking(false);
      super.stop();
   }

   protected double getAttackReachSqr(LivingEntity pAttackTarget) {
      return this.entity.hasTurret() ? 10.0 : this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pAttackTarget.getBbWidth();
   }
}
