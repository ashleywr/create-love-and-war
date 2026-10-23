package net.pttheta.loveandwar.item.custom;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class AmmoPouchItem extends Item {
   private final int capacity;
   private final TagKey<Item> accepts;

   public AmmoPouchItem(Properties properties, int capacity, TagKey<Item> accepts) {
      super(properties);
      this.capacity = capacity;
      this.accepts = accepts;
   }

   public int capacity() {
      return this.capacity;
   }

   public TagKey<Item> accepts() {
      return this.accepts;
   }
}
