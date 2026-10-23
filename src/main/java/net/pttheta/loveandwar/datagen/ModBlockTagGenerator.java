package net.pttheta.loveandwar.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.util.ModTags;
import org.jetbrains.annotations.Nullable;

public class ModBlockTagGenerator extends BlockTagsProvider {
   public ModBlockTagGenerator(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "createloveandwar", existingFileHelper);
   }

   protected void addTags(Provider pProvider) {
      this.tag(ModTags.Blocks.SULPHUR)
         .add((Block)ModBlocks.SULPHUR_BLOCK.get())
         .add((Block)ModBlocks.SULPHUR_ORE.get())
         .add((Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get())
         .add((Block)ModBlocks.NETHER_SULPHUR_ORE.get())
         .add((Block)ModBlocks.RAW_SULPHUR_BLOCK.get());
      this.tag(ModTags.Blocks.TUNGSTEN)
         .add((Block)ModBlocks.TUNGSTEN_BLOCK.get())
         .add((Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get())
         .add((Block)ModBlocks.RAW_TUNGSTEN_BLOCK.get());
      this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
         .add((Block)ModBlocks.SULPHUR_BLOCK.get())
         .add((Block)ModBlocks.SULPHUR_ORE.get())
         .add((Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get())
         .add((Block)ModBlocks.NETHER_SULPHUR_ORE.get())
         .add((Block)ModBlocks.RAW_SULPHUR_BLOCK.get())
         .add((Block)ModBlocks.TUNGSTEN_BLOCK.get())
         .add((Block)ModBlocks.RAW_TUNGSTEN_BLOCK.get())
         .add((Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
      this.tag(BlockTags.NEEDS_STONE_TOOL)
         .add((Block)ModBlocks.SULPHUR_BLOCK.get())
         .add((Block)ModBlocks.SULPHUR_ORE.get())
         .add((Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get())
         .add((Block)ModBlocks.NETHER_SULPHUR_ORE.get())
         .add((Block)ModBlocks.RAW_SULPHUR_BLOCK.get());
      this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
         .add((Block)ModBlocks.TUNGSTEN_BLOCK.get())
         .add((Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get())
         .add((Block)ModBlocks.RAW_TUNGSTEN_BLOCK.get());
      this.tag(BlockTags.FENCES).add((Block)ModBlocks.TUNGSTEN_FENCE.get());
      this.tag(BlockTags.FENCE_GATES).add((Block)ModBlocks.TUNGSTEN_GATE.get());
      this.tag(BlockTags.WALLS).add((Block)ModBlocks.TUNGSTEN_WALL.get());
   }
}
