package net.pttheta.loveandwar.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.pttheta.loveandwar.blocks.ModBlocks;

public class ModBlockStateProvider extends BlockStateProvider {
   public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
      super(output, "createloveandwar", exFileHelper);
   }

   protected void registerStatesAndModels() {
      this.blockWithItem(ModBlocks.SULPHUR_BLOCK);
      this.blockWithItem(ModBlocks.RAW_SULPHUR_BLOCK);
      this.blockWithItem(ModBlocks.SULPHUR_ORE);
      this.blockWithItem(ModBlocks.DEEPSLATE_SULPHUR_ORE);
      this.blockWithItem(ModBlocks.NETHER_SULPHUR_ORE);
      this.blockWithItem(ModBlocks.TUNGSTEN_BLOCK);
      this.blockWithItem(ModBlocks.RAW_TUNGSTEN_BLOCK);
      this.blockWithItem(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
      this.stairsBlock((StairBlock)ModBlocks.TUNGSTEN_STAIRS.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.slabBlock(
         (SlabBlock)ModBlocks.TUNGSTEN_SLAB.get(),
         this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()),
         this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get())
      );
      this.buttonBlock((ButtonBlock)ModBlocks.TUNGSTEN_BUTTON.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.pressurePlateBlock((PressurePlateBlock)ModBlocks.TUNGSTEN_PRESSURE_PLATE.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.fenceBlock((FenceBlock)ModBlocks.TUNGSTEN_FENCE.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.fenceGateBlock((FenceGateBlock)ModBlocks.TUNGSTEN_GATE.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.wallBlock((WallBlock)ModBlocks.TUNGSTEN_WALL.get(), this.blockTexture((Block)ModBlocks.TUNGSTEN_BLOCK.get()));
      this.doorBlockWithRenderType(
         (DoorBlock)ModBlocks.TUNGSTEN_DOOR.get(), this.modLoc("block/tungsten_door_bottom"), this.modLoc("block/tungsten_door_top"), "cutout"
      );
      this.trapdoorBlockWithRenderType((TrapDoorBlock)ModBlocks.TUNGSTEN_TRAPDOOR.get(), this.modLoc("block/tungsten_trapdoor"), true, "cutout");
   }

   private void blockWithItem(DeferredBlock<Block> blockRegistryObject) {
      this.simpleBlockWithItem((Block)blockRegistryObject.get(), this.cubeAll((Block)blockRegistryObject.get()));
   }
}
