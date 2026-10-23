package net.pttheta.loveandwar.gun.content.stat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

public record Modifier(String stat, List<String> matchGroupPaths, List<Accumulator> accumulators, String setValue) {
   private static final MapCodec<Modifier> MODERN_MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.STRING.fieldOf("stat").forGetter(Modifier::stat),
            Codec.STRING.listOf().optionalFieldOf("matchGroupPaths", List.of()).forGetter(Modifier::matchGroupPaths),
            Accumulator.CODEC.listOf().optionalFieldOf("accumulators", List.of()).forGetter(Modifier::accumulators),
            Codec.STRING.optionalFieldOf("setValue", "").forGetter(Modifier::setValue)
         )
         .apply(instance, Modifier::new)
   );
   private static final MapCodec<Modifier.Legacy> LEGACY_MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Codec.STRING.fieldOf("Stat").forGetter(Modifier.Legacy::stat),
            Codec.FLOAT.optionalFieldOf("Add", 0.0F).forGetter(Modifier.Legacy::add),
            Codec.FLOAT.optionalFieldOf("Multiply", 1.0F).forGetter(Modifier.Legacy::multiply),
            Codec.STRING.optionalFieldOf("SetValue", "").forGetter(Modifier.Legacy::setValue),
            Codec.STRING.listOf().optionalFieldOf("ApplyFilters", List.of()).forGetter(Modifier.Legacy::applyFilters)
         )
         .apply(instance, Modifier.Legacy::new)
   );
   public static final Codec<Modifier> CODEC = Codec.mapEither(MODERN_MAP_CODEC, LEGACY_MAP_CODEC)
      .xmap(either -> (Modifier)either.map(modern -> modern, Modifier::fromLegacy), Either::left)
      .codec();

   private static Modifier fromLegacy(Modifier.Legacy legacy) {
      List<Accumulator> accumulators = new ArrayList<>();
      if (legacy.add() != 0.0F) {
         accumulators.add(new Accumulator(AccumulatorOp.BASE_ADD, legacy.add(), List.of()));
      }

      if (legacy.multiply() != 1.0F) {
         float percentage = legacy.multiply() * 100.0F;
         accumulators.add(new Accumulator(AccumulatorOp.INDEPENDENT_PERCENTAGE, percentage, List.of()));
      }

      return new Modifier(legacy.stat(), legacy.applyFilters(), List.copyOf(accumulators), legacy.setValue());
   }

   public boolean isScoped() {
      return !this.matchGroupPaths.isEmpty();
   }

   public boolean isOverride() {
      return !this.setValue.isEmpty();
   }

   private record Legacy(String stat, float add, float multiply, String setValue, List<String> applyFilters) {
   }
}
