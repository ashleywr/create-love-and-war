package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ActionDefTest {
    @Test void delayRetainsOriginalFloatValueBeforeSchedulerTruncation() {
        for (float value : new float[]{2.9F, 0.4F, -0.4F, 0F}) {
            var action = ActionDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"actionType\":\"PlaySound\",\"delay\":" + value + "}")).getOrThrow();
            assertEquals(value, (float)action.delay());
        }
    }
}
