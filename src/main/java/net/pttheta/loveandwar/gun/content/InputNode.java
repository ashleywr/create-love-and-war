package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InputNode(
   String actionGroupToTrigger,
   String modalCheck,
   boolean canTriggerWhileReloading,
   boolean deferToAttachment,
   String attachmentType,
   int attachmentIndex,
   boolean andContinueEvaluating
) {
   public static final Codec<InputNode> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.optionalFieldOf("actionGroupToTrigger", "").forGetter(InputNode::actionGroupToTrigger),
            Codec.STRING.optionalFieldOf("modalCheck", "").forGetter(InputNode::modalCheck),
            Codec.BOOL.optionalFieldOf("canTriggerWhileReloading", false).forGetter(InputNode::canTriggerWhileReloading),
            Codec.BOOL.optionalFieldOf("deferToAttachment", false).forGetter(InputNode::deferToAttachment),
            Codec.STRING.optionalFieldOf("attachmentType", "Generic").forGetter(InputNode::attachmentType),
            Codec.INT.optionalFieldOf("attachmentIndex", 0).forGetter(InputNode::attachmentIndex),
            Codec.BOOL.optionalFieldOf("andContinueEvaluating", false).forGetter(InputNode::andContinueEvaluating)
         )
         .apply(instance, InputNode::new)
   );
}
