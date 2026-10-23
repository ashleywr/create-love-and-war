package net.pttheta.loveandwar.gun.client.anim;

import java.util.Map.Entry;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunAnimationModelHooks {
   private GunAnimationModelHooks() {
   }

   @SubscribeEvent
   static void modifyBakingResult(ModifyBakingResult event) {
      for (Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
         if (entry.getValue() instanceof TurboRigBakedModel turboRig) {
            entry.setValue(new AnimatedTurboRigModel(turboRig));
         }
      }
   }
}
