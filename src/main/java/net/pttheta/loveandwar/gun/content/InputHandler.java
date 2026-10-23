package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record InputHandler(String inputType, List<InputNode> nodes) {
   public static final Codec<InputHandler> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.fieldOf("inputType").forGetter(InputHandler::inputType),
            InputNode.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(InputHandler::nodes)
         )
         .apply(instance, InputHandler::new)
   );
}
