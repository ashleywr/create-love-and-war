package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.network.ModMessages;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunLeftClickFire {
   private static CGunInput firing;
   private static CGunInput aiming;
   private static Player inputOwner;

   private GunLeftClickFire() {
   }

   @SubscribeEvent
   static void onInteractionKey(InteractionKeyMappingTriggered event) {
      resetOwner(Minecraft.getInstance().player);
      if (event.isUseItem()) {
         Player user = Minecraft.getInstance().player;
         if (user != null && user.getMainHandItem().getItem() instanceof GunItem) {
            if (aiming == null) {
               CGunInput.capture(user, InteractionHand.MAIN_HAND, "Fire2").ifPresent(input -> {
                  aiming = input;
                  ModMessages.sendToServer(input);
               });
            }

            event.setSwingHand(false);
         }
      } else if (event.isAttack()) {
         Player player = Minecraft.getInstance().player;
         if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            if (firing == null) {
               CGunInput.capture(player, InteractionHand.MAIN_HAND, "Fire1").ifPresent(input -> {
                  firing = input;
                  ModMessages.sendToServer(input);
                  GunPrediction.onInputSent("Fire1");
               });
            }
            event.setSwingHand(false);
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      resetOwner(mc.player);
      if (aiming != null && (!mc.options.keyUse.isDown() || mc.screen != null)) {
         release(aiming);
         aiming = null;
      }

      if (firing != null) {
         boolean stillHeld = mc.options != null
            && mc.options.keyAttack.isDown()
            && mc.screen == null
            && mc.player != null
            && mc.player.getMainHandItem().getItem() instanceof GunItem;
         if (!stillHeld) {
            release(firing);
            firing = null;
         }
      }
   }

   private static void release(CGunInput input) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.getConnection() != null) ModMessages.sendToServer(input.released());
   }

   private static void resetOwner(Player player) {
      if (inputOwner != player) {
         firing = null;
         aiming = null;
         inputOwner = player;
      }
   }
}
