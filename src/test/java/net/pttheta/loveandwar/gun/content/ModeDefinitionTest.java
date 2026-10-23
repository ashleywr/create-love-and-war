package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModeDefinitionTest {
    @Test void explicitModeDefaultSurvivesCodecRoundTrip() {
        var entry = GunDef.ModeEntry.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"key\":\"m\",\"values\":[\"a\"],\"defaultValue\":\"ready\"}")).getOrThrow();
        var json = GunDef.ModeEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry).getOrThrow().getAsJsonObject();
        assertTrue(json.has("defaultValue"), "Mode default is data, independent of first cycle value");
        assertEquals("ready", json.get("defaultValue").getAsString());
    }
    @Test void omittedModeFieldsUseOriginalDefinitionDefaults() {
        var entry = assertDoesNotThrow(() -> GunDef.ModeEntry.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        assertEquals("mode", entry.key());
        assertEquals(java.util.List.of("on", "off"), entry.values());
    }
}
