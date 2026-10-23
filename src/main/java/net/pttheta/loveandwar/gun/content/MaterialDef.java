package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record MaterialDef(List<MaterialDef.MaterialSource> sources, int craftingTier, String materialType) {
   public static final Codec<MaterialDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            MaterialDef.MaterialSource.CODEC.listOf().optionalFieldOf("sources", List.of()).forGetter(MaterialDef::sources),
            Codec.INT.optionalFieldOf("craftingTier", 1).forGetter(MaterialDef::craftingTier),
            Codec.STRING.optionalFieldOf("materialType", "Metal").forGetter(MaterialDef::materialType)
         )
         .apply(instance, MaterialDef::new)
   );

   public record MaterialSource(List<String> matchItems, List<String> matchTags, int count, String icon) {
      public static final Codec<MaterialDef.MaterialSource> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.listOf().optionalFieldOf("matchItems", List.of()).forGetter(MaterialDef.MaterialSource::matchItems),
               Codec.STRING.listOf().optionalFieldOf("matchTags", List.of()).forGetter(MaterialDef.MaterialSource::matchTags),
               Codec.INT.optionalFieldOf("count", 1).forGetter(MaterialDef.MaterialSource::count),
               Codec.STRING.optionalFieldOf("icon", "ingot").forGetter(MaterialDef.MaterialSource::icon)
            )
            .apply(instance, MaterialDef.MaterialSource::new)
      );
   }
}
