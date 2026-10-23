package net.pttheta.loveandwar.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = "createloveandwar")
public class DataGenerators {
   @SubscribeEvent
   public static void gatherData(GatherDataEvent event) {
      DataGenerator generator = event.getGenerator();
      PackOutput packOutput = generator.getPackOutput();
      ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
      CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
      generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
      generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput, lookupProvider));
      generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
      generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
      ModBlockTagGenerator blockTagGenerator = (ModBlockTagGenerator)generator.addProvider(
         event.includeServer(), new ModBlockTagGenerator(packOutput, lookupProvider, existingFileHelper)
      );
      generator.addProvider(event.includeServer(), new ModItemTagGenerator(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));
      generator.addProvider(event.includeServer(), new ModLootModifiersProvider(packOutput, lookupProvider));
      generator.addProvider(event.includeServer(), new ModWorldGenProvider(packOutput, lookupProvider));
   }
}
