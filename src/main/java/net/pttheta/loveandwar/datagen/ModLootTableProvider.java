package net.pttheta.loveandwar.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.pttheta.loveandwar.datagen.loot.ModBlockLootTables;

public class ModLootTableProvider {
   public static LootTableProvider create(PackOutput output, CompletableFuture<Provider> registries) {
      return new LootTableProvider(output, Set.of(), List.of(new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)), registries);
   }
}
