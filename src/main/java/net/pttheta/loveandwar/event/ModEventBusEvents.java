package net.pttheta.loveandwar.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.pttheta.loveandwar.network.ModMessages;

@EventBusSubscriber(modid = "createloveandwar")
public class ModEventBusEvents {
   @SubscribeEvent
   public static void registerPayloads(RegisterPayloadHandlersEvent event) {
      ModMessages.register(event);
   }
}
