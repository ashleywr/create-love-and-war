package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileDefTest {
    @Test void omittedProjectileFieldsRetainOriginalDefaults() {
        var def = BulletDef.ProjectileDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        assertEquals(3, def.motion().launchSpeed());
        assertEquals(1, def.motion().gravityFactor());
        assertEquals(0.01F, def.motion().dragInAir());
        assertEquals(0.2F, def.motion().dragInWater());
        assertEquals(0.1F, def.motion().turnRate());
        assertEquals(0.25F, def.motion().timeBetweenTrailParticles());
        assertEquals("Bounce", def.behavior().responseToBlock());
    }

    @Test void authoredAccelerationAndGuidanceFieldsSurviveCodecRoundTrip() {
        var input = JsonParser.parseString("""
            {"launchSpeed":1,"acceleration":20,"maxSpeed":80,"lockRange":64,"lockCone":10,
             "trackCone":90,"lockTime":10,"topAttackHeight":30,"topAttackRange":20,
             "targetTypes":["flyingMob","monster","living"]}
            """);
        var def = BulletDef.ProjectileDef.CODEC.parse(JsonOps.INSTANCE, input).getOrThrow();
        var encoded = BulletDef.ProjectileDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow().getAsJsonObject();
        for (var field : input.getAsJsonObject().entrySet()) assertEquals(field.getValue(), encoded.get(field.getKey()), field.getKey());
    }

    @Test void trailDefaultsAndUnderwaterFlagsMatchOriginal() {
        var defaults = BulletDef.ParticleTrailDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        assertEquals(0, defaults.speed());
        assertEquals(Integer.MAX_VALUE, defaults.toTick());
        var input = JsonParser.parseString("{\"spawnUnderwater\":true,\"onlySpawnUnderwater\":true}");
        var trail = BulletDef.ParticleTrailDef.CODEC.parse(JsonOps.INSTANCE, input).getOrThrow();
        var encoded = BulletDef.ParticleTrailDef.CODEC.encodeStart(JsonOps.INSTANCE, trail).getOrThrow().getAsJsonObject();
        assertEquals(input.getAsJsonObject().get("spawnUnderwater"), encoded.get("spawnUnderwater"));
        assertEquals(input.getAsJsonObject().get("onlySpawnUnderwater"), encoded.get("onlySpawnUnderwater"));
    }
}
