package net.pttheta.loveandwar.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
   public static final Tier TUNGSTEN = new SimpleTier(
      BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 4.0F, 2.5F, 12, () -> Ingredient.of(new ItemLike[]{(ItemLike)ModItems.TUNGSTEN.get()})
   );
}
