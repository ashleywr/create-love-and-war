package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
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

   static void start(ServerPlayer player, ItemStack gunStack, GunDef def, GunDef.ReloadDef reload, GunActionState state) {
      var mag = GunEngine.findMagazineDef(player, def);
      if (mag.isEmpty()) {
         LOGGER.warn("Gun '{}' reload '{}' cannot resolve a magazine", GunLookup.contentKey(gunStack), reload.startActionKey());
         return;
      }
      if (!canLoad(player, gunStack, mag.get())) return;
      enter(player, gunStack, def, reload, reload.startActionKey(), state, mag.get());
   }

   static void advance(ServerPlayer player, ItemStack gunStack, GunDef def, GunActionState state) {
      var reload = def.reloads().stream().filter(value -> state.activeGroup().equals(value.startActionKey())
         || state.activeGroup().equals(value.ejectActionKey()) || state.activeGroup().equals(value.loadOneActionKey())
         || state.activeGroup().equals(value.endActionKey())).findFirst();
      var mag = GunEngine.findMagazineDef(player, def);
      if (reload.isEmpty() || mag.isEmpty()) {
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.idled());
         return;
      }
      var rd = reload.get();
      String next = "";
      if (state.activeGroup().equals(rd.startActionKey())) {
         next = !rd.ejectActionKey().isEmpty() ? rd.ejectActionKey()
            : !rd.loadOneActionKey().isEmpty() ? rd.loadOneActionKey() : rd.endActionKey();
      } else if (!state.activeGroup().equals(rd.endActionKey())) {
         next = canLoad(player, gunStack, mag.get()) && !rd.loadOneActionKey().isEmpty() ? rd.loadOneActionKey() : rd.endActionKey();
      }
      if (next.isEmpty()) gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.idled());
      else enter(player, gunStack, def, rd, next, state, mag.get());
   }

   private static void enter(ServerPlayer player, ItemStack stack, GunDef def, GunDef.ReloadDef reload, String key, GunActionState state, MagazineDef mag) {
      var group = def.actionGroups().stream().filter(value -> value.key().equals(key)).findFirst();
      int duration = group.map(GunEngine::groupDurationTicks).orElse(0);
      // The original retriggers an active LoadOne instance, retaining progress and increasing duration*triggerCount.
      stack.set(ModDataComponents.GUN_ACTION_STATE, state.withReloadGroup(key, duration, List.of()));
      group.ifPresent(value -> GunEngine.triggerActions(player, stack, def, value,
         stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY)));
      // EnterReloadState invokes LoadOne after the initial actions. FullMag transfers here too, never at End.
      if (key.equals(reload.loadOneActionKey())) {
         if ("FullMag".equals(mag.ammoLoadMode())) consumeFullMag(player, stack, mag);
         else consumeOneBullet(player, stack, mag);
      }
   }

   private static boolean canLoad(ServerPlayer player, ItemStack stack, MagazineDef mag) {
      return stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size() < mag.numRounds()
         && countMatchingBullets(player, stack, mag) > 0;
   }

   private static int countMatchingBullets(ServerPlayer player, ItemStack gunStack, MagazineDef mag) {
      int count = 0;
      var loaded = gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      String requiredType = mag.allRoundsMustBeIdentical() && !loaded.isEmpty() ? GunLookup.contentKey(loaded.items().getFirst()) : null;

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
