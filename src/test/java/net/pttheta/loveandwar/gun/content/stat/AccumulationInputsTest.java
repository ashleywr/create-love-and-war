package net.pttheta.loveandwar.gun.content.stat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccumulationInputsTest {
    private static Modifier modifier(String inputs) {
        return Modifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"stat\":\"test\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":4,\"multiplyPer\":" + inputs + "}]}" )).getOrThrow();
    }

    @Test void outOfContextHasZeroAttachments() {
        assertEquals(0, StatResolver.resolve("test", 0, List.of(ModifierSource.of("test", List.of(modifier("[\"PerAttachment\"]"))))));
    }

    @Test void originalActiveContextKeepsFullnessZeroAndEmptinessOne() {
        var full = modifier("[\"PerMagFullness\"]");
        var empty = modifier("[\"PerMagEmptiness\"]");
        assertEquals("PerMagFullness", full.accumulators().getFirst().multiplyPer().getFirst().key());
        assertEquals("PerMagEmptiness", empty.accumulators().getFirst().multiplyPer().getFirst().key());
        assertEquals(0, StatResolver.resolve("test", 0, List.of(ModifierSource.of("test", List.of(full)))));
        assertEquals(4, StatResolver.resolve("test", 0, List.of(ModifierSource.of("test", List.of(empty)))));
    }

    @Test void nonstackableLevelAndStackInputsRemainOne() {
        assertEquals(4, StatResolver.resolve("test", 0, List.of(ModifierSource.of("test", List.of(modifier("[\"PerLevel\",\"PerStacks\"]"))))));
    }
}
