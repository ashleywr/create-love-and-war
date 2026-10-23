package net.pttheta.loveandwar.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition.Builder;
import net.pttheta.loveandwar.item.ModItems;
import net.pttheta.loveandwar.loot.AddItemModifier;
import net.pttheta.loveandwar.loot.AddSuspiciousSandModifier;

public class ModLootModifiersProvider extends GlobalLootModifierProvider {
   public ModLootModifiersProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "createloveandwar");
   }

   protected void start() {
      this.add(
         "sulphur_from_creeper",
         new AddItemModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("entities/creeper")).build()}, (Item)ModItems.RAW_SULPHUR.get()
         ),
         new ICondition[0]
      );
      this.add(
         "cigarette_from_mineshaft",
         new AddItemModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft")).build()}, (Item)ModItems.CIGARETTE.get()
         ),
         new ICondition[0]
      );
      this.add(
         "cigarette_from_chest",
         new AddItemModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("chests/simple_dungeon")).build()}, (Item)ModItems.CIGARETTE.get()
         ),
         new ICondition[0]
      );
      this.add(
         "cigarette_from_suspicious_sand",
         new AddSuspiciousSandModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("archaeology/desert_pyramid")).build()}, (Item)ModItems.CIGARETTE.get()
         ),
         new ICondition[0]
      );
   }
}
