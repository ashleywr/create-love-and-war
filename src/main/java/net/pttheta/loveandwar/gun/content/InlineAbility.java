package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record InlineAbility(
   List<InlineAbility.TriggerRef> startTriggers,
   List<InlineAbility.TriggerRef> endTriggers,
   List<InlineAbility.TargetRef> targets,
   List<InlineAbility.EffectRef> effects,
   InlineAbility.StackingDef stacking
) {
   public static final Codec<InlineAbility> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            InlineAbility.TriggerRef.CODEC.listOf().optionalFieldOf("startTriggers", List.of()).forGetter(InlineAbility::startTriggers),
            InlineAbility.TriggerRef.CODEC.listOf().optionalFieldOf("endTriggers", List.of()).forGetter(InlineAbility::endTriggers),
            InlineAbility.TargetRef.CODEC.listOf().optionalFieldOf("targets", List.of()).forGetter(InlineAbility::targets),
            InlineAbility.EffectRef.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(InlineAbility::effects),
            InlineAbility.StackingDef.CODEC.optionalFieldOf("stacking", InlineAbility.StackingDef.NONE).forGetter(InlineAbility::stacking)
         )
         .apply(instance, InlineAbility::new)
   );

   public record EffectRef(String effectType, List<Modifier> modifiers) {
      public static final Codec<InlineAbility.EffectRef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("effectType", "").forGetter(InlineAbility.EffectRef::effectType),
               Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(InlineAbility.EffectRef::modifiers)
            )
            .apply(instance, InlineAbility.EffectRef::new)
      );
   }

   public record StackingDef(String stackingKey, int maxStacks, boolean decayAllAtOnce, float decayTime) {
      public static final InlineAbility.StackingDef NONE = new InlineAbility.StackingDef("", 0, false, 0.0F);
      public static final Codec<InlineAbility.StackingDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("stackingKey", "").forGetter(InlineAbility.StackingDef::stackingKey),
               Codec.INT.optionalFieldOf("maxStacks", 0).forGetter(InlineAbility.StackingDef::maxStacks),
               Codec.BOOL.optionalFieldOf("decayAllAtOnce", false).forGetter(InlineAbility.StackingDef::decayAllAtOnce),
               Codec.FLOAT.optionalFieldOf("decayTime", 0.0F).forGetter(InlineAbility.StackingDef::decayTime)
            )
            .apply(instance, InlineAbility.StackingDef::new)
      );
   }

   public record TargetRef(String targetType, List<String> matchIDs, List<String> matchTags) {
      public static final Codec<InlineAbility.TargetRef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("targetType", "Shooter").forGetter(InlineAbility.TargetRef::targetType),
               Codec.STRING.listOf().optionalFieldOf("matchIDs", List.of()).forGetter(InlineAbility.TargetRef::matchIDs),
               Codec.STRING.listOf().optionalFieldOf("matchTags", List.of()).forGetter(InlineAbility.TargetRef::matchTags)
            )
            .apply(instance, InlineAbility.TargetRef::new)
      );
   }

   public record TriggerRef(String triggerType, List<String> triggerConditions) {
      public static final Codec<InlineAbility.TriggerRef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("triggerType", "AlwaysOn").forGetter(InlineAbility.TriggerRef::triggerType),
               Codec.STRING.listOf().optionalFieldOf("triggerConditions", List.of()).forGetter(InlineAbility.TriggerRef::triggerConditions)
            )
            .apply(instance, InlineAbility.TriggerRef::new)
      );
   }
}
