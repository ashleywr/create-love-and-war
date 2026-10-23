package net.pttheta.loveandwar.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.pttheta.loveandwar.LoveAndWarMod;

public class ChestLootModifier extends LootModifier {
   public static final MapCodec<ChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ChestLootModifier::new));
   public static final ResourceLocation INJECTABLE_LOOT_CHEST = LoveAndWarMod.asResource("inject/loveandwarloot");
   private static final ResourceKey<LootTable> INJECTABLE_LOOT_CHEST_KEY = ResourceKey.create(Registries.LOOT_TABLE, INJECTABLE_LOOT_CHEST);

   public ChestLootModifier(LootItemCondition[] conditions) {
      super(conditions);
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
      LootTable table = context.getLevel().getServer().reloadableRegistries().getLootTable(INJECTABLE_LOOT_CHEST_KEY);
      table.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), loot::add));
      return loot;
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
