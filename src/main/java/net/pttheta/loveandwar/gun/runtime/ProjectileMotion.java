package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.gun.content.BulletDef.ProjectileDef.Motion;

/** Pinned Flans BulletEntity order: acceleration, drag, then gravity. */
public final class ProjectileMotion {
    private ProjectileMotion() {}

    public static Vec3 step(Vec3 velocity, Motion def, boolean water, boolean noGravity, boolean stuck) {
        if (stuck) velocity = Vec3.ZERO;
        double speed = velocity.length();
        if (def.acceleration() > 0 && speed < def.maxSpeed() / 20.0) {
            // Quirk kept: the original threshold permits one acceleration step to overshoot maxSpeed.
            velocity = velocity.normalize().scale(speed + def.acceleration() / 20.0F);
        }
        velocity = velocity.scale(Mth.clamp(1 - (water ? def.dragInWater() : def.dragInAir()), 0, 1));
        return noGravity || stuck ? velocity : velocity.add(0, -def.gravityFactor() * 0.02, 0);
    }

    public static boolean withinCone(Vec3 forward, Vec3 target, float degrees) {
        double length = forward.length() * target.length();
        return length > 0 && Math.toDegrees(Math.acos(Mth.clamp(forward.dot(target) / length, -1, 1))) < degrees;
    }

    public static Vec3 rotateTo(Vec3 velocity, Vec3 toward, float turnRate) {
        double speed = velocity.length();
        if (speed == 0 || toward.lengthSqr() == 0) return velocity;
        Vec3 current = velocity.normalize();
        Vec3 target = toward.normalize();
        double angle = Math.min((float)Math.acos(Mth.clamp(current.dot(target), -1, 1)), turnRate);
        Vec3 axis = current.cross(target).normalize();
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        return velocity.scale(cosine).add(axis.cross(velocity).scale(sine)).add(axis.scale(axis.dot(velocity) * (1 - cosine)));
    }
}
