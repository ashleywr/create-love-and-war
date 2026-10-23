package net.pttheta.loveandwar.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.item.ModItems;

public class LAWPonder {
   public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(DeferredHolder::getId);
      HELPER.addStoryBoard(LAWBlocks.DRAWING_PRESS, "drawing_press", LAWPonderScenes::drawing, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES});
      HELPER.addStoryBoard(
         LAWBlocks.STAMPING_PRESS, "stamping_press", LAWPonderScenes::stamping, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES}
      );
      HELPER.addStoryBoard(
         new ItemProviderEntry(LoveAndWarMod.LAWREGISTRATE, ModItems.THERMOSTAT),
         "distillation_column",
         LAWPonderScenes::distillation,
         new ResourceLocation[]{AllCreatePonderTags.FLUIDS}
      );
   }
}
