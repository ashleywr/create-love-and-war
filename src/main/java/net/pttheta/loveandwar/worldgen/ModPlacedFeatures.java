package net.pttheta.loveandwar.worldgen;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.pttheta.loveandwar.LoveAndWarMod;

public class ModPlacedFeatures {
   public static final ResourceKey<PlacedFeature> SULPHUR_ORE_PLACED_KEY = registerKey("sulphur_ore_placed");
   public static final ResourceKey<PlacedFeature> NETHER_SULPHUR_ORE_PLACED_KEY = registerKey("nether_sulphur_ore_placed");
   public static final ResourceKey<PlacedFeature> TUNGSTEN_ORE_PLACED_KEY = registerKey("tungsten_ore_placed");
   public static final ResourceKey<PlacedFeature> OIL_SAND_PLACED_KEY = registerKey("oil_sand_placed");

   public static void bootstrap(BootstrapContext<PlacedFeature> context) {
      HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
      register(
         context,
         SULPHUR_ORE_PLACED_KEY,
         configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_SULPHUR_ORE_KEY),
         ModOrePlacement.commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(80)))
      );
      register(
         context,
         NETHER_SULPHUR_ORE_PLACED_KEY,
         configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_SULPHUR_ORE_KEY),
         ModOrePlacement.commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(80)))
      );
      register(
         context,
         TUNGSTEN_ORE_PLACED_KEY,
         configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_TUNGSTEN_ORE_KEY),
         ModOrePlacement.commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
      );
      register(
         context,
         OIL_SAND_PLACED_KEY,
         configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_OIL_SAND_KEY),
         ModOrePlacement.commonOrePlacement(18, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(80)))
      );
   }

   private static ResourceKey<PlacedFeature> registerKey(String name) {
      return ResourceKey.create(Registries.PLACED_FEATURE, LoveAndWarMod.asResource(name));
   }

   private static void register(
      BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers
   ) {
      context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
   }
}
