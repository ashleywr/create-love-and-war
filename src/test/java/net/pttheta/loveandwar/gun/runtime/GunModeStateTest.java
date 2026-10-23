package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunModeStateTest {
    private static void assertInitialized(GunActionState state) {
        var json = GunActionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow().getAsJsonObject();
        assertTrue(json.has("modesInitialized") && json.get("modesInitialized").getAsBoolean(), "Explicit compound presence must survive state transforms and encoding");
    }
    @Test void emptyInitializedModeCompoundSurvivesEveryStateTransform() {
        var state = GunActionState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"modesInitialized\":true}")).getOrThrow();
        List<UnaryOperator<GunActionState>> transforms = List.of(s -> s, s -> s.withGroup("group", 10, List.of()),
                s -> s.withFiringFrame("fire", 2, 10, 20, 1, true), s -> s.withReloadGroup("load", 4, List.of()),
                s -> s.withReloadGroup("load", 4, List.of()).withReloadGroup("load", 4, List.of()),
                GunActionState::tickedGroup, GunActionState::idled, s -> s.withCooldown(10), s -> s.withToggle("aim", true),
                s -> s.withBurstRemaining(2), s -> s.withWaitingForRelease(true), GunActionState::released);
        for (var transform : transforms) assertInitialized(transform.apply(state));
    }
    @Test void legacyNonemptyModesInferCompoundPresence() {
        var state = GunActionState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"modeValues\":{\"other\":\"on\"}}")).getOrThrow();
        assertInitialized(state);
        assertEquals("on", state.modeValues().get("other"));
    }
}
