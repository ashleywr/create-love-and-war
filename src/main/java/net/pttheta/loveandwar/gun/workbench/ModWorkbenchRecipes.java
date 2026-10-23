package net.pttheta.loveandwar.gun.workbench;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.LoveAndWarMod;

public final class ModWorkbenchRecipes {
   public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "createloveandwar");
   public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "createloveandwar");
   public static final DeferredHolder<RecipeType<?>, RecipeType<GunFabricationRecipe>> GUN_FABRICATION_TYPE = TYPES.register(
      "gun_fabrication", () -> RecipeType.simple(LoveAndWarMod.asResource("gun_fabrication"))
   );
   public static final DeferredHolder<RecipeSerializer<?>, GunFabricationRecipeSerializer> GUN_FABRICATION_SERIALIZER = SERIALIZERS.register(
      "gun_fabrication", GunFabricationRecipeSerializer::new
   );

   private ModWorkbenchRecipes() {
   }

   public static void register(IEventBus modEventBus) {
      TYPES.register(modEventBus);
      SERIALIZERS.register(modEventBus);
   }
}
