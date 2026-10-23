package net.pttheta.loveandwar;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.pttheta.loveandwar.ponder.LAWPonder;

public class LAWPonderPlugin implements PonderPlugin {
   public String getModId() {
      return "createloveandwar";
   }

   public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      super.registerScenes(helper);
      LAWPonder.register(helper);
   }

   public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
      super.registerTags(helper);
   }

   public void registerSharedText(SharedTextRegistrationHelper helper) {
      super.registerSharedText(helper);
   }

   public void onPonderLevelRestore(PonderLevel ponderLevel) {
      super.onPonderLevelRestore(ponderLevel);
   }

   public void indexExclusions(IndexExclusionHelper helper) {
      super.indexExclusions(helper);
   }
}
