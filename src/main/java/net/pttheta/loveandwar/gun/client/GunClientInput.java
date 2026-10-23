package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.network.ModMessages;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunClientInput {
   private static boolean fire2Down;
   private static boolean reload1Down;
   private static boolean special1Down;
   private static boolean special2Down;

   private GunClientInput() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen == null) {
         fire2Down = poll(GunKeyMappings.FIRE2, "Fire2", fire2Down);
         reload1Down = poll(GunKeyMappings.RELOAD1, "Reload1", reload1Down);
         special1Down = poll(GunKeyMappings.SPECIAL_KEY_1, "SpecialKey1", special1Down);
         special2Down = poll(GunKeyMappings.SPECIAL_KEY_2, "SpecialKey2", special2Down);
      }
   }

   private static boolean poll(KeyMapping mapping, String inputType, boolean wasDown) {
      boolean isDown = mapping.isDown();
      if (isDown != wasDown) {
         ModMessages.sendToServer(new CGunInput(inputType, isDown));
         if (isDown) {
            GunPrediction.onInputSent(inputType);
         }
      }

      return isDown;
   }
}
