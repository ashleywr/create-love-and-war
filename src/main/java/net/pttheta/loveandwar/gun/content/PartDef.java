package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record PartDef(
   boolean canPlaceInMachiningTable,
   boolean canPlaceInModificationTable,
   List<String> compatiblityTags,
   ItemSettings itemSettings,
   List<Modifier> modifiers,
   List<PartDef.TraitRef> traits,
   List<InlineAbility> abilities,
   PartDef.EngineDef engine,
   String material
) {
   public static final Codec<PartDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.BOOL.optionalFieldOf("canPlaceInMachiningTable", false).forGetter(PartDef::canPlaceInMachiningTable),
            Codec.BOOL.optionalFieldOf("canPlaceInModificationTable", false).forGetter(PartDef::canPlaceInModificationTable),
            Codec.STRING.listOf().optionalFieldOf("compatiblityTags", List.of()).forGetter(PartDef::compatiblityTags),
            ItemSettings.CODEC.optionalFieldOf("itemSettings", ItemSettings.DEFAULT).forGetter(PartDef::itemSettings),
            Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(PartDef::modifiers),
            PartDef.TraitRef.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(PartDef::traits),
            InlineAbility.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(PartDef::abilities),
            PartDef.EngineDef.CODEC.optionalFieldOf("engine", PartDef.EngineDef.NONE).forGetter(PartDef::engine),
            Codec.STRING.optionalFieldOf("material", "").forGetter(PartDef::material)
         )
         .apply(instance, PartDef::new)
   );

   public record EngineDef(
      float maxAcceleration,
      float maxDeceleration,
      float maxSpeed,
      String fuelType,
      float fuelConsumptionRate,
      int solidFuelSlots,
      int liquidFuelCapacity,
      int batterySlots,
      int feCapacity
   ) {
      public static final PartDef.EngineDef NONE = new PartDef.EngineDef(0.0F, 0.0F, 0.0F, "", 0.0F, 0, 0, 0, 0);
      public static final Codec<PartDef.EngineDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.FLOAT.optionalFieldOf("maxAcceleration", 0.0F).forGetter(PartDef.EngineDef::maxAcceleration),
               Codec.FLOAT.optionalFieldOf("maxDeceleration", 0.0F).forGetter(PartDef.EngineDef::maxDeceleration),
               Codec.FLOAT.optionalFieldOf("maxSpeed", 0.0F).forGetter(PartDef.EngineDef::maxSpeed),
               Codec.STRING.optionalFieldOf("fuelType", "").forGetter(PartDef.EngineDef::fuelType),
               Codec.FLOAT.optionalFieldOf("fuelConsumptionRate", 0.0F).forGetter(PartDef.EngineDef::fuelConsumptionRate),
               Codec.INT.optionalFieldOf("solidFuelSlots", 0).forGetter(PartDef.EngineDef::solidFuelSlots),
               Codec.INT.optionalFieldOf("liquidFuelCapacity", 0).forGetter(PartDef.EngineDef::liquidFuelCapacity),
               Codec.INT.optionalFieldOf("batterySlots", 0).forGetter(PartDef.EngineDef::batterySlots),
               Codec.INT.optionalFieldOf("FECapacity", 0).forGetter(PartDef.EngineDef::feCapacity)
            )
            .apply(instance, PartDef.EngineDef::new)
      );
   }

   public record TraitRef(String trait, int level) {
      public static final Codec<PartDef.TraitRef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("trait").forGetter(PartDef.TraitRef::trait), Codec.INT.optionalFieldOf("level", 1).forGetter(PartDef.TraitRef::level)
            )
            .apply(instance, PartDef.TraitRef::new)
      );
   }
}
