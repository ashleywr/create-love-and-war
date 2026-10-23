package net.pttheta.loveandwar.gun.client.geometry;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class TurboRigLoaderRegistration {
   private static final ResourceLocation TURBORIG_LOADER_ID = ResourceLocation.fromNamespaceAndPath("flansmod", "turborig");

   @SubscribeEvent
   static void registerGeometryLoaders(RegisterGeometryLoaders event) {
      event.register(TURBORIG_LOADER_ID, new TurboRigLoader());
   }
}
