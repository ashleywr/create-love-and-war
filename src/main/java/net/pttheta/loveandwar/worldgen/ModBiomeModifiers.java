package net.pttheta.loveandwar.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.pttheta.loveandwar.LoveAndWarMod;

public class ModBiomeModifiers {
   public static final ResourceKey<BiomeModifier> ADD_SULPHUR_ORE = registerKey("add_sulphur_ore");
   public static final ResourceKey<BiomeModifier> ADD_NETHER_SULPHUR_ORE = registerKey("add_nether_sulphur_ore");
   public static final ResourceKey<BiomeModifier> ADD_TUNGSTEN_ORE = registerKey("add_tungsten_ore");
   public static final ResourceKey<BiomeModifier> ADD_OIL_SAND = registerKey("add_oil_sand");

   public static void bootstrap(BootstrapContext<BiomeModifier> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
      context.register(
         ADD_SULPHUR_ORE,
         new AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(new Holder[]{placedFeatures.getOrThrow(ModPlacedFeatures.SULPHUR_ORE_PLACED_KEY)}),
            Decoration.UNDERGROUND_ORES
         )
      );
      context.register(
         ADD_NETHER_SULPHUR_ORE,
         new AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_NETHER),
            HolderSet.direct(new Holder[]{placedFeatures.getOrThrow(ModPlacedFeatures.NETHER_SULPHUR_ORE_PLACED_KEY)}),
            Decoration.UNDERGROUND_ORES
         )
      );
      context.register(
         ADD_TUNGSTEN_ORE,
         new AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(new Holder[]{placedFeatures.getOrThrow(ModPlacedFeatures.TUNGSTEN_ORE_PLACED_KEY)}),
            Decoration.UNDERGROUND_ORES
         )
      );
      context.register(
         ADD_OIL_SAND,
         new AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(new Holder[]{placedFeatures.getOrThrow(ModPlacedFeatures.OIL_SAND_PLACED_KEY)}),
            Decoration.UNDERGROUND_ORES
         )
      );
   }

   private static ResourceKey<BiomeModifier> registerKey(String name) {
      return ResourceKey.create(Keys.BIOME_MODIFIERS, LoveAndWarMod.asResource(name));
   }
}
