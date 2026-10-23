package net.pttheta.loveandwar.gun.client.anim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flanimation(List<Flanimation.Keyframe> keyframes, List<Flanimation.Sequence> sequences) {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-anim");
   public static final Codec<Flanimation> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Flanimation.Keyframe.CODEC.listOf().optionalFieldOf("keyframes", List.of()).forGetter(Flanimation::keyframes),
            Flanimation.Sequence.CODEC.listOf().optionalFieldOf("sequences", List.of()).forGetter(Flanimation::sequences)
         )
         .apply(instance, Flanimation::new)
   );

   static float parseNum(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            return Float.parseFloat(raw.trim());
         } catch (NumberFormatException e) {
            LOGGER.warn("flanimation: non-numeric pose component '{}' (expressions are not supported), treating as 0", raw);
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   public record Frame(int tick, String entry, String exit, String frame) {
      public static final Codec<Flanimation.Frame> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.INT.fieldOf("tick").forGetter(Flanimation.Frame::tick),
               Codec.STRING.optionalFieldOf("entry", "linear").forGetter(Flanimation.Frame::entry),
               Codec.STRING.optionalFieldOf("exit", "linear").forGetter(Flanimation.Frame::exit),
               Codec.STRING.fieldOf("frame").forGetter(Flanimation.Frame::frame)
            )
            .apply(instance, Flanimation.Frame::new)
      );
   }

   public record Keyframe(String name, List<Flanimation.RawPose> poses, List<String> parents) {
      public static final Codec<Flanimation.Keyframe> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("name").forGetter(Flanimation.Keyframe::name),
               Flanimation.RawPose.CODEC.listOf().optionalFieldOf("poses", List.of()).forGetter(Flanimation.Keyframe::poses),
               Codec.STRING.listOf().optionalFieldOf("parents", List.of()).forGetter(Flanimation.Keyframe::parents)
            )
            .apply(instance, Flanimation.Keyframe::new)
      );
   }

   public record RawPose(String applyTo, List<String> position, List<String> rotation, List<Float> scale) {
      private static final List<String> ZERO3 = List.of("0", "0", "0");
      private static final List<Float> ONE3 = List.of(1.0F, 1.0F, 1.0F);
      public static final Codec<Flanimation.RawPose> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("applyTo").forGetter(Flanimation.RawPose::applyTo),
               Codec.STRING.listOf().optionalFieldOf("position", ZERO3).forGetter(Flanimation.RawPose::position),
               Codec.STRING.listOf().optionalFieldOf("rotation", ZERO3).forGetter(Flanimation.RawPose::rotation),
               Codec.FLOAT.listOf().optionalFieldOf("scale", ONE3).forGetter(Flanimation.RawPose::scale)
            )
            .apply(instance, Flanimation.RawPose::new)
      );
   }

   public record Sequence(String name, int ticks, List<Flanimation.Frame> frames) {
      public static final Codec<Flanimation.Sequence> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("name").forGetter(Flanimation.Sequence::name),
               Codec.INT.optionalFieldOf("ticks", 0).forGetter(Flanimation.Sequence::ticks),
               Flanimation.Frame.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(Flanimation.Sequence::frames)
            )
            .apply(instance, Flanimation.Sequence::new)
      );
   }
}
