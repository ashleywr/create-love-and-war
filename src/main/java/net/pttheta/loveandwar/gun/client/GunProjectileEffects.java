package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GunProjectileEffects {
    private GunProjectileEffects() {}

    public static Matrix4f trailRoot(float yaw, float pitch, float age) {
        // Original Transform.fromPosAndEuler -> quatFromEuler negates all supplied Euler angles.
        return new Matrix4f().rotateYXZ((float)Math.toRadians(yaw - 90), (float)Math.toRadians(-pitch), (float)Math.toRadians(-age));
    }

    public static BakedModel model(GunProjectile bullet) {
        var id = bullet.bulletId();
        if (id == null) return null;
        var item = BuiltInRegistries.ITEM.get(id);
        var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        return model instanceof AnimatedTurboRigModel animated ? animated.baseRig() : model;
    }

    public static void trails(GunProjectile bullet) {
        var def = bullet.definition();
        if (def == null || def.particles().particleTrails().isEmpty()) return;
        var model = model(bullet);
        for (var trail : def.particles().particleTrails()) {
            if ((bullet.isInWater() && !trail.spawnUnderwater()) || (!bullet.isInWater() && trail.onlySpawnUnderwater())
                    || bullet.lifeTime() < trail.fromTick() || bullet.lifeTime() >= trail.toTick()) continue;
            var id = ResourceLocation.tryParse(trail.particle());
            if (id == null || !(BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof ParticleOptions particle)) continue;
            for (String point : trail.spawnPoints()) {
                Matrix4f transform = trailRoot(bullet.getYRot(), bullet.getXRot(), bullet.lifeTime());
                if (model instanceof TurboRigBakedModel rig) {
                    var attach = rig.attachPointMatrix(point);
                    if (attach != null) transform.mul(attach);
                }
                var position = transform.transformPosition(new Vector3f());
                var velocity = transform.transformDirection(new Vector3f(1, 0, 0)).mul(trail.speed());
                bullet.level().addParticle(particle, bullet.getX() + position.x, bullet.getY() + position.y, bullet.getZ() + position.z,
                        velocity.x, velocity.y, velocity.z);
            }
        }
    }
}
