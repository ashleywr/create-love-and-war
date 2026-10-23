package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundDefTest {
    @Test void omittedSoundFieldsRetainOriginalDefaults() {
        var sound = SoundDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"sound\":\"flansmod:gunshot_slam\"}")).getOrThrow();
        assertEquals(100, sound.maxRange(), 0.001, "Original SoundDefinition.maxRange defaults to 100");
        assertEquals(1, sound.length(), 0.001, "Original sound length defaults to one second");
    }

    @Test void lodSelectionKeepsOriginalThresholdOrderingAndTies() {
        var sound = new SoundDef("test:base", 1, 1, 1, 1, 1, 100, List.of(
                new SoundDef.Lod("test:far", 50), new SoundDef.Lod("test:near", 10),
                new SoundDef.Lod("test:tie", 50), new SoundDef.Lod("test:zero", 0)));
        assertEquals("test:base", sound.resolve(0, 0, 1, 1).sound());
        assertEquals("test:base", sound.resolve(9.99, 0, 1, 1).sound());
        assertEquals("test:near", sound.resolve(10, 0, 1, 1).sound());
        assertEquals("test:far", sound.resolve(50, 0, 1, 1).sound());
        assertEquals("test:far", sound.resolve(150, 0, 1, 1).sound());
    }

    @Test void cameraDistanceControlsVolumeAndRandomnessOnlyControlsPitch() {
        var sound = new SoundDef("test:base", 1, 0.8F, 1.2F, 0.2F, 1, 100, List.of());
        var playback = sound.resolve(50, 0.25F, 0.5F, 2);
        assertEquals(0.3F, playback.volume(), 0.00001);
        assertEquals(1.8F, playback.pitch(), 0.00001);
        assertEquals(playback.volume(), sound.resolve(50, 0.9F, 0.5F, 2).volume());
        // Preserve the original interpolation beyond maxRange rather than inventing a clamp.
        assertEquals(-0.2F, sound.resolve(150, 0, 1, 1).volume(), 0.00001);
    }

    @Test void omittedLodFieldsRetainOriginalDefaults() {
        var sound = SoundDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"LODs\":[{}]}" )).getOrThrow();
        assertEquals("flansmod:null", sound.sound());
        assertEquals(List.of(new SoundDef.Lod("flansmod:null", 100)), sound.lods());
    }
}
