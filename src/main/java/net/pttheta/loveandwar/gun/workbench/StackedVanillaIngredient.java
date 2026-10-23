package net.pttheta.loveandwar.gun.workbench;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

public record StackedVanillaIngredient(List<Item> items) implements ICustomIngredient {
   public static final MapCodec<StackedVanillaIngredient> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items").forGetter(StackedVanillaIngredient::items))
         .apply(instance, StackedVanillaIngredient::new)
   );

   public boolean test(ItemStack stack) {
      return !stack.isEmpty() && this.items.contains(stack.getItem());
   }

   public Stream<ItemStack> getItems() {
      return this.items.stream().map(ItemStack::new);
   }

   public boolean isSimple() {
      return true;
   }

   public IngredientType<?> getType() {
      return (IngredientType<?>)ModWorkbenchIngredientTypes.STACKED_VANILLA.get();
   }
}
