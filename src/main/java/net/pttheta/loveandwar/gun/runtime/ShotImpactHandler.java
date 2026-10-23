package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.pttheta.loveandwar.entity.ShotProjectile;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;

@EventBusSubscriber(modid = "createloveandwar")
public final class ShotImpactHandler {
   private static final Map<Integer, ShotImpactHandler.PendingShot> PENDING = new ConcurrentHashMap<>();

   private ShotImpactHandler() {
   }

   static void track(ShotProjectile projectile, ShotImpactHandler.PendingShot shot) {
      PENDING.put(projectile.getId(), shot);
   }

   public static void forget(ShotProjectile projectile) {
      PENDING.remove(projectile.getId());
   }

   @SubscribeEvent
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getProjectile() instanceof ShotProjectile projectile) {
         ShotImpactHandler.PendingShot shot = PENDING.remove(projectile.getId());
         if (shot != null) {
            if (projectile.level() instanceof ServerLevel level) {
               if (projectile.owner instanceof ServerPlayer shooter) {
                  event.setCanceled(true);
                  HitResult hit = event.getRayTraceResult();
                  GunBallistics.applyImpacts(level, shooter, shot.impacts(), hit, shot.splashRadius(), shot.sources());
                  projectile.discard();
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLeaveLevel(EntityLeaveLevelEvent event) {
      if (event.getEntity() instanceof ShotProjectile projectile) {
         forget(projectile);
      }
   }

   public record PendingShot(List<BulletDef.ImpactDef> impacts, float splashRadius, List<ModifierSource> sources) {
   }
}
