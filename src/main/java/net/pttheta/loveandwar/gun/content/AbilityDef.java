package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record AbilityDef(
   int maxLevel, AbilityDef.TriggerInfo trigger, AbilityDef.EffectInfo effect, AbilityDef.IntensityInfo intensity, AbilityDef.DurationInfo duration
) {
   public static final Codec<AbilityDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.optionalFieldOf("maxLevel", 1).forGetter(AbilityDef::maxLevel),
            RecordCodecBuilder.of(AbilityDef::trigger, AbilityDef.TriggerInfo.MAP_CODEC),
            RecordCodecBuilder.of(AbilityDef::effect, AbilityDef.EffectInfo.MAP_CODEC),
            RecordCodecBuilder.of(AbilityDef::intensity, AbilityDef.IntensityInfo.MAP_CODEC),
            RecordCodecBuilder.of(AbilityDef::duration, AbilityDef.DurationInfo.MAP_CODEC)
         )
         .apply(instance, AbilityDef::new)
   );

   public record DurationInfo(float baseDuration, float extraDurationPerLevel, boolean stackAmount, float maxAmount, boolean stackDuration, float maxDuration) {
      public static final MapCodec<AbilityDef.DurationInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.FLOAT.optionalFieldOf("baseDuration", 1.0F).forGetter(AbilityDef.DurationInfo::baseDuration),
               Codec.FLOAT.optionalFieldOf("extraDurationPerLevel", 0.0F).forGetter(AbilityDef.DurationInfo::extraDurationPerLevel),
               Codec.BOOL.optionalFieldOf("stackAmount", false).forGetter(AbilityDef.DurationInfo::stackAmount),
               Codec.FLOAT.optionalFieldOf("maxAmount", 1.0F).forGetter(AbilityDef.DurationInfo::maxAmount),
               Codec.BOOL.optionalFieldOf("stackDuration", false).forGetter(AbilityDef.DurationInfo::stackDuration),
               Codec.FLOAT.optionalFieldOf("maxDuration", 1.0F).forGetter(AbilityDef.DurationInfo::maxDuration)
            )
            .apply(instance, AbilityDef.DurationInfo::new)
      );
   }

   public record EffectInfo(String targetType, String effectType, List<String> effectParameters, List<Modifier> modifiers) {
      public static final MapCodec<AbilityDef.EffectInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("targetType", "Shooter").forGetter(AbilityDef.EffectInfo::targetType),
               Codec.STRING.optionalFieldOf("effectType", "").forGetter(AbilityDef.EffectInfo::effectType),
               Codec.STRING.listOf().optionalFieldOf("effectParameters", List.of()).forGetter(AbilityDef.EffectInfo::effectParameters),
               Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AbilityDef.EffectInfo::modifiers)
            )
            .apply(instance, AbilityDef.EffectInfo::new)
      );
   }

   public record IntensityInfo(
      float baseIntensity, float addIntensityPerLevel, float mulIntensityPerAttachment, float mulIntensityForFullMag, boolean invertMagFullness
   ) {
      public static final MapCodec<AbilityDef.IntensityInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.FLOAT.optionalFieldOf("baseIntensity", 1.0F).forGetter(AbilityDef.IntensityInfo::baseIntensity),
               Codec.FLOAT.optionalFieldOf("addIntensityPerLevel", 0.0F).forGetter(AbilityDef.IntensityInfo::addIntensityPerLevel),
               Codec.FLOAT.optionalFieldOf("mulIntensityPerAttachment", 0.0F).forGetter(AbilityDef.IntensityInfo::mulIntensityPerAttachment),
               Codec.FLOAT.optionalFieldOf("mulIntensityForFullMag", 0.0F).forGetter(AbilityDef.IntensityInfo::mulIntensityForFullMag),
               Codec.BOOL.optionalFieldOf("invertMagFullness", false).forGetter(AbilityDef.IntensityInfo::invertMagFullness)
            )
            .apply(instance, AbilityDef.IntensityInfo::new)
      );
   }

   public record TriggerInfo(String startTrigger, String endTrigger, List<String> triggerConditions) {
      public static final MapCodec<AbilityDef.TriggerInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("startTrigger", "AlwaysOn").forGetter(AbilityDef.TriggerInfo::startTrigger),
               Codec.STRING.optionalFieldOf("endTrigger", "AlwaysOn").forGetter(AbilityDef.TriggerInfo::endTrigger),
               Codec.STRING.listOf().optionalFieldOf("triggerConditions", List.of()).forGetter(AbilityDef.TriggerInfo::triggerConditions)
            )
            .apply(instance, AbilityDef.TriggerInfo::new)
      );
   }
}
