package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttachmentTraitCodecTest {
    @Test void attachmentAbilitiesDecodeAsTraitProvidersRatherThanEmptyInlineAbilities() {
        var definition = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            {"abilities":[{"trait":"createloveandwar:traits/stoic","level":3}]}
            """)).getOrThrow();
        assertEquals("createloveandwar:traits/stoic", definition.abilities().getFirst().trait());
        assertEquals(3, definition.abilities().getFirst().level());
        var encoded = AttachmentDef.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow();
        assertEquals(definition.abilities(), AttachmentDef.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().abilities());
    }

}
