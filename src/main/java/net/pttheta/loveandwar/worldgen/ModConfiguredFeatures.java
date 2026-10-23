package net.pttheta.loveandwar.worldgen;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.blocks.ModBlocks;

public class ModConfiguredFeatures {
   public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_SULPHUR_ORE_KEY = registerKey("sulphur_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_SULPHUR_ORE_KEY = registerKey("nether_sulphur_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_TUNGSTEN_ORE_KEY = registerKey("tungsten_ore");
   public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_OIL_SAND_KEY = registerKey("oil_sand");

   public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
      RuleTest stoneReplacables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
      RuleTest deepslateReplacables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
      RuleTest netherReplacables = new BlockMatchTest(Blocks.NETHERRACK);
      List<TargetBlockState> overworldSulphurOres = List.of(
         OreConfiguration.target(stoneReplacables, ((Block)ModBlocks.SULPHUR_ORE.get()).defaultBlockState()),
         OreConfiguration.target(deepslateReplacables, ((Block)ModBlocks.DEEPSLATE_SULPHUR_ORE.get()).defaultBlockState())
      );
      register(context, OVERWORLD_SULPHUR_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSulphurOres, 8));
      register(
         context,
         NETHER_SULPHUR_ORE_KEY,
         Feature.ORE,
         new OreConfiguration(netherReplacables, ((Block)ModBlocks.NETHER_SULPHUR_ORE.get()).defaultBlockState(), 8)
      );
      register(
         context,
         OVERWORLD_TUNGSTEN_ORE_KEY,
         Feature.ORE,
         new OreConfiguration(deepslateReplacables, ((Block)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get()).defaultBlockState(), 5)
      );
      register(context, OVERWORLD_OIL_SAND_KEY, Feature.ORE, new OreConfiguration(stoneReplacables, ((Block)ModBlocks.OIL_SAND.get()).defaultBlockState(), 18));
   }

   public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
      return ResourceKey.create(Registries.CONFIGURED_FEATURE, LoveAndWarMod.asResource(name));
   }

   private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
      BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration
   ) {
      context.register(key, new ConfiguredFeature(feature, configuration));
   }
}
