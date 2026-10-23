package net.pttheta.loveandwar.gun.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class GunFabricationRecipeSerializer implements RecipeSerializer<GunFabricationRecipe> {
   private static final Codec<Item> RESULT_CODEC = RecordCodecBuilder.create(
      instance -> instance.group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(Function.identity())).apply(instance, Function.identity())
   );
   public static final MapCodec<GunFabricationRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(GunFabricationRecipe::ingredients),
            RESULT_CODEC.fieldOf("result").forGetter(GunFabricationRecipe::result),
            Codec.INT.optionalFieldOf("craft_time", 100).forGetter(GunFabricationRecipe::craftTimeTicks),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(GunFabricationRecipe::showNotification)
         )
         .apply(instance, GunFabricationRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, GunFabricationRecipe> STREAM_CODEC = StreamCodec.composite(
      SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
      GunFabricationRecipe::ingredients,
      ByteBufCodecs.registry(Registries.ITEM),
      GunFabricationRecipe::result,
      ByteBufCodecs.VAR_INT,
      GunFabricationRecipe::craftTimeTicks,
      ByteBufCodecs.BOOL,
      GunFabricationRecipe::showNotification,
      GunFabricationRecipe::new
   );

   public MapCodec<GunFabricationRecipe> codec() {
      return CODEC;
   }

   public StreamCodec<RegistryFriendlyByteBuf, GunFabricationRecipe> streamCodec() {
      return STREAM_CODEC;
   }
}
