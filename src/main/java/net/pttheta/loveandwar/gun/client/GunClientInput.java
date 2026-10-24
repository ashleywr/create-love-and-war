package net.pttheta.loveandwar.gun.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
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
   private static final Map<String, CGunInput> PRESSED = new HashMap<>();
   private static Player inputOwner;

   private GunClientInput() {
   }

   @SubscribeEvent(priority=EventPriority.HIGHEST)
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (inputOwner != mc.player) {
         PRESSED.clear();
         fire2Down = reload1Down = special1Down = special2Down = false;
         inputOwner = mc.player;
      }
      if (mc.player != null) {
         special1Down = poll(GunKeyMappings.SPECIAL_KEY_1, "SpecialKey1", special1Down);
         special2Down = poll(GunKeyMappings.SPECIAL_KEY_2, "SpecialKey2", special2Down);
         reload1Down = poll(GunKeyMappings.RELOAD1, "Reload1", reload1Down);
         fire2Down = poll(GunKeyMappings.FIRE2, "Fire2", fire2Down);
      }
   }

   private static boolean poll(KeyMapping mapping, String inputType, boolean wasDown) {
      Minecraft mc = Minecraft.getInstance();
      boolean isDown = mc.screen == null && mapping.isDown();
      if (isDown != wasDown) {
         if (isDown) {
            CGunInput.capture(mc.player, InteractionHand.MAIN_HAND, inputType).ifPresent(input -> {
               var sent=GunPrediction.beforeSend(input);
               PRESSED.put(inputType, sent);
               ModMessages.sendToServer(sent);
            });
         } else {
            var input = PRESSED.remove(inputType);
            if (input != null && mc.getConnection() != null) ModMessages.sendToServer(GunPrediction.beforeSend(input.released()));
         }
      }

      return isDown;
   }
}
