package net.pttheta.loveandwar.gun.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunAimZoom {
   private static float lastFactor = 1.0F;

   private GunAimZoom() {
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFov event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            Optional<GunDef> defOpt = GunLookup.gunDef(stack);
            if (!defOpt.isEmpty()) {
               GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
               if (!state.activeToggles().isEmpty() || !(AdsBlendState.blend() <= 0.0F)) {
                  GunDef def = defOpt.get();
                  GunAttachments attachments = (GunAttachments)stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
                  float factor = resolveZoomFactor(def, attachments, state);
                  if (factor > 1.0F) {
                     lastFactor = factor;
                  } else {
                     factor = lastFactor;
                  }

                  float blend = AdsBlendState.blend();
                  float strength = Config.ADS_ZOOM == null ? 1.0F : ((Double)Config.ADS_ZOOM.get()).floatValue();
                  float eased = 1.0F + (factor - 1.0F) * blend * strength;
                  if (eased > 1.0F) {
                     double before = event.getFOV();
                     event.setFOV(before / eased);
                     if (Config.GUN_DIAGNOSTICS != null
                        && (Boolean)Config.GUN_DIAGNOSTICS.get()
                        && blend > 0.99F
                        && Minecraft.getInstance().player.tickCount % 20 == 0) {
                        LoggerFactory.getLogger("createloveandwar/zoom")
                           .info(
                              "ZOOM factor={} strength={} eased={} fov {} -> {}",
                              new Object[]{
                                 factor, strength, String.format("%.3f", eased), String.format("%.1f", before), String.format("%.1f", before / eased)
                              }
                           );
                     }
                  }
               }
            }
         }
      }
   }

   private static float resolveZoomFactor(GunDef def, GunAttachments attachments, GunActionState state) {
      float best = 1.0F;

      for (String toggleKey : state.activeToggles()) {
         Optional<ActionGroup> groupOpt = findActionGroup(def, attachments, toggleKey);
         if (!groupOpt.isEmpty()) {
            ActionGroup group = groupOpt.get();
            boolean isAimGroup = group.actions().stream().anyMatch(a -> "AimDownSights".equals(a.actionType()) || "Scope".equals(a.actionType()));
            if (isAimGroup) {
               List<ModifierSource> sources = StatSources.forFiring(def, group, attachments);
               float factor = StatResolver.resolve("fov_factor", 1.0F, sources);
               best = Math.max(best, factor);
            }
         }
      }

      return best;
   }

   private static Optional<ActionGroup> findActionGroup(GunDef def, GunAttachments attachments, String key) {
      Optional<ActionGroup> base = def.actionGroups().stream().filter(g -> g.key().equals(key)).findFirst();
      if (base.isPresent()) {
         return base;
      }

      for (ItemStack attachmentStack : attachments.all()) {
         Optional<AttachmentDef> attDef = GunLookup.attachmentDef(attachmentStack);
         if (!attDef.isEmpty()) {
            Optional<ActionGroup> override = attDef.get().actionOverrides().stream().filter(g -> g.key().equals(key)).findFirst();
            if (override.isPresent()) {
               return override;
            }
         }
      }

      return Optional.empty();
   }
}
