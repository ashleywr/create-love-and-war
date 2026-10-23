package net.pttheta.loveandwar.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.pttheta.loveandwar.LAWPonderPlugin;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public class LAWPonderClient {
   @SubscribeEvent
   static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PonderIndex.addPlugin(new LAWPonderPlugin()));
   }
}
