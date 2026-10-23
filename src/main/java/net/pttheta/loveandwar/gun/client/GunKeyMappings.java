package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunKeyMappings {
   private static final String CATEGORY = "key.categories.createloveandwar";
   public static final KeyMapping FIRE2 = new KeyMapping("key.createloveandwar.fire2", 67, "key.categories.createloveandwar");
   public static final KeyMapping RELOAD1 = new KeyMapping("key.createloveandwar.reload1", 82, "key.categories.createloveandwar");
   public static final KeyMapping SPECIAL_KEY_1 = new KeyMapping("key.createloveandwar.special1", 86, "key.categories.createloveandwar");
   public static final KeyMapping SPECIAL_KEY_2 = new KeyMapping("key.createloveandwar.special2", 71, "key.categories.createloveandwar");

   private GunKeyMappings() {
   }

   @SubscribeEvent
   public static void register(RegisterKeyMappingsEvent event) {
      event.register(FIRE2);
      event.register(RELOAD1);
      event.register(SPECIAL_KEY_1);
      event.register(SPECIAL_KEY_2);
   }
}
