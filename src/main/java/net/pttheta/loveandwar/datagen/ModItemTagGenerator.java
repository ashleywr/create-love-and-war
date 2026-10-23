package net.pttheta.loveandwar.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.pttheta.loveandwar.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class ModItemTagGenerator extends ItemTagsProvider {
   public ModItemTagGenerator(
      PackOutput pOutput,
      CompletableFuture<Provider> pLookupProvider,
      CompletableFuture<TagLookup<Block>> pBlockTags,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(pOutput, pLookupProvider, pBlockTags, "createloveandwar", existingFileHelper);
   }

   protected void addTags(Provider pProvider) {
      this.tag(ItemTags.TRIMMABLE_ARMOR)
         .add(
            new Item[]{
               (Item)ModItems.TUNGSTEN_HELMET.get(),
               (Item)ModItems.TUNGSTEN_CHESTPLATE.get(),
               (Item)ModItems.TUNGSTEN_LEGGINGS.get(),
               (Item)ModItems.TUNGSTEN_BOOTS.get()
            }
         );
   }
}
