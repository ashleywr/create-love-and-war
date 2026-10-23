package net.pttheta.loveandwar.event;

import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(modid = "createloveandwar")
public class ModEvent {
   public static boolean count = false;
   public static int time = 0;
   protected static final RandomSource random = RandomSource.create();

   @SubscribeEvent
   public static void tickEvent(Post event) {
      if (count) {
         time++;
      }

      if (time >= 5000) {
         System.exit(0);
      }
   }
}
