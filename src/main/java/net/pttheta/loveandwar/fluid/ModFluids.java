package net.pttheta.loveandwar.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.item.ModItems;

public class ModFluids {
   public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "createloveandwar");
   public static final DeferredHolder<Fluid, Source> SOURCE_BITUMEN = FLUIDS.register("bitumen_fluid", () -> new Source(ModFluids.BITUMEN_FLUID_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_BITUMEN = FLUIDS.register(
      "flowing_bitumen", () -> new Flowing(ModFluids.BITUMEN_FLUID_PROPERTIES)
   );
   public static final Properties BITUMEN_FLUID_PROPERTIES = properties(ModFluidTypes.BITUMEN_FLUID_TYPE, SOURCE_BITUMEN, FLOWING_BITUMEN)
      .block(ModBlocks.BITUMEN_FLUID_BLOCK)
      .bucket(ModItems.BITUMEN_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_CRUDE_OIL = FLUIDS.register("crude_oil", () -> new Source(ModFluids.CRUDE_OIL_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_CRUDE_OIL = FLUIDS.register(
      "flowing_crude_oil", () -> new Flowing(ModFluids.CRUDE_OIL_PROPERTIES)
   );
   public static final Properties CRUDE_OIL_PROPERTIES = properties(ModFluidTypes.CRUDE_OIL_TYPE, SOURCE_CRUDE_OIL, FLOWING_CRUDE_OIL)
      .block(ModBlocks.CRUDE_OIL_BLOCK)
      .bucket(ModItems.CRUDE_OIL_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_DIESEL = FLUIDS.register("diesel", () -> new Source(ModFluids.DIESEL_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_DIESEL = FLUIDS.register("flowing_diesel", () -> new Flowing(ModFluids.DIESEL_PROPERTIES));
   public static final Properties DIESEL_PROPERTIES = properties(ModFluidTypes.DIESEL_TYPE, SOURCE_DIESEL, FLOWING_DIESEL)
      .block(ModBlocks.DIESEL_BLOCK)
      .bucket(ModItems.DIESEL_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_KEROSENE = FLUIDS.register("kerosene", () -> new Source(ModFluids.KEROSENE_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_KEROSENE = FLUIDS.register("flowing_kerosene", () -> new Flowing(ModFluids.KEROSENE_PROPERTIES));
   public static final Properties KEROSENE_PROPERTIES = properties(ModFluidTypes.KEROSENE_TYPE, SOURCE_KEROSENE, FLOWING_KEROSENE)
      .block(ModBlocks.KEROSENE_BLOCK)
      .bucket(ModItems.KEROSENE_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_ETHYLENE = FLUIDS.register("ethylene", () -> new Source(ModFluids.ETHYLENE_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_ETHYLENE = FLUIDS.register("flowing_ethylene", () -> new Flowing(ModFluids.ETHYLENE_PROPERTIES));
   public static final Properties ETHYLENE_PROPERTIES = properties(ModFluidTypes.ETHYLENE_TYPE, SOURCE_ETHYLENE, FLOWING_ETHYLENE)
      .block(ModBlocks.ETHYLENE_BLOCK)
      .bucket(ModItems.ETHYLENE_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_POLYMER = FLUIDS.register("polymer", () -> new Source(ModFluids.POLYMER_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_POLYMER = FLUIDS.register("flowing_polymer", () -> new Flowing(ModFluids.POLYMER_PROPERTIES));
   public static final Properties POLYMER_PROPERTIES = properties(ModFluidTypes.POLYMER_TYPE, SOURCE_POLYMER, FLOWING_POLYMER)
      .block(ModBlocks.POLYMER_BLOCK)
      .bucket(ModItems.POLYMER_BUCKET);
   public static final DeferredHolder<Fluid, Source> SOURCE_DURAPLAS = FLUIDS.register("duraplas", () -> new Source(ModFluids.DURAPLAS_PROPERTIES));
   public static final DeferredHolder<Fluid, Flowing> FLOWING_DURAPLAS = FLUIDS.register("flowing_duraplas", () -> new Flowing(ModFluids.DURAPLAS_PROPERTIES));
   public static final Properties DURAPLAS_PROPERTIES = properties(ModFluidTypes.DURAPLAS_TYPE, SOURCE_DURAPLAS, FLOWING_DURAPLAS)
      .block(ModBlocks.DURAPLAS_BLOCK)
      .bucket(ModItems.DURAPLAS_BUCKET);

   private static Properties properties(DeferredHolder<FluidType, FluidType> type, DeferredHolder<Fluid, Source> source, DeferredHolder<Fluid, Flowing> flowing) {
      return new Properties(type, source, flowing).slopeFindDistance(2).levelDecreasePerBlock(2);
   }

   public static void register(IEventBus eventBus) {
      FLUIDS.register(eventBus);
   }
}
