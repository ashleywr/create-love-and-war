package net.pttheta.loveandwar.gun.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.MaterialDef;
import net.pttheta.loveandwar.gun.content.PartDef;
import net.pttheta.loveandwar.gun.runtime.GunLookup;

public record TieredPartIngredient(List<String> materials, TagKey<Item> tag, int min, int max) implements ICustomIngredient {
   public static final MapCodec<TieredPartIngredient> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("materials", List.of()).forGetter(TieredPartIngredient::materials),
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TieredPartIngredient::tag),
            Codec.INT.optionalFieldOf("min", 0).forGetter(TieredPartIngredient::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(TieredPartIngredient::max)
         )
         .apply(instance, TieredPartIngredient::new)
   );

   public boolean test(ItemStack stack) {
      return !stack.isEmpty() && stack.is(this.tag)
         ? GunContent.part(GunLookup.contentKey(stack))
            .flatMap(TieredPartIngredient::materialOf)
            .map(mat -> this.materials.contains(mat.materialType()) && mat.craftingTier() >= this.min && mat.craftingTier() <= this.max)
            .orElse(false)
         : false;
   }

   private static Optional<MaterialDef> materialOf(PartDef part) {
      String materialId = part.material();
      String key = materialId.indexOf(58) >= 0 ? materialId.substring(materialId.indexOf(58) + 1) : materialId;
      return GunContent.material(key);
   }

   public Stream<ItemStack> getItems() {
      return BuiltInRegistries.ITEM.getTag(this.tag).map(holders -> holders.stream().map(h -> new ItemStack(h)).filter(this::test)).orElseGet(Stream::empty);
   }

   public boolean isSimple() {
      return true;
   }

   public IngredientType<?> getType() {
      return (IngredientType<?>)ModWorkbenchIngredientTypes.TIERED_PART.get();
   }
}
