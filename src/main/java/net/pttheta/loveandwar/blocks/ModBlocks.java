package net.pttheta.loveandwar.blocks;

import java.util.function.Supplier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.pttheta.loveandwar.fluid.ModFluids;
import net.pttheta.loveandwar.item.ModItems;

public class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("createloveandwar");
   public static final DeferredBlock<Block> SULPHUR_BLOCK = registerBlock(
      "sulphur_block", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COAL_BLOCK).sound(SoundType.CALCITE))
   );
   public static final DeferredBlock<Block> RAW_SULPHUR_BLOCK = registerBlock(
      "raw_sulphur_block", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COAL_BLOCK).sound(SoundType.CALCITE))
   );
   public static final DeferredBlock<Block> SULPHUR_ORE = registerBlock(
      "sulphur_ore",
      () -> new DropExperienceBlock(
         UniformInt.of(1, 2), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.STONE).strength(2.0F).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<Block> DEEPSLATE_SULPHUR_ORE = registerBlock(
      "deepslate_sulphur_ore",
      () -> new DropExperienceBlock(
         UniformInt.of(3, 4), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE).strength(3.0F).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<Block> NETHER_SULPHUR_ORE = registerBlock(
      "nether_sulphur_ore",
      () -> new DropExperienceBlock(
         UniformInt.of(3, 4), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERRACK).strength(1.0F).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<LiquidBlock> BITUMEN_FLUID_BLOCK = registerBlock(
      "bitumen_liquid_block",
      () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_BITUMEN.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<LiquidBlock> CRUDE_OIL_BLOCK = registerBlock(
      "crude_oil_block",
      () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_CRUDE_OIL.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).randomTicks())
   );
   public static final DeferredBlock<LiquidBlock> DIESEL_BLOCK = registerBlock(
      "diesel_block", () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_DIESEL.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<LiquidBlock> KEROSENE_BLOCK = registerBlock(
      "kerosene_block",
      () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_KEROSENE.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<LiquidBlock> ETHYLENE_BLOCK = registerBlock(
      "ethylene_block",
      () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_ETHYLENE.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<LiquidBlock> POLYMER_BLOCK = registerBlock(
      "polymer_block", () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_POLYMER.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<LiquidBlock> DURAPLAS_BLOCK = registerBlock(
      "duraplas_block",
      () -> new LiquidBlock((FlowingFluid)ModFluids.SOURCE_DURAPLAS.get(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER))
   );
   public static final DeferredBlock<Block> OIL_SAND = registerBlock(
      "oil_sand", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SANDSTONE))
   );
   public static final DeferredBlock<Block> SLIME_RESIN_BLOCK = registerBlock(
      "slime_resin_block", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> POLYMER_BLOCK_SOLID = registerBlock(
      "block_of_polymer", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> DURAPLAS_BLOCK_SOLID = registerBlock(
      "block_of_duraplas", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> BITUMEN_BLOCK = registerBlock(
      "bitumen", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> SMOOTH_ASPHALT = registerBlock(
      "smooth_asphalt", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> BLACK_PLASTIC = registerBlock(
      "black_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> BLUE_PLASTIC = registerBlock(
      "blue_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> BROWN_PLASTIC = registerBlock(
      "brown_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> CYAN_PLASTIC = registerBlock(
      "cyan_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> GRAY_PLASTIC = registerBlock(
      "gray_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> GREEN_PLASTIC = registerBlock(
      "green_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> LIGHT_BLUE_PLASTIC = registerBlock(
      "light_blue_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> LIGHT_GRAY_PLASTIC = registerBlock(
      "light_gray_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> LIME_PLASTIC = registerBlock(
      "lime_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> MAGENTA_PLASTIC = registerBlock(
      "magenta_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> ORANGE_PLASTIC = registerBlock(
      "orange_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> PINK_PLASTIC = registerBlock(
      "pink_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> PURPLE_PLASTIC = registerBlock(
      "purple_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> RED_PLASTIC = registerBlock(
      "red_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> WHITE_PLASTIC = registerBlock(
      "white_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> YELLOW_PLASTIC = registerBlock(
      "yellow_plastic", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> SMOOTH_ASPHALT_SLAB = registerBlock(
      "smooth_asphalt_slab", () -> new SlabBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE))
   );
   public static final DeferredBlock<Block> TUNGSTEN_BLOCK = registerBlock(
      "tungsten_block", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> RAW_TUNGSTEN_BLOCK = registerBlock(
      "raw_tungsten_block", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COAL_BLOCK).sound(SoundType.CALCITE))
   );
   public static final DeferredBlock<Block> DEEPSLATE_TUNGSTEN_ORE = registerBlock(
      "deepslate_tungsten_ore",
      () -> new DropExperienceBlock(
         UniformInt.of(3, 4), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE).strength(3.0F).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<Block> TUNGSTEN_STAIRS = registerBlock(
      "tungsten_stairs",
      () -> new StairBlock(((Block)TUNGSTEN_BLOCK.get()).defaultBlockState(), Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_SLAB = registerBlock(
      "tungsten_slab", () -> new SlabBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_BUTTON = registerBlock(
      "tungsten_button", () -> new ButtonBlock(BlockSetType.IRON, 10, Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.STONE_BUTTON))
   );
   public static final DeferredBlock<Block> TUNGSTEN_PRESSURE_PLATE = registerBlock(
      "tungsten_pressure_plate", () -> new PressurePlateBlock(BlockSetType.IRON, Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_FENCE = registerBlock(
      "tungsten_fence", () -> new FenceBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_GATE = registerBlock(
      "tungsten_gate", () -> new FenceGateBlock(ModWoodTypes.TUNGSTEN, Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_WALL = registerBlock(
      "tungsten_wall", () -> new WallBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK))
   );
   public static final DeferredBlock<Block> TUNGSTEN_DOOR = registerBlock(
      "tungsten_door", () -> new DoorBlock(BlockSetType.IRON, Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).noOcclusion())
   );
   public static final DeferredBlock<Block> TUNGSTEN_TRAPDOOR = registerBlock(
      "tungsten_trapdoor", () -> new TrapDoorBlock(BlockSetType.IRON, Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).noOcclusion())
   );

   private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
      DeferredBlock<T> toReturn = BLOCKS.register(name, block);
      ModItems.ITEMS.register(name, () -> new BlockItem((Block)toReturn.get(), new net.minecraft.world.item.Item.Properties()));
      return toReturn;
   }

   public static void register(IEventBus eventBus) {
      BLOCKS.register(eventBus);
   }
}
