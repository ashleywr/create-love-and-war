package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
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
   private static boolean firing;
   private static boolean aiming;

   private GunLeftClickFire() {
   }

   @SubscribeEvent
   static void onInteractionKey(InteractionKeyMappingTriggered event) {
      if (event.isUseItem()) {
         Player user = Minecraft.getInstance().player;
         if (user != null && user.getMainHandItem().getItem() instanceof GunItem) {
            if (!aiming) {
               aiming = true;
               ModMessages.sendToServer(new CGunInput("Fire2", true));
            }

            event.setSwingHand(false);
         }
      } else if (event.isAttack()) {
         Player player = Minecraft.getInstance().player;
         if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            if (!firing) {
               ModMessages.sendToServer(new CGunInput("Fire1", true));
               GunPrediction.onInputSent("Fire1");
               firing = true;
            }
            event.setSwingHand(false);
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      if (aiming && !Minecraft.getInstance().options.keyUse.isDown()) {
         aiming = false;
      }

      if (firing) {
         Minecraft mc = Minecraft.getInstance();
         boolean stillHeld = mc.options != null
            && mc.options.keyAttack.isDown()
            && mc.screen == null
            && mc.player != null
            && mc.player.getMainHandItem().getItem() instanceof GunItem;
         if (!stillHeld) {
            firing = false;
            if (mc.player != null && mc.getConnection() != null) ModMessages.sendToServer(new CGunInput("Fire1", false));
         }
      }
   }
}
