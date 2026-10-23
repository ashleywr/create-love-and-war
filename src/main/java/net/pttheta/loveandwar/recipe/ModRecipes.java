package net.pttheta.loveandwar.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.recipe.drawing.DrawingRecipe;
import net.pttheta.loveandwar.recipe.drawing.DrawingRecipeSerializer;
import net.pttheta.loveandwar.recipe.stamping.StampingRecipe;
import net.pttheta.loveandwar.recipe.stamping.StampingRecipeSerializer;

public class ModRecipes {
   public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "createloveandwar");
   public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "createloveandwar");
   public static final ResourceLocation DRAWING_ID = LoveAndWarMod.asResource("drawing");
   public static final ResourceLocation STAMPING_ID = LoveAndWarMod.asResource("stamping");
   public static final DeferredHolder<RecipeType<?>, RecipeType<DrawingRecipe>> DRAWING_TYPE = register("drawing");
   public static final DeferredHolder<RecipeType<?>, RecipeType<StampingRecipe>> STAMPING_TYPE = register("stamping");
   public static final DeferredHolder<RecipeSerializer<?>, DrawingRecipeSerializer> DRAWING = SERIALIZERS.register("drawing", DrawingRecipeSerializer::new);
   public static final DeferredHolder<RecipeSerializer<?>, StampingRecipeSerializer> STAMPING = SERIALIZERS.register("stamping", StampingRecipeSerializer::new);

   private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> register(String id) {
      return RECIPE_TYPES.register(id, () -> RecipeType.simple(LoveAndWarMod.asResource(id)));
   }

   public static void register(IEventBus event) {
      SERIALIZERS.register(event);
      RECIPE_TYPES.register(event);
   }
}
