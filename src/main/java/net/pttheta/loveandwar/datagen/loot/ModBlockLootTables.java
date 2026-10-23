package net.pttheta.loveandwar.datagen.loot;

import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.item.ModItems;

public class ModBlockLootTables extends BlockLootSubProvider {
   public ModBlockLootTables(Provider registries) {
      super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
   }

   protected void generate() {
      this.dropSelf((Block)ModBlocks.SULPHUR_BLOCK.get());
      this.dropSelf((Block)ModBlocks.RAW_SULPHUR_BLOCK.get());
      this.add((Block)ModBlocks.SULPHUR_ORE.get(), block -> this.createCopperLikeOreDrops((Block)ModBlocks.SULPHUR_ORE.get(), (Item)ModItems.RAW_SULPHUR.get()));
      this.add(
         (Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get(),
         block -> this.createCopperLikeOreDrops((Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get(), (Item)ModItems.RAW_SULPHUR.get())
      );
      this.add(
         (Block)ModBlocks.NETHER_SULPHUR_ORE.get(),
         block -> this.createCopperLikeOreDrops((Block)ModBlocks.NETHER_SULPHUR_ORE.get(), (Item)ModItems.RAW_SULPHUR.get())
      );
      this.dropSelf((Block)ModBlocks.TUNGSTEN_BLOCK.get());
      this.dropSelf((Block)ModBlocks.RAW_TUNGSTEN_BLOCK.get());
      this.add(
         (Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get(),
         block -> this.createOreDrop((Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get(), (Item)ModItems.RAW_TUNGSTEN.get())
      );
      this.dropSelf((Block)ModBlocks.TUNGSTEN_STAIRS.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_BUTTON.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_PRESSURE_PLATE.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_TRAPDOOR.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_FENCE.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_GATE.get());
      this.dropSelf((Block)ModBlocks.TUNGSTEN_WALL.get());
      this.add((Block)ModBlocks.TUNGSTEN_SLAB.get(), block -> this.createSlabItemTable((Block)ModBlocks.TUNGSTEN_SLAB.get()));
      this.add((Block)ModBlocks.TUNGSTEN_DOOR.get(), block -> this.createDoorTable((Block)ModBlocks.TUNGSTEN_DOOR.get()));
   }

   protected Builder createCopperLikeOreDrops(Block pBlock, Item item) {
      return this.createSilkTouchDispatchTable(
         pBlock,
         (net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder)this.applyExplosionDecay(
            pBlock,
            LootItem.lootTableItem(item)
               .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
               .apply(ApplyBonusCount.addOreBonusCount(this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)))
         )
      );
   }

   protected Iterable<Block> getKnownBlocks() {
      return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block)holder.get())::iterator;
   }
}
