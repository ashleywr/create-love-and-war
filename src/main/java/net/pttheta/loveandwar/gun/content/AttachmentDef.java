package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record AttachmentDef(
   ItemSettings itemSettings,
   String attachmentType,
   List<Modifier> modifiers,
   List<JsonElement> mechaEffects,
   String mechaEffectFilter,
   List<InputHandler> handlerOverrides,
   List<ActionGroup> actionOverrides,
   List<JsonElement> reloadOverrides,
   List<InlineAbility> abilities,
   String modeOverride,
   boolean overrideFireMode
) {
   public static final Codec<AttachmentDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ItemSettings.CODEC.optionalFieldOf("itemSettings", ItemSettings.DEFAULT).forGetter(AttachmentDef::itemSettings),
            Codec.STRING.optionalFieldOf("attachmentType", "Generic").forGetter(AttachmentDef::attachmentType),
            Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AttachmentDef::modifiers),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("mechaEffects", List.of()).forGetter(AttachmentDef::mechaEffects),
            Codec.STRING.optionalFieldOf("mechaEffectFilter", "").forGetter(AttachmentDef::mechaEffectFilter),
            InputHandler.CODEC.listOf().optionalFieldOf("handlerOverrides", List.of()).forGetter(AttachmentDef::handlerOverrides),
            ActionGroup.CODEC.listOf().optionalFieldOf("actionOverrides", List.of()).forGetter(AttachmentDef::actionOverrides),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("reloadOverrides", List.of()).forGetter(AttachmentDef::reloadOverrides),
            InlineAbility.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(AttachmentDef::abilities),
            Codec.STRING.optionalFieldOf("modeOverride", "").forGetter(AttachmentDef::modeOverride),
            Codec.BOOL.optionalFieldOf("overrideFireMode", false).forGetter(AttachmentDef::overrideFireMode)
         )
         .apply(instance, AttachmentDef::new)
   );
}
