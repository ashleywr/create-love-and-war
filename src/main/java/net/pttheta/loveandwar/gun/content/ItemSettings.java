package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record ItemSettings(int maxStackSize, List<String> tags) {
   public static final ItemSettings DEFAULT = new ItemSettings(64, List.of());
   public static final Codec<ItemSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.optionalFieldOf("maxStackSize", 64).forGetter(ItemSettings::maxStackSize),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(ItemSettings::tags)
         )
         .apply(instance, ItemSettings::new)
   );
}
