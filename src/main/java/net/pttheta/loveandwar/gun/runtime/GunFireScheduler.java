package net.pttheta.loveandwar.gun.runtime;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

/** Server owns retriggers; clients send input edges rather than supplying individual shots. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunFireScheduler {
    private static final Map<ServerPlayer, IdentityHashMap<ItemStack, Cycle>> CYCLES = new WeakHashMap<>();

    private static final class Cycle {
        final GunCadence cadence = new GunCadence();
        long lastTick;
        long seenTick;
        boolean held;
        GunDef gun;
        ActionGroup group;
        GunAttachments attachments;
        int retriggers;
        int progress;
        int triggers;
        int duration;

        Cycle(long now) { this.lastTick = now; this.seenTick = now; }

        void advance(long now) {
            int elapsed = (int)Math.min(Integer.MAX_VALUE, Math.max(0, now - this.lastTick));
            this.cadence.tick(elapsed);
            this.lastTick = now;
        }
    }

    private GunFireScheduler() {}

    private static Cycle cycle(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.computeIfAbsent(player, ignored -> new IdentityHashMap<>());
        return cycles.computeIfAbsent(stack, ignored -> {
            // Runtime fire controls are deliberately not persisted, like the original ActionStack.
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            stack.set(ModDataComponents.GUN_ACTION_STATE, state.withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            return new Cycle(player.level().getGameTime());
        });
    }

    static boolean press(ServerPlayer player, ItemStack stack) {
        var cycle = cycle(player, stack);
        boolean fresh = !cycle.held;
        cycle.held = true;
        return fresh;
    }

    static void release(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle == null) return;
        cycle.held = false;
        if (cycle.group != null && GunFireMode.of(cycle.group.repeatMode()) == GunFireMode.FULL_AUTO) finish(stack, cycle);
    }

    static void releaseAll(ServerPlayer player) {
        var cycles = CYCLES.get(player);
        if (cycles == null) return;
        for (var entry : cycles.entrySet()) {
            release(player, entry.getKey());
            var state = entry.getKey().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            entry.getKey().set(ModDataComponents.GUN_ACTION_STATE, state.released());
        }
    }

    static void start(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments) {
        var cycle = cycle(player, stack);
        cycle.advance(player.level().getGameTime());
        if (cycle.group == null || !cycle.group.key().equals(group.key())) {
            cycle.progress = 0;
            cycle.triggers = 0;
        }
        cycle.gun = gun;
        cycle.group = group;
        cycle.attachments = attachments;
        cycle.duration = durationPerTrigger(group);
        emit(player, stack, cycle, cycle.cadence.trigger(group.repeatDelay()));
        cycle.retriggers = group.repeatDelay() > 0 && GunFireMode.of(group.repeatMode()) == GunFireMode.BURST_FIRE
                ? Math.max(0, group.repeatCount() - 1) : 0;
        synchronize(stack, cycle, player.level().getGameTime());
    }

    static void cancel(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle != null) {
            cycle.group = null;
            cycle.retriggers = 0;
        }
    }

    /** True when this scheduler owns the active group's progress for this inventory tick. */
    static boolean tick(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle == null) {
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            if (!state.activeGroup().isEmpty() && GunLookup.gunDef(stack).stream().flatMap(def -> def.actionGroups().stream())
                    .anyMatch(group -> group.key().equals(state.activeGroup()) && group.actions().stream().anyMatch(action -> "Shoot".equals(action.actionType())))) {
                stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            } else if (state.activeGroup().isEmpty() && (state.waitingForRelease() || state.burstRemaining() != 0 || state.cooldownUntilGameTime() != 0)) {
                stack.set(ModDataComponents.GUN_ACTION_STATE, state.withFiringFrame("", 0, 0, 0, 0, false));
            }
            return false;
        }
        long now = player.level().getGameTime();
        cycle.seenTick = now;
        if (!player.isAlive() || player.isSpectator()) {
            if (cycle.group != null) finish(stack, cycle);
            cycle.held = false;
            return false;
        }
        if (now <= cycle.lastTick) return cycle.group != null;
        // A dropped/unloaded stack must not resume a stale burst when picked up later.
        if (now - cycle.lastTick > 1) {
            if (cycle.group != null) finish(stack, cycle);
            cycle.held = false;
        }
        cycle.advance(now);
        if (cycle.group == null) {
            if (cycle.held) GunEngine.tryHeldFire(player, stack);
            return cycle.group != null;
        }

        cycle.progress++;
        GunFireMode mode = GunFireMode.of(cycle.group.repeatMode());
        boolean repeat = mode == GunFireMode.FULL_AUTO && cycle.held;
        if (mode == GunFireMode.BURST_FIRE && cycle.retriggers > 0) {
            // Original CheckRetrigger counts checks, not successful shots, even while out of ammo.
            cycle.retriggers--;
            repeat = true;
        }
        if (repeat && !stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()) {
            emit(player, stack, cycle, cycle.cadence.trigger(cycle.group.repeatDelay() <= 0 ? 0.05F : cycle.group.repeatDelay()));
        }
        if ((mode == GunFireMode.BURST_FIRE || mode == GunFireMode.SEMI_AUTO)
                && cycle.progress > (long)cycle.duration * cycle.triggers) {
            finish(stack, cycle);
        } else synchronize(stack, cycle, now);
        return true;
    }

    static int progress(ServerPlayer player, ItemStack stack, String group) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle != null && cycle.group != null && cycle.group.key().equals(group)) return cycle.progress;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        return state.activeGroup().equals(group) ? state.groupTicksElapsed() : 0;
    }

    private static void emit(ServerPlayer player, ItemStack stack, Cycle cycle, int count) {
        for (int i = 0; i < count; i++) {
            cycle.triggers++;
            GunEngine.runImmediateActions(player, stack, cycle.gun, cycle.group, cycle.attachments);
        }
    }

    private static void synchronize(ItemStack stack, Cycle cycle, long now) {
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        long total = (long)cycle.duration * cycle.triggers + 1;
        stack.set(ModDataComponents.GUN_ACTION_STATE, state.withFiringFrame(cycle.group.key(), cycle.progress,
                (int)Math.min(Integer.MAX_VALUE, total), now + cycle.cadence.ticksUntilReady(), cycle.retriggers,
                cycle.held && GunFireMode.of(cycle.group.repeatMode()) != GunFireMode.FULL_AUTO));
    }

    private static void finish(ItemStack stack, Cycle cycle) {
        cycle.group = null;
        cycle.retriggers = 0;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0));
    }

    private static int durationPerTrigger(ActionGroup group) {
        float seconds = group.repeatDelay();
        for (var action : group.actions()) seconds = Math.max(seconds, action.duration());
        return Math.max(0, (int)Math.floor(seconds * 20));
    }

    @SubscribeEvent
    public static void prune(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var cycles = CYCLES.get(player);
        if (cycles == null) return;
        long now = player.level().getGameTime();
        cycles.entrySet().removeIf(entry -> entry.getKey().isEmpty() || entry.getValue().seenTick < now - 1
                || entry.getValue().group == null && !entry.getValue().held && entry.getValue().cadence.idle());
        if (cycles.isEmpty()) CYCLES.remove(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CYCLES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { CYCLES.clear(); }
}
