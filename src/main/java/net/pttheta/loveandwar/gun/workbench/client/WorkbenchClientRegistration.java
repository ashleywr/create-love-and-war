package net.pttheta.loveandwar.gun.workbench.client;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.pttheta.loveandwar.screen.ModMenuTypes;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class WorkbenchClientRegistration {
   private WorkbenchClientRegistration() {
   }

   @SubscribeEvent
   public static void registerScreens(RegisterMenuScreensEvent event) {
      event.register((MenuType)ModMenuTypes.WORKBENCH.get(), WorkbenchScreen::new);
   }
}
