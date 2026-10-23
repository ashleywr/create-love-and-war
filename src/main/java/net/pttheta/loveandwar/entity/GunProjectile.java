package net.pttheta.loveandwar.entity;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.gun.client.GunProjectileEffects;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.BulletDef.ProjectileDef;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierContext;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.runtime.GunBallistics;
import net.pttheta.loveandwar.gun.runtime.ProjectileMotion;

/** Gun-only projectile; the existing ShotProjectile remains Robotdog's entity. */
public final class GunProjectile extends Projectile {
    private static final EntityDataAccessor<CompoundTag> CONTEXT = SynchedEntityData.defineId(GunProjectile.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> LOCK = SynchedEntityData.defineId(GunProjectile.class, EntityDataSerializers.INT);
    private static final Codec<List<Modifier>> MODIFIERS = Modifier.CODEC.listOf();
    private CompoundTag cachedContext;
    private ProjectileDef definition;
    private ResourceLocation bulletId;
    private List<ModifierSource> sources = List.of();
    private int fuseRemaining;
    private int lifeTime;
    private boolean stuck;
    private boolean detonated;
    private java.util.UUID lockTargetUuid;

    public GunProjectile(EntityType<? extends GunProjectile> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTEXT, new CompoundTag());
        builder.define(LOCK, -1);
    }

    public void initialize(ServerPlayer owner, ResourceLocation id, ProjectileDef authored, List<ModifierSource> modifiers,
                           String group, Entity target, int progressTicks) {
        setOwner(owner);
        var context = ModifierContext.ofGroups(List.of(group));
        var motion = authored.motion();
        var resolvedMotion = new ProjectileDef.Motion(motion.guidanceType(),
                stat("launch_speed", motion.launchSpeed(), modifiers, context), stat("gravity_factor", motion.gravityFactor(), modifiers, context),
                stat("turn_rate", motion.turnRate(), modifiers, context), stat("drag_in_air", motion.dragInAir(), modifiers, context),
                stat("drag_in_water", motion.dragInWater(), modifiers, context), stat("time_between_particles", motion.timeBetweenTrailParticles(), modifiers, context),
                stat("acceleration", motion.acceleration(), modifiers, context), stat("max_speed", motion.maxSpeed(), modifiers, context),
                stat("lock_range", motion.lockRange(), modifiers, context), stat("lock_cone", motion.lockCone(), modifiers, context),
                stat("track_cone", motion.trackCone(), modifiers, context), motion.lockTime(), motion.topAttackHeight(), motion.topAttackRange(), motion.targetTypes());
        var behavior = authored.behavior();
        var resolvedBehavior = new ProjectileDef.Behavior(behavior.sticky(), stat("fuse_time", behavior.fuseTime(), modifiers, context),
                StatResolver.resolveOverride("response_to_block", behavior.responseToBlock(), modifiers, context),
                StatResolver.resolveOverride("response_to_entity", behavior.responseToEntity(), modifiers, context),
                StatResolver.resolveOverride("response_to_vehicle", behavior.responseToVehicle(), modifiers, context));
        var core = authored.core();
        var resolved = new ProjectileDef(new ProjectileDef.Core(core.shotCount(), stat("splash_radius", core.splashRadius(), modifiers, context),
                core.impacts(), core.penetrationPower()), resolvedMotion, authored.particles(), resolvedBehavior);
        var payload = new CompoundTag();
        payload.putString("Bullet", id.toString());
        payload.put("Definition", ProjectileDef.CODEC.encodeStart(NbtOps.INSTANCE, resolved).getOrThrow());
        // Impact effects need the firing context after save/reload; no global entity-ID lookup table.
        var flat = modifiers.stream().flatMap(source -> source.modifiers().stream()).toList();
        payload.put("Modifiers", MODIFIERS.encodeStart(NbtOps.INSTANCE, flat).getOrThrow());
        payload.putInt("AttachmentCount", ModifierContext.attachmentCount(modifiers));
        entityData.set(CONTEXT, payload);
        entityData.set(LOCK, target == null ? -1 : target.getId());
        lockTargetUuid = target == null ? null : target.getUUID();
        refreshContext();
        fuseRemaining = resolvedBehavior.fuseTime() > 0 ? Mth.ceil(resolvedBehavior.fuseTime() * 20) - Math.max(0, progressTicks) : 0;
    }

    private static float stat(String name, float base, List<ModifierSource> sources, ModifierContext context) {
        return StatResolver.resolve(name, base, sources, context);
    }

    private void refreshContext() {
        var payload = entityData.get(CONTEXT);
        if (payload == cachedContext) return;
        cachedContext = payload;
        if (!payload.contains("Definition")) { bulletId = null; definition = null; sources = List.of(); return; }
        bulletId = ResourceLocation.tryParse(payload.getString("Bullet"));
        definition = ProjectileDef.CODEC.parse(NbtOps.INSTANCE, payload.getCompound("Definition")).result().orElse(null);
        var modifiers = payload.contains("Modifiers") ? MODIFIERS.parse(NbtOps.INSTANCE, payload.get("Modifiers")).result().orElse(List.of()) : List.<Modifier>of();
        sources = List.of(ModifierSource.of("projectile", modifiers), ModifierSource.gunInputs(payload.getInt("AttachmentCount")));
    }

    public ProjectileDef definition() { refreshContext(); return definition; }
    public ResourceLocation bulletId() { refreshContext(); return bulletId; }
    public int lifeTime() { return lifeTime; }
    public int fuseRemaining() { return fuseRemaining; }
    public boolean stuck() { return stuck; }
    public int lockId() { return entityData.get(LOCK); }

    public void launch(Vec3 direction) {
        double speed = definition().motion().launchSpeed();
        // Quirk kept from ShootAction.Calculate -> ServerSpawnBullet: speed is applied twice.
        Vec3 trajectory = direction.scale(speed);
        setDeltaMovement(trajectory.scale(speed / 20));
        setOldPosAndRot();
        lookAt(EntityAnchorArgument.Anchor.FEET, trajectory);
    }

    @Override public void tick() {
        super.tick();
        var def = definition();
        if (def == null || bulletId == null) { if (!level().isClientSide) discard(); return; }
        Vec3 motion = ProjectileMotion.step(getDeltaMovement(), def.motion(), isInWater(), isNoGravity(), stuck);
        setDeltaMovement(motion);
        motion = impact(motion);
        if (isRemoved()) return;
        motion = guide(motion, def.motion());
        move(MoverType.SELF, motion);
        setDeltaMovement(motion);
        float blend = Math.min(def.motion().turnRate() * 20, 1);
        setXRot(Mth.rotLerp(blend, getXRot(), (float)Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance()))));
        setYRot(Mth.rotLerp(blend, getYRot(), (float)Math.toDegrees(Math.atan2(motion.x, motion.z))));
        if (!level().isClientSide && def.behavior().fuseTime() > 0 && --fuseRemaining <= 0) {
            detonate(BlockHitResult.miss(position(), Direction.DOWN, blockPosition()));
        }
        if (level().isClientSide) GunProjectileEffects.trails(this);
        lifeTime++;
    }

    private Vec3 impact(Vec3 motion) {
        // Original BulletEntity accepts all entity candidates rather than vanilla's owner exclusion.
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, entity -> true);
        String response = switch (hit.getType()) {
            case MISS -> "PassThrough";
            case BLOCK -> definition.behavior().responseToBlock();
            case ENTITY -> definition.behavior().responseToEntity();
        };
        return switch (response) {
            case "Stick" -> { stuck = true; yield Vec3.ZERO; }
            case "Detonate" -> { detonate(hit); yield Vec3.ZERO; }
            case "Bounce" -> {
                // Entity-bounce definitions crashed in the original; safely pass through those malformed responses.
                if (!(hit instanceof BlockHitResult block)) yield motion;
                yield switch (block.getDirection().getAxis()) {
                    case X -> new Vec3(-motion.x, motion.y, motion.z);
                    case Y -> new Vec3(motion.x, -motion.y, motion.z);
                    case Z -> new Vec3(motion.x, motion.y, -motion.z);
                };
            }
            default -> motion;
        };
    }

    private Vec3 guide(Vec3 motion, ProjectileDef.Motion def) {
        Entity target = level().getEntity(lockId());
        if (target == null && lockTargetUuid != null && level() instanceof net.minecraft.server.level.ServerLevel server) {
            target = server.getEntity(lockTargetUuid);
            if (target != null) entityData.set(LOCK, target.getId());
        }
        if (target != null && target.isRemoved()) target = null;
        Entity shooter = getOwner();
        String type = def.guidanceType();
        Vec3 goal;
        if (type.equals("lock_on") || type.equals("lock_on_predictive")) {
            if (target == null || !ProjectileMotion.withinCone(getDeltaMovement(), target.position().subtract(position()), def.trackCone())) return motion;
            goal = target.position();
            if (type.equals("lock_on_predictive") && motion.length() > 0) goal = goal.add(target.getDeltaMovement().scale(distanceTo(target) / (float)motion.length()));
        } else if (type.equals("lock_on_top") || ((type.equals("beam_and_lock") || type.equals("beam_and_lock_top")) && target != null)) {
            if (target == null || shooter == null) return motion;
            goal = target.position();
            if (type.endsWith("top")) goal = topAttack(goal, shooter, def, 3);
            else {
                if (!ProjectileMotion.withinCone(getDeltaMovement(), goal.subtract(position()), def.trackCone())) return motion;
                if (motion.length() > 0) goal = goal.add(target.getDeltaMovement().scale(distanceTo(target) / (float)motion.length()));
            }
        } else if (type.startsWith("beam_") && shooter != null) {
            if (type.endsWith("top")) {
                Vec3 start = shooter.getEyePosition();
                var hit = level().clip(new ClipContext(start, start.add(shooter.getLookAngle().scale(128)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()));
                goal = topAttack(hit.getLocation(), shooter, def, def.topAttackHeight());
            } else goal = shooter.getEyePosition().add(shooter.getLookAngle().scale(shooter.getEyePosition().distanceTo(position()) + 2));
        } else return motion;
        return ProjectileMotion.rotateTo(motion, goal.subtract(position()), def.turnRate());
    }

    private Vec3 topAttack(Vec3 goal, Entity shooter, ProjectileDef.Motion def, float forward) {
        double dx = goal.x - getX(), dz = goal.z - getZ();
        if (Math.sqrt(dx * dx + dz * dz) <= def.topAttackRange()) return goal;
        Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(forward);
        return new Vec3(getX() + horizontal.x, Math.max(shooter.getY() + def.topAttackHeight(), goal.y + def.topAttackHeight()), getZ() + horizontal.z);
    }

    public void detonate(HitResult hit) {
        if (!detonated) {
            detonated = true;
            if (level() instanceof net.minecraft.server.level.ServerLevel projectileLevel && getOwner() instanceof ServerPlayer player) {
                GunBallistics.applyImpacts(projectileLevel, player, definition().core().impacts(), hit, definition.core().splashRadius(), sources);
            }
        }
        if (!level().isClientSide) discard();
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("GunContext", entityData.get(CONTEXT).copy());
        tag.putInt("Fuse", fuseRemaining); tag.putInt("LifeTime", lifeTime);
        tag.putBoolean("Stuck", stuck); tag.putBoolean("Detonated", detonated);
        if (lockTargetUuid != null) tag.putUUID("LockTarget", lockTargetUuid);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CONTEXT, tag.getCompound("GunContext").copy());
        refreshContext();
        fuseRemaining = tag.getInt("Fuse"); lifeTime = tag.getInt("LifeTime");
        stuck = tag.getBoolean("Stuck"); detonated = tag.getBoolean("Detonated");
        lockTargetUuid = tag.hasUUID("LockTarget") ? tag.getUUID("LockTarget") : null;
        if (lockTargetUuid != null && level() instanceof net.minecraft.server.level.ServerLevel server) {
            Entity target = server.getEntity(lockTargetUuid);
            entityData.set(LOCK, target == null ? -1 : target.getId());
        }
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 64 * getViewScale() * 6;
        return distance < range * range;
    }
    @Override public boolean isPushable() { return false; }
    @Override public void setRemainingFireTicks(int ticks) {}
}
