package net.pttheta.loveandwar.blocks;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModWoodTypes {
   public static final WoodType TUNGSTEN = WoodType.register(
      new WoodType("createloveandwar:tungsten", BlockSetType.IRON, SoundType.METAL, SoundType.METAL, SoundEvents.CHAIN_PLACE, SoundEvents.ANVIL_BREAK)
   );

   public static void init() {
   }
}
