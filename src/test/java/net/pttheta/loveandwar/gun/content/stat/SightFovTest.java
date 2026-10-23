package net.pttheta.loveandwar.gun.content.stat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SightFovTest {
    private static ActionGroup group(String actions, float factor) {
        return ActionGroup.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"key\":\"arbitrary\",\"actions\":" + actions
                + ",\"modifiers\":[{\"stat\":\"fov_factor\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":" + factor + "}]}]}")).getOrThrow();
    }
    private static SightFov add(SightFov fov, ActionGroup group) {
        return fov.add(group, List.of(ModifierSource.of("group", group.modifiers())));
    }
    @Test void zeroBaseAndModifiersTakePrecedenceOverConflictingRawActionValue() {
        var fov = add(SightFov.EMPTY, group("[{\"actionType\":\"AimDownSights\",\"fovFactor\":99}]", 1.25F));
        assertEquals(1.25F, fov.mean());
        assertEquals(0.8F, fov.apply(1, 1));
    }
    @Test void everySightActionContributesAcrossGroupsAndHands() {
        var fov = add(SightFov.EMPTY, group("[{\"actionType\":\"AimDownSights\"},{\"actionType\":\"Scope\"},{\"actionType\":\"Animation\"}]", 2));
        fov = add(fov, group("[{\"actionType\":\"Scope\"}]", 6));
        assertEquals(3, fov.count()); assertEquals(10.0F / 3, fov.mean());
        assertEquals(0.33F, fov.apply(1.1F, 1), 0.00001F);
    }
    @Test void weakFactorsAndCustomStrengthDoNotAddTheirOwnBlendOrClamp() {
        var wide = add(SightFov.EMPTY, group("[{\"actionType\":\"AimDownSights\"}]", 0.5F));
        assertEquals(2, wide.apply(1, 1)); assertEquals(1, wide.apply(1, 0));
        assertEquals(4.0F / 3, wide.apply(1, 0.5F), 0.00001F);
        var strong = add(SightFov.EMPTY, group("[{\"actionType\":\"Scope\"}]", 100));
        assertEquals(0.01F, strong.apply(1, 1));
        assertEquals(100_000_000F, new SightFov(0.00000001F, 1).apply(1, 1));
        assertEquals(1.1F, SightFov.EMPTY.apply(1.1F, 1));
    }
    @Test void malformedDefinitionsCannotProduceInvalidProjection() {
        for (float value : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(1.1F, new SightFov(value, 1).apply(1.1F, 1));
        }
        assertEquals(Float.MAX_VALUE, new SightFov(Float.MIN_VALUE, 1).apply(Float.MAX_VALUE, 1));
    }
}
