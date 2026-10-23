package net.pttheta.loveandwar.gun.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileTrailTransformTest {
    @Test void originalTransformNegatesAllEulerAngles() {
        var roll = GunProjectileEffects.trailRoot(90, 0, 90).transformDirection(new Vector3f(1, 0, 0));
        assertEquals(0, roll.x, 0.00001); assertEquals(-1, roll.y, 0.00001); assertEquals(0, roll.z, 0.00001);
        var yaw = GunProjectileEffects.trailRoot(0, 0, 0).transformDirection(new Vector3f(1, 0, 0));
        assertEquals(0, yaw.x, 0.00001); assertEquals(1, yaw.z, 0.00001);
        var pitch = GunProjectileEffects.trailRoot(90, 90, 0).transformDirection(new Vector3f(0, 1, 0));
        assertEquals(0, pitch.y, 0.00001); assertEquals(-1, pitch.z, 0.00001);
    }
}
