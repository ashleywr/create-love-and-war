package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record TraitDef(int maxLevel, List<InlineAbility> abilities) {
   public static final Codec<TraitDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.optionalFieldOf("maxLevel", 1).forGetter(TraitDef::maxLevel),
            InlineAbility.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(TraitDef::abilities)
         )
         .apply(instance, TraitDef::new)
   );
}
