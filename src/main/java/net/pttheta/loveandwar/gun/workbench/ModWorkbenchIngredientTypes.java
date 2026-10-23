package net.pttheta.loveandwar.gun.workbench;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public final class ModWorkbenchIngredientTypes {
   public static final DeferredRegister<IngredientType<?>> TYPES = DeferredRegister.create(Keys.INGREDIENT_TYPES, "createloveandwar");
   public static final DeferredHolder<IngredientType<?>, IngredientType<TieredPartIngredient>> TIERED_PART = TYPES.register(
      "tiered_part", () -> new IngredientType(TieredPartIngredient.CODEC)
   );
   public static final DeferredHolder<IngredientType<?>, IngredientType<StackedVanillaIngredient>> STACKED_VANILLA = TYPES.register(
      "stacked_vanilla", () -> new IngredientType(StackedVanillaIngredient.CODEC)
   );

   private ModWorkbenchIngredientTypes() {
   }

   public static void register(IEventBus modEventBus) {
      TYPES.register(modEventBus);
   }
}
