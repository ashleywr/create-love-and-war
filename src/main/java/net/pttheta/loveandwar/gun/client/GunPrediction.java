package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.client.anim.LocalGunAnim;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;

public final class GunPrediction {
   private GunPrediction() {
   }

   public static void onInputSent(String inputType) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            String gunKey = GunLookup.contentKey(stack);
            GunEngine.predictStartedGroup(player, stack, inputType, player.level().getGameTime()).ifPresent(group -> {
               if (LocalGunAnim.wouldPredict(gunKey, group)) {
                  GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                  LocalGunAnim.predict(gunKey, state, group);
                  MuzzleFlash.onPredicted(stack, group.key());
               }
            });
         }
      }
   }
}
