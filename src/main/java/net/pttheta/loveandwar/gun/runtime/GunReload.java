package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.MagazineDef;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GunReload {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-runtime");

   private GunReload() {
   }

   static void start(ServerPlayer player, ItemStack gunStack, GunDef def, GunDef.ReloadDef reloadDef, GunActionState state) {
      Optional<MagazineDef> magOpt = GunEngine.findMagazineDef(player, def);
      if (magOpt.isEmpty()) {
         LOGGER.warn(
            "Gun '{}' reload '{}' could not resolve a MagazineDef for its magazines[0] slot (matchByNames={}, matchByTags={}) - no matching magazines/*.json tags/name found",
            new Object[]{
               GunLookup.contentKey(gunStack),
               reloadDef.startActionKey(),
               def.magazines().isEmpty() ? List.of() : def.magazines().get(0).matchByNames(),
               def.magazines().isEmpty() ? List.of() : def.magazines().get(0).matchByTags()
            }
         );
      } else {
         MagazineDef mag = magOpt.get();
         GunEngine.diag("reload start: gun={} magazine resolved, numRounds={} loadMode={}", GunLookup.contentKey(gunStack), mag.numRounds(), mag.ammoLoadMode());
         AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
         int needed = mag.numRounds() - rounds.items().size();
         if (needed <= 0) {
            GunEngine.diag("reload skipped: gun={} already full ({}/{})", GunLookup.contentKey(gunStack), rounds.items().size(), mag.numRounds());
         } else {
            List<String> queue = new ArrayList<>();
            if (!reloadDef.ejectActionKey().isEmpty()) {
               queue.add(reloadDef.ejectActionKey());
            }

            if (needed > 0 && "OneBulletAtATime".equals(mag.ammoLoadMode()) && !reloadDef.loadOneActionKey().isEmpty()) {
               int available = countMatchingBullets(player, mag);
               int toLoad = Math.min(needed, available);

               for (int i = 0; i < toLoad; i++) {
                  queue.add(reloadDef.loadOneActionKey());
               }
            }

            if ("FullMag".equals(mag.ammoLoadMode()) && !reloadDef.loadOneActionKey().isEmpty() && countMatchingBullets(player, mag) > 0) {
               queue.add(reloadDef.loadOneActionKey());
            }

            if (!reloadDef.endActionKey().isEmpty()) {
               queue.add(reloadDef.endActionKey());
            }

            Optional<ActionGroup> startGroup = def.actionGroups().stream().filter(g -> g.key().equals(reloadDef.startActionKey())).findFirst();
            int totalTicks = startGroup.map(GunEngine::groupDurationTicks).orElse(0);
            gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.withGroup(reloadDef.startActionKey(), totalTicks, queue));
            startGroup.ifPresent(g -> GunEngine.runImmediateSounds(player, gunStack, def, g));
         }
      }
   }

   static void advance(ServerPlayer player, ItemStack gunStack, GunDef def, GunActionState state) {
      List<String> pending = state.pendingReloadGroups();
      if (pending.isEmpty()) {
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.idled());
      } else {
         String nextKey = pending.get(0);
         List<String> rest = new ArrayList<>(pending.subList(1, pending.size()));
         Optional<GunDef.ReloadDef> reloadDef = def.reloads().stream().findFirst();
         Optional<MagazineDef> magOpt = GunEngine.findMagazineDef(player, def);
         boolean isLoadOne = reloadDef.<Boolean>map(rd -> rd.loadOneActionKey().equals(nextKey)).orElse(false);
         boolean isEnd = reloadDef.<Boolean>map(rd -> rd.endActionKey().equals(nextKey)).orElse(false);
         if (isLoadOne) {
            magOpt.ifPresent(mag -> consumeOneBullet(player, gunStack, mag));
         } else if (isEnd && magOpt.isPresent() && "FullMag".equals(magOpt.get().ammoLoadMode())) {
            consumeFullMag(player, gunStack, magOpt.get());
         }

         Optional<ActionGroup> group = def.actionGroups().stream().filter(g -> g.key().equals(nextKey)).findFirst();
         int totalTicks = group.map(GunEngine::groupDurationTicks).orElse(0);
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.withGroup(nextKey, totalTicks, rest));
         group.ifPresent(g -> GunEngine.runImmediateSounds(player, gunStack, def, g));
      }
   }

   private static int countMatchingBullets(ServerPlayer player, MagazineDef mag) {
      int count = 0;
      String requiredType = null;

      for (ItemStack s : player.getInventory().items) {
         if (!s.isEmpty() && GunLookup.matchesBullet(mag.matchingBullets(), s)) {
            String key = GunLookup.contentKey(s);
            if (mag.allRoundsMustBeIdentical()) {
               if (requiredType == null) {
                  requiredType = key;
               } else if (!requiredType.equals(key)) {
                  continue;
               }
            }

            count += s.getCount();
         }
      }

      return count;
   }

   private static void consumeOneBullet(ServerPlayer player, ItemStack gunStack, MagazineDef mag) {
      AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      String requiredType = mag.allRoundsMustBeIdentical() && !rounds.items().isEmpty() ? GunLookup.contentKey(rounds.items().get(0)) : null;

      for (ItemStack s : player.getInventory().items) {
         if (!s.isEmpty() && GunLookup.matchesBullet(mag.matchingBullets(), s) && (requiredType == null || GunLookup.contentKey(s).equals(requiredType))) {
            ItemStack loaded = s.copyWithCount(1);
            s.shrink(1);
            List<ItemStack> next = new ArrayList<>(rounds.items());
            next.add(loaded);
            gunStack.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(next));
            return;
         }
      }
   }

   private static void consumeFullMag(ServerPlayer player, ItemStack gunStack, MagazineDef mag) {
      AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      int needed = mag.numRounds() - rounds.items().size();
      if (needed > 0) {
         List<ItemStack> next = new ArrayList<>(rounds.items());
         String requiredType = mag.allRoundsMustBeIdentical() && !next.isEmpty() ? GunLookup.contentKey(next.get(0)) : null;

         for (ItemStack s : player.getInventory().items) {
            if (needed <= 0) {
               break;
            }

            if (!s.isEmpty() && GunLookup.matchesBullet(mag.matchingBullets(), s)) {
               String key = GunLookup.contentKey(s);
               if (requiredType == null || requiredType.equals(key)) {
                  if (requiredType == null) {
                     requiredType = key;
                  }

                  int take = Math.min(needed, s.getCount());

                  for (int i = 0; i < take; i++) {
                     next.add(s.copyWithCount(1));
                  }

                  s.shrink(take);
                  needed -= take;
               }
            }
         }

         gunStack.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(next));
      }
   }
}
