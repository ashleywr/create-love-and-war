package net.pttheta.loveandwar.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe.Factory;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.recipe.petrochemical.FractionalDistillationRecipe;
import net.pttheta.loveandwar.recipe.petrochemical.PetrochemRecipeParams;
import net.pttheta.loveandwar.recipe.petrochemical.PetrochemRecipeSerializer;
import net.pttheta.loveandwar.recipe.petrochemical.ReactionRecipe;

public enum RecipeRegister implements IRecipeTypeInfo {
   REACTION(ReactionRecipe::new),
   FRACTIONAL_DISTILLATION(FractionalDistillationRecipe::new);

   private final ResourceLocation id;
   private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
   private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;

   <R extends ProcessingRecipe<?, PetrochemRecipeParams>> RecipeRegister(Factory<PetrochemRecipeParams, R> processingFactory) {
      String name = this.name().toLowerCase(Locale.ROOT);
      this.id = LoveAndWarMod.asResource(name);
      RecipeSerializer<?> serializer = new PetrochemRecipeSerializer(processingFactory);
      this.serializerObject = RecipeRegister.Registers.SERIALIZERS.register(name, () -> serializer);
      RecipeType<?> type = RecipeType.simple(this.id);
      this.typeObject = RecipeRegister.Registers.TYPES.register(name, () -> type);
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public <T extends RecipeSerializer<?>> T getSerializer() {
      return (T)this.serializerObject.get();
   }

   public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
      return (RecipeType<R>)this.typeObject.get();
   }

   public static void register(IEventBus modEventBus) {
      RecipeRegister.Registers.SERIALIZERS.register(modEventBus);
      RecipeRegister.Registers.TYPES.register(modEventBus);
   }

   private static final class Registers {
      private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "createloveandwar");
      private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "createloveandwar");
   }
}
