package net.pttheta.loveandwar.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.pttheta.loveandwar.LoveAndWarMod;

public class ModTags {
   public static final String FLAN_NS = "flansmod";

   public static class Blocks {
      public static final TagKey<Block> SULPHUR = tag("sulphur");
      public static final TagKey<Block> TUNGSTEN = tag("tungsten");
      public static final TagKey<Block> PLASTIC = tag("plastic");
      public static final TagKey<Block> COLORED_PLASTIC = tag("colored_plastic");
      public static final TagKey<Block> NEEDS_TUNGSTEN_TOOL = tag("needs_tungsten_tool");

      private static TagKey<Block> tag(String name) {
         return TagKey.create(Registries.BLOCK, LoveAndWarMod.asResource(name));
      }
   }

   public static class Items {
      public static final TagKey<Item> STAMPING_TEMPLATE = tag("stamping_template");
      public static final TagKey<Item> PAPER_CARTRIDGE = tag("black_powder");
      public static final TagKey<Item> PISTOL_BULLET = tagFlan("pistol_bullet");
      public static final TagKey<Item> RIFLE_BULLET = tagFlan("rifle_bullet");
      public static final TagKey<Item> SHOTGUN_SHELL = tagFlan("shotgun_shell");
      public static final TagKey<Item> GRENADE_ROUND = tagFlan("launchable_grenade");
      public static final TagKey<Item> ARTILLERY_SHELL = tagFlan("artillery_shell");
      public static final TagKey<Item> TUNGSTEN_REPAIR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/tungsten"));

      private static TagKey<Item> tag(String name) {
         return TagKey.create(Registries.ITEM, LoveAndWarMod.asResource(name));
      }

      private static TagKey<Item> tagFlan(String name) {
         return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("flansmod", name));
      }
   }
}
