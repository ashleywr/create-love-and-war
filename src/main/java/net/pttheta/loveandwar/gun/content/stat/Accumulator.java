package net.pttheta.loveandwar.gun.content.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.ContentCodecs;

public record Accumulator(AccumulatorOp operation, float value, List<MultiplyPerKind> multiplyPer) {
   private static final Codec<AccumulatorOp> OP_CODEC = ContentCodecs.lenientEnum(AccumulatorOp.class, AccumulatorOp::key, AccumulatorOp.UNKNOWN);
   private static final Codec<MultiplyPerKind> MULTIPLY_PER_CODEC = ContentCodecs.lenientEnum(
      MultiplyPerKind.class, MultiplyPerKind::key, MultiplyPerKind.UNKNOWN
   );
   public static final Codec<Accumulator> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            OP_CODEC.fieldOf("operation").forGetter(Accumulator::operation),
            Codec.FLOAT.fieldOf("value").forGetter(Accumulator::value),
            MULTIPLY_PER_CODEC.listOf().optionalFieldOf("multiplyPer", List.of()).forGetter(Accumulator::multiplyPer)
         )
         .apply(instance, Accumulator::new)
   );
}
