package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record ActionGroup(
   String key,
   boolean canActUnderwater,
   boolean canActUnderOtherLiquid,
   boolean canBeOverriden,
   boolean twoHanded,
   String repeatMode,
   float repeatDelay,
   int repeatCount,
   float spinUpDuration,
   float loudness,
   String autoCancelIfNotInMode,
   List<ActionDef> actions,
   List<Modifier> modifiers
) {
   /** Original ActionGroupInstance duration excludes action delays and floors authored seconds to ticks. */
   public int durationTicks() {
      float seconds = repeatDelay;
      for (var action : actions) seconds = Math.max(seconds, action.duration());
      return Math.max(0, (int)Math.floor(seconds * 20));
   }

   public static final Codec<ActionGroup> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(ActionGroup::key),
            Codec.BOOL.optionalFieldOf("canActUnderwater", true).forGetter(ActionGroup::canActUnderwater),
            Codec.BOOL.optionalFieldOf("canActUnderOtherLiquid", false).forGetter(ActionGroup::canActUnderOtherLiquid),
            Codec.BOOL.optionalFieldOf("canBeOverriden", true).forGetter(ActionGroup::canBeOverriden),
            Codec.BOOL.optionalFieldOf("twoHanded", false).forGetter(ActionGroup::twoHanded),
            Codec.STRING.optionalFieldOf("repeatMode", "SemiAuto").forGetter(ActionGroup::repeatMode),
            Codec.FLOAT.optionalFieldOf("repeatDelay", 0.0F).forGetter(ActionGroup::repeatDelay),
            Codec.INT.optionalFieldOf("repeatCount", 0).forGetter(ActionGroup::repeatCount),
            Codec.FLOAT.optionalFieldOf("spinUpDuration", 0.0F).forGetter(ActionGroup::spinUpDuration),
            Codec.FLOAT.optionalFieldOf("loudness", 0.0F).forGetter(ActionGroup::loudness),
            Codec.STRING.optionalFieldOf("autoCancelIfNotInMode", "").forGetter(ActionGroup::autoCancelIfNotInMode),
            ActionDef.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(ActionGroup::actions),
            Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(ActionGroup::modifiers)
         )
         .apply(instance, ActionGroup::new)
   );
}
