package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

/** Server-owned equivalent of GunItem.HandleLock; target IDs are never accepted from a client. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunTargeting {
    private static final Map<ServerPlayer, Lock> LOCKS = new WeakHashMap<>();
    private static final List<Class<? extends Entity>> FLYING_TARGETS = List.of(FlyingMob.class, EnderDragon.class, WitherBoss.class, Blaze.class);
    private static final List<Class<? extends Entity>> MONSTER_TARGETS = List.of(Monster.class, Raider.class);
    private static final List<Class<? extends Entity>> OTHER_TARGETS = List.of(Entity.class);
    private static final class Lock {
        ItemStack stack;
        int candidate = -1, confirmed = -1, ticks;
    }
    private GunTargeting() {}

    public static boolean hasLockOn(String type) {
        return switch (type) {
            case "lock_on", "lock_on_predictive", "lock_on_top", "beam_and_lock", "beam_and_lock_top" -> true;
            default -> false;
        };
    }

    public static void tick(ServerPlayer player, ItemStack gun) {
        var rounds = gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
        var bullet = rounds.items().stream().map(GunLookup::bulletDef).flatMap(java.util.Optional::stream).findFirst().orElse(null);
        if (bullet == null || bullet.projectiles().isEmpty()) { LOCKS.remove(player); return; }
        var def = bullet.projectiles().getFirst().motion();
        var lock = LOCKS.computeIfAbsent(player, key -> new Lock());
        if (lock.stack != gun) { lock.stack = gun; lock.candidate = -1; lock.confirmed = -1; lock.ticks = 0; }
        Entity target = player.level().getEntity(lock.candidate);
        if (target != null && target.isRemoved()) target = null;
        if (target == null && hasLockOn(def.guidanceType())) {
            float range = def.lockRange();
            var candidates = player.level().getEntities(player, new AABB(player.getX() - range, player.getY() - range, player.getZ() - range,
                    player.getX() + range, player.getY() + range, player.getZ() + range));
            float closest = 90000;
            // ProjectileDefinition.Targets expands each category before querying; strict distance preserves class ties.
            // Reuse one world query, traversing its order separately for every expanded class.
            for (String type : def.targetTypes()) for (var targetClass : targetClasses(type)) for (Entity entity : candidates) {
                if (!targetClass.isInstance(entity)) continue;
                float distance = player.distanceTo(entity);
                if (matches(type, entity) && distance < closest && ProjectileMotion.withinCone(player.getLookAngle(), entity.position().subtract(player.position()), def.lockCone())
                        && player.level().clip(new ClipContext(player.getEyePosition(), entity.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS) {
                    closest = distance; target = entity;
                }
            }
        } else if (target != null && !ProjectileMotion.withinCone(player.getLookAngle(), target.position().subtract(player.position()), def.lockCone())) target = null;
        lock.candidate = target == null ? -1 : target.getId();
        if (target == null) { lock.ticks = 0; lock.confirmed = -1; }
        // Quirk kept: lockTime is ticks, strict greater-than, with no LOS recheck for a retained lock.
        else if (++lock.ticks > def.lockTime()) lock.confirmed = target.getId();
    }

    private static List<Class<? extends Entity>> targetClasses(String type) {
        return switch (type) {
            case "flyingMob" -> FLYING_TARGETS;
            case "monster" -> MONSTER_TARGETS;
            default -> OTHER_TARGETS;
        };
    }

    private static boolean matches(String type, Entity entity) {
        return switch (type) {
            case "flyingMob" -> entity instanceof FlyingMob || entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Blaze;
            case "monster" -> entity instanceof Monster || entity instanceof Raider;
            case "player" -> entity instanceof Player;
            case "living" -> entity instanceof LivingEntity;
            // The replacement does not supply Flans' VehicleEntity subsystem (no authored vehicle targets).
            case "vehicle" -> false;
            default -> {
                var id = ResourceLocation.tryParse(type);
                yield id != null && entity.getType().builtInRegistryHolder().key().location().equals(id);
            }
        };
    }

    public static Entity target(ServerPlayer player, ItemStack stack) {
        var lock = LOCKS.get(player);
        return lock != null && lock.stack == stack ? player.level().getEntity(lock.confirmed) : null;
    }

    @SubscribeEvent public static void prune(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var lock = LOCKS.get(player);
            if (lock != null && (player.getMainHandItem() != lock.stack || !player.isAlive() || player.isSpectator())) LOCKS.remove(player);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { LOCKS.remove(event.getEntity()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { LOCKS.clear(); }
}
