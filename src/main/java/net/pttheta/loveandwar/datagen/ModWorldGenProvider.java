package net.pttheta.loveandwar.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.pttheta.loveandwar.worldgen.ModBiomeModifiers;
import net.pttheta.loveandwar.worldgen.ModConfiguredFeatures;
import net.pttheta.loveandwar.worldgen.ModPlacedFeatures;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {
   public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
      .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
      .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
      .add(Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);

   public ModWorldGenProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, BUILDER, Set.of("createloveandwar"));
   }
}
