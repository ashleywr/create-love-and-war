package net.pttheta.loveandwar.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public class ModLootModifiers {
   public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "createloveandwar"
   );
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIER_SERIALIZERS.register(
      "add_item", () -> AddItemModifier.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddSuspiciousSandModifier>> ADD_SUSPICIOUS_SAND_ITEM = LOOT_MODIFIER_SERIALIZERS.register(
      "add_sand_item", () -> AddSuspiciousSandModifier.CODEC
   );
   public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ChestLootModifier>> ADD_CHEST_LOOT = LOOT_MODIFIER_SERIALIZERS.register(
      "add_chest_loot", () -> ChestLootModifier.CODEC
   );

   public static void register(IEventBus eventBus) {
      LOOT_MODIFIER_SERIALIZERS.register(eventBus);
   }
}
