package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.network.CGunAuthored;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Validate owner-authored callback phases; retain world-clock firing for server-origin groups. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunFireScheduler {
    private static final Map<ServerPlayer, IdentityHashMap<ItemStack, GunState>> CYCLES = new WeakHashMap<>();

    private static final class GunState {
        final GunCadence cadence = new GunCadence();
        final Map<String, Cycle> groups = new LinkedHashMap<>();
        Cycle first;
        long lastTick, seenTick;
        boolean held, clientControlled;

        GunState(long now) { lastTick = seenTick = now; }

        void advance(long now) {
            cadence.tick((int)Math.min(Integer.MAX_VALUE, Math.max(0, now - lastTick)));
            lastTick = now;
        }
    }

    private static final class Cycle {
        final GunState state;
        final GunCadence cadence;
        long lastTick;
        GunDef gun;
        ActionGroup group;
        GunAttachments attachments;
        GunActiveGroups.Origin origin;
        int retriggers;
        int progress;
        int triggers;
        int duration;
        boolean finishing;
        GunAuthoredFire.Remote remote;

        Cycle(GunState state, long now) { this.state = state; this.cadence = state.cadence; this.lastTick = now; }

        void advance(long now) {
            state.advance(now);
            this.lastTick = now;
        }
    }

    private GunFireScheduler() {}

    private static GunState cycle(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.computeIfAbsent(player, ignored -> new IdentityHashMap<>());
        return cycles.computeIfAbsent(stack, ignored -> {
            // Runtime fire controls are deliberately not persisted, like the original ActionStack.
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            stack.set(ModDataComponents.GUN_ACTION_STATE, state.withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            return new GunState(player.level().getGameTime());
        });
    }

    static boolean press(ServerPlayer player, ItemStack stack) {
        var cycle = cycle(player, stack);
        boolean fresh = !cycle.held;
        cycle.held = true;
        cycle.clientControlled |= GunPresentation.controlledInput();
        return fresh;
    }

    static void release(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var state = cycles == null ? null : cycles.get(stack);
        if (state == null) return;
        state.held = false;
        for (var cycle : new ArrayList<>(state.groups.values())) {
            if (cycle.remote != null) {
                if (!cycle.remote.release(
                        GunPresentation.authored().present()
                                ? GunPresentation.authored().tick()
                                : cycle.remote.gate.tick)) cancel(player, stack, cycle);
            } else if (cycle.group != null
                    && GunFireMode.of(cycle.group.repeatMode()) == GunFireMode.FULL_AUTO) {
                GunActionTimeline.finishAfterTick(player, stack, cycle.group.key());
                finish(stack, cycle);
            }
        }
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
        start(player, stack, gun, group, attachments, GunActiveGroups.Origin.base(stack));
    }

    static void start(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments, GunActiveGroups.Origin origin) {
        var retained = GunActionTimeline.context(player, stack, group.key());
        if (retained.isPresent()) {
            gun = retained.get().gun(); group = retained.get().group(); origin = retained.get().origin();
        }
        var state = cycle(player, stack);
        var cycle = state.groups.get(group.key());
        if (cycle == null) cycle = new Cycle(state, player.level().getGameTime());
        var authored = GunPresentation.authored();
        if (authored.present() && GunPresentation.claim().present()) {
            // Aggregate owner claims/shared phase validation are a separate transport unit.
            // Preserve the previous single authored-cycle contract until that schema is available.
            for (var other : state.groups.values()) if (other != cycle && other.remote != null) return;
            if (cycle.remote != null
                    && !cycle.remote.activation.equals(GunPresentation.claim().activation()))
                return;
            var remote =
                    cycle.remote == null
                            ? new GunAuthoredFire.Remote(GunPresentation.claim(), group)
                            : cycle.remote;
            if (!remote.start(player, stack, authored)) {
                if (cycle.remote != null) cancel(player, stack, cycle);
                return;
            }
            cycle.gun = gun;
            cycle.group = group;
            cycle.attachments = attachments;
            cycle.origin = origin;
            cycle.duration = durationPerTrigger(group);
            cycle.remote = remote;
            retain(cycle);
            cycle.progress = authored.tick();
            cycle.lastTick = player.level().getGameTime();
            state.seenTick = cycle.lastTick;
            GunActionTimeline.beginAuthored(player, stack, gun, group, origin);
            GunActionTimeline.advanceAuthored(player, stack, group.key(), authored.tick());
            for (int i = 0; i < authored.count(); i++)
                GunEngine.triggerActions(player, stack, gun, group, attachments, origin);
            cycle.triggers = (int) Math.min(Integer.MAX_VALUE, remote.gate.triggers);
            synchronize(stack, cycle, player.level().getGameTime());
            return;
        }
        if (cycle.remote != null) return;
        cycle.advance(player.level().getGameTime());
        if (cycle.group == null || !cycle.group.key().equals(group.key())) {
            cycle.progress = 0;
            cycle.triggers = 0;
            cycle.finishing = false;
            cycle.remote = null;
        }
        cycle.gun = gun;
        cycle.group = group;
        cycle.attachments = attachments;
        cycle.origin = origin;
        cycle.duration = durationPerTrigger(group);
        retain(cycle);
        // Source OnStart creates the instance even when another group spent all shared cadence.
        GunActionTimeline.beginAuthored(player, stack, gun, group, origin);
        emit(player, stack, cycle, cycle.cadence.trigger(group.repeatDelay()));
        cycle.retriggers = group.repeatDelay() > 0 && GunFireMode.of(group.repeatMode()) == GunFireMode.BURST_FIRE
                ? Math.max(0, group.repeatCount() - 1) : 0;
        synchronize(stack, cycle, player.level().getGameTime());
    }

    static void cancel(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var state = cycles == null ? null : cycles.get(stack);
        if (state != null) for (var cycle : new ArrayList<>(state.groups.values())) cancel(player, stack, cycle);
    }

    private static void cancel(ServerPlayer player, ItemStack stack, Cycle cycle) {
        if (cycle.group != null) GunActionTimeline.cancel(player, stack, cycle.group.key());
        finish(stack, cycle);
    }

    static void finishModeGroup(ServerPlayer player, ItemStack stack, String key) {
        var cycles = CYCLES.get(player);
        var state = cycles == null ? null : cycles.get(stack);
        var cycle = state == null ? null : state.groups.get(key);
        // Retain the final pending feedback tick, without adding a client-only final prediction as server damage.
        if (cycle != null && cycle.group != null && cycle.group.key().equals(key)) cycle.finishing = true;
    }

    /** True when this scheduler owns the active group's progress for this inventory tick. */
    static boolean tick(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var runtime = cycles == null ? null : cycles.get(stack);
        if (runtime == null) {
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            if (!state.activeGroup().isEmpty() && GunLookup.gunDef(stack).stream().flatMap(def -> def.actionGroups().stream())
                    .anyMatch(group -> group.key().equals(state.activeGroup()) && group.actions().stream().anyMatch(action -> "Shoot".equals(action.actionType())))) {
                stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            } else if (state.activeGroup().isEmpty() && (state.waitingForRelease() || state.burstRemaining() != 0 || state.cooldownUntilGameTime() != 0)) {
                stack.set(
                        ModDataComponents.GUN_ACTION_STATE,
                        state.withFiringFrame("", 0, 0, 0, 0, false));
            }
            return false;
        }
        long now = player.level().getGameTime();
        runtime.seenTick = now;
        if (!player.isAlive() || player.isSpectator()) {
            for (var cycle : new ArrayList<>(runtime.groups.values())) finish(stack, cycle);
            runtime.held = false;
            return false;
        }
        // Pinned ActionStack.TickActions visits the most recently activated group first.
        boolean owned = !runtime.groups.isEmpty();
        if (runtime.groups.size() == 1) tick(player, stack, runtime.first, now);
        else if (!runtime.groups.isEmpty()) {
            var ordered = new ArrayList<>(runtime.groups.values());
            for (int i = ordered.size() - 1; i >= 0; i--) tick(player, stack, ordered.get(i), now);
        }
        runtime.advance(now);
        if (runtime.groups.isEmpty() && runtime.held && !runtime.clientControlled) GunEngine.tryHeldFire(player, stack);
        return owned || !runtime.groups.isEmpty();
    }

    static void tickGroup(ServerPlayer player, ItemStack stack, String key, long now) {
        var states = CYCLES.get(player);
        var state = states == null ? null : states.get(stack);
        var cycle = state == null ? null : state.groups.get(key);
        if (cycle != null) tick(player, stack, cycle, now);
    }

    private static void tick(ServerPlayer player, ItemStack stack, Cycle cycle, long now) {
        if (cycle.group == null) return;
        if (cycle.remote != null) {
            cycle.lastTick = now;
            if (cycle.group != null && !GunActionTimeline.owns(player, stack, cycle.group.key()))
                finish(stack, cycle);
            return;
        }
        if (now <= cycle.lastTick) return;
        // A dropped/unloaded stack must not resume a stale burst when picked up later.
        if (now - cycle.lastTick > 1) {
            if (cycle.group != null) finish(stack, cycle);
            cycle.state.held = false;
        }
        cycle.advance(now);
        if (cycle.group == null) return;

        cycle.progress++;
        GunFireMode mode = GunFireMode.of(cycle.group.repeatMode());
        boolean repeat = !cycle.finishing && mode == GunFireMode.FULL_AUTO && cycle.state.held;
        if (!cycle.finishing && mode == GunFireMode.BURST_FIRE && cycle.retriggers > 0) {
            // Original CheckRetrigger counts checks, not successful shots, even while out of ammo.
            cycle.retriggers--;
            repeat = true;
        }
        if (repeat && !stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()) {
            emit(player, stack, cycle, cycle.cadence.trigger(cycle.group.repeatDelay() <= 0 ? 0.05F : cycle.group.repeatDelay()));
        }
        if (cycle.finishing || (mode == GunFireMode.BURST_FIRE || mode == GunFireMode.SEMI_AUTO)
                && cycle.progress > (long)cycle.duration * cycle.triggers) {
            finish(stack, cycle);
        } else synchronize(stack, cycle, now);
    }

    static int progress(ServerPlayer player, ItemStack stack, String group) {
        var cycles = CYCLES.get(player);
        var runtime = cycles == null ? null : cycles.get(stack);
        var cycle = runtime == null ? null : runtime.groups.get(group);
        if (cycle != null && cycle.group != null) return cycle.progress;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        return state.activeGroup().equals(group) ? state.groupTicksElapsed() : 0;
    }

    static boolean owns(ServerPlayer player, ItemStack stack, String group) {
        var cycles = CYCLES.get(player);
        var state = cycles == null ? null : cycles.get(stack);
        var cycle = state == null ? null : state.groups.get(group);
        return cycle != null && cycle.group != null;
    }

    private static void emit(ServerPlayer player, ItemStack stack, Cycle cycle, int count) {
        for (int i = 0; i < count; i++) {
            if (cycle.group == null) break;
            if (!GunAuthoredFire.spend(player, stack, cycle.group.repeatDelay(), 1)) break;
            cycle.triggers++;
            GunEngine.triggerActions(player, stack, cycle.gun, cycle.group, cycle.attachments, cycle.origin);
        }
    }

    private static void synchronize(ItemStack stack, Cycle cycle, long now) {
        if (cycle.group == null) return;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        long total = (long)cycle.duration * cycle.triggers + 1;
        stack.set(ModDataComponents.GUN_ACTION_STATE, state.withFiringFrame(cycle.group.key(), cycle.progress,
                (int)Math.min(Integer.MAX_VALUE, total), now + cycle.cadence.ticksUntilReady(), cycle.retriggers,
                cycle.state.held && GunFireMode.of(cycle.group.repeatMode()) != GunFireMode.FULL_AUTO));
    }

    private static void retain(Cycle cycle) {
        var state = cycle.state;
        state.groups.put(cycle.group.key(), cycle);
        if (state.first == null) state.first = cycle;
    }

    private static void finish(ItemStack stack, Cycle cycle) {
        String key = cycle.group == null ? "" : cycle.group.key();
        cycle.state.groups.remove(key, cycle);
        if (cycle.state.first == cycle)
            cycle.state.first = cycle.state.groups.isEmpty() ? null : cycle.state.groups.values().iterator().next();
        cycle.group = null;
        cycle.retriggers = 0;
        cycle.finishing = false;
        cycle.remote = null;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        if (state.activeGroup().equals(key)) stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0));
    }

    public static void authored(ServerPlayer player, ItemStack stack, CGunAuthored packet) {
        var cycles = CYCLES.get(player);
        var state = cycles == null ? null : cycles.get(stack);
        var cycle = state == null ? null : state.groups.values().stream()
                .filter(value -> value.remote != null && value.remote.activation.equals(packet.activation())).findFirst().orElse(null);
        if (cycle == null
                || cycle.remote == null
                || cycle.group == null
                || !cycle.remote.activation.equals(packet.activation())) return;
        if (!player.isAlive() || player.isSpectator()) {
            cancel(player, stack, cycle);
            return;
        }
        var remote = cycle.remote;
        var gate = remote.gate;
        if (packet.tick() <= gate.tick
                && (packet.kind() == CGunAuthored.Kind.TRIGGER && packet.ordinal() < gate.triggers
                        || packet.kind() == CGunAuthored.Kind.STEP
                                && packet.ordinal() <= gate.triggers))
            return; // Idempotent old delivery; never replay gameplay.
        boolean valid = remote.time(packet.tick());
        if (valid)
            switch (packet.kind()) {
                case TRIGGER ->
                        valid =
                                gate.trigger(packet.tick(), packet.ordinal())
                                        && GunAuthoredFire.spend(
                                                player,
                                                stack,
                                                gate.delay <= 0 ? .05F : gate.delay,
                                                1);
                case STEP -> valid = gate.step(packet.tick(), packet.ordinal());
                case FINISH -> valid = gate.finish(packet.tick(), packet.ordinal());
                case CANCEL -> valid = gate.step(packet.tick(), packet.ordinal());
            }
        if (!valid) {
            cancel(player, stack, cycle);
            return;
        }
        GunActionTimeline.advanceAuthored(player, stack, cycle.group.key(), packet.tick());
        if (cycle.group == null) return;
        if (packet.kind() == CGunAuthored.Kind.TRIGGER)
            GunEngine.triggerActions(
                    player,
                    stack,
                    cycle.gun,
                    cycle.group,
                    stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY),
                    cycle.origin);
        if (cycle.group == null) return;
        cycle.progress = packet.tick();
        cycle.triggers = (int) Math.min(Integer.MAX_VALUE, gate.triggers);
        if (packet.kind() == CGunAuthored.Kind.FINISH
                || packet.kind() == CGunAuthored.Kind.CANCEL) {
            cancel(player, stack, cycle);
        } else synchronize(stack, cycle, player.level().getGameTime());
    }

    private static int durationPerTrigger(ActionGroup group) {
        return GunEngine.groupDurationTicks(group);
    }

    @SubscribeEvent
    public static void prune(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var cycles = CYCLES.get(player);
        if (cycles == null) return;
        long now = player.level().getGameTime();
        cycles.entrySet().removeIf(entry -> entry.getKey().isEmpty() || entry.getValue().seenTick < now - 1
                || entry.getValue().groups.isEmpty() && !entry.getValue().held && entry.getValue().cadence.idle());
        if (cycles.isEmpty()) CYCLES.remove(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CYCLES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { CYCLES.clear(); }
}
