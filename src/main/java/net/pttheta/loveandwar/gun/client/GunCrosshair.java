package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.runtime.GunItem;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunCrosshair {
   private GunCrosshair() {
   }

   @SubscribeEvent
   static void onRenderCrosshair(Pre event) {
      if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
         if (Config.CROSSHAIR != null) {
            switch ((Config.CrosshairMode)Config.CROSSHAIR.get()) {
               case HIDE_ALWAYS:
                  event.setCanceled(true);
                  break;
               case HIDE_WITH_GUN:
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof GunItem) {
                     event.setCanceled(true);
                  }
            }
         }
      }
   }
}
