package net.pttheta.loveandwar.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddSuspiciousSandModifier extends LootModifier {
   public static final MapCodec<AddSuspiciousSandModifier> CODEC = RecordCodecBuilder.mapCodec(
      inst -> codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item)).apply(inst, AddSuspiciousSandModifier::new)
   );
   private final Item item;

   public AddSuspiciousSandModifier(LootItemCondition[] conditionsIn, Item item) {
      super(conditionsIn);
      this.item = item;
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      for (LootItemCondition condition : this.conditions) {
         if (!condition.test(context)) {
            return generatedLoot;
         }
      }

      if (context.getRandom().nextFloat() < 0.1F) {
         generatedLoot.clear();
         generatedLoot.add(new ItemStack(this.item));
      }

      generatedLoot.add(new ItemStack(this.item));
      return generatedLoot;
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
