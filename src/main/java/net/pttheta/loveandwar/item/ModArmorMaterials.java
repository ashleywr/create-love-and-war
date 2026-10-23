package net.pttheta.loveandwar.item;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.util.ModTags;

public class ModArmorMaterials {
   public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "createloveandwar");
   public static final Holder<ArmorMaterial> TUNGSTEN = ARMOR_MATERIALS.register(
      "tungsten",
      () -> new ArmorMaterial(
         defense(4, 5, 7, 5),
         25,
         SoundEvents.ARMOR_EQUIP_IRON,
         () -> Ingredient.of(ModTags.Items.TUNGSTEN_REPAIR),
         List.of(new Layer(LoveAndWarMod.asResource("tungsten"))),
         1.0F,
         0.0F
      )
   );
   public static final int TUNGSTEN_DURABILITY = 26;

   private static EnumMap<Type, Integer> defense(int boots, int legs, int chest, int helmet) {
      EnumMap<Type, Integer> map = new EnumMap<>(Type.class);
      map.put(Type.BOOTS, boots);
      map.put(Type.LEGGINGS, legs);
      map.put(Type.CHESTPLATE, chest);
      map.put(Type.HELMET, helmet);
      map.put(Type.BODY, chest);
      return map;
   }

   public static void register(IEventBus eventBus) {
      ARMOR_MATERIALS.register(eventBus);
   }
}
