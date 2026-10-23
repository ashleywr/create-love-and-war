package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record ActionDef(
   String actionType,
   float delay,
   float duration,
   List<SoundDef> sounds,
   String itemStack,
   List<Modifier> modifiers,
   float fovFactor,
   String scopeOverlay,
   String anim,
   float toolLevel,
   float harvestSpeed,
   float reach
) {
   public static final Codec<ActionDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.fieldOf("actionType").forGetter(ActionDef::actionType),
            Codec.FLOAT.optionalFieldOf("delay", 0.0F).forGetter(ActionDef::delay),
            Codec.FLOAT.optionalFieldOf("duration", 0.0F).forGetter(ActionDef::duration),
            SoundDef.CODEC.listOf().optionalFieldOf("sounds", List.of()).forGetter(ActionDef::sounds),
            Codec.STRING.optionalFieldOf("itemStack", "").forGetter(ActionDef::itemStack),
            Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(ActionDef::modifiers),
            Codec.FLOAT.optionalFieldOf("fovFactor", 1.0F).forGetter(ActionDef::fovFactor),
            Codec.STRING.optionalFieldOf("scopeOverlay", "").forGetter(ActionDef::scopeOverlay),
            Codec.STRING.optionalFieldOf("anim", "").forGetter(ActionDef::anim),
            Codec.FLOAT.optionalFieldOf("toolLevel", 1.0F).forGetter(ActionDef::toolLevel),
            Codec.FLOAT.optionalFieldOf("harvestSpeed", 1.0F).forGetter(ActionDef::harvestSpeed),
            Codec.FLOAT.optionalFieldOf("reach", 1.0F).forGetter(ActionDef::reach)
         )
         .apply(instance, ActionDef::new)
   );
}
