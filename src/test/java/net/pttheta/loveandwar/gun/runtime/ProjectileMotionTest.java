package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.gun.content.BulletDef;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileMotionTest {
    private static BulletDef.ProjectileDef.Motion motion(String json) {
        return BulletDef.ProjectileDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow().motion();
    }
    @Test void accelerationCanOvershootOriginalSpeedThreshold() {
        var def = motion("{\"acceleration\":20,\"maxSpeed\":80,\"dragInAir\":0,\"gravityFactor\":0}");
        var speed = ProjectileMotion.step(new Vec3(0, 0, 3.05), def, false, false, false);
        assertEquals(4.05, speed.length(), 0.000001);
        assertEquals(speed, ProjectileMotion.step(speed, def, false, false, false));
    }
    @Test void dragRunsAfterAccelerationAndBeforeGravity() {
        var def = motion("{\"acceleration\":20,\"maxSpeed\":80,\"dragInAir\":0.5,\"dragInWater\":0.75}");
        assertEquals(new Vec3(0, -0.02, 1), ProjectileMotion.step(new Vec3(0, 0, 1), def, false, false, false));
        assertEquals(new Vec3(0, -0.02, 0.5), ProjectileMotion.step(new Vec3(0, 0, 1), def, true, false, false));
    }
    @Test void originalDragClampAndNoGravityFlagsAreKept() {
        var def = motion("{\"dragInAir\":2,\"dragInWater\":-1}");
        assertEquals(Vec3.ZERO, ProjectileMotion.step(new Vec3(0, 0, 1), def, false, true, false));
        assertEquals(new Vec3(0, 0, 1), ProjectileMotion.step(new Vec3(0, 0, 1), def, true, true, false));
        assertEquals(Vec3.ZERO, ProjectileMotion.step(new Vec3(0, 0, 1), def, false, false, true));
    }
    @Test void guidanceRotatesByTheAuthoredRadiansPerTick() {
        var velocity = ProjectileMotion.rotateTo(new Vec3(0, 0, 2), new Vec3(1, 0, 1), 0.1F);
        assertEquals(2 * Math.sin(0.1), velocity.x, 0.000001);
        assertEquals(2 * Math.cos(0.1), velocity.z, 0.000001);
        assertEquals(2, velocity.length(), 0.000001);
        assertEquals(Vec3.ZERO, ProjectileMotion.rotateTo(Vec3.ZERO, new Vec3(1, 0, 0), 0.1F));
    }
    @Test void originalTrackingConeUsesStrictAngularBoundary() {
        assertFalse(ProjectileMotion.withinCone(new Vec3(0, 0, 1), new Vec3(1, 0, 0), 90));
        assertTrue(ProjectileMotion.withinCone(new Vec3(0, 0, 1), new Vec3(1, 0, 0), 91));
        assertFalse(ProjectileMotion.withinCone(Vec3.ZERO, new Vec3(1, 0, 0), 91));
    }
}
