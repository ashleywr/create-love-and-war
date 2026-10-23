package net.pttheta.loveandwar.gun.client.anim;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.ActionDef;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

final class GunPoseResolver {
   private GunPoseResolver() {
   }

   static Map<String, PartPose> currentPose(ItemStack stack, float partialTick) {
      return currentPose(stack, partialTick, false);
   }

   static Map<String, PartPose> currentPose(ItemStack stack, float partialTick, boolean localPlayer) {
      Optional<GunDef> defOpt = GunLookup.gunDef(stack);
      if (defOpt.isEmpty()) {
         return Map.of();
      }

      GunDef def = defOpt.get();
      Optional<ResolvedAnimationSet> animOpt = FlanimationLoader.get(def.animationSet());
      if (animOpt.isEmpty()) {
         return Map.of();
      }

      ResolvedAnimationSet anim = animOpt.get();
      GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
      boolean serverIdle = false;
      float carryRaw = -1.0F;
      String groupKey;
      float rawT;
      if (localPlayer) {
         LocalGunAnim.Playing playing = LocalGunAnim.resolve(GunLookup.contentKey(stack), state);
         if (playing == null) {
            return anim.pose("idle");
         }

         groupKey = playing.groupKey();
         rawT = playing.ticks();
         serverIdle = playing.serverIdle();
         carryRaw = playing.carryTicks();
      } else {
         if (state.activeGroup().isEmpty()) {
            return anim.pose("idle");
         }

         groupKey = state.activeGroup();
         rawT = state.groupTicksElapsed() + partialTick;
      }

      var currentGroup = def.actionGroups().stream().filter(g -> g.key().equals(groupKey)).findFirst();
      if (!localPlayer && def.reloads().stream().anyMatch(reload -> reload.loadOneActionKey().equals(groupKey)) && currentGroup.isPresent()) {
         rawT = state.reloadAnimationElapsed(currentGroup.get().durationTicks()) + partialTick;
      }
      Optional<ActionDef> animAction = currentGroup.flatMap(GunPoseResolver::animationOf);
      Optional<String> seqName = animAction.map(ActionDef::anim);
      Optional<Flanimation.Sequence> seqOpt = seqName.flatMap(anim::sequence);
      float rawSpeed = 1.0F;
      if (seqOpt.isPresent() && animAction.get().duration() > 0.0F) {
         rawSpeed = seqOpt.get().ticks() / (animAction.get().duration() * 20.0F);
      }

      float scaledT = rawT * rawSpeed;
      if (!seqOpt.isEmpty() && (!serverIdle || !(scaledT >= seqOpt.get().ticks()))) {
         Flanimation.Sequence seq = seqOpt.get();
         boolean shoot = "shoot".equals(seq.name());
         if (shoot && carryRaw >= 0.0F && seq.frames().size() >= 2) {
            float peak = Math.max(0.001F, seq.frames().get(1).tick());
            float prev = carryRaw * rawSpeed;
            float level = prev <= peak ? prev / peak : Math.max(0.0F, 1.0F - (prev - peak) / Math.max(0.001F, seq.ticks() - peak));
            scaledT += level * peak;
         }

         float t = Math.min(scaledT, seq.ticks());
         boolean smoothKick = "shoot".equals(seq.name()) && Config.SMOOTH_RECOIL_KICK != null && (Boolean)Config.SMOOTH_RECOIL_KICK.get();
         Map<String, PartPose> resolved = anim.poseAtTime(seq, t, smoothKick || shoot && isAutomatic(def, groupKey));
         if (shoot && isAutomatic(def, groupKey) && Config.AUTO_RECOIL != null) {
            float k = ((Double)Config.AUTO_RECOIL.get()).floatValue();
            if (k < 0.999F) {
               Map<String, PartPose> idle = anim.pose("idle");
               Map<String, PartPose> scaled = new LinkedHashMap<>();
               Set<String> parts = new LinkedHashSet<>(resolved.keySet());
               parts.addAll(idle.keySet());

               for (String part : parts) {
                  scaled.put(part, PartPose.lerp(idle.getOrDefault(part, PartPose.IDENTITY), resolved.getOrDefault(part, PartPose.IDENTITY), k));
               }

               resolved = scaled;
            }
         }

         if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
            PartPose body = resolved.get("body");
            LoggerFactory.getLogger("createloveandwar/pose")
               .info(
                  "POSE seq={} t={} ticks={} parts={} body={}",
                  new Object[]{seqName.get(), String.format("%.2f", t), seq.ticks(), resolved.keySet(), body == null ? "none" : body}
               );
         }

         return resolved;
      } else {
         if (serverIdle) {
            LocalGunAnim.clear();
         }

         return anim.pose("idle");
      }
   }

   private static boolean isAutomatic(GunDef def, String groupKey) {
      return def.actionGroups()
         .stream()
         .filter(g -> g.key().equals(groupKey))
         .findFirst()
         .map(g -> "FullAuto".equals(g.repeatMode()) || "BurstFire".equals(g.repeatMode()))
         .orElse(false);
   }

   private static Optional<ActionDef> animationOf(ActionGroup group) {
      return group.actions().stream().filter(a -> "Animation".equals(a.actionType()) && !a.anim().isEmpty()).findFirst();
   }
}
